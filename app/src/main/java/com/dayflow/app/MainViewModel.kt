package com.dayflow.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayflow.app.domain.model.ThemePreference
import com.dayflow.app.domain.repository.SettingsRepository
import com.dayflow.app.scheduling.WorkflowReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MainActivityUiState {
    data object Loading : MainActivityUiState
    data class Success(
        val themePreference: ThemePreference,
        val isOnboardingCompleted: Boolean
    ) : MainActivityUiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val workflowReminderScheduler: WorkflowReminderScheduler
) : ViewModel() {

    init {
        viewModelScope.launch {
            workflowReminderScheduler.ensureWorkflowRemindersScheduled()
        }
    }

    val uiState: StateFlow<MainActivityUiState> = settingsRepository.userProfile
        .map { profile ->
            MainActivityUiState.Success(
                themePreference = profile.themePreference,
                isOnboardingCompleted = profile.isOnboardingCompleted
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MainActivityUiState.Loading
        )
}
