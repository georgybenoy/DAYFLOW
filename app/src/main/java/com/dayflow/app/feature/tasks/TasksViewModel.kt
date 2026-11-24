package com.dayflow.app.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayflow.app.domain.model.Category
import com.dayflow.app.domain.model.DeadlineReminderLeadTime
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskStatus
import com.dayflow.app.domain.repository.CategoryRepository
import com.dayflow.app.domain.repository.TaskRepository
import com.dayflow.app.domain.usecase.ChangeTaskDeadlineUseCase
import com.dayflow.app.domain.usecase.CompleteTaskUseCase
import com.dayflow.app.domain.usecase.DeleteTaskUseCase
import com.dayflow.app.domain.usecase.DeriveOverdue
import com.dayflow.app.domain.usecase.ReopenTaskUseCase
import com.dayflow.app.domain.usecase.RescheduleTaskPlannedTimeUseCase
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

enum class TaskFilterTab(val title: String) {
    TODAY("Today"),
    UPCOMING("Upcoming"),
    OVERDUE("Overdue"),
    COMPLETED("Completed"),
    ALL("All")
}

data class TasksUiState(
    val selectedTab: TaskFilterTab = TaskFilterTab.TODAY,
    val selectedCategoryId: String? = null,
    val selectedPriority: Priority? = null,
    val tasks: List<Task> = emptyList(),
    val categories: List<Category> = emptyList(),
    val overdueTaskIds: Set<String> = emptySet(),
    val activeTaskCounts: Map<TaskFilterTab, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val reopenTaskUseCase: ReopenTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val rescheduleTaskPlannedTimeUseCase: RescheduleTaskPlannedTimeUseCase,
    private val changeTaskDeadlineUseCase: ChangeTaskDeadlineUseCase,
    private val deriveOverdue: DeriveOverdue
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(TaskFilterTab.TODAY)
    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    private val _selectedPriority = MutableStateFlow<Priority?>(null)
    private val _isProcessing = MutableStateFlow(false)

    private data class TaskFilterParams(
        val tab: TaskFilterTab,
        val categoryId: String?,
        val priority: Priority?,
        val isProcessing: Boolean
    )

    private val filterParams = combine(
        _selectedTab,
        _selectedCategoryId,
        _selectedPriority,
        _isProcessing
    ) { tab, cat, prio, proc ->
        TaskFilterParams(tab, cat, prio, proc)
    }

    val uiState: StateFlow<TasksUiState> = combine(
        taskRepository.getAllTasks(),
        categoryRepository.getAllCategories(),
        filterParams
    ) { allTasks, categories, params ->
        val now = LocalDateTime.now()
        val today = now.toLocalDate()

        val overdueIds = allTasks.filter { deriveOverdue(it, now) }.map { it.id }.toSet()

        // Tab counts
        val tabCounts = mapOf(
            TaskFilterTab.TODAY to allTasks.count { task ->
                task.status == TaskStatus.PENDING &&
                    !overdueIds.contains(task.id) &&
                    (task.plannedDate == today || task.deadlineDate == today)
            },
            TaskFilterTab.UPCOMING to allTasks.count { task ->
                task.status == TaskStatus.PENDING &&
                    !overdueIds.contains(task.id) &&
                    ((task.plannedDate != null && task.plannedDate.isAfter(today)) ||
                        (task.deadlineDate != null && task.deadlineDate.isAfter(today)))
            },
            TaskFilterTab.OVERDUE to overdueIds.size,
            TaskFilterTab.COMPLETED to allTasks.count { it.status == TaskStatus.COMPLETED },
            TaskFilterTab.ALL to allTasks.size
        )

        // Filter by tab
        val tabFiltered = when (params.tab) {
            TaskFilterTab.TODAY -> allTasks.filter { task ->
                task.status == TaskStatus.PENDING &&
                    !overdueIds.contains(task.id) &&
                    (task.plannedDate == today || task.deadlineDate == today)
            }
            TaskFilterTab.UPCOMING -> allTasks.filter { task ->
                task.status == TaskStatus.PENDING &&
                    !overdueIds.contains(task.id) &&
                    ((task.plannedDate != null && task.plannedDate.isAfter(today)) ||
                        (task.deadlineDate != null && task.deadlineDate.isAfter(today)))
            }
            TaskFilterTab.OVERDUE -> allTasks.filter { overdueIds.contains(it.id) }
            TaskFilterTab.COMPLETED -> allTasks.filter { it.status == TaskStatus.COMPLETED }
            TaskFilterTab.ALL -> allTasks
        }

        // Filter by category
        val categoryFiltered = if (params.categoryId != null) {
            tabFiltered.filter { it.categoryId == params.categoryId }
        } else {
            tabFiltered
        }

        // Filter by priority
        val priorityFiltered = if (params.priority != null) {
            categoryFiltered.filter { it.priority == params.priority }
        } else {
            categoryFiltered
        }

        // Sort: Overdue & high priority first, then deadline
        val sorted = priorityFiltered.sortedWith(
            compareBy<Task> { it.status == TaskStatus.COMPLETED }
                .thenBy { it.priority }
                .thenBy { it.deadlineDate ?: LocalDate.MAX }
                .thenBy { it.deadlineTime ?: LocalTime.MAX }
        )

        TasksUiState(
            selectedTab = params.tab,
            selectedCategoryId = params.categoryId,
            selectedPriority = params.priority,
            tasks = sorted,
            categories = categories,
            overdueTaskIds = overdueIds,
            activeTaskCounts = tabCounts,
            isLoading = params.isProcessing,
            errorMessage = null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TasksUiState(isLoading = true)
    )

    fun selectTab(tab: TaskFilterTab) {
        _selectedTab.value = tab
    }

    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = if (_selectedCategoryId.value == categoryId) null else categoryId
    }

    fun selectPriority(priority: Priority?) {
        _selectedPriority.value = if (_selectedPriority.value == priority) null else priority
    }

    fun toggleTaskComplete(task: Task) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                if (task.status == TaskStatus.COMPLETED) {
                    reopenTaskUseCase(task.id)
                } else {
                    completeTaskUseCase(task.id)
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                deleteTaskUseCase(taskId)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun reschedulePlannedTime(taskId: String, plannedDate: LocalDate?, plannedStart: LocalTime?, plannedEnd: LocalTime?) {
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
        taskId: String,
        deadlineDate: LocalDate?,
        deadlineTime: LocalTime?,
        reminders: Set<DeadlineReminderLeadTime>
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                changeTaskDeadlineUseCase(taskId, deadlineDate, deadlineTime, reminders)
            } finally {
                _isProcessing.value = false
            }
        }
    }
}
