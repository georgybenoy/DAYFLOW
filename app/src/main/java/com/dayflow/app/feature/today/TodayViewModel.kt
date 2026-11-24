package com.dayflow.app.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayflow.app.core.time.TimeProvider
import com.dayflow.app.domain.briefing.BriefingContent
import com.dayflow.app.domain.briefing.BriefingGenerator
import com.dayflow.app.domain.model.Category
import com.dayflow.app.domain.model.DailyPlan
import com.dayflow.app.domain.model.DailyPlanItem
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskStatus
import com.dayflow.app.domain.recurrence.ActiveDays
import com.dayflow.app.domain.recurrence.HabitProgressCalculator
import com.dayflow.app.domain.repository.CategoryRepository
import com.dayflow.app.domain.repository.DailyPlanRepository
import com.dayflow.app.domain.repository.HabitRepository
import com.dayflow.app.domain.repository.SettingsRepository
import com.dayflow.app.domain.repository.TaskRepository
import com.dayflow.app.domain.usecase.CompleteReminderOccurrenceUseCase
import com.dayflow.app.domain.usecase.CompleteTaskUseCase
import com.dayflow.app.domain.usecase.DeriveOverdue
import com.dayflow.app.domain.usecase.GetUpcomingRemindersUseCase
import com.dayflow.app.domain.usecase.ReopenTaskUseCase
import com.dayflow.app.domain.usecase.UpcomingReminderItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

data class TodayUiState(
    val userName: String = "",
    val selectedDate: LocalDate,
    val currentDate: LocalDate,
    val priorityTasks: List<Task> = emptyList(),
    val upcomingDeadlines: List<Task> = emptyList(),
    val categories: List<Category> = emptyList(),
    val overdueTaskIds: Set<String> = emptySet(),
    val upcomingReminders: List<UpcomingReminderItem> = emptyList(),
    val dailyPlan: DailyPlan? = null,
    val briefing: BriefingContent? = null,
    val tasksCompletedCount: Int = 0,
    val totalTasksCount: Int = 0,
    val habitsCompletedCount: Int = 0,
    val totalHabitsCount: Int = 0,
    val isLoading: Boolean = false
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider,
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository,
    private val habitRepository: HabitRepository,
    private val dailyPlanRepository: DailyPlanRepository,
    private val getUpcomingRemindersUseCase: GetUpcomingRemindersUseCase,
    private val completeReminderOccurrenceUseCase: CompleteReminderOccurrenceUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val reopenTaskUseCase: ReopenTaskUseCase,
    private val deriveOverdue: DeriveOverdue
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(timeProvider.nowLocalDate())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val uiState: StateFlow<TodayUiState> = _selectedDate
        .flatMapLatest { selected ->
            val habitsAndPlanFlow = combine(
                habitRepository.getActiveHabits(),
                habitRepository.getLogsForDate(selected),
                dailyPlanRepository.getPlanByDate(selected)
            ) { activeHabits, habitLogs, plan ->
                Triple(activeHabits, habitLogs, plan)
            }

            combine(
                settingsRepository.userProfile,
                getUpcomingRemindersUseCase(),
                taskRepository.getAllTasks(),
                categoryRepository.getAllCategories(),
                habitsAndPlanFlow
            ) { profile, upcoming, allTasks, categories, (activeHabits, habitLogs, plan) ->
                val now = LocalDateTime.now()
                val overdueIds = allTasks.filter { deriveOverdue(it, now) }.map { it.id }.toSet()

                val tasksForSelectedDate = allTasks.filter { task ->
                    task.plannedDate == selected || task.deadlineDate == selected
                }

                val completedCount = tasksForSelectedDate.count { it.status == TaskStatus.COMPLETED }
                val totalCount = tasksForSelectedDate.size

                val priorityTasks = tasksForSelectedDate
                    .filter { it.status == TaskStatus.PENDING }
                    .sortedWith(
                        compareBy<Task> { it.priority }
                            .thenBy { it.deadlineTime ?: LocalTime.MAX }
                    )

                val upcomingDeadlines = allTasks.filter { task ->
                    task.status == TaskStatus.PENDING &&
                        task.deadlineDate != null &&
                        (task.deadlineDate == selected ||
                            (task.deadlineDate.isAfter(selected) && task.deadlineDate.isBefore(selected.plusDays(4))))
                }.sortedWith(
                    compareBy<Task> { it.deadlineDate }
                        .thenBy { it.deadlineTime ?: LocalTime.MAX }
                )

                // Habits for selected date
                val scheduledHabits = activeHabits.filter { habit ->
                    ActiveDays.isDayActive(habit.activeDaysMask, selected.dayOfWeek)
                }
                val logsByHabitId = habitLogs.associateBy { it.habitId }
                val habitsCompleted = scheduledHabits.count { habit ->
                    val log = logsByHabitId[habit.id]
                    HabitProgressCalculator.calculateProgress(habit, log).isCompleted
                }
                val totalHabits = scheduledHabits.size

                val briefing = BriefingGenerator.generate(
                    userName = profile.name,
                    date = selected,
                    tasks = allTasks,
                    scheduledPlanItems = plan?.items ?: emptyList(),
                    habits = activeHabits,
                    habitLogs = habitLogs,
                    reminders = upcoming.map { it.reminder }
                )

                TodayUiState(
                    userName = profile.name,
                    selectedDate = selected,
                    currentDate = timeProvider.nowLocalDate(),
                    priorityTasks = priorityTasks.take(5),
                    upcomingDeadlines = upcomingDeadlines.take(5),
                    categories = categories,
                    overdueTaskIds = overdueIds,
                    upcomingReminders = upcoming.take(5),
                    dailyPlan = plan,
                    briefing = briefing,
                    tasksCompletedCount = completedCount,
                    totalTasksCount = totalCount,
                    habitsCompletedCount = habitsCompleted,
                    totalHabitsCount = totalHabits,
                    isLoading = false
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = TodayUiState(
                selectedDate = timeProvider.nowLocalDate(),
                currentDate = timeProvider.nowLocalDate()
            )
        )

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun completeReminder(occurrenceId: String, reminderId: String) {
        viewModelScope.launch {
            completeReminderOccurrenceUseCase(occurrenceId, reminderId)
        }
    }

    fun toggleTaskComplete(task: Task) {
        viewModelScope.launch {
            if (task.status == TaskStatus.COMPLETED) {
                reopenTaskUseCase(task.id)
            } else {
                completeTaskUseCase(task.id)
            }
        }
    }

    fun togglePlanItemComplete(item: DailyPlanItem) {
        viewModelScope.launch {
            dailyPlanRepository.updatePlanItem(item.copy(isCompleted = !item.isCompleted))
        }
    }
}
