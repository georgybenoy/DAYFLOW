package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class DeriveOverdueTest {

    private val deriveOverdue = DeriveOverdue()

    private val referenceDate = LocalDate.of(2026, 11, 18)
    private val referenceTime = LocalTime.of(14, 0)
    private val referenceDateTime = LocalDateTime.of(referenceDate, referenceTime)

    @Test
    fun nullDeadline_isNeverOverdue() {
        val task = Task(
            title = "No deadline",
            deadlineDate = null,
            deadlineTime = null,
            status = TaskStatus.PENDING
        )
        assertFalse(deriveOverdue(task, referenceDateTime))
    }

    @Test
    fun completedTask_isNeverOverdue_evenIfDeadlineInPast() {
        val task = Task(
            title = "Completed task",
            deadlineDate = referenceDate.minusDays(5),
            deadlineTime = LocalTime.of(10, 0),
            status = TaskStatus.COMPLETED
        )
        assertFalse(deriveOverdue(task, referenceDateTime))
    }

    @Test
    fun cancelledTask_isNeverOverdue_evenIfDeadlineInPast() {
        val task = Task(
            title = "Cancelled task",
            deadlineDate = referenceDate.minusDays(1),
            deadlineTime = LocalTime.of(12, 0),
            status = TaskStatus.CANCELLED
        )
        assertFalse(deriveOverdue(task, referenceDateTime))
    }

    @Test
    fun pastDeadlineDate_isOverdue() {
        val task = Task(
            title = "Due yesterday",
            deadlineDate = referenceDate.minusDays(1),
            deadlineTime = null,
            status = TaskStatus.PENDING
        )
        assertTrue(deriveOverdue(task, referenceDateTime))
    }

    @Test
    fun futureDeadlineDate_isNotOverdue() {
        val task = Task(
            title = "Due tomorrow",
            deadlineDate = referenceDate.plusDays(1),
            deadlineTime = LocalTime.of(9, 0),
            status = TaskStatus.PENDING
        )
        assertFalse(deriveOverdue(task, referenceDateTime))
    }

    @Test
    fun sameDay_beforeDeadlineTime_isNotOverdue() {
        val task = Task(
            title = "Due later today",
            deadlineDate = referenceDate,
            deadlineTime = LocalTime.of(14, 30),
            status = TaskStatus.PENDING
        )
        assertFalse(deriveOverdue(task, referenceDateTime)) // reference is 14:00
    }

    @Test
    fun sameDay_exactDeadlineTime_isNotOverdue() {
        val task = Task(
            title = "Due right now",
            deadlineDate = referenceDate,
            deadlineTime = LocalTime.of(14, 0),
            status = TaskStatus.PENDING
        )
        // At 14:00:00, currentTime.isAfter(deadlineTime) is false
        assertFalse(deriveOverdue(task, referenceDateTime))
    }

    @Test
    fun sameDay_afterDeadlineTime_isOverdue() {
        val task = Task(
            title = "Due earlier today",
            deadlineDate = referenceDate,
            deadlineTime = LocalTime.of(13, 59),
            status = TaskStatus.PENDING
        )
        assertTrue(deriveOverdue(task, referenceDateTime))
    }

    @Test
    fun sameDay_noDeadlineTimeSpecified_isNotOverdueDuringTheDay() {
        val task = Task(
            title = "Due today anytime",
            deadlineDate = referenceDate,
            deadlineTime = null,
            status = TaskStatus.PENDING
        )
        // On the day of deadline without time, whole day is available
        val endOfDay = LocalDateTime.of(referenceDate, LocalTime.of(23, 59))
        assertFalse(deriveOverdue(task, endOfDay))

        // Starting midnight the next day, it becomes overdue
        val nextDay = LocalDateTime.of(referenceDate.plusDays(1), LocalTime.of(0, 1))
        assertTrue(deriveOverdue(task, nextDay))
    }
}
