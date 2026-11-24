package com.dayflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "daily_reviews",
    indices = [
        Index(value = ["localDate"], unique = true)
    ]
)
data class DailyReviewEntity(
    @PrimaryKey val id: String,
    val localDate: LocalDate,
    val notes: String?,
    val reviewedAt: Instant,
    val tasksCompletedCount: Int,
    val tasksUnfinishedCount: Int,
    val habitsCompletedCount: Int,
    val decisionsJson: String
)

@Entity(
    tableName = "app_settings"
)
data class AppSettingEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Instant
)
