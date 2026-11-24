package com.dayflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dayflow.app.domain.model.PlanItemSourceType
import com.dayflow.app.domain.model.PlanStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Entity(
    tableName = "daily_plans",
    indices = [
        Index(value = ["localDate"], unique = true)
    ]
)
data class DailyPlanEntity(
    @PrimaryKey val id: String,
    val localDate: LocalDate,
    val status: PlanStatus,
    val createdAt: Instant,
    val approvedAt: Instant?,
    val updatedAt: Instant
)

@Entity(
    tableName = "daily_plan_items",
    foreignKeys = [
        ForeignKey(
            entity = DailyPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("planId"),
        Index(value = ["planId", "orderIndex"]),
        Index(value = ["sourceType", "sourceId"])
    ]
)
data class DailyPlanItemEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val sourceType: PlanItemSourceType,
    val sourceId: String?,
    val title: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val orderIndex: Int,
    val isCompleted: Boolean
)
