package com.dayflow.app.domain.recurrence

import com.dayflow.app.domain.model.ReminderType
import java.time.LocalDate
import java.time.LocalTime

sealed interface ReminderValidationError {
    data object BlankTitle : ReminderValidationError
    data object MissingInterval : ReminderValidationError
    data object IntervalTooShort : ReminderValidationError
    data object MissingWindowTimes : ReminderValidationError
    data object NoWeekdaysSelected : ReminderValidationError
    data object EndDateBeforeStartDate : ReminderValidationError
}

sealed interface ReminderValidationResult {
    data object Valid : ReminderValidationResult
    data class Invalid(val errors: Set<ReminderValidationError>) : ReminderValidationResult
}

object ReminderValidator {
    fun validate(
        title: String,
        type: ReminderType,
        startDate: LocalDate,
        endDate: LocalDate?,
        intervalMinutes: Int?,
        activeDaysMask: Int,
        startTime: LocalTime?,
        endTime: LocalTime?
    ): ReminderValidationResult {
        val errors = mutableSetOf<ReminderValidationError>()

        if (title.trim().isBlank()) {
            errors.add(ReminderValidationError.BlankTitle)
        }

        if (type == ReminderType.FIXED_INTERVAL || type == ReminderType.COMPLETION_INTERVAL) {
            if (intervalMinutes == null) {
                errors.add(ReminderValidationError.MissingInterval)
            } else if (intervalMinutes < 1) {
                errors.add(ReminderValidationError.IntervalTooShort)
            }
            if (startTime == null || endTime == null) {
                errors.add(ReminderValidationError.MissingWindowTimes)
            }
        }

        if (type == ReminderType.CUSTOM_WEEKDAYS) {
            if ((activeDaysMask and 127) == 0) {
                errors.add(ReminderValidationError.NoWeekdaysSelected)
            }
        }

        if (endDate != null && endDate.isBefore(startDate)) {
            errors.add(ReminderValidationError.EndDateBeforeStartDate)
        }

        return if (errors.isEmpty()) {
            ReminderValidationResult.Valid
        } else {
            ReminderValidationResult.Invalid(errors)
        }
    }
}
