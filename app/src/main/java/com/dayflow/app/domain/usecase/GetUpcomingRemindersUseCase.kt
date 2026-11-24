package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderOccurrence
import com.dayflow.app.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import javax.inject.Inject

data class UpcomingReminderItem(
    val occurrence: ReminderOccurrence,
    val reminder: Reminder
)

class GetUpcomingRemindersUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository
) {
    operator fun invoke(fromInstant: Instant = Instant.now(), limit: Int = 20): Flow<List<UpcomingReminderItem>> {
        return combine(
            reminderRepository.getUpcomingOccurrences(fromInstant, limit),
            reminderRepository.getAllReminders()
        ) { occurrences, reminders ->
            val reminderMap = reminders.associateBy { it.id }
            occurrences.mapNotNull { occ ->
                val rem = reminderMap[occ.reminderId]
                if (rem != null && rem.isEnabled && !rem.isPaused) {
                    UpcomingReminderItem(occurrence = occ, reminder = rem)
                } else {
                    null
                }
            }
        }
    }
}
