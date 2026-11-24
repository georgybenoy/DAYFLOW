package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.model.DailyPlan
import com.dayflow.app.domain.model.PlanStatus
import com.dayflow.app.domain.model.TaskStatus
import com.dayflow.app.domain.planner.DeterministicDailyPlanner
import com.dayflow.app.domain.planner.DraftPlanResult
import com.dayflow.app.domain.planner.PlannerAvailabilityWindow
import com.dayflow.app.domain.planner.PlannerInput
import com.dayflow.app.domain.planner.UnscheduledItem
import com.dayflow.app.domain.repository.DailyPlanRepository
import com.dayflow.app.domain.repository.DailyReviewRepository
import com.dayflow.app.domain.repository.HabitRepository
import com.dayflow.app.domain.repository.SettingsRepository
import com.dayflow.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

class GenerateDailyPlanUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val dailyPlanRepository: DailyPlanRepository,
    private val dailyReviewRepository: DailyReviewRepository,
    private val settingsRepository: SettingsRepository
) {

    suspend operator fun invoke(
        date: LocalDate,
        forceRegenerate: Boolean = false
    ): DraftPlanResult {
        val existingPlan = dailyPlanRepository.getPlanByDateDirect(date)

        // If an approved plan already exists and the caller did not force a regeneration,
        // preserve the user's approved plan.
        if (existingPlan != null && existingPlan.status == PlanStatus.APPROVED && !forceRegenerate) {
            val allTasks = taskRepository.getAllTasks().first()
            val scheduledTaskIds = existingPlan.items.mapNotNull { it.sourceId }.toSet()
            val unscheduled = allTasks
                .filter { it.status == TaskStatus.PENDING && !scheduledTaskIds.contains(it.id) && (it.plannedDate == date || it.deadlineDate == date) }
                .map { UnscheduledItem(task = it, reason = "Not in approved plan", reschedulingSuggestion = "Add to schedule or postpone") }

            return DraftPlanResult(
                date = date,
                scheduledItems = existingPlan.items,
                unscheduledItems = unscheduled,
                conflicts = emptyList(),
                totalScheduledMinutes = existingPlan.items.sumOf { java.time.Duration.between(it.startTime, it.endTime).toMinutes().toInt() },
                availableMinutes = 0
            )
        }

        // 1. Gather all tasks
        val allTasks = taskRepository.getAllTasks().first()
        val pendingTasks = allTasks.filter { it.status == TaskStatus.PENDING }

        // 2. Gather active habits
        val activeHabits = habitRepository.getActiveHabits().first()

        // 3. Gather settings for availability window
        val userProfile = settingsRepository.userProfile.first()
        val windowStart = userProfile.quietHoursEnd
        val windowEnd = userProfile.quietHoursStart
        val window = if (windowEnd.isAfter(windowStart)) {
            PlannerAvailabilityWindow(startTime = windowStart, endTime = windowEnd)
        } else {
            PlannerAvailabilityWindow(startTime = LocalTime.of(8, 0), endTime = LocalTime.of(21, 0))
        }

        // 4. Gather previous day review decisions
        val previousDate = date.minusDays(1)
        val previousReview = dailyReviewRepository.getReviewByDateDirect(previousDate)
        val reviewDecisions = previousReview?.decisions ?: emptyList()

        // 5. Build PlannerInput
        val input = PlannerInput(
            date = date,
            tasks = pendingTasks,
            habits = activeHabits,
            reviewDecisions = reviewDecisions,
            availabilityWindow = window,
            referenceTime = LocalDateTime.of(date, LocalTime.MIN)
        )

        // 6. Generate deterministic plan
        val result = DeterministicDailyPlanner.generatePlan(input)

        // 7. Persist as DRAFT in DailyPlanRepository so it can be edited, reordered, or reviewed
        val planId = existingPlan?.id ?: UUID.randomUUID().toString()
        val itemsWithPlanId = result.scheduledItems.map { it.copy(planId = planId) }
        val draftPlan = DailyPlan(
            id = planId,
            localDate = date,
            status = PlanStatus.DRAFT,
            items = itemsWithPlanId
        )
        dailyPlanRepository.savePlanWithItems(draftPlan, itemsWithPlanId)

        return result.copy(scheduledItems = itemsWithPlanId)
    }
}
