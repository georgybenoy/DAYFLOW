package com.dayflow.app.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayflow.app.core.time.TimeProvider
import com.dayflow.app.domain.analytics.GetInsightsUseCase
import com.dayflow.app.domain.analytics.InsightsData
import com.dayflow.app.domain.model.WeekStartDay
import com.dayflow.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class InsightsViewFilter(val label: String) {
    WEEK("Week"),
    MONTH("Month"),
    ALL_TIME("Overview")
}

sealed interface InsightsUiState {
    data object Loading : InsightsUiState
    data class Success(
        val data: InsightsData,
        val weekStartDay: WeekStartDay = WeekStartDay.MONDAY,
        val selectedFilter: InsightsViewFilter = InsightsViewFilter.WEEK
    ) : InsightsUiState
    data class Error(val message: String) : InsightsUiState
}

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val getInsightsUseCase: GetInsightsUseCase,
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(InsightsViewFilter.WEEK)
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _uiState = MutableStateFlow<InsightsUiState>(InsightsUiState.Loading)
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        loadInsights()
    }

    fun loadInsights(date: LocalDate = timeProvider.nowLocalDate()) {
        viewModelScope.launch {
            _uiState.value = InsightsUiState.Loading
            try {
                settingsRepository.userProfile.collect { profile ->
                    val weekStart = profile.weekStartDay
                    val data = getInsightsUseCase(
                        targetDate = date,
                        weekStartDay = weekStart,
                        zoneId = timeProvider.zoneId()
                    )
                    _uiState.value = InsightsUiState.Success(
                        data = data,
                        weekStartDay = weekStart,
                        selectedFilter = _selectedFilter.value
                    )
                }
            } catch (e: Exception) {
                _uiState.value = InsightsUiState.Error(e.message ?: "Failed to load productivity insights")
            }
        }
    }

    fun setFilter(filter: InsightsViewFilter) {
        _selectedFilter.value = filter
        val current = _uiState.value
        if (current is InsightsUiState.Success) {
            _uiState.value = current.copy(selectedFilter = filter)
        }
    }

    fun refresh() {
        loadInsights()
    }
}
