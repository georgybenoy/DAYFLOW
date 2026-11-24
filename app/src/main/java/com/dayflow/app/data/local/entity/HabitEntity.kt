package com.dayflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dayflow.app.domain.model.HabitTrackingType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Entity(
    tableName = "habits",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("categoryId"),
        Index("isEnabled")
    ]
)
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val categoryId: String?,
    val trackingType: HabitTrackingType,
    val targetValue: Double,
    val unit: String?,
    val activeDaysMask: Int,
    val reminderPreference: Boolean,
    val reminderTime: LocalTime?,
    val isEnabled: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habitId", "localDate"], unique = true),
        Index("habitId"),
        Index("localDate")
    ]
)
data class HabitLogEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val localDate: LocalDate,
    val completedValue: Double,
    val isCompleted: Boolean,
    val loggedAt: Instant
)
