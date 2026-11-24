package com.dayflow.app.data.mapper

import com.dayflow.app.data.local.entity.ReminderEntity
import com.dayflow.app.data.local.entity.ReminderOccurrenceEntity
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderOccurrence

fun ReminderEntity.toDomain(): Reminder = Reminder(
    id = id,
    title = title,
    description = description,
    categoryId = categoryId,
    type = type,
    startDate = startDate,
    startTime = startTime,
    intervalMinutes = intervalMinutes,
    activeDaysMask = activeDaysMask,
    endTime = endTime,
    endDate = endDate,
    priority = priority,
    precision = precision,
    isEnabled = isEnabled,
    isPaused = isPaused,
    timeZoneId = timeZoneId,
    anchorDate = anchorDate,
    anchorTime = anchorTime,
    lastCompletedAt = lastCompletedAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Reminder.toEntity(): ReminderEntity = ReminderEntity(
    id = id,
    title = title,
    description = description,
    categoryId = categoryId,
    type = type,
    startDate = startDate,
    startTime = startTime,
    intervalMinutes = intervalMinutes,
    activeDaysMask = activeDaysMask,
    endTime = endTime,
    endDate = endDate,
    priority = priority,
    precision = precision,
    isEnabled = isEnabled,
    isPaused = isPaused,
    timeZoneId = timeZoneId,
    anchorDate = anchorDate,
    anchorTime = anchorTime,
    lastCompletedAt = lastCompletedAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ReminderOccurrenceEntity.toDomain(): ReminderOccurrence = ReminderOccurrence(
    id = id,
    reminderId = reminderId,
    scheduledAt = scheduledAt,
    status = status,
    snoozedUntil = snoozedUntil,
    deliveredAt = deliveredAt,
    handledAt = handledAt,
    alarmRequestCode = alarmRequestCode
)

fun ReminderOccurrence.toEntity(): ReminderOccurrenceEntity = ReminderOccurrenceEntity(
    id = id,
    reminderId = reminderId,
    scheduledAt = scheduledAt,
    status = status,
    snoozedUntil = snoozedUntil,
    deliveredAt = deliveredAt,
    handledAt = handledAt,
    alarmRequestCode = alarmRequestCode
)
