package com.dayflow.app.feature.plan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayflow.app.core.time.TimeProvider
import com.dayflow.app.domain.model.DailyPlanItem
import com.dayflow.app.domain.model.PlanItemSourceType
import com.dayflow.app.domain.model.PlanStatus
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.planner.DeterministicDailyPlanner
import com.dayflow.app.domain.planner.PlanConflict
import com.dayflow.app.domain.planner.UnscheduledItem
import com.dayflow.app.domain.repository.DailyPlanRepository
import com.dayflow.app.domain.repository.TaskRepository
import com.dayflow.app.domain.usecase.ApproveDailyPlanUseCase
import com.dayflow.app.domain.usecase.GenerateDailyPlanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

data class PlanUiState(
    val date: LocalDate,
    val planStatus: PlanStatus = PlanStatus.DRAFT,
    val scheduledItems: List<DailyPlanItem> = emptyList(),
    val unscheduledItems: List<UnscheduledItem> = emptyList(),
    val conflicts: List<PlanConflict> = emptyList(),
    val totalScheduledMinutes: Int = 0,
    val availableMinutes: Int = 0,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class PlanViewModel @Inject constructor(
    private val generateDailyPlanUseCase: GenerateDailyPlanUseCase,
    private val approveDailyPlanUseCase: ApproveDailyPlanUseCase,
    private val dailyPlanRepository: DailyPlanRepository,
    private val taskRepository: TaskRepository,
    private val timeProvider: TimeProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val targetDate: LocalDate = savedStateHandle.get<String>("dateString")?.let {
        try {
            LocalDate.parse(it)
        } catch (_: Exception) {
            timeProvider.nowLocalDate()
        }
    } ?: timeProvider.nowLocalDate()

    private val _uiState = MutableStateFlow(
        PlanUiState(
            date = targetDate,
            isLoading = true
        )
    )
    val uiState: StateFlow<PlanUiState> = _uiState.asStateFlow()

    init {
        loadPlan(forceRegenerate = false)
    }

    fun loadPlan(forceRegenerate: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val existing = dailyPlanRepository.getPlanByDateDirect(targetDate)
                val result = generateDailyPlanUseCase(targetDate, forceRegenerate = forceRegenerate)

                _uiState.update {
                    it.copy(
                        planStatus = existing?.status ?: PlanStatus.DRAFT,
                        scheduledItems = result.scheduledItems,
                        unscheduledItems = result.unscheduledItems,
                        conflicts = result.conflicts,
                        totalScheduledMinutes = result.totalScheduledMinutes,
                        availableMinutes = result.availableMinutes,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to generate plan"
                    )
                }
            }
        }
    }

    fun moveItemUp(index: Int) {
        if (index <= 0) return
        val items = _uiState.value.scheduledItems.toMutableList()
        val itemA = items[index - 1]
        val itemB = items[index]

        // Swap positions and re-chain times
        val durationA = Duration.between(itemA.startTime, itemA.endTime).toMinutes()
        val durationB = Duration.between(itemB.startTime, itemB.endTime).toMinutes()

        val newStartB = itemA.startTime
        val newEndB = newStartB.plusMinutes(durationB)
        val newStartA = newEndB
        val newEndA = newStartA.plusMinutes(durationA)

        items[index - 1] = itemB.copy(startTime = newStartB, endTime = newEndB, orderIndex = index - 1)
        items[index] = itemA.copy(startTime = newStartA, endTime = newEndA, orderIndex = index)

        recalculateConflictsAndPersist(items)
    }

    fun moveItemDown(index: Int) {
        val items = _uiState.value.scheduledItems.toMutableList()
        if (index >= items.size - 1) return
        moveItemUp(index + 1)
    }

    fun updateItemTime(itemId: String, newStartTime: LocalTime, newEndTime: LocalTime) {
        if (!newEndTime.isAfter(newStartTime)) return
        val items = _uiState.value.scheduledItems.map { item ->
            if (item.id == itemId) {
                item.copy(startTime = newStartTime, endTime = newEndTime)
            } else {
                item
            }
        }.sortedBy { it.startTime }
            .mapIndexed { index, item -> item.copy(orderIndex = index) }

        recalculateConflictsAndPersist(items)
    }

    fun removeItemToUnscheduled(itemId: String) {
        val currentScheduled = _uiState.value.scheduledItems
        val targetItem = currentScheduled.find { it.id == itemId } ?: return
        val newScheduled = currentScheduled.filterNot { it.id == itemId }
            .mapIndexed { index, item -> item.copy(orderIndex = index) }

        viewModelScope.launch {
            val allTasks = taskRepository.getAllTasks().first()
            val task = allTasks.find { it.id == targetItem.sourceId }

            val newUnscheduled = _uiState.value.unscheduledItems.toMutableList()
            if (task != null && newUnscheduled.none { it.task.id == task.id }) {
                newUnscheduled.add(
                    UnscheduledItem(
                        task = task,
                        reason = "Manually removed from daily schedule",
                        reschedulingSuggestion = "Add back or reschedule to another day"
                    )
                )
            }

            val conflicts = DeterministicDailyPlanner.detectConflicts(
                date = targetDate,
                items = newScheduled,
                tasks = allTasks
            )

            _uiState.update {
                it.copy(
                    scheduledItems = newScheduled,
                    unscheduledItems = newUnscheduled,
                    conflicts = conflicts,
                    totalScheduledMinutes = newScheduled.sumOf { item ->
                        Duration.between(item.startTime, item.endTime).toMinutes().toInt()
                    }
                )
            }
        }
    }

    fun scheduleUnscheduledTask(task: Task) {
        viewModelScope.launch {
            val currentScheduled = _uiState.value.scheduledItems.toMutableList()
            val durationMinutes = task.estimatedDurationMinutes?.takeIf { it > 0 } ?: 60

            val startTime = if (currentScheduled.isNotEmpty()) {
                currentScheduled.last().endTime
            } else {
                LocalTime.of(9, 0)
            }
            val endTime = startTime.plusMinutes(durationMinutes.toLong())

            val newItem = DailyPlanItem(
                id = UUID.randomUUID().toString(),
                planId = "",
                sourceType = PlanItemSourceType.TASK,
                sourceId = task.id,
                title = task.title,
                startTime = startTime,
                endTime = endTime,
                orderIndex = currentScheduled.size
            )
            currentScheduled.add(newItem)

            val newUnscheduled = _uiState.value.unscheduledItems.filterNot { it.task.id == task.id }
            val allTasks = taskRepository.getAllTasks().first()
            val conflicts = DeterministicDailyPlanner.detectConflicts(
                date = targetDate,
                items = currentScheduled,
                tasks = allTasks
            )

            _uiState.update {
                it.copy(
                    scheduledItems = currentScheduled,
                    unscheduledItems = newUnscheduled,
                    conflicts = conflicts,
                    totalScheduledMinutes = currentScheduled.sumOf { item ->
                        Duration.between(item.startTime, item.endTime).toMinutes().toInt()
                    }
                )
            }
        }
    }

    fun toggleItemCompletion(itemId: String) {
        val items = _uiState.value.scheduledItems.map { item ->
            if (item.id == itemId) {
                item.copy(isCompleted = !item.isCompleted)
            } else {
                item
            }
        }
        _uiState.update { it.copy(scheduledItems = items) }
        viewModelScope.launch {
            val target = items.find { it.id == itemId }
            if (target != null) {
                dailyPlanRepository.updatePlanItem(target)
            }
        }
    }

    fun approvePlan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val approvedPlan = approveDailyPlanUseCase(targetDate, _uiState.value.scheduledItems)
                _uiState.update {
                    it.copy(
                        planStatus = PlanStatus.APPROVED,
                        scheduledItems = approvedPlan.items,
                        isSaving = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "Failed to approve plan"
                    )
                }
            }
        }
    }

    fun regeneratePlan() {
        loadPlan(forceRegenerate = true)
    }

    private fun recalculateConflictsAndPersist(items: List<DailyPlanItem>) {
        viewModelScope.launch {
            val allTasks = taskRepository.getAllTasks().first()
            val conflicts = DeterministicDailyPlanner.detectConflicts(
                date = targetDate,
                items = items,
                tasks = allTasks
            )
            val totalMinutes = items.sumOf {
                Duration.between(it.startTime, it.endTime).toMinutes().toInt()
            }
            _uiState.update {
                it.copy(
                    scheduledItems = items,
                    conflicts = conflicts,
                    totalScheduledMinutes = totalMinutes
                )
            }
        }
    }
}
