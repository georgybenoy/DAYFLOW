package com.dayflow.app.data.mapper

import com.dayflow.app.data.local.entity.HabitEntity
import com.dayflow.app.data.local.entity.HabitLogEntity
import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.HabitLog

fun HabitEntity.toDomain(): Habit = Habit(
    id = id,
    name = name,
    description = description,
    categoryId = categoryId,
    trackingType = trackingType,
    targetValue = targetValue,
    unit = unit,
    activeDaysMask = activeDaysMask,
    reminderPreference = reminderPreference,
    reminderTime = reminderTime,
    isEnabled = isEnabled,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Habit.toEntity(): HabitEntity = HabitEntity(
    id = id,
    name = name,
    description = description,
    categoryId = categoryId,
    trackingType = trackingType,
    targetValue = targetValue,
    unit = unit,
    activeDaysMask = activeDaysMask,
    reminderPreference = reminderPreference,
    reminderTime = reminderTime,
    isEnabled = isEnabled,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun HabitLogEntity.toDomain(): HabitLog = HabitLog(
    id = id,
    habitId = habitId,
    localDate = localDate,
    completedValue = completedValue,
    isCompleted = isCompleted,
    loggedAt = loggedAt
)

fun HabitLog.toEntity(): HabitLogEntity = HabitLogEntity(
    id = id,
    habitId = habitId,
    localDate = localDate,
    completedValue = completedValue,
    isCompleted = isCompleted,
    loggedAt = loggedAt
)
