package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.repository.HabitRepository
import com.dayflow.app.scheduling.HabitReminderScheduler
import javax.inject.Inject

class DeleteHabitUseCase @Inject constructor(
    private val habitRepository: HabitRepository,
    private val habitReminderScheduler: HabitReminderScheduler
) {
    suspend operator fun invoke(habitId: String) {
        habitReminderScheduler.cancelHabitReminder(habitId)
        habitRepository.deleteHabitById(habitId)
    }
}
