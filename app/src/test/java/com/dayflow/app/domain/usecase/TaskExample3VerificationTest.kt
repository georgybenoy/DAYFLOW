package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.model.DeadlineReminderLeadTime
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderType
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskStatus
import com.dayflow.app.domain.repository.ReminderRepository
import com.dayflow.app.domain.repository.TaskRepository
import com.dayflow.app.notifications.DayflowNotificationChannels
import com.dayflow.app.scheduling.DeadlineReminderScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Verifies Example 3 / Workflow C from PRD:
 * "Add assignment due tomorrow: appears in Upcoming, gets reminders on Critical Deadlines channel."
 */
class TaskExample3VerificationTest {

    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val reminderRepository: ReminderRepository = mockk(relaxed = true)
    private val scheduleReminderUseCase: ScheduleReminderUseCase = mockk(relaxed = true)
    private val deleteReminderUseCase: DeleteReminderUseCase = mockk(relaxed = true)

    private val deadlineReminderScheduler = DeadlineReminderScheduler(
        reminderRepository = reminderRepository,
        scheduleReminderUseCase = scheduleReminderUseCase,
        deleteReminderUseCase = deleteReminderUseCase
    )

    private val createTaskUseCase = CreateTaskUseCase(taskRepository, deadlineReminderScheduler)
    private val completeTaskUseCase = CompleteTaskUseCase(taskRepository, deadlineReminderScheduler)
    private val deriveOverdue = DeriveOverdue()

    private val nowLocalDate = LocalDate.of(2026, 11, 18)
    private val nowLocalTime = LocalTime.of(10, 0)
    private val nowLocalDateTime = LocalDateTime.of(nowLocalDate, nowLocalTime)
    private val nowInstant = nowLocalDateTime.atZone(ZoneId.systemDefault()).toInstant()

    @Test
    fun example3_assignmentDueTomorrow_appearsInUpcoming_andReceivesCriticalDeadlineReminders() = runTest {
        // Step 1: Create an assignment due tomorrow at 15:00 with deadline reminders:
        // - 1 day before (which is today at 15:00)
        // - At deadline (tomorrow at 15:00)
        val tomorrow = nowLocalDate.plusDays(1)
        val deadlineTime = LocalTime.of(15, 0)

        val assignmentTask = Task(
            id = "task-assignment-ml-1",
            title = "Machine Learning Assignment 2",
            description = "Complete classification and cross-validation report",
            categoryId = "cat_academics",
            priority = Priority.HIGH,
            deadlineDate = tomorrow,
            deadlineTime = deadlineTime,
            estimatedDurationMinutes = 90,
            status = TaskStatus.PENDING
        )

        val scheduledReminders = mutableListOf<Reminder>()
        coEvery { scheduleReminderUseCase(capture(scheduledReminders), any()) } returns true
        coEvery { reminderRepository.getDeadlineRemindersForTaskDirect("task-assignment-ml-1") } answers {
            scheduledReminders.map { it }
        }

        val leadTimes = setOf(DeadlineReminderLeadTime.ONE_DAY_BEFORE, DeadlineReminderLeadTime.AT_DEADLINE)

        createTaskUseCase(
            task = assignmentTask,
            subtasks = emptyList(),
            deadlineReminders = leadTimes,
            now = nowInstant
        )

        // Step 2: Verification of Upcoming status
        // A task due tomorrow is strictly NOT overdue
        assertFalse(deriveOverdue(assignmentTask, nowLocalDateTime))

        // It qualifies for the UPCOMING tab filter because deadlineDate > today
        val isUpcoming = assignmentTask.status == TaskStatus.PENDING &&
            !deriveOverdue(assignmentTask, nowLocalDateTime) &&
            assignmentTask.deadlineDate?.isAfter(nowLocalDate) == true
        assertTrue(isUpcoming)

        // Step 3: Verification of Reminder Pipeline reuse and Critical Deadlines channel
        // Both reminders must be scheduled via ScheduleReminderUseCase
        assertEquals(2, scheduledReminders.size)

        for (reminder in scheduledReminders) {
            // Priority must be URGENT to route directly to CHANNEL_CRITICAL_DEADLINES
            assertEquals(Priority.URGENT, reminder.priority)
            val channel = DayflowNotificationChannels.getChannelForPriority(reminder.priority)
            assertEquals(DayflowNotificationChannels.CHANNEL_CRITICAL_DEADLINES, channel)

            // Must be one-time exact reminder
            assertEquals(ReminderType.ONE_TIME, reminder.type)
            assertTrue(reminder.id.startsWith("task_deadline_task-assignment-ml-1"))
        }

        // Reminders trigger times check:
        // 1 day before tomorrow 15:00 -> today 15:00
        val oneDayBefore = scheduledReminders.first { it.id.endsWith(DeadlineReminderLeadTime.ONE_DAY_BEFORE.name) }
        assertEquals(nowLocalDate, oneDayBefore.startDate)
        assertEquals(deadlineTime, oneDayBefore.startTime)

        // At deadline -> tomorrow 15:00
        val atDeadline = scheduledReminders.first { it.id.endsWith(DeadlineReminderLeadTime.AT_DEADLINE.name) }
        assertEquals(tomorrow, atDeadline.startDate)
        assertEquals(deadlineTime, atDeadline.startTime)

        // Step 4: Completing the task must cancel the deadline reminders
        coEvery { taskRepository.getTaskByIdDirect("task-assignment-ml-1") } returns assignmentTask

        completeTaskUseCase("task-assignment-ml-1", nowInstant)

        coVerify { deleteReminderUseCase(oneDayBefore.id) }
        coVerify { deleteReminderUseCase(atDeadline.id) }
    }
}
