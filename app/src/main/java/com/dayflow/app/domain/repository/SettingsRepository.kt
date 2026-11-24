package com.dayflow.app.domain.repository

import com.dayflow.app.domain.model.DateFormatPreference
import com.dayflow.app.domain.model.ReminderPrecision
import com.dayflow.app.domain.model.ThemePreference
import com.dayflow.app.domain.model.TimeFormatPreference
import com.dayflow.app.domain.model.UserProfile
import com.dayflow.app.domain.model.WeekStartDay
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

interface SettingsRepository {
    val userProfile: Flow<UserProfile>
    val themePreference: Flow<ThemePreference>
    val isOnboardingCompleted: Flow<Boolean>

    suspend fun setThemePreference(theme: ThemePreference)
    suspend fun setUserName(name: String)
    suspend fun setMorningBriefingTime(time: LocalTime)
    suspend fun setEveningReviewTime(time: LocalTime)
    suspend fun setQuietHours(start: LocalTime, end: LocalTime)
    suspend fun setExactAlarmAcknowledged(acknowledged: Boolean)
    suspend fun setWeekStartDay(weekStartDay: WeekStartDay)
    suspend fun setDateFormatPreference(dateFormat: DateFormatPreference)
    suspend fun setTimeFormatPreference(timeFormat: TimeFormatPreference)
    suspend fun setNotificationSoundEnabled(enabled: Boolean)
    suspend fun setNotificationVibrationEnabled(enabled: Boolean)
    suspend fun setDefaultReminderPrecision(precision: ReminderPrecision)
    suspend fun setDefaultSnoozeMinutes(minutes: Int)
    suspend fun completeOnboarding(
        name: String,
        theme: ThemePreference,
        briefingTime: LocalTime,
        reviewTime: LocalTime,
        quietStart: LocalTime,
        quietEnd: LocalTime
    )
    suspend fun skipOnboarding()
}
