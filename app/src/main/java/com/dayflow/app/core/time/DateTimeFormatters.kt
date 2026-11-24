package com.dayflow.app.core.time

import com.dayflow.app.domain.model.DateFormatPreference
import com.dayflow.app.domain.model.TimeFormatPreference
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeFormatters {
    private val fullDateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault())
    private val shortDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
    private val time12Formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
    private val time24Formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    fun formatFullDate(date: LocalDate): String = date.format(fullDateFormatter)

    fun formatShortDate(date: LocalDate, preference: DateFormatPreference = DateFormatPreference.SYSTEM): String {
        return when (preference) {
            DateFormatPreference.SYSTEM -> date.format(shortDateFormatter)
            DateFormatPreference.ISO_YYYY_MM_DD -> date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            DateFormatPreference.MM_DD_YYYY -> date.format(DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.getDefault()))
            DateFormatPreference.DD_MM_YYYY -> date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault()))
        }
    }

    fun formatTime(time: LocalTime, preference: TimeFormatPreference = TimeFormatPreference.H12): String {
        return when (preference) {
            TimeFormatPreference.SYSTEM, TimeFormatPreference.H12 -> time.format(time12Formatter)
            TimeFormatPreference.H24 -> time.format(time24Formatter)
        }
    }

    fun formatDateTime(
        instant: Instant,
        zoneId: ZoneId = ZoneId.systemDefault(),
        datePreference: DateFormatPreference = DateFormatPreference.SYSTEM,
        timePreference: TimeFormatPreference = TimeFormatPreference.H12
    ): String {
        val zdt = instant.atZone(zoneId)
        val formattedDate = formatShortDate(zdt.toLocalDate(), datePreference)
        val formattedTime = formatTime(zdt.toLocalTime(), timePreference)
        return "$formattedDate at $formattedTime"
    }
}
