package com.dayflow.app.feature.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayflow.app.core.time.ReminderFormatter
import com.dayflow.app.domain.model.Category
import com.dayflow.app.domain.model.OccurrenceStatus
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderOccurrence
import com.dayflow.app.domain.repository.CategoryRepository
import com.dayflow.app.domain.repository.ReminderRepository
import com.dayflow.app.domain.usecase.CompleteReminderOccurrenceUseCase
import com.dayflow.app.domain.usecase.DeleteReminderUseCase
import com.dayflow.app.domain.usecase.PauseReminderUseCase
import com.dayflow.app.domain.usecase.ResumeReminderUseCase
import com.dayflow.app.domain.usecase.SkipReminderOccurrenceUseCase
import com.dayflow.app.domain.usecase.SnoozeReminderOccurrenceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class OccurrenceHistoryItem(
    val occurrence: ReminderOccurrence,
    val formattedTime: String,
    val statusText: String,
    val isActionable: Boolean
)

data class ReminderDetailUiState(
    val reminder: Reminder? = null,
    val category: Category? = null,
    val recurrenceSummary: String = "",
    val activeOccurrence: ReminderOccurrence? = null,
    val occurrenceHistory: List<OccurrenceHistoryItem> = emptyList(),
    val isLoading: Boolean = true,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null
)

sealed interface ReminderDetailEvent {
    data object ReminderDeleted : ReminderDetailEvent
    data class ShowMessage(val message: String) : ReminderDetailEvent
}

@HiltViewModel
class ReminderDetailViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val categoryRepository: CategoryRepository,
    private val completeReminderOccurrenceUseCase: CompleteReminderOccurrenceUseCase,
    private val snoozeReminderOccurrenceUseCase: SnoozeReminderOccurrenceUseCase,
    private val skipReminderOccurrenceUseCase: SkipReminderOccurrenceUseCase,
    private val pauseReminderUseCase: PauseReminderUseCase,
    private val resumeReminderUseCase: ResumeReminderUseCase,
    private val deleteReminderUseCase: DeleteReminderUseCase
) : ViewModel() {

    private val _reminderId = MutableStateFlow<String?>(null)
    private val _isProcessing = MutableStateFlow(false)

    private val _events = MutableSharedFlow<ReminderDetailEvent>()
    val events: SharedFlow<ReminderDetailEvent> = _events.asSharedFlow()

    private val _uiState = MutableStateFlow(ReminderDetailUiState())
    val uiState: StateFlow<ReminderDetailUiState> = _uiState.asStateFlow()

    fun initForReminder(id: String) {
        _reminderId.value = id
        viewModelScope.launch {
            _reminderId.filterNotNull().flatMapLatest { reminderId ->
                combine(
                    reminderRepository.getReminderById(reminderId),
                    reminderRepository.getOccurrencesForReminder(reminderId),
                    categoryRepository.getAllCategories(),
                    _isProcessing
                ) { reminder, occurrences, categories, isProcessing ->
                    if (reminder == null) {
                        ReminderDetailUiState(
                            reminder = null,
                            isLoading = false,
                            errorMessage = "Reminder not found"
                        )
                    } else {
                        val category = categories.find { it.id == reminder.categoryId }
                        val sortedOccurrences = occurrences.sortedByDescending { it.scheduledAt }

                        val active = sortedOccurrences.firstOrNull {
                            it.status == OccurrenceStatus.SCHEDULED || it.status == OccurrenceStatus.SNOOZED || it.status == OccurrenceStatus.DELIVERED
                        }

                        val historyItems = sortedOccurrences.map { occ ->
                            OccurrenceHistoryItem(
                                occurrence = occ,
                                formattedTime = ReminderFormatter.formatOccurrenceTime(occ.scheduledAt, reminder.timeZoneId),
                                statusText = occ.status.name,
                                isActionable = occ.id == active?.id
                            )
                        }

                        ReminderDetailUiState(
                            reminder = reminder,
                            category = category,
                            recurrenceSummary = ReminderFormatter.formatRecurrenceSummary(reminder),
                            activeOccurrence = active,
                            occurrenceHistory = historyItems,
                            isLoading = false,
                            isProcessing = isProcessing,
                            errorMessage = null
                        )
                    }
                }
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun completeActiveOccurrence(occurrenceId: String) {
        val reminderId = _reminderId.value ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                completeReminderOccurrenceUseCase(occurrenceId, reminderId)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun snoozeActiveOccurrence(occurrenceId: String, durationMinutes: Long = 10L) {
        val reminderId = _reminderId.value ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                snoozeReminderOccurrenceUseCase(occurrenceId, reminderId, durationMinutes)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun skipActiveOccurrence(occurrenceId: String) {
        val reminderId = _reminderId.value ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                skipReminderOccurrenceUseCase(occurrenceId, reminderId)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun togglePause() {
        val rem = _uiState.value.reminder ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                if (rem.isPaused) {
                    resumeReminderUseCase(rem.id)
                } else {
                    pauseReminderUseCase(rem.id)
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun deleteReminder() {
        val rem = _uiState.value.reminder ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                deleteReminderUseCase(rem.id)
                _events.emit(ReminderDetailEvent.ReminderDeleted)
            } finally {
                _isProcessing.value = false
            }
        }
    }
}
