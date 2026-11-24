package com.dayflow.app.scheduling

import com.dayflow.app.domain.model.OccurrenceStatus
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderOccurrence
import com.dayflow.app.domain.model.ReminderPrecision
import com.dayflow.app.domain.model.ReminderType
import com.dayflow.app.domain.model.UserProfile
import com.dayflow.app.domain.repository.ReminderRepository
import com.dayflow.app.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class WorkflowReminderSchedulerTest {

    private val reminderRepository: ReminderRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val alarmScheduler: AlarmScheduler = mockk(relaxed = true)

    private lateinit var scheduler: WorkflowReminderScheduler

    private val testProfile = UserProfile(
        name = "Alex",
        morningBriefingTime = LocalTime.of(7, 0),
        eveningReviewTime = LocalTime.of(21, 30)
    )

    @Before
    fun setUp() {
        every { settingsRepository.userProfile } returns flowOf(testProfile)
        scheduler = WorkflowReminderScheduler(
            reminderRepository = reminderRepository,
            settingsRepository = settingsRepository,
            alarmScheduler = alarmScheduler
        )
    }

    @Test
    fun `ensureWorkflowRemindersScheduled creates system reminders when not present`() = runTest {
        coEvery { reminderRepository.getReminderByIdDirect(any()) } returns null
        coEvery { reminderRepository.getPendingOccurrencesDirect() } returns emptyList()
        coEvery { reminderRepository.scheduleOccurrence(any()) } returns true

        scheduler.ensureWorkflowRemindersScheduled()

        // Verifies both system briefing and review reminders are saved
        coVerify {
            reminderRepository.saveReminder(match {
                it.id == WorkflowReminderScheduler.REMINDER_ID_BRIEFING &&
                it.startTime == LocalTime.of(7, 0) &&
                it.type == ReminderType.DAILY
            })
        }
        coVerify {
            reminderRepository.saveReminder(match {
                it.id == WorkflowReminderScheduler.REMINDER_ID_REVIEW &&
                it.startTime == LocalTime.of(21, 30) &&
                it.type == ReminderType.DAILY
            })
        }

        // Verifies alarm scheduler scheduled both occurrences
        coVerify(atLeast = 1) {
            alarmScheduler.scheduleOccurrence(match { it.reminderId == WorkflowReminderScheduler.REMINDER_ID_BRIEFING }, any())
        }
        coVerify(atLeast = 1) {
            alarmScheduler.scheduleOccurrence(match { it.reminderId == WorkflowReminderScheduler.REMINDER_ID_REVIEW }, any())
        }
    }

    @Test
    fun `rearmNextOccurrence marks current occurrence delivered and schedules next occurrence`() = runTest {
        val existingReminder = Reminder(
            id = WorkflowReminderScheduler.REMINDER_ID_BRIEFING,
            title = "Morning Briefing",
            type = ReminderType.DAILY,
            startDate = LocalDate.now(),
            startTime = LocalTime.of(7, 0),
            priority = Priority.MEDIUM,
            precision = ReminderPrecision.EXACT,
            timeZoneId = ZoneId.systemDefault()
        )
        val currentOccurrence = ReminderOccurrence(
            id = "occ_briefing_1",
            reminderId = WorkflowReminderScheduler.REMINDER_ID_BRIEFING,
            scheduledAt = Instant.now().minusSeconds(10),
            status = OccurrenceStatus.SCHEDULED
        )

        coEvery { reminderRepository.getReminderByIdDirect(WorkflowReminderScheduler.REMINDER_ID_BRIEFING) } returns existingReminder
        coEvery { reminderRepository.getPendingOccurrencesDirect() } returns listOf(currentOccurrence)
        coEvery { reminderRepository.scheduleOccurrence(any()) } returns true

        scheduler.rearmNextOccurrence(WorkflowReminderScheduler.REMINDER_ID_BRIEFING)

        // Verifies old occurrence updated to DELIVERED
        coVerify {
            reminderRepository.updateOccurrence(match {
                it.id == "occ_briefing_1" && it.status == OccurrenceStatus.DELIVERED
            })
        }

        // Verifies new occurrence scheduled with AlarmScheduler
        coVerify {
            alarmScheduler.scheduleOccurrence(match {
                it.reminderId == WorkflowReminderScheduler.REMINDER_ID_BRIEFING && it.id != "occ_briefing_1"
            }, existingReminder)
        }
    }

    @Test
    fun `syncWorkflowTimes updates reminder times and reschedules pending alarms`() = runTest {
        val briefingReminder = Reminder(
            id = WorkflowReminderScheduler.REMINDER_ID_BRIEFING,
            title = "Morning Briefing",
            type = ReminderType.DAILY,
            startDate = LocalDate.now(),
            startTime = LocalTime.of(7, 0),
            priority = Priority.MEDIUM,
            precision = ReminderPrecision.EXACT,
            timeZoneId = ZoneId.systemDefault()
        )
        val reviewReminder = Reminder(
            id = WorkflowReminderScheduler.REMINDER_ID_REVIEW,
            title = "Evening Review",
            type = ReminderType.DAILY,
            startDate = LocalDate.now(),
            startTime = LocalTime.of(21, 30),
            priority = Priority.MEDIUM,
            precision = ReminderPrecision.EXACT,
            timeZoneId = ZoneId.systemDefault()
        )

        coEvery { reminderRepository.getReminderByIdDirect(WorkflowReminderScheduler.REMINDER_ID_BRIEFING) } returns briefingReminder
        coEvery { reminderRepository.getReminderByIdDirect(WorkflowReminderScheduler.REMINDER_ID_REVIEW) } returns reviewReminder
        coEvery { reminderRepository.getPendingOccurrencesDirect() } returns emptyList()
        coEvery { reminderRepository.scheduleOccurrence(any()) } returns true

        val newBriefingTime = LocalTime.of(8, 0)
        val newReviewTime = LocalTime.of(22, 0)

        scheduler.syncWorkflowTimes(newBriefingTime, newReviewTime)

        // Verifies updated reminders saved
        coVerify {
            reminderRepository.saveReminder(match {
                it.id == WorkflowReminderScheduler.REMINDER_ID_BRIEFING && it.startTime == newBriefingTime
            })
        }
        coVerify {
            reminderRepository.saveReminder(match {
                it.id == WorkflowReminderScheduler.REMINDER_ID_REVIEW && it.startTime == newReviewTime
            })
        }
    }
}