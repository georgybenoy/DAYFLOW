package com.dayflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.TaskStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Entity(
    tableName = "tasks",
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
        Index("status"),
        Index("deadlineDate"),
        Index("plannedDate"),
        Index("priority")
    ]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val categoryId: String?,
    val priority: Priority,
    val deadlineDate: LocalDate?,
    val deadlineTime: LocalTime?,
    val estimatedDurationMinutes: Int?,
    val plannedDate: LocalDate?,
    val plannedStart: LocalTime?,
    val plannedEnd: LocalTime?,
    val status: TaskStatus,
    val shouldCarryForward: Boolean,
    val createdAt: Instant,
    val completedAt: Instant?,
    val updatedAt: Instant
)

@Entity(
    tableName = "task_subtasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("taskId"),
        Index(value = ["taskId", "orderIndex"])
    ]
)
data class TaskSubtaskEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val title: String,
    val isCompleted: Boolean,
    val orderIndex: Int,
    val createdAt: Instant
)
