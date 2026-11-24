package com.dayflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.ReminderPrecision
import com.dayflow.app.domain.model.ReminderType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@Entity(
    tableName = "reminders",
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
        Index(value = ["isEnabled", "isPaused"]),
        Index("priority")
    ]
)
data class ReminderEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val categoryId: String?,
    val type: ReminderType,
    val startDate: LocalDate,
    val startTime: LocalTime,
    val intervalMinutes: Int?,
    val activeDaysMask: Int,
    val endTime: LocalTime?,
    val endDate: LocalDate?,
    val priority: Priority,
    val precision: ReminderPrecision,
    val isEnabled: Boolean,
    val isPaused: Boolean,
    val timeZoneId: ZoneId,
    val anchorDate: LocalDate?,
    val anchorTime: LocalTime?,
    val lastCompletedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant
)
