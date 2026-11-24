package com.dayflow.app.domain.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class ReviewDecisionType {
    COMPLETED,
    RESCHEDULED,
    CARRIED_FORWARD,
    EXCLUDED,
    CANCELLED
}

@Serializable
data class TaskReviewDecision(
    val taskId: String,
    val taskTitle: String,
    val decision: ReviewDecisionType,
    val rescheduledToDate: String? = null,
    val timestampMillis: Long = System.currentTimeMillis()
)

data class DailyReview(
    val id: String = UUID.randomUUID().toString(),
    val localDate: LocalDate,
    val notes: String? = null,
    val reviewedAt: Instant = Instant.now(),
    val tasksCompletedCount: Int = 0,
    val tasksUnfinishedCount: Int = 0,
    val habitsCompletedCount: Int = 0,
    val decisions: List<TaskReviewDecision> = emptyList()
)

data class AppSetting(
    val key: String,
    val value: String,
    val updatedAt: Instant = Instant.now()
)
