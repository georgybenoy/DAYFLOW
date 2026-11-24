package com.dayflow.app.feature.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayflow.app.domain.model.Category
import com.dayflow.app.domain.model.DeadlineReminderLeadTime
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskStatus
import com.dayflow.app.domain.repository.CategoryRepository
import com.dayflow.app.domain.repository.TaskRepository
import com.dayflow.app.domain.usecase.AddSubtaskUseCase
import com.dayflow.app.domain.usecase.CancelTaskUseCase
import com.dayflow.app.domain.usecase.ChangeTaskDeadlineUseCase
import com.dayflow.app.domain.usecase.CompleteTaskUseCase
import com.dayflow.app.domain.usecase.DeleteSubtaskUseCase
import com.dayflow.app.domain.usecase.DeleteTaskUseCase
import com.dayflow.app.domain.usecase.DeriveOverdue
import com.dayflow.app.domain.usecase.ReopenTaskUseCase
import com.dayflow.app.domain.usecase.RescheduleTaskPlannedTimeUseCase
import com.dayflow.app.domain.usecase.ToggleSubtaskUseCase
import com.dayflow.app.scheduling.DeadlineReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

data class TaskDetailUiState(
    val task: Task? = null,
    val category: Category? = null,
    val isOverdue: Boolean = false,
    val deadlineReminders: Set<DeadlineReminderLeadTime> = emptySet(),
    val isLoading: Boolean = false,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val reopenTaskUseCase: ReopenTaskUseCase,
    private val cancelTaskUseCase: CancelTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val toggleSubtaskUseCase: ToggleSubtaskUseCase,
    private val addSubtaskUseCase: AddSubtaskUseCase,
    private val deleteSubtaskUseCase: DeleteSubtaskUseCase,
    private val rescheduleTaskPlannedTimeUseCase: RescheduleTaskPlannedTimeUseCase,
    private val changeTaskDeadlineUseCase: ChangeTaskDeadlineUseCase,
    private val deadlineReminderScheduler: DeadlineReminderScheduler,
    private val deriveOverdue: DeriveOverdue
) : ViewModel() {

    val taskId: String = checkNotNull(savedStateHandle["taskId"])

    private val _isProcessing = MutableStateFlow(false)
    private val _isDeleted = MutableStateFlow(false)
    private val _deadlineReminders = MutableStateFlow<Set<DeadlineReminderLeadTime>>(emptySet())

    init {
        loadDeadlineReminders()
    }

    private fun loadDeadlineReminders() {
        viewModelScope.launch {
            val reminders = deadlineReminderScheduler.getActiveLeadTimes(taskId)
            _deadlineReminders.value = reminders
        }
    }

    val uiState: StateFlow<TaskDetailUiState> = combine(
        taskRepository.getTaskById(taskId),
        categoryRepository.getAllCategories(),
        _deadlineReminders,
        _isProcessing,
        _isDeleted
    ) { task, categories, reminders, isProcessing, isDeleted ->
        val category = task?.categoryId?.let { catId -> categories.firstOrNull { it.id == catId } }
        val overdue = if (task != null) deriveOverdue(task, LocalDateTime.now()) else false

        TaskDetailUiState(
            task = task,
            category = category,
            isOverdue = overdue,
            deadlineReminders = reminders,
            isLoading = isProcessing,
            isDeleted = isDeleted,
            errorMessage = null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = TaskDetailUiState(isLoading = true)
    )

    fun toggleTaskComplete() {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val currentTask = taskRepository.getTaskByIdDirect(taskId) ?: return@launch
                if (currentTask.status == TaskStatus.COMPLETED) {
                    reopenTaskUseCase(taskId, _deadlineReminders.value)
                } else {
                    completeTaskUseCase(taskId)
                }
                loadDeadlineReminders()
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun cancelTask() {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                cancelTaskUseCase(taskId)
                loadDeadlineReminders()
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun deleteTask() {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                deleteTaskUseCase(taskId)
                _isDeleted.value = true
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun toggleSubtask(subtaskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            toggleSubtaskUseCase(subtaskId, isCompleted)
        }
    }

    fun addSubtask(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            addSubtaskUseCase(taskId, title)
        }
    }

    fun deleteSubtask(subtaskId: String) {
        viewModelScope.launch {
            deleteSubtaskUseCase(subtaskId)
        }
    }

    fun reschedulePlannedTime(plannedDate: LocalDate?, plannedStart: LocalTime?, plannedEnd: LocalTime?) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                rescheduleTaskPlannedTimeUseCase(taskId, plannedDate, plannedStart, plannedEnd)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun changeDeadline(
        deadlineDate: LocalDate?,
        deadlineTime: LocalTime?,
        reminders: Set<DeadlineReminderLeadTime>
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                changeTaskDeadlineUseCase(taskId, deadlineDate, deadlineTime, reminders)
                _deadlineReminders.value = reminders
            } finally {
                _isProcessing.value = false
            }
        }
    }
}
