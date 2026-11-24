package com.dayflow.app.domain.usecase

import com.dayflow.app.core.time.TimeProvider
import com.dayflow.app.domain.briefing.BriefingContent
import com.dayflow.app.domain.briefing.BriefingGenerator
import com.dayflow.app.domain.model.PlanStatus
import com.dayflow.app.domain.repository.DailyPlanRepository
import com.dayflow.app.domain.repository.HabitRepository
import com.dayflow.app.domain.repository.ReminderRepository
import com.dayflow.app.domain.repository.SettingsRepository
import com.dayflow.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

class GetBriefingUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val dailyPlanRepository: DailyPlanRepository,
    private val reminderRepository: ReminderRepository,
    private val timeProvider: TimeProvider
) {

    suspend operator fun invoke(date: LocalDate = timeProvider.nowLocalDate()): BriefingContent {
        val profile = settingsRepository.userProfile.first()
        val tasks = taskRepository.getAllTasks().first()
        val habits = habitRepository.getActiveHabits().first()
        val habitLogs = habitRepository.getLogsForDate(date).first()
        val reminders = reminderRepository.getActiveRemindersDirect()

        val dailyPlan = dailyPlanRepository.getPlanByDateDirect(date)
        val scheduledItems = if (dailyPlan != null && dailyPlan.status == PlanStatus.APPROVED) {
            dailyPlan.items
        } else {
            emptyList()
        }

        return BriefingGenerator.generate(
            userName = profile.name,
            date = date,
            tasks = tasks,
            scheduledPlanItems = scheduledItems,
            habits = habits,
            habitLogs = habitLogs,
            reminders = reminders
        )
    }
}
