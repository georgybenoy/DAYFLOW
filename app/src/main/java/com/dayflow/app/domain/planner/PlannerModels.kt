package com.dayflow.app.domain.planner

import com.dayflow.app.domain.model.DailyPlanItem
import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskReviewDecision
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Defines the user's available execution window for a single day.
 * Tasks are only scheduled between [startTime] and [endTime], respecting quiet hours.
 */
data class PlannerAvailabilityWindow(
    val startTime: LocalTime = LocalTime.of(8, 0),
    val endTime: LocalTime = LocalTime.of(21, 0)
)

/**
 * An immovable commitment on the calendar, such as a blocking reminder,
 * an approved plan item, or a task with a fixed planned execution time.
 */
data class FixedCommitment(
    val id: String,
    val title: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val isBlocking: Boolean = true
)

/**
 * Types of conflicts detected by the deterministic planner engine.
 */
enum class PlanConflictType {
    OVERLAP_WITH_FIXED_COMMITMENT,
    DEADLINE_VIOLATION,
    CAPACITY_EXCEEDED,
    OVERDUE_TASK
}

/**
 * Structured warning/conflict emitted during plan generation.
 */
data class PlanConflict(
    val type: PlanConflictType,
    val message: String,
    val affectedTaskId: String? = null
)

/**
 * An eligible task that could not be scheduled into the available window.
 * Invariant: Never silently dropped from the plan.
 */
data class UnscheduledItem(
    val task: Task,
    val reason: String,
    val reschedulingSuggestion: String? = null
)

/**
 * Full input contract for generating a daily execution plan.
 */
data class PlannerInput(
    val date: LocalDate,
    val tasks: List<Task>,
    val fixedCommitments: List<FixedCommitment> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val reviewDecisions: List<TaskReviewDecision> = emptyList(),
    val availabilityWindow: PlannerAvailabilityWindow = PlannerAvailabilityWindow(),
    val bufferMinutes: Int = PlannerConstants.DEFAULT_BUFFER_MINUTES,
    val defaultTaskDurationMinutes: Int = PlannerConstants.DEFAULT_TASK_DURATION_MINUTES,
    val referenceTime: LocalDateTime = LocalDateTime.of(date, LocalTime.MIN)
)

/**
 * The output result of the deterministic planning algorithm.
 */
data class DraftPlanResult(
    val date: LocalDate,
    val scheduledItems: List<DailyPlanItem>,
    val unscheduledItems: List<UnscheduledItem>,
    val conflicts: List<PlanConflict>,
    val totalScheduledMinutes: Int,
    val availableMinutes: Int
)
