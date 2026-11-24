package com.dayflow.app.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

data class Reminder(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String? = null,
    val categoryId: String? = null,
    val type: ReminderType = ReminderType.ONE_TIME,
    val startDate: LocalDate,
    val startTime: LocalTime,
    val intervalMinutes: Int? = null,
    val activeDaysMask: Int = 127, // All 7 days default (bits 0..6)
    val endTime: LocalTime? = null,
    val endDate: LocalDate? = null,
    val priority: Priority = Priority.MEDIUM,
    val precision: ReminderPrecision = ReminderPrecision.EXACT,
    val isEnabled: Boolean = true,
    val isPaused: Boolean = false,
    val timeZoneId: ZoneId = ZoneId.systemDefault(),
    val anchorDate: LocalDate? = null,
    val anchorTime: LocalTime? = null,
    val lastCompletedAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

data class ReminderOccurrence(
    val id: String = UUID.randomUUID().toString(),
    val reminderId: String,
    val scheduledAt: Instant,
    val status: OccurrenceStatus = OccurrenceStatus.SCHEDULED,
    val snoozedUntil: Instant? = null,
    val deliveredAt: Instant? = null,
    val handledAt: Instant? = null,
    val alarmRequestCode: Int = (scheduledAt.toEpochMilli() % Int.MAX_VALUE).toInt()
)
