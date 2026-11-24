package com.dayflow.app.domain.model

import java.time.LocalTime

sealed interface TaskValidationError {
    data object BlankTitle : TaskValidationError
    data object InvalidPlannedTimeWindow : TaskValidationError
    data object InvalidDuration : TaskValidationError
}

sealed interface TaskValidationResult {
    data object Valid : TaskValidationResult
    data class Invalid(val errors: Set<TaskValidationError>) : TaskValidationResult
}

object TaskValidator {
    fun validate(
        title: String,
        plannedStart: LocalTime?,
        plannedEnd: LocalTime?,
        estimatedDurationMinutes: Int?
    ): TaskValidationResult {
        val errors = mutableSetOf<TaskValidationError>()

        if (title.isBlank()) {
            errors.add(TaskValidationError.BlankTitle)
        }

        if (plannedStart != null && plannedEnd != null) {
            if (!plannedEnd.isAfter(plannedStart)) {
                errors.add(TaskValidationError.InvalidPlannedTimeWindow)
            }
        }

        if (estimatedDurationMinutes != null && estimatedDurationMinutes <= 0) {
            errors.add(TaskValidationError.InvalidDuration)
        }

        return if (errors.isEmpty()) {
            TaskValidationResult.Valid
        } else {
            TaskValidationResult.Invalid(errors)
        }
    }
}
