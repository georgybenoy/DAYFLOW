package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.repository.HabitRepository
import com.dayflow.app.scheduling.HabitReminderScheduler
import java.time.Instant
import javax.inject.Inject

class UpdateHabitUseCase @Inject constructor(
    private val habitRepository: HabitRepository,
    private val habitReminderScheduler: HabitReminderScheduler
) {
    suspend operator fun invoke(habit: Habit): Result<Habit> {
        if (habit.name.isBlank()) {
            return Result.failure(IllegalArgumentException("Habit name cannot be blank"))
        }
        if (habit.targetValue <= 0.0) {
            return Result.failure(IllegalArgumentException("Target value must be greater than zero"))
        }

        val habitToSave = habit.copy(updatedAt = Instant.now())
        habitRepository.saveHabit(habitToSave)
        habitReminderScheduler.syncHabitReminder(habitToSave)

        return Result.success(habitToSave)
    }
}
