package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.model.DeadlineReminderLeadTime
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskStatus
import com.dayflow.app.domain.repository.TaskRepository
import com.dayflow.app.scheduling.DeadlineReminderScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class TaskRescheduleTest {

    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val deadlineReminderScheduler: DeadlineReminderScheduler = mockk(relaxed = true)

    private val rescheduleUseCase = RescheduleTaskPlannedTimeUseCase(taskRepository)
    private val changeDeadlineUseCase = ChangeTaskDeadlineUseCase(taskRepository, deadlineReminderScheduler)

    @Test
    fun reschedulePlannedTime_strictlyPreservesTaskIdAndDeadline() = runTest {
        val originalTaskId = "task-uuid-12345"
        val originalDeadlineDate = LocalDate.of(2026, 12, 1)
        val originalDeadlineTime = LocalTime.of(17, 0)
        val originalTask = Task(
            id = originalTaskId,
            title = "Final Project Documentation",
            priority = Priority.HIGH,
            deadlineDate = originalDeadlineDate,
            deadlineTime = originalDeadlineTime,
            plannedDate = LocalDate.of(2026, 11, 20),
            plannedStart = LocalTime.of(10, 0),
            plannedEnd = LocalTime.of(12, 0),
            status = TaskStatus.PENDING
        )

        coEvery { taskRepository.getTaskByIdDirect(originalTaskId) } returns originalTask

        val savedTaskSlot = slot<Task>()
        coEvery { taskRepository.saveTask(capture(savedTaskSlot)) } returns Unit

        val newPlannedDate = LocalDate.of(2026, 11, 22)
        val newPlannedStart = LocalTime.of(14, 0)
        val newPlannedEnd = LocalTime.of(16, 0)

        val result = rescheduleUseCase(
            taskId = originalTaskId,
            plannedDate = newPlannedDate,
            plannedStart = newPlannedStart,
            plannedEnd = newPlannedEnd
        )

        assertNotNull(result)
        // 1. Identity must be strictly preserved
        assertEquals(originalTaskId, result?.id)
        assertEquals(originalTaskId, savedTaskSlot.captured.id)

        // 2. Deadline must be completely unchanged
        assertEquals(originalDeadlineDate, result?.deadlineDate)
        assertEquals(originalDeadlineTime, result?.deadlineTime)
        assertEquals(originalDeadlineDate, savedTaskSlot.captured.deadlineDate)
        assertEquals(originalDeadlineTime, savedTaskSlot.captured.deadlineTime)

        // 3. Planned execution window is updated independently
        assertEquals(newPlannedDate, result?.plannedDate)
        assertEquals(newPlannedStart, result?.plannedStart)
        assertEquals(newPlannedEnd, result?.plannedEnd)
    }

    @Test
    fun changeDeadlineExplicitOnly_updatesDeadlineAndRecalculatesReminders() = runTest {
        val originalTaskId = "task-uuid-999"
        val originalTask = Task(
            id = originalTaskId,
            title = "Term Paper",
            deadlineDate = LocalDate.of(2026, 11, 20),
            deadlineTime = LocalTime.of(15, 0),
            plannedDate = LocalDate.of(2026, 11, 19),
            status = TaskStatus.PENDING
        )

        coEvery { taskRepository.getTaskByIdDirect(originalTaskId) } returns originalTask
        val savedTaskSlot = slot<Task>()
        coEvery { taskRepository.saveTask(capture(savedTaskSlot)) } returns Unit

        val updatedDeadlineDate = LocalDate.of(2026, 11, 25)
        val updatedDeadlineTime = LocalTime.of(18, 0)
        val reminders = setOf(DeadlineReminderLeadTime.AT_DEADLINE, DeadlineReminderLeadTime.ONE_DAY_BEFORE)

        val result = changeDeadlineUseCase(
            taskId = originalTaskId,
            deadlineDate = updatedDeadlineDate,
            deadlineTime = updatedDeadlineTime,
            deadlineReminders = reminders
        )

        assertNotNull(result)
        assertEquals(originalTaskId, result?.id)
        assertEquals(updatedDeadlineDate, result?.deadlineDate)
        assertEquals(updatedDeadlineTime, result?.deadlineTime)

        // Old deadline reminders must be cancelled, new ones scheduled
        coVerify { deadlineReminderScheduler.cancelDeadlineReminders(originalTaskId) }
        coVerify { deadlineReminderScheduler.scheduleDeadlineReminders(any(), reminders, any(), any()) }
    }
}
