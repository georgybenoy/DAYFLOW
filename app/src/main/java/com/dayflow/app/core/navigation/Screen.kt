package com.dayflow.app.core.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object Onboarding : Screen

    @Serializable
    data object Today : Screen

    @Serializable
    data object Reminders : Screen

    @Serializable
    data object Tasks : Screen

    @Serializable
    data object Habits : Screen

    @Serializable
    data object Insights : Screen

    @Serializable
    data object Calendar : Screen

    @Serializable
    data class Review(val dateString: String? = null) : Screen

    @Serializable
    data class Briefing(val dateString: String? = null) : Screen

    @Serializable
    data object Settings : Screen

    @Serializable
    data object Backup : Screen

    @Serializable
    data class ReminderCreate(val reminderId: String? = null) : Screen

    @Serializable
    data class ReminderDetail(val reminderId: String) : Screen

    @Serializable
    data class TaskCreate(val taskId: String? = null) : Screen

    @Serializable
    data class TaskDetail(val taskId: String) : Screen

    @Serializable
    data class HabitCreate(val habitId: String? = null) : Screen

    @Serializable
    data class HabitDetail(val habitId: String) : Screen

    @Serializable
    data class Plan(val dateString: String? = null) : Screen
}
