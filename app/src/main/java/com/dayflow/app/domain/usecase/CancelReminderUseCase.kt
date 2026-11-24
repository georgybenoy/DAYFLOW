package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.model.OccurrenceStatus
import com.dayflow.app.domain.repository.ReminderRepository
import com.dayflow.app.scheduling.AlarmScheduler
import javax.inject.Inject

class CancelReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(reminderId: String) {
        val pendingOccurrences = reminderRepository.getPendingOccurrencesDirect()
            .filter { it.reminderId == reminderId }

        for (occ in pendingOccurrences) {
            alarmScheduler.cancelOccurrence(occ.id, reminderId)
            reminderRepository.updateOccurrence(occ.copy(status = OccurrenceStatus.CANCELLED))
        }
    }
}
