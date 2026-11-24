package com.dayflow.app.data.local.converter

import com.dayflow.app.domain.model.HabitTrackingType
import com.dayflow.app.domain.model.OccurrenceStatus
import com.dayflow.app.domain.model.PlanItemSourceType
import com.dayflow.app.domain.model.PlanStatus
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.ReminderPrecision
import com.dayflow.app.domain.model.ReminderType
import com.dayflow.app.domain.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class DayflowConvertersTest {

    private val converters = DayflowConverters()

    @Test
    fun instantConverter_roundtripPreservesEpochMillis() {
        val original = Instant.ofEpochMilli(1700000000000L)
        val millis = converters.fromInstant(original)
        val restored = converters.toInstant(millis)

        assertEquals(original, restored)
        assertNull(converters.fromInstant(null))
        assertNull(converters.toInstant(null))
    }

    @Test
    fun localDateConverter_roundtripPreservesDate() {
        val original = LocalDate.of(2026, 11, 18)
        val string = converters.fromLocalDate(original)
        val restored = converters.toLocalDate(string)

        assertEquals("2026-11-18", string)
        assertEquals(original, restored)
        assertNull(converters.fromLocalDate(null))
        assertNull(converters.toLocalDate(null))
    }

    @Test
    fun localTimeConverter_roundtripPreservesTime() {
        val original = LocalTime.of(14, 30)
        val string = converters.fromLocalTime(original)
        val restored = converters.toLocalTime(string)

        assertEquals("14:30", string)
        assertEquals(original, restored)
        assertNull(converters.fromLocalTime(null))
        assertNull(converters.toLocalTime(null))
    }

    @Test
    fun zoneIdConverter_roundtripPreservesZone() {
        val original = ZoneId.of("Asia/Kolkata")
        val string = converters.fromZoneId(original)
        val restored = converters.toZoneId(string)

        assertEquals(original, restored)
        assertNull(converters.fromZoneId(null))
        assertNull(converters.toZoneId(null))
    }

    @Test
    fun enumConverters_roundtripPreserveValues() {
        assertEquals(ReminderType.FIXED_INTERVAL, converters.toReminderType(converters.fromReminderType(ReminderType.FIXED_INTERVAL)))
        assertEquals(ReminderPrecision.EXACT, converters.toReminderPrecision(converters.fromReminderPrecision(ReminderPrecision.EXACT)))
        assertEquals(OccurrenceStatus.DELIVERED, converters.toOccurrenceStatus(converters.fromOccurrenceStatus(OccurrenceStatus.DELIVERED)))
        assertEquals(Priority.URGENT, converters.toPriority(converters.fromPriority(Priority.URGENT)))
        assertEquals(TaskStatus.COMPLETED, converters.toTaskStatus(converters.fromTaskStatus(TaskStatus.COMPLETED)))
        assertEquals(HabitTrackingType.QUANTITY, converters.toHabitTrackingType(converters.fromHabitTrackingType(HabitTrackingType.QUANTITY)))
        assertEquals(PlanStatus.APPROVED, converters.toPlanStatus(converters.fromPlanStatus(PlanStatus.APPROVED)))
        assertEquals(PlanItemSourceType.HABIT, converters.toPlanItemSourceType(converters.fromPlanItemSourceType(PlanItemSourceType.HABIT)))
    }
}
