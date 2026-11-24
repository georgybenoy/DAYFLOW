package com.dayflow.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayflow.app.domain.model.ThemePreference
import com.dayflow.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

data class OnboardingUiState(
    val name: String = "",
    val theme: ThemePreference = ThemePreference.DARK,
    val morningBriefingTime: LocalTime = LocalTime.of(7, 0),
    val eveningReviewTime: LocalTime = LocalTime.of(21, 30),
    val quietHoursStart: LocalTime = LocalTime.of(22, 0),
    val quietHoursEnd: LocalTime = LocalTime.of(7, 0),
    val isSaving: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun updateTheme(theme: ThemePreference) {
        _uiState.update { it.copy(theme = theme) }
    }

    fun updateMorningBriefingTime(time: LocalTime) {
        _uiState.update { it.copy(morningBriefingTime = time) }
    }

    fun updateEveningReviewTime(time: LocalTime) {
        _uiState.update { it.copy(eveningReviewTime = time) }
    }

    fun updateQuietHours(start: LocalTime, end: LocalTime) {
        _uiState.update { it.copy(quietHoursStart = start, quietHoursEnd = end) }
    }

    fun completeOnboarding(onCompleted: () -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            settingsRepository.completeOnboarding(
                name = state.name.trim(),
                theme = state.theme,
                briefingTime = state.morningBriefingTime,
                reviewTime = state.eveningReviewTime,
                quietStart = state.quietHoursStart,
                quietEnd = state.quietHoursEnd
            )
            onCompleted()
        }
    }

    fun skipOnboarding(onCompleted: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            settingsRepository.skipOnboarding()
            onCompleted()
        }
    }
}
