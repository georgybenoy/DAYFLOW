package com.dayflow.app.domain.calendar

import com.dayflow.app.core.time.TimeProvider
import com.dayflow.app.domain.model.OccurrenceStatus
import com.dayflow.app.domain.model.PlanStatus
import com.dayflow.app.domain.model.TaskStatus
import com.dayflow.app.domain.recurrence.RecurrenceCalculator
import com.dayflow.app.domain.repository.DailyPlanRepository
import com.dayflow.app.domain.repository.HabitRepository
import com.dayflow.app.domain.repository.ReminderRepository
import com.dayflow.app.domain.repository.TaskRepository
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class GetCalendarEventsUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val reminderRepository: ReminderRepository,
    private val dailyPlanRepository: DailyPlanRepository,
    private val timeProvider: TimeProvider
) {

    suspend operator fun invoke(
        startDate: LocalDate,
        endDate: LocalDate,
        zoneId: ZoneId = timeProvider.zoneId()
    ): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()

        val startInstant = startDate.atStartOfDay(zoneId).toInstant()
        val endInstant = endDate.plusDays(1).atStartOfDay(zoneId).toInstant().minusNanos(1)
        val startEpoch = startInstant.toEpochMilli()
        val endEpoch = endInstant.toEpochMilli()

        // 1. Tasks: Tasks planned or with deadline within range
        val tasks = taskRepository.getTasksBetweenDirect(startDate, endDate)
        tasks.forEach { task ->
            // Planned task event
            if (task.plannedDate != null && task.plannedDate in startDate..endDate) {
                events.add(
                    CalendarEvent(
                        id = "task_plan_${task.id}",
                        title = task.title,
                        description = task.description,
                        type = CalendarEventType.TASK,
                        date = task.plannedDate,
                        startTime = task.plannedStart,
                        endTime = task.plannedEnd,
                        isCompleted = task.status == TaskStatus.COMPLETED,
                        priorityName = task.priority.name,
                        referenceId = task.id
                    )
                )
            }
            // Deadline event (if distinct from plannedDate)
            if (task.deadlineDate != null && task.deadlineDate in startDate..endDate && task.deadlineDate != task.plannedDate) {
                events.add(
                    CalendarEvent(
                        id = "task_deadline_${task.id}",
                        title = "Deadline: ${task.title}",
                        description = task.description,
                        type = CalendarEventType.DEADLINE,
                        date = task.deadlineDate,
                        startTime = task.deadlineTime,
                        endTime = null,
                        isCompleted = task.status == TaskStatus.COMPLETED,
                        priorityName = task.priority.name,
                        referenceId = task.id
                    )
                )
            }
        }

        // 2. Reminders:
        // A. Existing stored occurrences in database (past & currently scheduled)
        val storedOccurrences = reminderRepository.getOccurrencesBetweenDirect(startEpoch, endEpoch)
        val storedReminderIds = mutableSetOf<String>()
        val storedSlots = mutableSetOf<Pair<String, Long>>()

        val activeReminders = reminderRepository.getActiveRemindersDirect()
        val reminderMap = activeReminders.associateBy { it.id }.toMutableMap()

        storedOccurrences.forEach { occ ->
            val reminder = reminderMap[occ.reminderId] ?: reminderRepository.getReminderByIdDirect(occ.reminderId)
            val remTitle = reminder?.title ?: "Reminder"
            val occurrenceZdt = occ.scheduledAt.atZone(zoneId)
            val occurrenceDate = occurrenceZdt.toLocalDate()
            val occurrenceTime = occurrenceZdt.toLocalTime()

            if (occurrenceDate in startDate..endDate) {
                storedReminderIds.add(occ.reminderId)
                storedSlots.add(Pair(occ.reminderId, occ.scheduledAt.toEpochMilli()))

                events.add(
                    CalendarEvent(
                        id = "rem_occ_${occ.id}",
                        title = remTitle,
                        description = reminder?.description,
                        type = CalendarEventType.REMINDER,
                        date = occurrenceDate,
                        startTime = occurrenceTime,
                        endTime = null,
                        isCompleted = occ.status == OccurrenceStatus.COMPLETED,
                        priorityName = reminder?.priority?.name,
                        referenceId = occ.reminderId
                    )
                )
            }
        }

        // B. Future occurrences dynamically generated via RecurrenceCalculator (zero stored future spam)
        activeReminders.forEach { reminder ->
            val dynamicInstants = RecurrenceCalculator.occurrencesInRange(
                reminder = reminder,
                from = startInstant,
                to = endInstant,
                zoneId = zoneId
            )

            dynamicInstants.forEach { instant ->
                val epoch = instant.toEpochMilli()
                if (Pair(reminder.id, epoch) !in storedSlots) {
                    val zdt = instant.atZone(zoneId)
                    val date = zdt.toLocalDate()
                    if (date in startDate..endDate) {
                        events.add(
                            CalendarEvent(
                                id = "rem_dyn_${reminder.id}_$epoch",
                                title = reminder.title,
                                description = reminder.description,
                                type = CalendarEventType.REMINDER,
                                date = date,
                                startTime = zdt.toLocalTime(),
                                endTime = null,
                                isCompleted = false,
                                priorityName = reminder.priority.name,
                                referenceId = reminder.id
                            )
                        )
                    }
                }
            }
        }

        // 3. Habits: Check activeDaysMask for each day in range
        val activeHabits = habitRepository.getActiveHabitsDirect()
        val daysCount = ChronoUnit.DAYS.between(startDate, endDate)

        for (i in 0..daysCount) {
            val date = startDate.plusDays(i)
            val dayOfWeekValue = date.dayOfWeek.value // 1 (Mon) to 7 (Sun)
            val bit = 1 shl (dayOfWeekValue - 1)

            val scheduledHabits = activeHabits.filter { (it.activeDaysMask and bit) != 0 }
            scheduledHabits.forEach { habit ->
                val log = habitRepository.getLogForHabitAndDateDirect(habit.id, date)
                val isCompleted = log?.isCompleted == true

                events.add(
                    CalendarEvent(
                        id = "habit_${habit.id}_$date",
                        title = habit.name,
                        description = habit.description,
                        type = CalendarEventType.HABIT,
                        date = date,
                        startTime = habit.reminderTime,
                        endTime = null,
                        isCompleted = isCompleted,
                        priorityName = null,
                        referenceId = habit.id
                    )
                )
            }
        }

        // 4. Approved Daily Plan Items
        val plans = dailyPlanRepository.getPlansBetweenDirect(startDate, endDate)
        plans.filter { it.status == PlanStatus.APPROVED }.forEach { plan ->
            plan.items.forEach { item ->
                events.add(
                    CalendarEvent(
                        id = "plan_item_${item.id}",
                        title = "Plan: ${item.title}",
                        description = null,
                        type = CalendarEventType.PLAN_ITEM,
                        date = plan.localDate,
                        startTime = item.startTime,
                        endTime = item.endTime,
                        isCompleted = item.isCompleted,
                        priorityName = null,
                        referenceId = plan.localDate.toString()
                    )
                )
            }
        }

        // Sort events chronologically
        return events.sortedWith(
            compareBy<CalendarEvent> { it.date }
                .thenBy { it.startTime?.toNanoOfDay() ?: Long.MAX_VALUE }
                .thenBy { it.title }
        )
    }
}
