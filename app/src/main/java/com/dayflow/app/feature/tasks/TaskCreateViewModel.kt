package com.dayflow.app.feature.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayflow.app.domain.model.Category
import com.dayflow.app.domain.model.DeadlineReminderLeadTime
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskStatus
import com.dayflow.app.domain.model.TaskSubtask
import com.dayflow.app.domain.model.TaskValidationError
import com.dayflow.app.domain.model.TaskValidationResult
import com.dayflow.app.domain.model.TaskValidator
import com.dayflow.app.domain.repository.CategoryRepository
import com.dayflow.app.domain.repository.TaskRepository
import com.dayflow.app.domain.usecase.CreateTaskUseCase
import com.dayflow.app.domain.usecase.UpdateTaskUseCase
import com.dayflow.app.scheduling.DeadlineReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

data class TaskCreateUiState(
    val isEditMode: Boolean = false,
    val initialTaskId: String? = null,
    val title: String = "",
    val description: String = "",
    val categoryId: String? = null,
    val priority: Priority = Priority.MEDIUM,
    val deadlineDate: LocalDate? = null,
    val deadlineTime: LocalTime? = null,
    val estimatedDurationMinutes: Int? = null,
    val plannedDate: LocalDate? = null,
    val plannedStart: LocalTime? = null,
    val plannedEnd: LocalTime? = null,
    val shouldCarryForward: Boolean = true,
    val deadlineReminders: Set<DeadlineReminderLeadTime> = emptySet(),
    val subtasks: List<TaskSubtask> = emptyList(),
    val categories: List<Category> = emptyList(),
    val validationError: TaskValidationError? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class TaskCreateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository,
    private val createTaskUseCase: CreateTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val deadlineReminderScheduler: DeadlineReminderScheduler
) : ViewModel() {

    private val editTaskId: String? = savedStateHandle["taskId"]

    private val _uiState = MutableStateFlow(
        TaskCreateUiState(
            isEditMode = editTaskId != null,
            initialTaskId = editTaskId
        )
    )
    val uiState: StateFlow<TaskCreateUiState> = _uiState.asStateFlow()

    private var existingTask: Task? = null

    init {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
        if (!editTaskId.isNullOrBlank()) {
            loadExistingTask(editTaskId)
        }
    }

    private fun loadExistingTask(taskId: String) {
        viewModelScope.launch {
            val task = taskRepository.getTaskByIdDirect(taskId)
            if (task != null) {
                existingTask = task
                val subtasks = taskRepository.getSubtasksForTaskDirect(taskId)
                val reminders = deadlineReminderScheduler.getActiveLeadTimes(taskId)

                _uiState.update { current ->
                    current.copy(
                        title = task.title,
                        description = task.description ?: "",
                        categoryId = task.categoryId,
                        priority = task.priority,
                        deadlineDate = task.deadlineDate,
                        deadlineTime = task.deadlineTime,
                        estimatedDurationMinutes = task.estimatedDurationMinutes,
                        plannedDate = task.plannedDate,
                        plannedStart = task.plannedStart,
                        plannedEnd = task.plannedEnd,
                        shouldCarryForward = task.shouldCarryForward,
                        subtasks = subtasks,
                        deadlineReminders = reminders
                    )
                }
            }
        }
    }

    fun updateTitle(value: String) {
        _uiState.update { current ->
            current.copy(
                title = value,
                validationError = if (current.validationError == TaskValidationError.BlankTitle && value.isNotBlank()) null
                else current.validationError
            )
        }
    }

    fun updateDescription(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun updateCategory(categoryId: String?) {
        _uiState.update { current ->
            current.copy(categoryId = if (current.categoryId == categoryId) null else categoryId)
        }
    }

    fun updatePriority(priority: Priority) {
        _uiState.update { it.copy(priority = priority) }
    }

    fun updateDeadlineDate(date: LocalDate?) {
        _uiState.update { current ->
            if (date == null) {
                current.copy(deadlineDate = null, deadlineTime = null, deadlineReminders = emptySet())
            } else {
                current.copy(
                    deadlineDate = date,
                    deadlineReminders = if (current.deadlineReminders.isEmpty()) setOf(DeadlineReminderLeadTime.AT_DEADLINE)
                    else current.deadlineReminders
                )
            }
        }
    }

    fun updateDeadlineTime(time: LocalTime?) {
        _uiState.update { it.copy(deadlineTime = time) }
    }

    fun toggleDeadlineReminder(leadTime: DeadlineReminderLeadTime) {
        _uiState.update { current ->
            val updated = if (current.deadlineReminders.contains(leadTime)) {
                current.deadlineReminders - leadTime
            } else {
                current.deadlineReminders + leadTime
            }
            current.copy(deadlineReminders = updated)
        }
    }

    fun updateEstimatedDuration(minutes: Int?) {
        _uiState.update { it.copy(estimatedDurationMinutes = minutes) }
    }

    fun updatePlannedDate(date: LocalDate?) {
        _uiState.update { current ->
            if (date == null) {
                current.copy(plannedDate = null, plannedStart = null, plannedEnd = null)
            } else {
                current.copy(plannedDate = date)
            }
        }
    }

    fun updatePlannedStart(time: LocalTime?) {
        _uiState.update { it.copy(plannedStart = time) }
    }

    fun updatePlannedEnd(time: LocalTime?) {
        _uiState.update { it.copy(plannedEnd = time) }
    }

    fun updateCarryForward(value: Boolean) {
        _uiState.update { it.copy(shouldCarryForward = value) }
    }

    fun addSubtask(title: String) {
        if (title.isBlank()) return
        _uiState.update { current ->
            val newSubtask = TaskSubtask(
                id = UUID.randomUUID().toString(),
                taskId = editTaskId ?: "",
                title = title.trim(),
                isCompleted = false,
                orderIndex = current.subtasks.size,
                createdAt = Instant.now()
            )
            current.copy(subtasks = current.subtasks + newSubtask)
        }
    }

    fun removeSubtask(subtaskId: String) {
        _uiState.update { current ->
            val filtered = current.subtasks.filterNot { it.id == subtaskId }
            current.copy(subtasks = filtered.mapIndexed { index, sub -> sub.copy(orderIndex = index) })
        }
    }

    fun moveSubtaskUp(index: Int) {
        if (index <= 0) return
        _uiState.update { current ->
            val list = current.subtasks.toMutableList()
            val temp = list[index]
            list[index] = list[index - 1]
            list[index - 1] = temp
            current.copy(subtasks = list.mapIndexed { i, sub -> sub.copy(orderIndex = i) })
        }
    }

    fun moveSubtaskDown(index: Int) {
        _uiState.update { current ->
            if (index >= current.subtasks.size - 1) return@update current
            val list = current.subtasks.toMutableList()
            val temp = list[index]
            list[index] = list[index + 1]
            list[index + 1] = temp
            current.copy(subtasks = list.mapIndexed { i, sub -> sub.copy(orderIndex = i) })
        }
    }

    fun saveTask() {
        val state = _uiState.value
        val validation = TaskValidator.validate(
            title = state.title,
            plannedStart = state.plannedStart,
            plannedEnd = state.plannedEnd,
            estimatedDurationMinutes = state.estimatedDurationMinutes
        )

        if (validation is TaskValidationResult.Invalid) {
            _uiState.update { it.copy(validationError = validation.errors.first()) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val taskId = editTaskId ?: UUID.randomUUID().toString()
                val preparedSubtasks = state.subtasks.mapIndexed { index, sub ->
                    sub.copy(taskId = taskId, orderIndex = index)
                }

                val task = Task(
                    id = taskId,
                    title = state.title.trim(),
                    description = state.description.trim().ifBlank { null },
                    categoryId = state.categoryId,
                    priority = state.priority,
                    deadlineDate = state.deadlineDate,
                    deadlineTime = state.deadlineTime,
                    estimatedDurationMinutes = state.estimatedDurationMinutes,
                    plannedDate = state.plannedDate,
                    plannedStart = state.plannedStart,
                    plannedEnd = state.plannedEnd,
                    status = existingTask?.status ?: TaskStatus.PENDING,
                    shouldCarryForward = state.shouldCarryForward,
                    createdAt = existingTask?.createdAt ?: Instant.now(),
                    completedAt = existingTask?.completedAt,
                    updatedAt = Instant.now(),
                    subtasks = preparedSubtasks
                )

                if (editTaskId != null) {
                    updateTaskUseCase(task, preparedSubtasks, state.deadlineReminders)
                } else {
                    createTaskUseCase(task, preparedSubtasks, state.deadlineReminders)
                }

                _uiState.update { it.copy(saveSuccess = true) }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
