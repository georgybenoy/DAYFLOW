package com.dayflow.app.feature.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayflow.app.core.time.TimeProvider
import com.dayflow.app.domain.model.DailyReview
import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.HabitLog
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.ReviewDecisionType
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskReviewDecision
import com.dayflow.app.domain.model.TaskStatus
import com.dayflow.app.domain.recurrence.ActiveDays
import com.dayflow.app.domain.recurrence.HabitProgressCalculator
import com.dayflow.app.domain.repository.DailyReviewRepository
import com.dayflow.app.domain.repository.HabitRepository
import com.dayflow.app.domain.repository.TaskRepository
import com.dayflow.app.domain.usecase.CancelTaskUseCase
import com.dayflow.app.domain.usecase.CompleteTaskUseCase
import com.dayflow.app.domain.usecase.CreateTaskUseCase
import com.dayflow.app.domain.usecase.GenerateDailyPlanUseCase
import com.dayflow.app.domain.usecase.RescheduleTaskPlannedTimeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

data class HabitReviewItem(
    val habit: Habit,
    val log: HabitLog?,
    val isCompleted: Boolean,
    val progressFormatted: String,
    val goalFormatted: String
)

data class ReviewUiState(
    val date: LocalDate,
    val completedTasks: List<Task> = emptyList(),
    val unfinishedTasks: List<Task> = emptyList(),
    val decisions: Map<String, TaskReviewDecision> = emptyMap(),
    val habits: List<HabitReviewItem> = emptyList(),
    val completedHabitsCount: Int = 0,
    val totalScheduledHabitsCount: Int = 0,
    val overdueTasksCount: Int = 0,
    val notes: String = "",
    val newCommitments: List<Task> = emptyList(),
    val isExistingReviewSaved: Boolean = false,
    val existingReviewId: String? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val dailyReviewRepository: DailyReviewRepository,
    private val generateDailyPlanUseCase: GenerateDailyPlanUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val cancelTaskUseCase: CancelTaskUseCase,
    private val rescheduleTaskPlannedTimeUseCase: RescheduleTaskPlannedTimeUseCase,
    private val createTaskUseCase: CreateTaskUseCase,
    private val timeProvider: TimeProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialDate: LocalDate = savedStateHandle.get<String>("dateString")?.let {
        try {
            LocalDate.parse(it)
        } catch (_: Exception) {
            timeProvider.nowLocalDate()
        }
    } ?: timeProvider.nowLocalDate()

    private val _uiState = MutableStateFlow(ReviewUiState(date = initialDate))
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    init {
        loadReview(initialDate)
    }

    fun loadReview(date: LocalDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(date = date, isLoading = true, errorMessage = null) }
            try {
                val allTasks = taskRepository.getAllTasks().first()
                val habits = habitRepository.getActiveHabits().first()
                val habitLogs = habitRepository.getLogsForDate(date).first()
                val existingReview = dailyReviewRepository.getReviewByDateDirect(date)

                // 1. Completed tasks for date
                val zoneId = ZoneId.systemDefault()
                val completedTasks = allTasks.filter { task ->
                    task.status == TaskStatus.COMPLETED && (
                        (task.completedAt != null && task.completedAt.atZone(zoneId).toLocalDate() == date) ||
                        (task.plannedDate == date)
                    )
                }

                // 2. Unfinished tasks for date: pending and planned on/before date, or deadline on/before date
                val unfinishedTasks = allTasks.filter { task ->
                    task.status == TaskStatus.PENDING && (
                        task.plannedDate == date ||
                        (task.deadlineDate != null && task.deadlineDate <= date) ||
                        (task.plannedDate != null && task.plannedDate < date)
                    )
                }

                // 3. Habits scheduled for date
                val scheduledHabits = habits.filter { habit ->
                    habit.isEnabled && ActiveDays.isDayActive(habit.activeDaysMask, date.dayOfWeek)
                }
                val logMap = habitLogs.associateBy { it.habitId }
                val habitItems = scheduledHabits.map { habit ->
                    val log = logMap[habit.id]
                    val progress = HabitProgressCalculator.calculateProgress(habit, log)
                    val goalUnit = if (!habit.unit.isNullOrBlank()) " ${habit.unit}" else ""
                    HabitReviewItem(
                        habit = habit,
                        log = log,
                        isCompleted = progress.isCompleted,
                        progressFormatted = progress.formattedProgress,
                        goalFormatted = "${habit.targetValue}$goalUnit"
                    )
                }

                // 4. Overdue count
                val overdueCount = allTasks.count { task ->
                    task.status == TaskStatus.PENDING && task.deadlineDate != null && task.deadlineDate < date
                }

                // 5. Invariant: Undecided unfinished tasks default to CARRIED_FORWARD and are shown as such.
                val existingDecisionMap = existingReview?.decisions?.associateBy { it.taskId } ?: emptyMap()
                val initialDecisions = unfinishedTasks.associate { task ->
                    task.id to (existingDecisionMap[task.id] ?: TaskReviewDecision(
                        taskId = task.id,
                        taskTitle = task.title,
                        decision = ReviewDecisionType.CARRIED_FORWARD
                    ))
                }

                _uiState.update { current ->
                    current.copy(
                        date = date,
                        completedTasks = completedTasks,
                        unfinishedTasks = unfinishedTasks,
                        decisions = initialDecisions,
                        habits = habitItems,
                        completedHabitsCount = habitItems.count { it.isCompleted },
                        totalScheduledHabitsCount = habitItems.size,
                        overdueTasksCount = overdueCount,
                        notes = existingReview?.notes ?: current.notes,
                        isExistingReviewSaved = existingReview != null,
                        existingReviewId = existingReview?.id,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun setTaskDecision(
        taskId: String,
        decision: ReviewDecisionType,
        rescheduledDate: LocalDate? = null
    ) {
        val task = _uiState.value.unfinishedTasks.find { it.id == taskId } ?: return
        val updatedDecision = TaskReviewDecision(
            taskId = taskId,
            taskTitle = task.title,
            decision = decision,
            rescheduledToDate = rescheduledDate?.toString()
        )
        _uiState.update { current ->
            current.copy(decisions = current.decisions + (taskId to updatedDecision))
        }
    }

    fun updateNotes(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun addInlineCommitment(
        title: String,
        priority: Priority = Priority.HIGH,
        estimatedDurationMinutes: Int = 30
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val tomorrow = _uiState.value.date.plusDays(1)
            val newTask = Task(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                priority = priority,
                plannedDate = tomorrow,
                estimatedDurationMinutes = estimatedDurationMinutes
            )
            createTaskUseCase(newTask)
            _uiState.update { current ->
                current.copy(newCommitments = current.newCommitments + newTask)
            }
        }
    }

    fun finishReview(onFinished: (tomorrowDate: LocalDate) -> Unit) {
        val state = _uiState.value
        if (state.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val nowInstant = timeProvider.nowInstant()
                val tomorrow = state.date.plusDays(1)

                // 1. Apply decisions to unfinished tasks
                state.decisions.forEach { (taskId, decision) ->
                    when (decision.decision) {
                        ReviewDecisionType.COMPLETED -> {
                            completeTaskUseCase(taskId, completedAt = nowInstant)
                        }
                        ReviewDecisionType.CANCELLED -> {
                            cancelTaskUseCase(taskId, now = nowInstant)
                        }
                        ReviewDecisionType.RESCHEDULED -> {
                            val targetRescheduledDate = decision.rescheduledToDate?.let {
                                try {
                                    LocalDate.parse(it)
                                } catch (_: Exception) {
                                    tomorrow
                                }
                            } ?: tomorrow
                            rescheduleTaskPlannedTimeUseCase(
                                taskId = taskId,
                                plannedDate = targetRescheduledDate,
                                now = nowInstant
                            )
                        }
                        ReviewDecisionType.CARRIED_FORWARD -> {
                            // Task remains pending. Decision is persisted in DailyReview,
                            // which adds +350 carry-forward boost during planner execution.
                        }
                        ReviewDecisionType.EXCLUDED -> {
                            // Task remains pending, but decision is persisted in DailyReview,
                            // which excludes it from tomorrow's auto-generated draft plan.
                        }
                    }
                }

                // 2. Persist DailyReview (upsert: unique per localDate)
                val reviewId = state.existingReviewId ?: UUID.randomUUID().toString()
                val review = DailyReview(
                    id = reviewId,
                    localDate = state.date,
                    notes = state.notes.ifBlank { null },
                    reviewedAt = nowInstant,
                    tasksCompletedCount = state.completedTasks.size + state.decisions.values.count { it.decision == ReviewDecisionType.COMPLETED },
                    tasksUnfinishedCount = state.unfinishedTasks.size - state.decisions.values.count {
                        it.decision == ReviewDecisionType.COMPLETED || it.decision == ReviewDecisionType.CANCELLED
                    },
                    habitsCompletedCount = state.habits.count { it.isCompleted },
                    decisions = state.decisions.values.toList()
                )
                dailyReviewRepository.saveReview(review)

                // 3. Immediately trigger generation of tomorrow's draft plan using the review decisions!
                generateDailyPlanUseCase(date = tomorrow, forceRegenerate = true)

                _uiState.update { it.copy(isSaving = false, isExistingReviewSaved = true, existingReviewId = reviewId) }
                onFinished(tomorrow)
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message) }
            }
        }
    }
}