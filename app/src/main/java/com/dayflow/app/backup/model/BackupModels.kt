package com.dayflow.app.backup.model

import com.dayflow.app.data.local.entity.AppSettingEntity
import com.dayflow.app.data.local.entity.CategoryEntity
import com.dayflow.app.data.local.entity.DailyPlanEntity
import com.dayflow.app.data.local.entity.DailyPlanItemEntity
import com.dayflow.app.data.local.entity.DailyReviewEntity
import com.dayflow.app.data.local.entity.HabitEntity
import com.dayflow.app.data.local.entity.HabitLogEntity
import com.dayflow.app.data.local.entity.ReminderEntity
import com.dayflow.app.data.local.entity.ReminderOccurrenceEntity
import com.dayflow.app.data.local.entity.TaskEntity
import com.dayflow.app.data.local.entity.TaskSubtaskEntity
import com.dayflow.app.domain.model.HabitTrackingType
import com.dayflow.app.domain.model.OccurrenceStatus
import com.dayflow.app.domain.model.PlanItemSourceType
import com.dayflow.app.domain.model.PlanStatus
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.ReminderPrecision
import com.dayflow.app.domain.model.ReminderType
import com.dayflow.app.domain.model.TaskStatus
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@Serializable
data class BackupCategory(
    val id: String,
    val name: String,
    val colorHex: String,
    val iconName: String,
    val isDefault: Boolean = false,
    val createdAtEpochMillis: Long
)

@Serializable
data class BackupReminder(
    val id: String,
    val title: String,
    val description: String? = null,
    val categoryId: String? = null,
    val type: String,
    val startDate: String,
    val startTime: String,
    val intervalMinutes: Int? = null,
    val activeDaysMask: Int = 127,
    val endTime: String? = null,
    val endDate: String? = null,
    val priority: String,
    val precision: String,
    val isEnabled: Boolean = true,
    val isPaused: Boolean = false,
    val timeZoneId: String,
    val anchorDate: String? = null,
    val anchorTime: String? = null,
    val lastCompletedAtEpochMillis: Long? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)

@Serializable
data class BackupReminderOccurrence(
    val id: String,
    val reminderId: String,
    val scheduledAtEpochMillis: Long,
    val status: String,
    val snoozedUntilEpochMillis: Long? = null,
    val deliveredAtEpochMillis: Long? = null,
    val handledAtEpochMillis: Long? = null,
    val alarmRequestCode: Int
)

@Serializable
data class BackupTask(
    val id: String,
    val title: String,
    val description: String? = null,
    val categoryId: String? = null,
    val priority: String,
    val deadlineDate: String? = null,
    val deadlineTime: String? = null,
    val estimatedDurationMinutes: Int? = null,
    val plannedDate: String? = null,
    val plannedStart: String? = null,
    val plannedEnd: String? = null,
    val status: String,
    val shouldCarryForward: Boolean = true,
    val createdAtEpochMillis: Long,
    val completedAtEpochMillis: Long? = null,
    val updatedAtEpochMillis: Long
)

@Serializable
data class BackupTaskSubtask(
    val id: String,
    val taskId: String,
    val title: String,
    val isCompleted: Boolean = false,
    val orderIndex: Int = 0,
    val createdAtEpochMillis: Long
)

@Serializable
data class BackupHabit(
    val id: String,
    val name: String,
    val description: String? = null,
    val categoryId: String? = null,
    val trackingType: String,
    val targetValue: Double,
    val unit: String? = null,
    val activeDaysMask: Int = 127,
    val reminderPreference: Boolean = false,
    val reminderTime: String? = null,
    val isEnabled: Boolean = true,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)

@Serializable
data class BackupHabitLog(
    val id: String,
    val habitId: String,
    val localDate: String,
    val completedValue: Double,
    val isCompleted: Boolean,
    val loggedAtEpochMillis: Long
)

@Serializable
data class BackupDailyPlan(
    val id: String,
    val localDate: String,
    val status: String,
    val createdAtEpochMillis: Long,
    val approvedAtEpochMillis: Long? = null,
    val updatedAtEpochMillis: Long
)

@Serializable
data class BackupDailyPlanItem(
    val id: String,
    val planId: String,
    val sourceType: String,
    val sourceId: String? = null,
    val title: String,
    val startTime: String,
    val endTime: String,
    val orderIndex: Int = 0,
    val isCompleted: Boolean = false
)

@Serializable
data class BackupDailyReview(
    val id: String,
    val localDate: String,
    val notes: String? = null,
    val reviewedAtEpochMillis: Long,
    val tasksCompletedCount: Int,
    val tasksUnfinishedCount: Int,
    val habitsCompletedCount: Int,
    val decisionsJson: String
)

@Serializable
data class BackupAppSetting(
    val key: String,
    val value: String,
    val updatedAtEpochMillis: Long
)

@Serializable
data class BackupPayload(
    val schemaVersion: Int = 1,
    val appVersion: String = "1.0.0",
    val createdAt: Long, // Epoch millis UTC
    val categories: List<BackupCategory> = emptyList(),
    val reminders: List<BackupReminder> = emptyList(),
    val reminderOccurrences: List<BackupReminderOccurrence> = emptyList(),
    val tasks: List<BackupTask> = emptyList(),
    val subtasks: List<BackupTaskSubtask> = emptyList(),
    val habits: List<BackupHabit> = emptyList(),
    val habitLogs: List<BackupHabitLog> = emptyList(),
    val dailyPlans: List<BackupDailyPlan> = emptyList(),
    val planItems: List<BackupDailyPlanItem> = emptyList(),
    val reviews: List<BackupDailyReview> = emptyList(),
    val appSettings: List<BackupAppSetting> = emptyList(),
    val preferences: Map<String, String> = emptyMap()
)

@Serializable
data class BackupManifest(
    val schemaVersion: Int,
    val appVersion: String,
    val createdAt: Long,
    val dataSha256: String,
    val totalRecords: Int,
    val recordsCount: Map<String, Int>
)

// --- Lossless Conversion Extensions ---

fun CategoryEntity.toBackup() = BackupCategory(
    id = id,
    name = name,
    colorHex = colorHex,
    iconName = iconName,
    isDefault = isDefault,
    createdAtEpochMillis = createdAt.toEpochMilli()
)

fun BackupCategory.toEntity() = CategoryEntity(
    id = id,
    name = name,
    colorHex = colorHex,
    iconName = iconName,
    isDefault = isDefault,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis)
)

fun ReminderEntity.toBackup() = BackupReminder(
    id = id,
    title = title,
    description = description,
    categoryId = categoryId,
    type = type.name,
    startDate = startDate.toString(),
    startTime = startTime.toString(),
    intervalMinutes = intervalMinutes,
    activeDaysMask = activeDaysMask,
    endTime = endTime?.toString(),
    endDate = endDate?.toString(),
    priority = priority.name,
    precision = precision.name,
    isEnabled = isEnabled,
    isPaused = isPaused,
    timeZoneId = timeZoneId.id,
    anchorDate = anchorDate?.toString(),
    anchorTime = anchorTime?.toString(),
    lastCompletedAtEpochMillis = lastCompletedAt?.toEpochMilli(),
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli()
)

fun BackupReminder.toEntity() = ReminderEntity(
    id = id,
    title = title,
    description = description,
    categoryId = categoryId,
    type = ReminderType.valueOf(type),
    startDate = LocalDate.parse(startDate),
    startTime = LocalTime.parse(startTime),
    intervalMinutes = intervalMinutes,
    activeDaysMask = activeDaysMask,
    endTime = endTime?.let { LocalTime.parse(it) },
    endDate = endDate?.let { LocalDate.parse(it) },
    priority = Priority.valueOf(priority),
    precision = ReminderPrecision.valueOf(precision),
    isEnabled = isEnabled,
    isPaused = isPaused,
    timeZoneId = ZoneId.of(timeZoneId),
    anchorDate = anchorDate?.let { LocalDate.parse(it) },
    anchorTime = anchorTime?.let { LocalTime.parse(it) },
    lastCompletedAt = lastCompletedAtEpochMillis?.let { Instant.ofEpochMilli(it) },
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis)
)

fun ReminderOccurrenceEntity.toBackup() = BackupReminderOccurrence(
    id = id,
    reminderId = reminderId,
    scheduledAtEpochMillis = scheduledAt.toEpochMilli(),
    status = status.name,
    snoozedUntilEpochMillis = snoozedUntil?.toEpochMilli(),
    deliveredAtEpochMillis = deliveredAt?.toEpochMilli(),
    handledAtEpochMillis = handledAt?.toEpochMilli(),
    alarmRequestCode = alarmRequestCode
)

fun BackupReminderOccurrence.toEntity() = ReminderOccurrenceEntity(
    id = id,
    reminderId = reminderId,
    scheduledAt = Instant.ofEpochMilli(scheduledAtEpochMillis),
    status = OccurrenceStatus.valueOf(status),
    snoozedUntil = snoozedUntilEpochMillis?.let { Instant.ofEpochMilli(it) },
    deliveredAt = deliveredAtEpochMillis?.let { Instant.ofEpochMilli(it) },
    handledAt = handledAtEpochMillis?.let { Instant.ofEpochMilli(it) },
    alarmRequestCode = alarmRequestCode
)

fun TaskEntity.toBackup() = BackupTask(
    id = id,
    title = title,
    description = description,
    categoryId = categoryId,
    priority = priority.name,
    deadlineDate = deadlineDate?.toString(),
    deadlineTime = deadlineTime?.toString(),
    estimatedDurationMinutes = estimatedDurationMinutes,
    plannedDate = plannedDate?.toString(),
    plannedStart = plannedStart?.toString(),
    plannedEnd = plannedEnd?.toString(),
    status = status.name,
    shouldCarryForward = shouldCarryForward,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    completedAtEpochMillis = completedAt?.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli()
)

fun BackupTask.toEntity() = TaskEntity(
    id = id,
    title = title,
    description = description,
    categoryId = categoryId,
    priority = Priority.valueOf(priority),
    deadlineDate = deadlineDate?.let { LocalDate.parse(it) },
    deadlineTime = deadlineTime?.let { LocalTime.parse(it) },
    estimatedDurationMinutes = estimatedDurationMinutes,
    plannedDate = plannedDate?.let { LocalDate.parse(it) },
    plannedStart = plannedStart?.let { LocalTime.parse(it) },
    plannedEnd = plannedEnd?.let { LocalTime.parse(it) },
    status = TaskStatus.valueOf(status),
    shouldCarryForward = shouldCarryForward,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    completedAt = completedAtEpochMillis?.let { Instant.ofEpochMilli(it) },
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis)
)

fun TaskSubtaskEntity.toBackup() = BackupTaskSubtask(
    id = id,
    taskId = taskId,
    title = title,
    isCompleted = isCompleted,
    orderIndex = orderIndex,
    createdAtEpochMillis = createdAt.toEpochMilli()
)

fun BackupTaskSubtask.toEntity() = TaskSubtaskEntity(
    id = id,
    taskId = taskId,
    title = title,
    isCompleted = isCompleted,
    orderIndex = orderIndex,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis)
)

fun HabitEntity.toBackup() = BackupHabit(
    id = id,
    name = name,
    description = description,
    categoryId = categoryId,
    trackingType = trackingType.name,
    targetValue = targetValue,
    unit = unit,
    activeDaysMask = activeDaysMask,
    reminderPreference = reminderPreference,
    reminderTime = reminderTime?.toString(),
    isEnabled = isEnabled,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli()
)

fun BackupHabit.toEntity() = HabitEntity(
    id = id,
    name = name,
    description = description,
    categoryId = categoryId,
    trackingType = HabitTrackingType.valueOf(trackingType),
    targetValue = targetValue,
    unit = unit,
    activeDaysMask = activeDaysMask,
    reminderPreference = reminderPreference,
    reminderTime = reminderTime?.let { LocalTime.parse(it) },
    isEnabled = isEnabled,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis)
)

fun HabitLogEntity.toBackup() = BackupHabitLog(
    id = id,
    habitId = habitId,
    localDate = localDate.toString(),
    completedValue = completedValue,
    isCompleted = isCompleted,
    loggedAtEpochMillis = loggedAt.toEpochMilli()
)

fun BackupHabitLog.toEntity() = HabitLogEntity(
    id = id,
    habitId = habitId,
    localDate = LocalDate.parse(localDate),
    completedValue = completedValue,
    isCompleted = isCompleted,
    loggedAt = Instant.ofEpochMilli(loggedAtEpochMillis)
)

fun DailyPlanEntity.toBackup() = BackupDailyPlan(
    id = id,
    localDate = localDate.toString(),
    status = status.name,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    approvedAtEpochMillis = approvedAt?.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli()
)

fun BackupDailyPlan.toEntity() = DailyPlanEntity(
    id = id,
    localDate = LocalDate.parse(localDate),
    status = PlanStatus.valueOf(status),
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    approvedAt = approvedAtEpochMillis?.let { Instant.ofEpochMilli(it) },
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis)
)

fun DailyPlanItemEntity.toBackup() = BackupDailyPlanItem(
    id = id,
    planId = planId,
    sourceType = sourceType.name,
    sourceId = sourceId,
    title = title,
    startTime = startTime.toString(),
    endTime = endTime.toString(),
    orderIndex = orderIndex,
    isCompleted = isCompleted
)

fun BackupDailyPlanItem.toEntity() = DailyPlanItemEntity(
    id = id,
    planId = planId,
    sourceType = PlanItemSourceType.valueOf(sourceType),
    sourceId = sourceId,
    title = title,
    startTime = LocalTime.parse(startTime),
    endTime = LocalTime.parse(endTime),
    orderIndex = orderIndex,
    isCompleted = isCompleted
)

fun DailyReviewEntity.toBackup() = BackupDailyReview(
    id = id,
    localDate = localDate.toString(),
    notes = notes,
    reviewedAtEpochMillis = reviewedAt.toEpochMilli(),
    tasksCompletedCount = tasksCompletedCount,
    tasksUnfinishedCount = tasksUnfinishedCount,
    habitsCompletedCount = habitsCompletedCount,
    decisionsJson = decisionsJson
)

fun BackupDailyReview.toEntity() = DailyReviewEntity(
    id = id,
    localDate = LocalDate.parse(localDate),
    notes = notes,
    reviewedAt = Instant.ofEpochMilli(reviewedAtEpochMillis),
    tasksCompletedCount = tasksCompletedCount,
    tasksUnfinishedCount = tasksUnfinishedCount,
    habitsCompletedCount = habitsCompletedCount,
    decisionsJson = decisionsJson
)

fun AppSettingEntity.toBackup() = BackupAppSetting(
    key = key,
    value = value,
    updatedAtEpochMillis = updatedAt.toEpochMilli()
)

fun BackupAppSetting.toEntity() = AppSettingEntity(
    key = key,
    value = value,
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis)
)