package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.repository.ReminderRepository
import javax.inject.Inject

class DeleteReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val cancelReminderUseCase: CancelReminderUseCase
) {
    suspend operator fun invoke(reminderId: String): Boolean {
        cancelReminderUseCase(reminderId)
        reminderRepository.deleteOccurrencesByReminder(reminderId)
        reminderRepository.deleteReminderById(reminderId)
        return true
    }
}
