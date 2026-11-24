package com.dayflow.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayflow.app.domain.model.DateFormatPreference
import com.dayflow.app.domain.model.ReminderPrecision
import com.dayflow.app.domain.model.ThemePreference
import com.dayflow.app.domain.model.TimeFormatPreference
import com.dayflow.app.domain.model.UserProfile
import com.dayflow.app.domain.model.WeekStartDay
import com.dayflow.app.domain.repository.SettingsRepository
import com.dayflow.app.scheduling.WorkflowReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val workflowReminderScheduler: WorkflowReminderScheduler
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = settingsRepository.userProfile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserProfile()
        )

    fun setTheme(theme: ThemePreference) {
        viewModelScope.launch {
            settingsRepository.setThemePreference(theme)
        }
    }

    fun setMorningBriefingTime(time: LocalTime) {
        viewModelScope.launch {
            settingsRepository.setMorningBriefingTime(time)
            val currentProfile = userProfile.value
            workflowReminderScheduler.syncWorkflowTimes(time, currentProfile.eveningReviewTime)
        }
    }

    fun setEveningReviewTime(time: LocalTime) {
        viewModelScope.launch {
            settingsRepository.setEveningReviewTime(time)
            val currentProfile = userProfile.value
            workflowReminderScheduler.syncWorkflowTimes(currentProfile.morningBriefingTime, time)
        }
    }

    fun setWeekStartDay(weekStartDay: WeekStartDay) {
        viewModelScope.launch {
            settingsRepository.setWeekStartDay(weekStartDay)
        }
    }

    fun setDateFormatPreference(dateFormat: DateFormatPreference) {
        viewModelScope.launch {
            settingsRepository.setDateFormatPreference(dateFormat)
        }
    }

    fun setTimeFormatPreference(timeFormat: TimeFormatPreference) {
        viewModelScope.launch {
            settingsRepository.setTimeFormatPreference(timeFormat)
        }
    }

    fun setNotificationSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationSoundEnabled(enabled)
        }
    }

    fun setNotificationVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationVibrationEnabled(enabled)
        }
    }

    fun setDefaultReminderPrecision(precision: ReminderPrecision) {
        viewModelScope.launch {
            settingsRepository.setDefaultReminderPrecision(precision)
        }
    }

    fun setDefaultSnoozeMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setDefaultSnoozeMinutes(minutes)
        }
    }
}
