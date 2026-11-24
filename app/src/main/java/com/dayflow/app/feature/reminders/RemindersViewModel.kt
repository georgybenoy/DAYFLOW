package com.dayflow.app.feature.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayflow.app.core.time.ReminderFormatter
import com.dayflow.app.domain.model.Category
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.recurrence.RecurrenceCalculator
import com.dayflow.app.domain.repository.CategoryRepository
import com.dayflow.app.domain.repository.ReminderRepository
import com.dayflow.app.domain.usecase.DeleteReminderUseCase
import com.dayflow.app.domain.usecase.GetUpcomingRemindersUseCase
import com.dayflow.app.domain.usecase.PauseReminderUseCase
import com.dayflow.app.domain.usecase.ResumeReminderUseCase
import com.dayflow.app.domain.usecase.UpcomingReminderItem
import com.dayflow.app.scheduling.AlarmScheduler
import com.dayflow.app.scheduling.SchedulingCapability
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class ReminderListItem(
    val reminder: Reminder,
    val recurrenceSummary: String,
    val nextDeliverySummary: String,
    val nextDeliveryInstant: Instant?,
    val category: Category? = null
)

data class RemindersUiState(
    val activeReminders: List<ReminderListItem> = emptyList(),
    val pausedReminders: List<ReminderListItem> = emptyList(),
    val upcomingOccurrences: List<UpcomingReminderItem> = emptyList(),
    val schedulingCapability: SchedulingCapability = SchedulingCapability.EXACT,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val categoryRepository: CategoryRepository,
    private val alarmScheduler: AlarmScheduler,
    private val pauseReminderUseCase: PauseReminderUseCase,
    private val resumeReminderUseCase: ResumeReminderUseCase,
    private val deleteReminderUseCase: DeleteReminderUseCase,
    private val getUpcomingRemindersUseCase: GetUpcomingRemindersUseCase
) : ViewModel() {

    private val _isProcessing = MutableStateFlow(false)

    val uiState: StateFlow<RemindersUiState> = combine(
        reminderRepository.getStandaloneReminders(),
        categoryRepository.getAllCategories(),
        getUpcomingRemindersUseCase(),
        alarmScheduler.schedulingCapability,
        _isProcessing
    ) { reminders, categories, upcoming, capability, isProcessing ->
        val categoryMap = categories.associateBy { it.id }
        val now = Instant.now()

        val items = reminders.map { reminder ->
            val nextInstant = if (reminder.isEnabled && !reminder.isPaused) {
                RecurrenceCalculator.nextOccurrence(reminder, now, reminder.timeZoneId)
            } else {
                null
            }

            ReminderListItem(
                reminder = reminder,
                recurrenceSummary = ReminderFormatter.formatRecurrenceSummary(reminder),
                nextDeliverySummary = ReminderFormatter.formatNextDelivery(nextInstant, reminder.timeZoneId),
                nextDeliveryInstant = nextInstant,
                category = reminder.categoryId?.let { categoryMap[it] }
            )
        }

        val active = items.filter { it.reminder.isEnabled && !it.reminder.isPaused }
            .sortedBy { it.nextDeliveryInstant ?: Instant.MAX }
        val paused = items.filter { it.reminder.isPaused || !it.reminder.isEnabled }
            .sortedByDescending { it.reminder.updatedAt }

        RemindersUiState(
            activeReminders = active,
            pausedReminders = paused,
            upcomingOccurrences = upcoming.take(5),
            schedulingCapability = capability,
            isLoading = isProcessing,
            errorMessage = null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RemindersUiState(isLoading = true)
    )

    fun pauseReminder(reminderId: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                pauseReminderUseCase(reminderId)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun resumeReminder(reminderId: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                resumeReminderUseCase(reminderId)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun deleteReminder(reminderId: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                deleteReminderUseCase(reminderId)
            } finally {
                _isProcessing.value = false
            }
        }
    }
}
