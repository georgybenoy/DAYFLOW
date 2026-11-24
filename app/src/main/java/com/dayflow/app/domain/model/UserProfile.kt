package com.dayflow.app.domain.model

import java.time.LocalTime

data class UserProfile(
    val name: String = "",
    val themePreference: ThemePreference = ThemePreference.DARK,
    val morningBriefingTime: LocalTime = LocalTime.of(7, 0),
    val eveningReviewTime: LocalTime = LocalTime.of(21, 30),
    val quietHoursStart: LocalTime = LocalTime.of(22, 0),
    val quietHoursEnd: LocalTime = LocalTime.of(7, 0),
    val isOnboardingCompleted: Boolean = false,
    val exactAlarmAcknowledged: Boolean = false,
    val weekStartDay: WeekStartDay = WeekStartDay.MONDAY,
    val dateFormatPreference: DateFormatPreference = DateFormatPreference.SYSTEM,
    val timeFormatPreference: TimeFormatPreference = TimeFormatPreference.H12,
    val isSoundEnabled: Boolean = true,
    val isVibrationEnabled: Boolean = true,
    val defaultReminderPrecision: ReminderPrecision = ReminderPrecision.APPROXIMATE,
    val defaultSnoozeMinutes: Int = 10
)
