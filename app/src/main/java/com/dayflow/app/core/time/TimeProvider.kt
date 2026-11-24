package com.dayflow.app.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

interface TimeProvider {
    fun nowInstant(): Instant
    fun nowLocalDateTime(): LocalDateTime
    fun nowLocalDate(): LocalDate
    fun nowLocalTime(): LocalTime
    fun zoneId(): ZoneId
}

@Singleton
class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun nowInstant(): Instant = Instant.now()
    override fun nowLocalDateTime(): LocalDateTime = LocalDateTime.now()
    override fun nowLocalDate(): LocalDate = LocalDate.now()
    override fun nowLocalTime(): LocalTime = LocalTime.now()
    override fun zoneId(): ZoneId = ZoneId.systemDefault()
}
