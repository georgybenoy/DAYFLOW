package com.dayflow.app.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

enum class PlanStatus {
    DRAFT,
    APPROVED
}

enum class PlanItemSourceType {
    TASK,
    HABIT,
    CUSTOM
}

data class DailyPlanItem(
    val id: String = UUID.randomUUID().toString(),
    val planId: String,
    val sourceType: PlanItemSourceType = PlanItemSourceType.TASK,
    val sourceId: String? = null,
    val title: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val orderIndex: Int = 0,
    val isCompleted: Boolean = false
)

data class DailyPlan(
    val id: String = UUID.randomUUID().toString(),
    val localDate: LocalDate,
    val status: PlanStatus = PlanStatus.DRAFT,
    val createdAt: Instant = Instant.now(),
    val approvedAt: Instant? = null,
    val updatedAt: Instant = Instant.now(),
    val items: List<DailyPlanItem> = emptyList()
)
