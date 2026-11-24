package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.repository.HabitRepository
import com.dayflow.app.scheduling.HabitReminderScheduler
import java.time.Instant
import javax.inject.Inject

class ToggleHabitEnableUseCase @Inject constructor(
    private val habitRepository: HabitRepository,
    private val habitReminderScheduler: HabitReminderScheduler
) {
    suspend operator fun invoke(habitId: String): Boolean {
        val habit = habitRepository.getHabitByIdDirect(habitId) ?: return false
        val updatedHabit = habit.copy(
            isEnabled = !habit.isEnabled,
            updatedAt = Instant.now()
        )
        habitRepository.saveHabit(updatedHabit)
        habitReminderScheduler.syncHabitReminder(updatedHabit)
        return updatedHabit.isEnabled
    }
}
