package com.dayflow.app.domain.planner

/**
 * Named constants and algorithm weights for the DAYFLOW Deterministic Daily Planner.
 * All scoring weights, default durations, and buffers are defined here and documented
 * to ensure complete mathematical transparency and reproducibility.
 */
object PlannerConstants {

    // --- Priority Level Weights (Step 6) ---
    const val WEIGHT_PRIORITY_URGENT: Int = 1000
    const val WEIGHT_PRIORITY_HIGH: Int = 500
    const val WEIGHT_PRIORITY_MEDIUM: Int = 200
    const val WEIGHT_PRIORITY_LOW: Int = 50

    // --- Deadline Proximity Weights (Step 6) ---
    /** Base bonus awarded if the task's deadline is in the past (overdue). */
    const val WEIGHT_OVERDUE_BASE: Int = 800

    /** Incremental boost per day that a task has remained overdue. */
    const val WEIGHT_OVERDUE_PER_DAY: Int = 50

    /** Maximum cap on the overdue age boost to avoid runaway scores. */
    const val MAX_OVERDUE_AGE_BOOST: Int = 300

    /** Base bonus for tasks due on the planned date. */
    const val WEIGHT_DUE_TODAY_BASE: Int = 500

    /** Additional hourly weight for tasks due today (earlier deadline = higher score). */
    const val WEIGHT_DUE_TODAY_HOURLY: Int = 10

    /** Bonus for tasks due tomorrow. */
    const val WEIGHT_DUE_TOMORROW: Int = 200

    /** Bonus for tasks due within the next 3 days. */
    const val WEIGHT_DUE_WITHIN_3_DAYS: Int = 100

    // --- Review Decision Boosts (Step 5 & 6) ---
    /** Priority boost for tasks carried forward from an evening review. */
    const val WEIGHT_CARRIED_FORWARD: Int = 150

    // --- Execution Parameters (Step 8) ---
    /** Default duration assigned to a task when estimatedDurationMinutes is not specified. */
    const val DEFAULT_TASK_DURATION_MINUTES: Int = 30

    /** Default duration assigned to habit execution slots. */
    const val DEFAULT_HABIT_DURATION_MINUTES: Int = 15

    /** Minimum transition buffer inserted between consecutive scheduled items. */
    const val DEFAULT_BUFFER_MINUTES: Int = 5
}
