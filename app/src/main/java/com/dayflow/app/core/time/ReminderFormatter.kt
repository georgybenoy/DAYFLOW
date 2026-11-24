package com.dayflow.app.core.time

import com.dayflow.app.domain.model.OccurrenceStatus
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object ReminderFormatter {

    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
    private val time24Formatter = DateTimeFormatter.ofPattern("HH:mm")
    private val shortDateFormatter = DateTimeFormatter.ofPattern("MMM d")
    private val fullDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    fun formatRecurrenceSummary(reminder: Reminder): String {
        return when (reminder.type) {
            ReminderType.ONE_TIME -> {
                val dateStr = reminder.startDate.format(shortDateFormatter)
                val timeStr = reminder.startTime.format(timeFormatter)
                "One-time · $dateStr at $timeStr"
            }
            ReminderType.DAILY -> {
                val timeStr = reminder.startTime.format(timeFormatter)
                "Daily at $timeStr"
            }
            ReminderType.WEEKLY -> {
                val dayOfWeek = reminder.startDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
                val timeStr = reminder.startTime.format(timeFormatter)
                "Weekly on $dayOfWeek at $timeStr"
            }
            ReminderType.CUSTOM_WEEKDAYS -> {
                val days = formatActiveDays(reminder.activeDaysMask)
                val timeStr = reminder.startTime.format(timeFormatter)
                "Every $days at $timeStr"
            }
            ReminderType.FIXED_INTERVAL -> {
                val intervalStr = formatMinutes(reminder.intervalMinutes ?: 60)
                val startStr = reminder.startTime.format(time24Formatter)
                val endStr = reminder.endTime?.format(time24Formatter) ?: "23:59"
                "Every $intervalStr ($startStr - $endStr)"
            }
            ReminderType.COMPLETION_INTERVAL -> {
                val intervalStr = formatMinutes(reminder.intervalMinutes ?: 60)
                val startStr = reminder.startTime.format(time24Formatter)
                val endStr = reminder.endTime?.format(time24Formatter) ?: "23:59"
                "$intervalStr after completion ($startStr - $endStr)"
            }
        }
    }

    fun formatNextDelivery(instant: Instant?, zoneId: ZoneId = ZoneId.systemDefault()): String {
        if (instant == null) return "No upcoming delivery"

        val zonedDateTime = instant.atZone(zoneId)
        val today = LocalDate.now(zoneId)
        val targetDate = zonedDateTime.toLocalDate()
        val timeStr = zonedDateTime.toLocalTime().format(timeFormatter)

        val daysDiff = ChronoUnit.DAYS.between(today, targetDate)
        return when (daysDiff) {
            0L -> "Today at $timeStr"
            1L -> "Tomorrow at $timeStr"
            -1L -> "Yesterday at $timeStr"
            in 2L..6L -> "${targetDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }} at $timeStr"
            else -> "${targetDate.format(shortDateFormatter)} at $timeStr"
        }
    }

    fun formatOccurrenceTime(instant: Instant, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val zonedDateTime = instant.atZone(zoneId)
        val today = LocalDate.now(zoneId)
        val targetDate = zonedDateTime.toLocalDate()
        val timeStr = zonedDateTime.toLocalTime().format(timeFormatter)

        return if (targetDate == today) {
            "Today, $timeStr"
        } else {
            "${targetDate.format(shortDateFormatter)}, $timeStr"
        }
    }

    private fun formatMinutes(minutes: Int): String {
        return when {
            minutes % 60 == 0 && minutes >= 60 -> {
                val hours = minutes / 60
                if (hours == 1) "1 hour" else "$hours hours"
            }
            else -> "$minutes mins"
        }
    }

    private fun formatActiveDays(mask: Int): String {
        if (mask and 127 == 127) return "Daily"
        if (mask and 127 == 62) return "Weekdays" // Mon-Fri
        if (mask and 127 == 65) return "Weekends" // Sat-Sun

        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val active = mutableListOf<String>()
        for (i in 0..6) {
            if ((mask and (1 shl i)) != 0) {
                active.add(dayNames[i])
            }
        }
        return if (active.isEmpty()) "None" else active.joinToString(", ")
    }
}
