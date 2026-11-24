package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.repository.ReminderRepository
import java.time.Instant
import javax.inject.Inject

class PauseReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val cancelReminderUseCase: CancelReminderUseCase
) {
    suspend operator fun invoke(reminderId: String, now: Instant = Instant.now()): Boolean {
        val reminder = reminderRepository.getReminderByIdDirect(reminderId) ?: return false
        val updated = reminder.copy(isPaused = true, updatedAt = now)
        reminderRepository.saveReminder(updated)
        cancelReminderUseCase(reminderId)
        return true
    }
}
