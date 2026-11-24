package com.dayflow.app.domain.model

enum class DeadlineReminderLeadTime(val minutesBefore: Long, val label: String) {
    AT_DEADLINE(0, "At deadline"),
    ONE_HOUR_BEFORE(60, "1 hour before"),
    TWO_HOURS_BEFORE(120, "2 hours before"),
    ONE_DAY_BEFORE(1440, "1 day before")
}
