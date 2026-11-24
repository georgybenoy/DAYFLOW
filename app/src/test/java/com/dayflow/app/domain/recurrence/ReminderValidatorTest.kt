package com.dayflow.app.domain.recurrence

import com.dayflow.app.domain.model.ReminderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ReminderValidatorTest {

    private val baseDate = LocalDate.of(2026, 11, 18)
    private val baseTime = LocalTime.of(10, 0)

    @Test
    fun blankTitle_returnsBlankTitleError() {
        val result = ReminderValidator.validate(
            title = "   ",
            type = ReminderType.ONE_TIME,
            startDate = baseDate,
            endDate = null,
            intervalMinutes = null,
            activeDaysMask = 127,
            startTime = baseTime,
            endTime = null
        )

        assertTrue(result is ReminderValidationResult.Invalid)
        val errors = (result as ReminderValidationResult.Invalid).errors
        assertTrue(errors.contains(ReminderValidationError.BlankTitle))
    }

    @Test
    fun fixedInterval_missingOrZeroInterval_returnsIntervalError() {
        val resultNull = ReminderValidator.validate(
            title = "Drink Water",
            type = ReminderType.FIXED_INTERVAL,
            startDate = baseDate,
            endDate = null,
            intervalMinutes = null,
            activeDaysMask = 127,
            startTime = baseTime,
            endTime = LocalTime.of(22, 0)
        )

        assertTrue(resultNull is ReminderValidationResult.Invalid)
        assertTrue((resultNull as ReminderValidationResult.Invalid).errors.contains(ReminderValidationError.MissingInterval))

        val resultZero = ReminderValidator.validate(
            title = "Drink Water",
            type = ReminderType.FIXED_INTERVAL,
            startDate = baseDate,
            endDate = null,
            intervalMinutes = 0,
            activeDaysMask = 127,
            startTime = baseTime,
            endTime = LocalTime.of(22, 0)
        )
        assertTrue(resultZero is ReminderValidationResult.Invalid)
        assertTrue((resultZero as ReminderValidationResult.Invalid).errors.contains(ReminderValidationError.IntervalTooShort))
    }

    @Test
    fun fixedInterval_missingWindow_returnsMissingWindowError() {
        val result = ReminderValidator.validate(
            title = "Drink Water",
            type = ReminderType.FIXED_INTERVAL,
            startDate = baseDate,
            endDate = null,
            intervalMinutes = 120,
            activeDaysMask = 127,
            startTime = baseTime,
            endTime = null
        )

        assertTrue(result is ReminderValidationResult.Invalid)
        assertTrue((result as ReminderValidationResult.Invalid).errors.contains(ReminderValidationError.MissingWindowTimes))
    }

    @Test
    fun customWeekdays_noDaysSelected_returnsNoWeekdaysSelectedError() {
        val result = ReminderValidator.validate(
            title = "Gym Workout",
            type = ReminderType.CUSTOM_WEEKDAYS,
            startDate = baseDate,
            endDate = null,
            intervalMinutes = null,
            activeDaysMask = 0,
            startTime = baseTime,
            endTime = null
        )

        assertTrue(result is ReminderValidationResult.Invalid)
        assertTrue((result as ReminderValidationResult.Invalid).errors.contains(ReminderValidationError.NoWeekdaysSelected))
    }

    @Test
    fun endDateBeforeStartDate_returnsEndDateBeforeStartDateError() {
        val result = ReminderValidator.validate(
            title = "Project Sprint",
            type = ReminderType.DAILY,
            startDate = baseDate,
            endDate = baseDate.minusDays(1),
            intervalMinutes = null,
            activeDaysMask = 127,
            startTime = baseTime,
            endTime = null
        )

        assertTrue(result is ReminderValidationResult.Invalid)
        assertTrue((result as ReminderValidationResult.Invalid).errors.contains(ReminderValidationError.EndDateBeforeStartDate))
    }

    @Test
    fun validConfigurations_returnValid() {
        val validOneTime = ReminderValidator.validate(
            title = "Doctor Appointment",
            type = ReminderType.ONE_TIME,
            startDate = baseDate,
            endDate = null,
            intervalMinutes = null,
            activeDaysMask = 127,
            startTime = baseTime,
            endTime = null
        )
        assertEquals(ReminderValidationResult.Valid, validOneTime)

        val validWater = ReminderValidator.validate(
            title = "Drink Water",
            type = ReminderType.FIXED_INTERVAL,
            startDate = baseDate,
            endDate = null,
            intervalMinutes = 120,
            activeDaysMask = 127,
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(22, 0)
        )
        assertEquals(ReminderValidationResult.Valid, validWater)
    }
}
