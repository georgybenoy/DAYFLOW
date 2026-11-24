package com.dayflow.app.domain.planner

import com.dayflow.app.domain.model.DailyPlanItem
import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.PlanItemSourceType
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.ReviewDecisionType
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskReviewDecision
import com.dayflow.app.domain.model.TaskStatus
import com.dayflow.app.domain.recurrence.ActiveDays
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Pure Kotlin deterministic daily planning engine for DAYFLOW.
 *
 * Guarantees:
 * 1. 100% Deterministic: Zero randomness, no AI, stable ordering with ID tie-breaking.
 * 2. Strict Invariant: Eligible tasks are NEVER dropped or silently discarded. If a task
 *    cannot fit into the day's available time window, it is placed in the unscheduled list
 *    with a structured explanation and suggestion.
 * 3. Never mutates task deadlines.
 */
object DeterministicDailyPlanner {

    /**
     * Generates a deterministic daily draft plan according to the 10-step algorithm.
     */
    fun generatePlan(input: PlannerInput): DraftPlanResult {
        // --- Step 1: Collect eligible tasks ---
        val reviewDecisionMap = input.reviewDecisions.associateBy { it.taskId }
        val eligibleTasks = input.tasks.filter { task ->
            isTaskEligible(task, input.date, reviewDecisionMap[task.id])
        }

        // --- Step 2 & 3: Identify overdue and today's deadlines ---
        val overdueTasks = eligibleTasks.filter { isOverdue(it, input.date, input.referenceTime) }
        val dueTodayTasks = eligibleTasks.filter { it.deadlineDate == input.date }

        // --- Step 4: Scheduled habits for the date ---
        val activeHabits = input.habits.filter { habit ->
            habit.isEnabled && ActiveDays.isDayActive(habit.activeDaysMask, input.date.dayOfWeek)
        }

        // --- Step 5 & 6: Compute priority scores ---
        val scoredTasks = eligibleTasks.map { task ->
            val decision = reviewDecisionMap[task.id]
            val isCarriedForward = decision?.decision == ReviewDecisionType.CARRIED_FORWARD
            val score = calculatePriorityScore(
                task = task,
                date = input.date,
                referenceTime = input.referenceTime,
                isCarriedForward = isCarriedForward
            )
            ScoredTask(task = task, score = score)
        }

        // --- Step 7: Stable sort ---
        // Ties are broken strictly by deadlineDate, deadlineTime, and finally task.id (stable key)
        val sortedTasks = scoredTasks.sortedWith(
            compareByDescending<ScoredTask> { it.score }
                .thenBy { it.task.deadlineDate ?: LocalDate.MAX }
                .thenBy { it.task.deadlineTime ?: LocalTime.MAX }
                .thenBy { it.task.id }
        ).map { it.task }

        // --- Step 8: Slot assignment into free intervals ---
        val conflicts = mutableListOf<PlanConflict>()
        val scheduledItems = mutableListOf<DailyPlanItem>()
        val unscheduledItems = mutableListOf<UnscheduledItem>()

        val windowStart = input.availabilityWindow.startTime
        val windowEnd = input.availabilityWindow.endTime

        val totalAvailableMinutes = if (windowEnd.isAfter(windowStart)) {
            Duration.between(windowStart, windowEnd).toMinutes().toInt()
        } else 0

        // Handle edge case: empty or invalid availability window
        if (totalAvailableMinutes <= 0) {
            sortedTasks.forEach { task ->
                unscheduledItems.add(
                    UnscheduledItem(
                        task = task,
                        reason = "Availability window is closed or invalid for this date",
                        reschedulingSuggestion = "Adjust availability hours in settings"
                    )
                )
            }
            conflicts.add(
                PlanConflict(
                    type = PlanConflictType.CAPACITY_EXCEEDED,
                    message = "Availability window is 0 minutes."
                )
            )
            return DraftPlanResult(
                date = input.date,
                scheduledItems = emptyList(),
                unscheduledItems = unscheduledItems,
                conflicts = conflicts,
                totalScheduledMinutes = 0,
                availableMinutes = 0
            )
        }

        // Build busy intervals from fixed commitments, habits with reminder times, and pre-planned tasks
        val busyIntervals = mutableListOf<TimeInterval>()

        // 1. Fixed commitments from input
        input.fixedCommitments.forEach { commitment ->
            if (commitment.endTime.isAfter(commitment.startTime)) {
                busyIntervals.add(TimeInterval(commitment.startTime, commitment.endTime, commitment.title))
            }
        }

        // 2. Fixed habit reminder times
        activeHabits.forEach { habit ->
            habit.reminderTime?.let { rTime ->
                val hEnd = rTime.plusMinutes(PlannerConstants.DEFAULT_HABIT_DURATION_MINUTES.toLong())
                if (hEnd.isAfter(rTime)) {
                    busyIntervals.add(TimeInterval(rTime, hEnd, "Habit: ${habit.name}"))
                    scheduledItems.add(
                        DailyPlanItem(
                            id = UUID.randomUUID().toString(),
                            planId = "",
                            sourceType = PlanItemSourceType.HABIT,
                            sourceId = habit.id,
                            title = habit.name,
                            startTime = rTime,
                            endTime = hEnd,
                            orderIndex = 0
                        )
                    )
                }
            }
        }

        // Merge busy intervals
        val mergedBusy = mergeIntervals(busyIntervals)

        // Assign slots to tasks
        var currentOrderIndex = scheduledItems.size

        for (task in sortedTasks) {
            val durationMinutes = task.estimatedDurationMinutes?.takeIf { it > 0 }
                ?: input.defaultTaskDurationMinutes

            // If task has fixed planned times
            if (task.plannedStart != null && task.plannedEnd != null && task.plannedEnd.isAfter(task.plannedStart)) {
                val fixedInterval = TimeInterval(task.plannedStart, task.plannedEnd, task.title)
                val overlaps = mergedBusy.any { it.overlaps(fixedInterval) }

                if (overlaps || task.plannedStart.isBefore(windowStart) || task.plannedEnd.isAfter(windowEnd)) {
                    conflicts.add(
                        PlanConflict(
                            type = PlanConflictType.OVERLAP_WITH_FIXED_COMMITMENT,
                            message = "Fixed planned time (${task.plannedStart} - ${task.plannedEnd}) conflicts with another commitment or falls outside availability",
                            affectedTaskId = task.id
                        )
                    )
                    unscheduledItems.add(
                        UnscheduledItem(
                            task = task,
                            reason = "Fixed time slot conflicts with another commitment",
                            reschedulingSuggestion = "Remove or adjust fixed planned times"
                        )
                    )
                } else {
                    mergedBusy.add(fixedInterval)
                    mergedBusy.sortWith(compareBy { it.start })
                    scheduledItems.add(
                        DailyPlanItem(
                            id = UUID.randomUUID().toString(),
                            planId = "",
                            sourceType = PlanItemSourceType.TASK,
                            sourceId = task.id,
                            title = task.title,
                            startTime = task.plannedStart,
                            endTime = task.plannedEnd,
                            orderIndex = currentOrderIndex++
                        )
                    )
                }
            } else {
                // Flexible task: find earliest available slot
                val freeIntervals = calculateFreeIntervals(windowStart, windowEnd, mergedBusy)
                val slot = findEarliestSlot(freeIntervals, durationMinutes)

                if (slot != null) {
                    val (slotStart, slotEnd) = slot
                    // Add buffer to busy interval to give space between tasks
                    val busyEnd = if (slotEnd.plusMinutes(input.bufferMinutes.toLong()).isBefore(windowEnd)) {
                        slotEnd.plusMinutes(input.bufferMinutes.toLong())
                    } else {
                        slotEnd
                    }
                    mergedBusy.add(TimeInterval(slotStart, busyEnd, task.title))
                    mergedBusy.sortWith(compareBy { it.start })

                    scheduledItems.add(
                        DailyPlanItem(
                            id = UUID.randomUUID().toString(),
                            planId = "",
                            sourceType = PlanItemSourceType.TASK,
                            sourceId = task.id,
                            title = task.title,
                            startTime = slotStart,
                            endTime = slotEnd,
                            orderIndex = currentOrderIndex++
                        )
                    )
                } else {
                    // Could not fit in today's window
                    unscheduledItems.add(
                        UnscheduledItem(
                            task = task,
                            reason = "Insufficient time available in today's schedule (${durationMinutes}m needed)",
                            reschedulingSuggestion = "Reschedule to ${input.date.plusDays(1)}"
                        )
                    )
                }
            }
        }

        // --- Step 9: Conflict detection ---
        // 1. Deadline violations on scheduled items
        val taskMap = eligibleTasks.associateBy { it.id }
        scheduledItems.forEach { item ->
            if (item.sourceType == PlanItemSourceType.TASK && item.sourceId != null) {
                val task = taskMap[item.sourceId]
                if (task != null && task.deadlineDate == input.date && task.deadlineTime != null) {
                    if (item.endTime.isAfter(task.deadlineTime)) {
                        conflicts.add(
                            PlanConflict(
                                type = PlanConflictType.DEADLINE_VIOLATION,
                                message = "Task '${task.title}' is scheduled to finish at ${item.endTime}, which is after its deadline of ${task.deadlineTime}",
                                affectedTaskId = task.id
                            )
                        )
                    }
                }
            }
        }

        // 2. Overdue task warnings
        overdueTasks.forEach { overdueTask ->
            conflicts.add(
                PlanConflict(
                    type = PlanConflictType.OVERDUE_TASK,
                    message = "Task '${overdueTask.title}' is overdue (deadline was ${overdueTask.deadlineDate ?: "past"})",
                    affectedTaskId = overdueTask.id
                )
            )
        }

        // 3. Capacity exceeded warning
        if (unscheduledItems.isNotEmpty()) {
            conflicts.add(
                PlanConflict(
                    type = PlanConflictType.CAPACITY_EXCEEDED,
                    message = "${unscheduledItems.size} tasks could not be scheduled due to availability constraints."
                )
            )
        }

        // Re-index scheduled items chronologically
        val finalScheduled = scheduledItems.sortedWith(compareBy { it.startTime })
            .mapIndexed { index, item -> item.copy(orderIndex = index) }

        val totalScheduledMinutes = finalScheduled.sumOf {
            Duration.between(it.startTime, it.endTime).toMinutes().toInt()
        }

        return DraftPlanResult(
            date = input.date,
            scheduledItems = finalScheduled,
            unscheduledItems = unscheduledItems,
            conflicts = conflicts,
            totalScheduledMinutes = totalScheduledMinutes,
            availableMinutes = totalAvailableMinutes
        )
    }

    /**
     * Detects schedule conflicts on an arbitrary or user-edited list of plan items.
     * Checks item overlaps, fixed commitment overlaps, and deadline violations.
     */
    fun detectConflicts(
        date: LocalDate,
        items: List<DailyPlanItem>,
        tasks: List<Task>,
        fixedCommitments: List<FixedCommitment> = emptyList()
    ): List<PlanConflict> {
        val conflicts = mutableListOf<PlanConflict>()
        val taskMap = tasks.associateBy { it.id }

        // 1. Check item-to-item overlaps
        val sortedItems = items.sortedBy { it.startTime }
        for (i in 0 until sortedItems.size - 1) {
            val curr = sortedItems[i]
            val next = sortedItems[i + 1]
            if (curr.endTime.isAfter(next.startTime)) {
                conflicts.add(
                    PlanConflict(
                        type = PlanConflictType.OVERLAP_WITH_FIXED_COMMITMENT,
                        message = "'${curr.title}' (${curr.startTime}-${curr.endTime}) overlaps with '${next.title}' (${next.startTime}-${next.endTime})",
                        affectedTaskId = curr.sourceId
                    )
                )
            }
        }

        // 2. Check overlap with fixed commitments
        for (item in items) {
            for (fixed in fixedCommitments) {
                if (item.startTime.isBefore(fixed.endTime) && fixed.startTime.isBefore(item.endTime)) {
                    conflicts.add(
                        PlanConflict(
                            type = PlanConflictType.OVERLAP_WITH_FIXED_COMMITMENT,
                            message = "'${item.title}' conflicts with fixed commitment '${fixed.title}' (${fixed.startTime}-${fixed.endTime})",
                            affectedTaskId = item.sourceId
                        )
                    )
                }
            }
        }

        // 3. Deadline violations
        for (item in items) {
            if (item.sourceType == PlanItemSourceType.TASK && item.sourceId != null) {
                val task = taskMap[item.sourceId]
                if (task != null && task.deadlineDate == date && task.deadlineTime != null) {
                    if (item.endTime.isAfter(task.deadlineTime)) {
                        conflicts.add(
                            PlanConflict(
                                type = PlanConflictType.DEADLINE_VIOLATION,
                                message = "Task '${task.title}' ends at ${item.endTime}, which is after its deadline of ${task.deadlineTime}",
                                affectedTaskId = task.id
                            )
                        )
                    }
                }
            }
        }

        return conflicts
    }

    // --- Helper Methods ---

    private fun isTaskEligible(task: Task, date: LocalDate, reviewDecision: TaskReviewDecision?): Boolean {
        // Pending only
        if (task.status != TaskStatus.PENDING) return false

        // Check review decisions first
        if (reviewDecision != null) {
            when (reviewDecision.decision) {
                ReviewDecisionType.EXCLUDED,
                ReviewDecisionType.CANCELLED,
                ReviewDecisionType.COMPLETED -> return false
                ReviewDecisionType.RESCHEDULED -> return reviewDecision.rescheduledToDate == date.toString()
                ReviewDecisionType.CARRIED_FORWARD -> return true
            }
        }

        // No review decision: check plannedDate, deadlineDate, or overdue
        if (task.plannedDate == date) return true
        if (task.deadlineDate == date) return true
        if (task.deadlineDate != null && task.deadlineDate.isBefore(date)) return true
        // Backlog tasks with no planned date and no deadline are eligible
        if (task.plannedDate == null && task.deadlineDate == null) return true

        return false
    }

    private fun isOverdue(task: Task, referenceDate: LocalDate, referenceTime: LocalDateTime): Boolean {
        if (task.status != TaskStatus.PENDING || task.deadlineDate == null) return false
        if (task.deadlineDate.isBefore(referenceDate)) return true
        if (task.deadlineDate == referenceDate && task.deadlineTime != null) {
            return referenceTime.toLocalTime().isAfter(task.deadlineTime)
        }
        return false
    }

    private fun calculatePriorityScore(
        task: Task,
        date: LocalDate,
        referenceTime: LocalDateTime,
        isCarriedForward: Boolean
    ): Int {
        var score = when (task.priority) {
            Priority.URGENT -> PlannerConstants.WEIGHT_PRIORITY_URGENT
            Priority.HIGH -> PlannerConstants.WEIGHT_PRIORITY_HIGH
            Priority.MEDIUM -> PlannerConstants.WEIGHT_PRIORITY_MEDIUM
            Priority.LOW -> PlannerConstants.WEIGHT_PRIORITY_LOW
        }

        // Deadline proximity
        if (task.deadlineDate != null) {
            if (task.deadlineDate.isBefore(date)) {
                val daysOverdue = ChronoUnit.DAYS.between(task.deadlineDate, date).toInt()
                val ageBonus = minOf(daysOverdue * PlannerConstants.WEIGHT_OVERDUE_PER_DAY, PlannerConstants.MAX_OVERDUE_AGE_BOOST)
                score += PlannerConstants.WEIGHT_OVERDUE_BASE + ageBonus
            } else if (task.deadlineDate == date) {
                var dueTodayScore = PlannerConstants.WEIGHT_DUE_TODAY_BASE
                if (task.deadlineTime != null) {
                    dueTodayScore += (24 - task.deadlineTime.hour) * PlannerConstants.WEIGHT_DUE_TODAY_HOURLY
                }
                score += dueTodayScore
            } else if (task.deadlineDate == date.plusDays(1)) {
                score += PlannerConstants.WEIGHT_DUE_TOMORROW
            } else if (task.deadlineDate.isBefore(date.plusDays(4))) {
                score += PlannerConstants.WEIGHT_DUE_WITHIN_3_DAYS
            }
        }

        if (isCarriedForward) {
            score += PlannerConstants.WEIGHT_CARRIED_FORWARD
        }

        return score
    }

    private fun mergeIntervals(intervals: List<TimeInterval>): MutableList<TimeInterval> {
        if (intervals.isEmpty()) return mutableListOf()
        val sorted = intervals.sortedWith(compareBy { it.start })
        val merged = mutableListOf<TimeInterval>()
        var current = sorted[0]

        for (i in 1 until sorted.size) {
            val next = sorted[i]
            if (current.overlapsOrTouches(next)) {
                val newEnd = if (current.end.isAfter(next.end)) current.end else next.end
                current = TimeInterval(current.start, newEnd, "${current.label} / ${next.label}")
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)
        return merged
    }

    private fun calculateFreeIntervals(
        windowStart: LocalTime,
        windowEnd: LocalTime,
        busyIntervals: List<TimeInterval>
    ): List<TimeInterval> {
        val free = mutableListOf<TimeInterval>()
        var cursor = windowStart

        for (busy in busyIntervals) {
            // If busy is before cursor, skip
            if (busy.end.isBefore(cursor) || busy.end == cursor) continue

            // If busy starts after cursor, there is free space
            if (busy.start.isAfter(cursor)) {
                val freeEnd = if (busy.start.isBefore(windowEnd)) busy.start else windowEnd
                if (freeEnd.isAfter(cursor)) {
                    free.add(TimeInterval(cursor, freeEnd, "Free"))
                }
            }

            if (busy.end.isAfter(cursor)) {
                cursor = busy.end
            }
            if (!cursor.isBefore(windowEnd)) break
        }

        if (cursor.isBefore(windowEnd)) {
            free.add(TimeInterval(cursor, windowEnd, "Free"))
        }

        return free
    }

    private fun findEarliestSlot(
        freeIntervals: List<TimeInterval>,
        durationMinutes: Int
    ): Pair<LocalTime, LocalTime>? {
        for (interval in freeIntervals) {
            val intervalMinutes = Duration.between(interval.start, interval.end).toMinutes().toInt()
            if (intervalMinutes >= durationMinutes) {
                val slotEnd = interval.start.plusMinutes(durationMinutes.toLong())
                return Pair(interval.start, slotEnd)
            }
        }
        return null
    }

    private data class ScoredTask(val task: Task, val score: Int)

    private data class TimeInterval(val start: LocalTime, val end: LocalTime, val label: String) {
        fun overlaps(other: TimeInterval): Boolean {
            return this.start.isBefore(other.end) && other.start.isBefore(this.end)
        }

        fun overlapsOrTouches(other: TimeInterval): Boolean {
            return !this.end.isBefore(other.start) && !other.end.isBefore(this.start)
        }
    }
}
