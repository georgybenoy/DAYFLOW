package com.dayflow.app.data.mapper

import com.dayflow.app.data.local.entity.TaskEntity
import com.dayflow.app.data.local.entity.TaskSubtaskEntity
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskSubtask

fun TaskEntity.toDomain(subtasks: List<TaskSubtaskEntity> = emptyList()): Task = Task(
    id = id,
    title = title,
    description = description,
    categoryId = categoryId,
    priority = priority,
    deadlineDate = deadlineDate,
    deadlineTime = deadlineTime,
    estimatedDurationMinutes = estimatedDurationMinutes,
    plannedDate = plannedDate,
    plannedStart = plannedStart,
    plannedEnd = plannedEnd,
    status = status,
    shouldCarryForward = shouldCarryForward,
    createdAt = createdAt,
    completedAt = completedAt,
    updatedAt = updatedAt,
    subtasks = subtasks.map { it.toDomain() }
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    title = title,
    description = description,
    categoryId = categoryId,
    priority = priority,
    deadlineDate = deadlineDate,
    deadlineTime = deadlineTime,
    estimatedDurationMinutes = estimatedDurationMinutes,
    plannedDate = plannedDate,
    plannedStart = plannedStart,
    plannedEnd = plannedEnd,
    status = status,
    shouldCarryForward = shouldCarryForward,
    createdAt = createdAt,
    completedAt = completedAt,
    updatedAt = updatedAt
)

fun TaskSubtaskEntity.toDomain(): TaskSubtask = TaskSubtask(
    id = id,
    taskId = taskId,
    title = title,
    isCompleted = isCompleted,
    orderIndex = orderIndex,
    createdAt = createdAt
)

fun TaskSubtask.toEntity(): TaskSubtaskEntity = TaskSubtaskEntity(
    id = id,
    taskId = taskId,
    title = title,
    isCompleted = isCompleted,
    orderIndex = orderIndex,
    createdAt = createdAt
)
