package com.dayflow.app.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

enum class HabitTrackingType {
    BINARY,
    QUANTITY,
    DURATION,
    REPETITION
}

data class Habit(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val categoryId: String? = null,
    val trackingType: HabitTrackingType = HabitTrackingType.BINARY,
    val targetValue: Double = 1.0,
    val unit: String? = null,
    val activeDaysMask: Int = 127, // All 7 days default
    val reminderPreference: Boolean = false,
    val reminderTime: LocalTime? = null,
    val isEnabled: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

data class HabitLog(
    val id: String = UUID.randomUUID().toString(),
    val habitId: String,
    val localDate: LocalDate,
    val completedValue: Double = 0.0,
    val isCompleted: Boolean = false,
    val loggedAt: Instant = Instant.now()
)
