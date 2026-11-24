package com.dayflow.app.data.local.converter

import androidx.room.TypeConverter
import com.dayflow.app.domain.model.HabitTrackingType
import com.dayflow.app.domain.model.OccurrenceStatus
import com.dayflow.app.domain.model.PlanItemSourceType
import com.dayflow.app.domain.model.PlanStatus
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.ReminderPrecision
import com.dayflow.app.domain.model.ReminderType
import com.dayflow.app.domain.model.TaskStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class DayflowConverters {

    // Temporal Instants (UTC epoch millis)
    @TypeConverter
    fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun toInstant(millis: Long?): Instant? = millis?.let { Instant.ofEpochMilli(it) }

    // Calendar Dates (ISO-8601 yyyy-MM-dd)
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    // Daily Clock Times (ISO-8601 HH:mm)
    @TypeConverter
    fun fromLocalTime(time: LocalTime?): String? = time?.toString()

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let { LocalTime.parse(it) }

    // Timezones
    @TypeConverter
    fun fromZoneId(zoneId: ZoneId?): String? = zoneId?.id

    @TypeConverter
    fun toZoneId(value: String?): ZoneId? = value?.let { ZoneId.of(it) }

    // Enums
    @TypeConverter
    fun fromReminderType(type: ReminderType?): String? = type?.name

    @TypeConverter
    fun toReminderType(value: String?): ReminderType? = value?.let { ReminderType.valueOf(it) }

    @TypeConverter
    fun fromReminderPrecision(precision: ReminderPrecision?): String? = precision?.name

    @TypeConverter
    fun toReminderPrecision(value: String?): ReminderPrecision? = value?.let { ReminderPrecision.valueOf(it) }

    @TypeConverter
    fun fromOccurrenceStatus(status: OccurrenceStatus?): String? = status?.name

    @TypeConverter
    fun toOccurrenceStatus(value: String?): OccurrenceStatus? = value?.let { OccurrenceStatus.valueOf(it) }

    @TypeConverter
    fun fromPriority(priority: Priority?): String? = priority?.name

    @TypeConverter
    fun toPriority(value: String?): Priority? = value?.let { Priority.valueOf(it) }

    @TypeConverter
    fun fromTaskStatus(status: TaskStatus?): String? = status?.name

    @TypeConverter
    fun toTaskStatus(value: String?): TaskStatus? = value?.let { TaskStatus.valueOf(it) }

    @TypeConverter
    fun fromHabitTrackingType(type: HabitTrackingType?): String? = type?.name

    @TypeConverter
    fun toHabitTrackingType(value: String?): HabitTrackingType? = value?.let { HabitTrackingType.valueOf(it) }

    @TypeConverter
    fun fromPlanStatus(status: PlanStatus?): String? = status?.name

    @TypeConverter
    fun toPlanStatus(value: String?): PlanStatus? = value?.let { PlanStatus.valueOf(it) }

    @TypeConverter
    fun fromPlanItemSourceType(sourceType: PlanItemSourceType?): String? = sourceType?.name

    @TypeConverter
    fun toPlanItemSourceType(value: String?): PlanItemSourceType? = value?.let { PlanItemSourceType.valueOf(it) }
}
