package com.dayflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dayflow.app.domain.model.OccurrenceStatus
import java.time.Instant

@Entity(
    tableName = "reminder_occurrences",
    foreignKeys = [
        ForeignKey(
            entity = ReminderEntity::class,
            parentColumns = ["id"],
            childColumns = ["reminderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["reminderId", "scheduledAt"], unique = true),
        Index("reminderId"),
        Index(value = ["status", "scheduledAt"])
    ]
)
data class ReminderOccurrenceEntity(
    @PrimaryKey val id: String,
    val reminderId: String,
    val scheduledAt: Instant,
    val status: OccurrenceStatus,
    val snoozedUntil: Instant?,
    val deliveredAt: Instant?,
    val handledAt: Instant?,
    val alarmRequestCode: Int
)
