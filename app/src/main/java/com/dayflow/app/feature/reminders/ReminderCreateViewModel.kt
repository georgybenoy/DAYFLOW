package com.dayflow.app.feature.reminders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayflow.app.domain.model.Category
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderPrecision
import com.dayflow.app.domain.model.ReminderType
import com.dayflow.app.domain.recurrence.RecurrenceCalculator
import com.dayflow.app.domain.recurrence.ReminderValidationError
import com.dayflow.app.domain.recurrence.ReminderValidationResult
import com.dayflow.app.domain.recurrence.ReminderValidator
import com.dayflow.app.domain.repository.CategoryRepository
import com.dayflow.app.domain.repository.ReminderRepository
import com.dayflow.app.domain.usecase.ScheduleReminderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

data class ReminderCreateUiState(
    val reminderId: String? = null,
    val isEditing: Boolean = false,
    val title: String = "",
    val description: String = "",
    val categoryId: String? = null,
    val availableCategories: List<Category> = emptyList(),
    val type: ReminderType = ReminderType.ONE_TIME,
    val startDate: LocalDate = LocalDate.now(),
    val startTime: LocalTime = LocalTime.now().plusHours(1).withMinute(0).withSecond(0),
    val intervalMinutes: Int? = 60,
    val activeDaysMask: Int = 127,
    val endTime: LocalTime? = LocalTime.of(22, 0),
    val endDate: LocalDate? = null,
    val priority: Priority = Priority.MEDIUM,
    val precision: ReminderPrecision = ReminderPrecision.EXACT,
    val snoozeDurationMinutes: Long = 10L,
    val validationResult: ReminderValidationResult = ReminderValidationResult.Valid,
    val previewOccurrences: List<Instant> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false
)

sealed interface ReminderCreateEvent {
    data object SaveSuccess : ReminderCreateEvent
    data class ShowError(val message: String) : ReminderCreateEvent
}

@HiltViewModel
class ReminderCreateViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val categoryRepository: CategoryRepository,
    private val scheduleReminderUseCase: ScheduleReminderUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReminderCreateUiState())
    val uiState: StateFlow<ReminderCreateUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ReminderCreateEvent>()
    val events: SharedFlow<ReminderCreateEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val categories = categoryRepository.getAllCategories().first()
            _uiState.update { it.copy(availableCategories = categories) }
        }
    }

    fun initForReminder(reminderId: String?) {
        if (reminderId == null) {
            updatePreview(_uiState.value)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val existing = reminderRepository.getReminderByIdDirect(reminderId)
            if (existing != null) {
                _uiState.update { current ->
                    current.copy(
                        reminderId = existing.id,
                        isEditing = true,
                        title = existing.title,
                        description = existing.description ?: "",
                        categoryId = existing.categoryId,
                        type = existing.type,
                        startDate = existing.startDate,
                        startTime = existing.startTime,
                        intervalMinutes = existing.intervalMinutes ?: 60,
                        activeDaysMask = existing.activeDaysMask,
                        endTime = existing.endTime ?: LocalTime.of(22, 0),
                        endDate = existing.endDate,
                        priority = existing.priority,
                        precision = existing.precision,
                        isLoading = false
                    )
                }
                updatePreview(_uiState.value)
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onTitleChange(title: String) {
        _uiState.update { it.copy(title = title) }
        validateAndComputePreview()
    }

    fun onDescriptionChange(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun onCategorySelect(categoryId: String?) {
        _uiState.update { it.copy(categoryId = categoryId) }
    }

    fun onTypeSelect(type: ReminderType) {
        _uiState.update { it.copy(type = type) }
        validateAndComputePreview()
    }

    fun onStartDateChange(date: LocalDate) {
        _uiState.update { it.copy(startDate = date) }
        validateAndComputePreview()
    }

    fun onStartTimeChange(time: LocalTime) {
        _uiState.update { it.copy(startTime = time) }
        validateAndComputePreview()
    }

    fun onIntervalMinutesChange(minutes: Int?) {
        _uiState.update { it.copy(intervalMinutes = minutes) }
        validateAndComputePreview()
    }

    fun onActiveDaysMaskChange(mask: Int) {
        _uiState.update { it.copy(activeDaysMask = mask) }
        validateAndComputePreview()
    }

    fun toggleWeekday(dayIndex: Int) { // 0 for Mon ... 6 for Sun
        val currentMask = _uiState.value.activeDaysMask
        val newMask = currentMask xor (1 shl dayIndex)
        onActiveDaysMaskChange(newMask)
    }

    fun onEndTimeChange(time: LocalTime?) {
        _uiState.update { it.copy(endTime = time) }
        validateAndComputePreview()
    }

    fun onEndDateChange(date: LocalDate?) {
        _uiState.update { it.copy(endDate = date) }
        validateAndComputePreview()
    }

    fun onPrioritySelect(priority: Priority) {
        _uiState.update { it.copy(priority = priority) }
    }

    fun onPrecisionSelect(precision: ReminderPrecision) {
        _uiState.update { it.copy(precision = precision) }
    }

    fun onSnoozeDurationChange(minutes: Long) {
        _uiState.update { it.copy(snoozeDurationMinutes = minutes) }
    }

    private fun validateAndComputePreview() {
        val state = _uiState.value
        val result = ReminderValidator.validate(
            title = state.title,
            type = state.type,
            startDate = state.startDate,
            endDate = state.endDate,
            intervalMinutes = state.intervalMinutes,
            activeDaysMask = state.activeDaysMask,
            startTime = state.startTime,
            endTime = state.endTime
        )
        _uiState.update { it.copy(validationResult = result) }
        updatePreview(state)
    }

    private fun updatePreview(state: ReminderCreateUiState) {
        val preview = computeNextOccurrences(state)
        _uiState.update { it.copy(previewOccurrences = preview) }
    }

    private fun computeNextOccurrences(state: ReminderCreateUiState): List<Instant> {
        val tempReminder = Reminder(
            id = state.reminderId ?: "preview_temp_id",
            title = state.title.ifBlank { "Preview" },
            description = state.description,
            categoryId = state.categoryId,
            type = state.type,
            startDate = state.startDate,
            startTime = state.startTime,
            intervalMinutes = state.intervalMinutes,
            activeDaysMask = state.activeDaysMask,
            endTime = state.endTime,
            endDate = state.endDate,
            priority = state.priority,
            precision = state.precision,
            timeZoneId = ZoneId.systemDefault()
        )

        val list = mutableListOf<Instant>()
        var after = Instant.now()
        for (i in 0 until 5) {
            val next = RecurrenceCalculator.nextOccurrence(tempReminder, after, tempReminder.timeZoneId)
            if (next != null) {
                list.add(next)
                after = next
            } else {
                break
            }
        }
        return list
    }

    fun saveReminder() {
        val state = _uiState.value
        val validation = ReminderValidator.validate(
            title = state.title,
            type = state.type,
            startDate = state.startDate,
            endDate = state.endDate,
            intervalMinutes = state.intervalMinutes,
            activeDaysMask = state.activeDaysMask,
            startTime = state.startTime,
            endTime = state.endTime
        )

        if (validation is ReminderValidationResult.Invalid) {
            _uiState.update { it.copy(validationResult = validation) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val now = Instant.now()
                val reminder = Reminder(
                    id = state.reminderId ?: UUID.randomUUID().toString(),
                    title = state.title.trim(),
                    description = state.description.trim().ifBlank { null },
                    categoryId = state.categoryId,
                    type = state.type,
                    startDate = state.startDate,
                    startTime = state.startTime,
                    intervalMinutes = state.intervalMinutes,
                    activeDaysMask = state.activeDaysMask,
                    endTime = state.endTime,
                    endDate = state.endDate,
                    priority = state.priority,
                    precision = state.precision,
                    isEnabled = true,
                    isPaused = false,
                    timeZoneId = ZoneId.systemDefault(),
                    createdAt = if (state.isEditing) (reminderRepository.getReminderByIdDirect(state.reminderId!!)?.createdAt ?: now) else now,
                    updatedAt = now
                )

                scheduleReminderUseCase(reminder, now)
                _events.emit(ReminderCreateEvent.SaveSuccess)
            } catch (e: Exception) {
                _events.emit(ReminderCreateEvent.ShowError(e.message ?: "Failed to save reminder"))
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
