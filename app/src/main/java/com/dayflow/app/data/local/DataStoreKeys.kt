package com.dayflow.app.data.local

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object DataStoreKeys {
    val USER_NAME = stringPreferencesKey("user_name")
    val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
    val MORNING_BRIEFING_TIME = stringPreferencesKey("morning_briefing_time")
    val EVENING_REVIEW_TIME = stringPreferencesKey("evening_review_time")
    val QUIET_HOURS_START = stringPreferencesKey("quiet_hours_start")
    val QUIET_HOURS_END = stringPreferencesKey("quiet_hours_end")
    val IS_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
    val EXACT_ALARM_ACKNOWLEDGED = booleanPreferencesKey("exact_alarm_acknowledged")

    // Backup & Restore
    val LAST_BACKUP_TIME = stringPreferencesKey("last_backup_time")
    val LAST_BACKUP_STATUS = stringPreferencesKey("last_backup_status")
    val LAST_BACKUP_SNAPSHOT_NAME = stringPreferencesKey("last_backup_snapshot_name")
    val BACKUP_RETENTION_COUNT = androidx.datastore.preferences.core.intPreferencesKey("backup_retention_count")

    // Calendar & App Preferences
    val WEEK_START_DAY = stringPreferencesKey("week_start_day")
    val DATE_FORMAT_PREFERENCE = stringPreferencesKey("date_format_preference")
    val TIME_FORMAT_PREFERENCE = stringPreferencesKey("time_format_preference")
    val NOTIFICATION_SOUND = booleanPreferencesKey("notification_sound")
    val NOTIFICATION_VIBRATION = booleanPreferencesKey("notification_vibration")
    val DEFAULT_REMINDER_PRECISION = stringPreferencesKey("default_reminder_precision")
    val DEFAULT_SNOOZE_MINUTES = androidx.datastore.preferences.core.intPreferencesKey("default_snooze_minutes")
}
