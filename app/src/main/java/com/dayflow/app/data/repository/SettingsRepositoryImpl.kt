package com.dayflow.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.dayflow.app.data.local.DataStoreKeys
import com.dayflow.app.domain.model.DateFormatPreference
import com.dayflow.app.domain.model.ReminderPrecision
import com.dayflow.app.domain.model.ThemePreference
import com.dayflow.app.domain.model.TimeFormatPreference
import com.dayflow.app.domain.model.UserProfile
import com.dayflow.app.domain.model.WeekStartDay
import com.dayflow.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalTime
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override val userProfile: Flow<UserProfile> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val name = preferences[DataStoreKeys.USER_NAME] ?: ""
            val themeStr = preferences[DataStoreKeys.THEME_PREFERENCE]
            val theme = parseTheme(themeStr)
            val briefingTime = parseLocalTime(preferences[DataStoreKeys.MORNING_BRIEFING_TIME], LocalTime.of(7, 0))
            val reviewTime = parseLocalTime(preferences[DataStoreKeys.EVENING_REVIEW_TIME], LocalTime.of(21, 30))
            val quietStart = parseLocalTime(preferences[DataStoreKeys.QUIET_HOURS_START], LocalTime.of(22, 0))
            val quietEnd = parseLocalTime(preferences[DataStoreKeys.QUIET_HOURS_END], LocalTime.of(7, 0))
            val isOnboardingCompleted = preferences[DataStoreKeys.IS_ONBOARDING_COMPLETED] ?: false
            val exactAlarmAck = preferences[DataStoreKeys.EXACT_ALARM_ACKNOWLEDGED] ?: false

            val weekStartStr = preferences[DataStoreKeys.WEEK_START_DAY]
            val weekStart = parseEnum(weekStartStr, WeekStartDay.MONDAY)

            val dateFormatStr = preferences[DataStoreKeys.DATE_FORMAT_PREFERENCE]
            val dateFormat = parseEnum(dateFormatStr, DateFormatPreference.SYSTEM)

            val timeFormatStr = preferences[DataStoreKeys.TIME_FORMAT_PREFERENCE]
            val timeFormat = parseEnum(timeFormatStr, TimeFormatPreference.H12)

            val soundEnabled = preferences[DataStoreKeys.NOTIFICATION_SOUND] ?: true
            val vibrationEnabled = preferences[DataStoreKeys.NOTIFICATION_VIBRATION] ?: true

            val precisionStr = preferences[DataStoreKeys.DEFAULT_REMINDER_PRECISION]
            val precision = parseEnum(precisionStr, ReminderPrecision.APPROXIMATE)

            val snoozeMinutes = preferences[DataStoreKeys.DEFAULT_SNOOZE_MINUTES] ?: 10

            UserProfile(
                name = name,
                themePreference = theme,
                morningBriefingTime = briefingTime,
                eveningReviewTime = reviewTime,
                quietHoursStart = quietStart,
                quietHoursEnd = quietEnd,
                isOnboardingCompleted = isOnboardingCompleted,
                exactAlarmAcknowledged = exactAlarmAck,
                weekStartDay = weekStart,
                dateFormatPreference = dateFormat,
                timeFormatPreference = timeFormat,
                isSoundEnabled = soundEnabled,
                isVibrationEnabled = vibrationEnabled,
                defaultReminderPrecision = precision,
                defaultSnoozeMinutes = snoozeMinutes
            )
        }

    override val themePreference: Flow<ThemePreference> = userProfile.map { it.themePreference }

    override val isOnboardingCompleted: Flow<Boolean> = userProfile.map { it.isOnboardingCompleted }

    override suspend fun setThemePreference(theme: ThemePreference) {
        dataStore.edit { preferences ->
            preferences[DataStoreKeys.THEME_PREFERENCE] = theme.name
        }
    }

    override suspend fun setUserName(name: String) {
        dataStore.edit { preferences ->
            preferences[DataStoreKeys.USER_NAME] = name
        }
    }

    override suspend fun setMorningBriefingTime(time: LocalTime) {
        dataStore.edit { preferences ->
            preferences[DataStoreKeys.MORNING_BRIEFING_TIME] = time.toString()
        }
    }

    override suspend fun setEveningReviewTime(time: LocalTime) {
        dataStore.edit { preferences ->
            preferences[DataStoreKeys.EVENING_REVIEW_TIME] = time.toString()
        }
    }

    override suspend fun setQuietHours(start: LocalTime, end: LocalTime) {
        dataStore.edit { preferences ->
            preferences[DataStoreKeys.QUIET_HOURS_START] = start.toString()
            preferences[DataStoreKeys.QUIET_HOURS_END] = end.toString()
        }
    }

    override suspend fun setExactAlarmAcknowledged(acknowledged: Boolean) {
        dataStore.edit { preferences ->
            preferences[DataStoreKeys.EXACT_ALARM_ACKNOWLEDGED] = acknowledged
        }
    }

    override suspend fun setWeekStartDay(weekStartDay: WeekStartDay) {
        dataStore.edit { preferences ->
            preferences[DataStoreKeys.WEEK_START_DAY] = weekStartDay.name
        }
    }

    override suspend fun setDateFormatPreference(dateFormat: DateFormatPreference) {
        dataStore.edit { preferences ->
            preferences[DataStoreKeys.DATE_FORMAT_PREFERENCE] = dateFormat.name
        }
    }

    override suspend fun setTimeFormatPreference(timeFormat: TimeFormatPreference) {
        dataStore.edit { preferences ->
            preferences[DataStoreKeys.TIME_FORMAT_PREFERENCE] = timeFormat.name
        }
    }

    override suspend fun setNotificationSoundEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DataStoreKeys.NOTIFICATION_SOUND] = enabled
        }
    }

    override suspend fun setNotificationVibrationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DataStoreKeys.NOTIFICATION_VIBRATION] = enabled
        }
    }

    override suspend fun setDefaultReminderPrecision(precision: ReminderPrecision) {
        dataStore.edit { preferences ->
            preferences[DataStoreKeys.DEFAULT_REMINDER_PRECISION] = precision.name
        }
    }

    override suspend fun setDefaultSnoozeMinutes(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[DataStoreKeys.DEFAULT_SNOOZE_MINUTES] = minutes
        }
    }

    override suspend fun completeOnboarding(
        name: String,
        theme: ThemePreference,
        briefingTime: LocalTime,
        reviewTime: LocalTime,
        quietStart: LocalTime,
        quietEnd: LocalTime
    ) {
        dataStore.edit { preferences ->
            preferences[DataStoreKeys.USER_NAME] = name
            preferences[DataStoreKeys.THEME_PREFERENCE] = theme.name
            preferences[DataStoreKeys.MORNING_BRIEFING_TIME] = briefingTime.toString()
            preferences[DataStoreKeys.EVENING_REVIEW_TIME] = reviewTime.toString()
            preferences[DataStoreKeys.QUIET_HOURS_START] = quietStart.toString()
            preferences[DataStoreKeys.QUIET_HOURS_END] = quietEnd.toString()
            preferences[DataStoreKeys.IS_ONBOARDING_COMPLETED] = true
        }
    }

    override suspend fun skipOnboarding() {
        dataStore.edit { preferences ->
            preferences[DataStoreKeys.IS_ONBOARDING_COMPLETED] = true
        }
    }

    private fun parseTheme(name: String?): ThemePreference {
        return try {
            if (name != null) ThemePreference.valueOf(name) else ThemePreference.DARK
        } catch (e: IllegalArgumentException) {
            ThemePreference.DARK
        }
    }

    private inline fun <reified T : Enum<T>> parseEnum(name: String?, defaultValue: T): T {
        if (name.isNullOrBlank()) return defaultValue
        return try {
            enumValueOf<T>(name)
        } catch (_: IllegalArgumentException) {
            defaultValue
        }
    }

    private fun parseLocalTime(timeStr: String?, defaultTime: LocalTime): LocalTime {
        if (timeStr.isNullOrBlank()) return defaultTime
        return try {
            LocalTime.parse(timeStr)
        } catch (e: DateTimeParseException) {
            defaultTime
        }
    }
}
