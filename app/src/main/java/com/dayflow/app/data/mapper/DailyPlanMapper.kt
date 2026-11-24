package com.dayflow.app.data.mapper

import com.dayflow.app.data.local.entity.DailyPlanEntity
import com.dayflow.app.data.local.entity.DailyPlanItemEntity
import com.dayflow.app.domain.model.DailyPlan
import com.dayflow.app.domain.model.DailyPlanItem

fun DailyPlanEntity.toDomain(items: List<DailyPlanItemEntity> = emptyList()): DailyPlan = DailyPlan(
    id = id,
    localDate = localDate,
    status = status,
    createdAt = createdAt,
    approvedAt = approvedAt,
    updatedAt = updatedAt,
    items = items.map { it.toDomain() }
)

fun DailyPlan.toEntity(): DailyPlanEntity = DailyPlanEntity(
    id = id,
    localDate = localDate,
    status = status,
    createdAt = createdAt,
    approvedAt = approvedAt,
    updatedAt = updatedAt
)

fun DailyPlanItemEntity.toDomain(): DailyPlanItem = DailyPlanItem(
    id = id,
    planId = planId,
    sourceType = sourceType,
    sourceId = sourceId,
    title = title,
    startTime = startTime,
    endTime = endTime,
    orderIndex = orderIndex,
    isCompleted = isCompleted
)

fun DailyPlanItem.toEntity(): DailyPlanItemEntity = DailyPlanItemEntity(
    id = id,
    planId = planId,
    sourceType = sourceType,
    sourceId = sourceId,
    title = title,
    startTime = startTime,
    endTime = endTime,
    orderIndex = orderIndex,
    isCompleted = isCompleted
)
