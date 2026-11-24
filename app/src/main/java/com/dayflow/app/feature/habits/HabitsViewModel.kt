package com.dayflow.app.feature.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayflow.app.domain.model.Category
import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.HabitLog
import com.dayflow.app.domain.model.HabitTrackingType
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class HabitItemUiModel(
    val habit: Habit,
    val category: Category?,
    val isScheduledToday: Boolean,
    val log: HabitLog?,
    val progress: HabitProgress,
    val streak: StreakResult,
    val weeklyStatus: List<HabitDayStatus>
)

data class HabitsUiState(
    val isLoading: Boolean = true,
    val selectedDate: LocalDate = LocalDate.now(),
    val habits: List<HabitItemUiModel> = emptyList(),
    val activeHabits: List<HabitItemUiModel> = emptyList(),
    val pausedHabits: List<HabitItemUiModel> = emptyList(),
    val dailyProgress: Float = 0f,
    val completedCount: Int = 0,
    val totalScheduledCount: Int = 0,
    val quickLogHabit: Habit? = null,
    val quickLogCurrentLog: HabitLog? = null
)

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val categoryRepository: CategoryRepository,
    private val logHabitProgressUseCase: LogHabitProgressUseCase,
    private val toggleHabitEnableUseCase: ToggleHabitEnableUseCase,
    private val deleteHabitUseCase: DeleteHabitUseCase
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _quickLogState = MutableStateFlow<Pair<Habit?, HabitLog?>?>(null)

    val uiState: StateFlow<HabitsUiState> = _selectedDate
        .flatMapLatest { selectedDate ->
            val startOfWeek = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val endOfWeek = startOfWeek.plusDays(6)

            combine(
                habitRepository.getAllHabits(),
                categoryRepository.getAllCategories(),
                habitRepository.getLogsInRange(startOfWeek.minusDays(30), endOfWeek),
                _quickLogState
            ) { habits, categories, logs, quickLogPair ->
                val categoryMap = categories.associateBy { it.id }
                val logsByHabit = logs.groupBy { it.habitId }

                val habitUiModels = habits.map { habit ->
                    val habitLogs = logsByHabit[habit.id] ?: emptyList()
                    val logForDate = habitLogs.firstOrNull { it.localDate == selectedDate }
                    val isScheduled = ActiveDays.isDayActive(habit.activeDaysMask, selectedDate.dayOfWeek)
                    val progress = HabitProgressCalculator.calculateProgress(habit, logForDate)
                    val streak = StreakCalculator.calculateStreak(
                        habit = habit,
                        logs = habitLogs,
                        today = selectedDate
                    )
                    val weeklyStatus = StreakCalculator.getWeeklyHistory(
                        habit = habit,
                        logs = habitLogs,
                        weekStartDate = startOfWeek
                    )

                    HabitItemUiModel(
                        habit = habit,
                        category = categoryMap[habit.categoryId],
                        isScheduledToday = isScheduled,
                        log = logForDate,
                        progress = progress,
                        streak = streak,
                        weeklyStatus = weeklyStatus
                    )
                }

                val activeHabits = habitUiModels.filter { it.habit.isEnabled }
                val pausedHabits = habitUiModels.filter { !it.habit.isEnabled }

                val scheduledActive = activeHabits.filter { it.isScheduledToday }
                val completedCount = scheduledActive.count { it.progress.isCompleted }
                val totalScheduled = scheduledActive.size
                val dailyProgress = if (totalScheduled > 0) completedCount.toFloat() / totalScheduled.toFloat() else 0f

                HabitsUiState(
                    isLoading = false,
                    selectedDate = selectedDate,
                    habits = habitUiModels,
                    activeHabits = activeHabits,
                    pausedHabits = pausedHabits,
                    dailyProgress = dailyProgress,
                    completedCount = completedCount,
                    totalScheduledCount = totalScheduled,
                    quickLogHabit = quickLogPair?.first,
                    quickLogCurrentLog = quickLogPair?.second
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = HabitsUiState()
        )

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun toggleBinaryHabit(habitId: String) {
        viewModelScope.launch {
            val date = _selectedDate.value
            val existingLog = habitRepository.getLogForHabitAndDateDirect(habitId, date)
            val isNowCompleted = !(existingLog?.isCompleted ?: false)
            logHabitProgressUseCase(
                habitId = habitId,
                date = date,
                completedValue = if (isNowCompleted) 1.0 else 0.0,
                isCompleted = isNowCompleted
            )
        }
    }

    fun openQuickLog(habit: Habit) {
        viewModelScope.launch {
            val date = _selectedDate.value
            val log = habitRepository.getLogForHabitAndDateDirect(habit.id, date)
            _quickLogState.value = Pair(habit, log)
        }
    }

    fun closeQuickLog() {
        _quickLogState.value = null
    }

    fun saveQuickLog(value: Double) {
        val habit = _quickLogState.value?.first ?: return
        val date = _selectedDate.value
        viewModelScope.launch {
            logHabitProgressUseCase(
                habitId = habit.id,
                date = date,
                completedValue = value
            )
            closeQuickLog()
        }
    }

    fun toggleHabitEnable(habitId: String) {
        viewModelScope.launch {
            toggleHabitEnableUseCase(habitId)
        }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            deleteHabitUseCase(habitId)
        }
    }
}
