package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.model.HabitLog
import com.dayflow.app.domain.recurrence.HabitProgressCalculator
import com.dayflow.app.domain.repository.HabitRepository
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class LogHabitProgressUseCase @Inject constructor(
    private val habitRepository: HabitRepository
) {
    suspend operator fun invoke(
        habitId: String,
        date: LocalDate,
        completedValue: Double,
        isCompleted: Boolean? = null
    ): HabitLog {
        val habit = habitRepository.getHabitByIdDirect(habitId)
        val existingLog = habitRepository.getLogForHabitAndDateDirect(habitId, date)
        val now = Instant.now()

        val resolvedCompleted = isCompleted ?: if (habit != null) {
            val tempLog = (existingLog ?: HabitLog(habitId = habitId, localDate = date))
                .copy(completedValue = completedValue)
            HabitProgressCalculator.calculateProgress(habit, tempLog).isCompleted
        } else {
            completedValue >= 1.0
        }

        val logToSave = (existingLog ?: HabitLog(
            habitId = habitId,
            localDate = date
        )).copy(
            completedValue = completedValue,
            isCompleted = resolvedCompleted,
            loggedAt = now
        )

        habitRepository.logHabitProgress(logToSave)
        return logToSave
    }
}
