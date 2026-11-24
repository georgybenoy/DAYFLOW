package com.dayflow.app.feature.habits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayflow.app.domain.model.Category
import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.HabitTrackingType
import com.dayflow.app.domain.recurrence.ActiveDays
import com.dayflow.app.domain.repository.CategoryRepository
import com.dayflow.app.domain.repository.HabitRepository
import com.dayflow.app.domain.usecase.CreateHabitUseCase
import com.dayflow.app.domain.usecase.UpdateHabitUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

data class HabitCreateUiState(
    val habitId: String? = null,
    val isEditMode: Boolean = false,
    val name: String = "",
    val description: String = "",
    val categoryId: String? = null,
    val trackingType: HabitTrackingType = HabitTrackingType.BINARY,
    val targetValue: String = "1",
    val unit: String = "",
    val activeDaysMask: Int = ActiveDays.ALL_DAYS_MASK,
    val reminderPreference: Boolean = false,
    val reminderTime: LocalTime = LocalTime.of(9, 0),
    val categories: List<Category> = emptyList(),
    val nameError: String? = null,
    val targetValueError: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false
)

@HiltViewModel
class HabitCreateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val habitRepository: HabitRepository,
    private val categoryRepository: CategoryRepository,
    private val createHabitUseCase: CreateHabitUseCase,
    private val updateHabitUseCase: UpdateHabitUseCase
) : ViewModel() {

    private val navHabitId: String? = savedStateHandle.get<String>("habitId")
    private val _uiState = MutableStateFlow(HabitCreateUiState(habitId = navHabitId, isEditMode = navHabitId != null))
    val uiState: StateFlow<HabitCreateUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
        if (navHabitId != null) {
            loadExistingHabit(navHabitId)
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    private fun loadExistingHabit(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val habit = habitRepository.getHabitByIdDirect(id)
            if (habit != null) {
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        name = habit.name,
                        description = habit.description ?: "",
                        categoryId = habit.categoryId,
                        trackingType = habit.trackingType,
                        targetValue = if (habit.targetValue % 1.0 == 0.0) habit.targetValue.toLong().toString() else habit.targetValue.toString(),
                        unit = habit.unit ?: "",
                        activeDaysMask = habit.activeDaysMask,
                        reminderPreference = habit.reminderPreference,
                        reminderTime = habit.reminderTime ?: LocalTime.of(9, 0)
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name, nameError = if (name.isBlank()) "Name cannot be blank" else null) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun updateCategory(categoryId: String?) {
        _uiState.update { it.copy(categoryId = categoryId) }
    }

    fun updateTrackingType(type: HabitTrackingType) {
        _uiState.update { current ->
            val defaultTarget = when (type) {
                HabitTrackingType.BINARY -> "1"
                HabitTrackingType.DURATION -> "30"
                HabitTrackingType.QUANTITY -> "2"
                HabitTrackingType.REPETITION -> "10"
            }
            val defaultUnit = when (type) {
                HabitTrackingType.BINARY -> ""
                HabitTrackingType.DURATION -> "min"
                HabitTrackingType.QUANTITY -> if (current.unit.isBlank() || current.unit in listOf("min", "reps")) "L" else current.unit
                HabitTrackingType.REPETITION -> if (current.unit.isBlank() || current.unit in listOf("min", "L")) "reps" else current.unit
            }
            current.copy(
                trackingType = type,
                targetValue = defaultTarget,
                unit = defaultUnit,
                targetValueError = null
            )
        }
    }

    fun updateTargetValue(value: String) {
        val parsed = value.toDoubleOrNull()
        val error = if (parsed == null || parsed <= 0.0) "Must be greater than 0" else null
        _uiState.update { it.copy(targetValue = value, targetValueError = error) }
    }

    fun updateUnit(unit: String) {
        _uiState.update { it.copy(unit = unit) }
    }

    fun toggleWeekday(day: DayOfWeek) {
        _uiState.update { current ->
            val bit = 1 shl (day.value - 1)
            val newMask = current.activeDaysMask xor bit
            // Prevent deselecting all days
            if (newMask == 0) current else current.copy(activeDaysMask = newMask)
        }
    }

    fun toggleReminderPreference(enabled: Boolean) {
        _uiState.update { it.copy(reminderPreference = enabled) }
    }

    fun updateReminderTime(time: LocalTime) {
        _uiState.update { it.copy(reminderTime = time) }
    }

    fun saveHabit() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = "Name cannot be blank") }
            return
        }

        val targetDouble = state.targetValue.toDoubleOrNull()
        if (state.trackingType != HabitTrackingType.BINARY && (targetDouble == null || targetDouble <= 0.0)) {
            _uiState.update { it.copy(targetValueError = "Target must be greater than 0") }
            return
        }

        val finalTarget = if (state.trackingType == HabitTrackingType.BINARY) 1.0 else (targetDouble ?: 1.0)

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val habit = Habit(
                id = state.habitId ?: java.util.UUID.randomUUID().toString(),
                name = state.name.trim(),
                description = state.description.trim().ifBlank { null },
                categoryId = state.categoryId,
                trackingType = state.trackingType,
                targetValue = finalTarget,
                unit = state.unit.trim().ifBlank { null },
                activeDaysMask = state.activeDaysMask,
                reminderPreference = state.reminderPreference,
                reminderTime = if (state.reminderPreference) state.reminderTime else null,
                isEnabled = true
            )

            val result = if (state.isEditMode) {
                updateHabitUseCase(habit)
            } else {
                createHabitUseCase(habit)
            }

            if (result.isSuccess) {
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } else {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
