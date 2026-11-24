package com.dayflow.app.domain.model

import java.time.DayOfWeek

enum class WeekStartDay(val dayOfWeek: DayOfWeek, val displayName: String) {
    MONDAY(DayOfWeek.MONDAY, "Monday"),
    SUNDAY(DayOfWeek.SUNDAY, "Sunday"),
    SATURDAY(DayOfWeek.SATURDAY, "Saturday")
}

enum class DateFormatPreference(val pattern: String, val displayName: String) {
    SYSTEM("", "System Default"),
    ISO_YYYY_MM_DD("yyyy-MM-dd", "2026-11-19 (YYYY-MM-DD)"),
    MM_DD_YYYY("MM/dd/yyyy", "11/19/2026 (MM/DD/YYYY)"),
    DD_MM_YYYY("dd/MM/yyyy", "19/11/2026 (DD/MM/YYYY)")
}

enum class TimeFormatPreference(val displayName: String) {
    SYSTEM("System Default"),
    H12("12-hour (e.g. 02:30 PM)"),
    H24("24-hour (e.g. 14:30)")
}
