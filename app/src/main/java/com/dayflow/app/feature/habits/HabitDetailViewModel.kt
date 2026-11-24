package com.dayflow.app.feature.habits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayflow.app.domain.model.Category
import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.HabitLog
import com.dayflow.app.domain.recurrence.ActiveDays
import com.dayflow.app.domain.recurrence.HabitDayStatus
import com.dayflow.app.domain.recurrence.HabitProgress
import com.dayflow.app.domain.recurrence.HabitProgressCalculator
import com.dayflow.app.domain.recurrence.StreakCalculator
import com.dayflow.app.domain.recurrence.StreakResult
import com.dayflow.app.domain.repository.CategoryRepository
import com.dayflow.app.domain.repository.HabitRepository
import com.dayflow.app.domain.usecase.DeleteHabitUseCase
import com.dayflow.app.domain.usecase.LogHabitProgressUseCase
import com.dayflow.app.domain.usecase.ToggleHabitEnableUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class HabitDetailUiState(
    val habit: Habit? = null,
    val category: Category? = null,
    val todayLog: HabitLog? = null,
    val todayProgress: HabitProgress? = null,
    val isScheduledToday: Boolean = false,
    val streak: StreakResult = StreakResult(0, 0),
    val allLogs: List<HabitLog> = emptyList(),
    val weeklyStatus: List<HabitDayStatus> = emptyList(),
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val quickLogOpen: Boolean = false
)

@HiltViewModel
class HabitDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val habitRepository: HabitRepository,
    private val categoryRepository: CategoryRepository,
    private val logHabitProgressUseCase: LogHabitProgressUseCase,
    private val toggleHabitEnableUseCase: ToggleHabitEnableUseCase,
    private val deleteHabitUseCase: DeleteHabitUseCase
) : ViewModel() {

    private val habitId: String = checkNotNull(savedStateHandle.get<String>("habitId"))
    private val _quickLogOpen = MutableStateFlow(false)
    private val _isDeleted = MutableStateFlow(false)

    val uiState: StateFlow<HabitDetailUiState> = habitRepository.getHabitById(habitId)
        .flatMapLatest { habit ->
            val today = LocalDate.now()
            val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

            combine(
                categoryRepository.getAllCategories(),
                habitRepository.getLogsForHabit(habitId),
                _quickLogOpen,
                _isDeleted
            ) { categories, logs, quickLogOpen, isDeleted ->
                if (habit == null) {
                    HabitDetailUiState(isLoading = false, isDeleted = isDeleted)
                } else {
                    val category = categories.firstOrNull { it.id == habit.categoryId }
                    val todayLog = logs.firstOrNull { it.localDate == today }
                    val todayProgress = HabitProgressCalculator.calculateProgress(habit, todayLog)
                    val isScheduledToday = ActiveDays.isDayActive(habit.activeDaysMask, today.dayOfWeek)
                    val streak = StreakCalculator.calculateStreak(
                        habit = habit,
                        logs = logs,
                        today = today
                    )
                    val weeklyStatus = StreakCalculator.getWeeklyHistory(
                        habit = habit,
                        logs = logs,
                        weekStartDate = startOfWeek
                    )

                    HabitDetailUiState(
                        habit = habit,
                        category = category,
                        todayLog = todayLog,
                        todayProgress = todayProgress,
                        isScheduledToday = isScheduledToday,
                        streak = streak,
                        allLogs = logs,
                        weeklyStatus = weeklyStatus,
                        isLoading = false,
                        isDeleted = isDeleted,
                        quickLogOpen = quickLogOpen
                    )
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = HabitDetailUiState()
        )

    fun toggleTodayBinary() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val existingLog = habitRepository.getLogForHabitAndDateDirect(habitId, today)
            val isNowCompleted = !(existingLog?.isCompleted ?: false)
            logHabitProgressUseCase(
                habitId = habitId,
                date = today,
                completedValue = if (isNowCompleted) 1.0 else 0.0,
                isCompleted = isNowCompleted
            )
        }
    }

    fun openQuickLog() {
        _quickLogOpen.value = true
    }

    fun closeQuickLog() {
        _quickLogOpen.value = false
    }

    fun saveQuickLog(value: Double) {
        viewModelScope.launch {
            logHabitProgressUseCase(
                habitId = habitId,
                date = LocalDate.now(),
                completedValue = value
            )
            closeQuickLog()
        }
    }

    fun toggleEnable() {
        viewModelScope.launch {
            toggleHabitEnableUseCase(habitId)
        }
    }

    fun deleteHabit() {
        viewModelScope.launch {
            deleteHabitUseCase(habitId)
            _isDeleted.value = true
        }
    }
}
