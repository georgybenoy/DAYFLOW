package com.dayflow.app.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayflow.app.core.time.TimeProvider
import com.dayflow.app.domain.calendar.CalendarEvent
import com.dayflow.app.domain.calendar.CalendarEventType
import com.dayflow.app.domain.calendar.GetCalendarEventsUseCase
import com.dayflow.app.domain.model.WeekStartDay
import com.dayflow.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

enum class CalendarViewMode(val label: String) {
    DAY("Day"),
    WEEK("Week"),
    MONTH("Month")
}

data class CalendarUiState(
    val selectedDate: LocalDate,
    val viewMode: CalendarViewMode = CalendarViewMode.DAY,
    val selectedFilter: CalendarEventType? = null, // null = ALL
    val events: List<CalendarEvent> = emptyList(),
    val weekStartDay: WeekStartDay = WeekStartDay.MONDAY,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getCalendarEventsUseCase: GetCalendarEventsUseCase,
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(timeProvider.nowLocalDate())
    private val _viewMode = MutableStateFlow(CalendarViewMode.DAY)
    private val _selectedFilter = MutableStateFlow<CalendarEventType?>(null)
    private val _weekStartDay = MutableStateFlow(WeekStartDay.MONDAY)
    private val _events = MutableStateFlow<List<CalendarEvent>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow(
        CalendarUiState(selectedDate = timeProvider.nowLocalDate())
    )
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.userProfile.collect { profile ->
                _weekStartDay.value = profile.weekStartDay
                loadEvents()
            }
        }
    }

    fun setViewMode(mode: CalendarViewMode) {
        _viewMode.value = mode
        loadEvents()
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        loadEvents()
    }

    fun setFilter(type: CalendarEventType?) {
        _selectedFilter.value = type
        updateUiState()
    }

    fun goToToday() {
        _selectedDate.value = timeProvider.nowLocalDate()
        loadEvents()
    }

    fun navigatePrevious() {
        val current = _selectedDate.value
        _selectedDate.value = when (_viewMode.value) {
            CalendarViewMode.DAY -> current.minusDays(1)
            CalendarViewMode.WEEK -> current.minusWeeks(1)
            CalendarViewMode.MONTH -> current.minusMonths(1)
        }
        loadEvents()
    }

    fun navigateNext() {
        val current = _selectedDate.value
        _selectedDate.value = when (_viewMode.value) {
            CalendarViewMode.DAY -> current.plusDays(1)
            CalendarViewMode.WEEK -> current.plusWeeks(1)
            CalendarViewMode.MONTH -> current.plusMonths(1)
        }
        loadEvents()
    }

    fun loadEvents() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            updateUiState()

            try {
                val (rangeStart, rangeEnd) = computeDateRange(
                    date = _selectedDate.value,
                    mode = _viewMode.value,
                    weekStart = _weekStartDay.value
                )

                val loaded = getCalendarEventsUseCase(
                    startDate = rangeStart,
                    endDate = rangeEnd,
                    zoneId = timeProvider.zoneId()
                )
                _events.value = loaded
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load calendar events"
            } finally {
                _isLoading.value = false
                updateUiState()
            }
        }
    }

    private fun updateUiState() {
        val allEvents = _events.value
        val filter = _selectedFilter.value
        val filtered = if (filter == null) {
            allEvents
        } else {
            allEvents.filter { it.type == filter }
        }

        _uiState.value = CalendarUiState(
            selectedDate = _selectedDate.value,
            viewMode = _viewMode.value,
            selectedFilter = filter,
            events = filtered,
            weekStartDay = _weekStartDay.value,
            isLoading = _isLoading.value,
            error = _error.value
        )
    }

    private fun computeDateRange(
        date: LocalDate,
        mode: CalendarViewMode,
        weekStart: WeekStartDay
    ): Pair<LocalDate, LocalDate> {
        val firstDayOfWeek = when (weekStart) {
            WeekStartDay.MONDAY -> DayOfWeek.MONDAY
            WeekStartDay.SUNDAY -> DayOfWeek.SUNDAY
            WeekStartDay.SATURDAY -> DayOfWeek.SATURDAY
        }

        return when (mode) {
            CalendarViewMode.DAY -> Pair(date, date)
            CalendarViewMode.WEEK -> {
                val start = date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
                val end = start.plusDays(6)
                Pair(start, end)
            }
            CalendarViewMode.MONTH -> {
                val ym = YearMonth.from(date)
                val monthStart = ym.atDay(1)
                val monthEnd = ym.atEndOfMonth()
                // Buffer to full calendar grid weeks
                val gridStart = monthStart.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
                val lastDayOfWeek = firstDayOfWeek.plus(6)
                val gridEnd = monthEnd.with(TemporalAdjusters.nextOrSame(lastDayOfWeek))
                Pair(gridStart, gridEnd)
            }
        }
    }
}
