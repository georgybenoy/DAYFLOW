package com.dayflow.app.domain.model

enum class ReminderType {
    ONE_TIME,
    DAILY,
    WEEKLY,
    CUSTOM_WEEKDAYS,
    FIXED_INTERVAL,
    COMPLETION_INTERVAL
}

enum class ReminderPrecision {
    EXACT,
    APPROXIMATE
}
