package com.dayflow.app

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.components.DayflowBottomNavigation
import com.dayflow.app.core.designsystem.components.DayflowScaffold
import com.dayflow.app.core.designsystem.components.DayflowTab
import com.dayflow.app.core.navigation.Screen
import com.dayflow.app.feature.backup.BackupScreen
import com.dayflow.app.feature.briefing.BriefingScreen
import com.dayflow.app.feature.calendar.CalendarScreen
import com.dayflow.app.feature.habits.HabitCreateScreen
import com.dayflow.app.feature.habits.HabitDetailScreen
import com.dayflow.app.feature.habits.HabitsScreen
import com.dayflow.app.feature.insights.InsightsScreen
import com.dayflow.app.feature.onboarding.OnboardingScreen
import com.dayflow.app.feature.plan.PlanScreen
import com.dayflow.app.feature.reminders.ReminderCreateScreen
import com.dayflow.app.feature.reminders.ReminderDetailScreen
import com.dayflow.app.feature.reminders.RemindersScreen
import com.dayflow.app.feature.review.ReviewScreen
import com.dayflow.app.feature.settings.SettingsScreen
import com.dayflow.app.feature.tasks.TaskCreateScreen
import com.dayflow.app.feature.tasks.TaskDetailScreen
import com.dayflow.app.feature.tasks.TasksScreen
import com.dayflow.app.feature.today.TodayScreen

@Composable
fun DayflowApp(
    startDestination: Screen,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isTopLevelDestination = currentDestination?.let { dest ->
        dest.hasRoute(Screen.Today::class) ||
        dest.hasRoute(Screen.Reminders::class) ||
        dest.hasRoute(Screen.Tasks::class) ||
        dest.hasRoute(Screen.Habits::class) ||
        dest.hasRoute(Screen.Insights::class)
    } ?: false

    val currentTab = when {
        currentDestination?.hasRoute(Screen.Reminders::class) == true -> DayflowTab.REMINDERS
        currentDestination?.hasRoute(Screen.Tasks::class) == true -> DayflowTab.TASKS
        currentDestination?.hasRoute(Screen.Habits::class) == true -> DayflowTab.HABITS
        currentDestination?.hasRoute(Screen.Insights::class) == true -> DayflowTab.INSIGHTS
        else -> DayflowTab.TODAY
    }

    DayflowScaffold(
        modifier = modifier,
        bottomBar = {
            if (isTopLevelDestination) {
                DayflowBottomNavigation(
                    currentTab = currentTab,
                    onTabSelected = { tab ->
                        val targetRoute: Screen = when (tab) {
                            DayflowTab.TODAY -> Screen.Today
                            DayflowTab.REMINDERS -> Screen.Reminders
                            DayflowTab.TASKS -> Screen.Tasks
                            DayflowTab.HABITS -> Screen.Habits
                            DayflowTab.INSIGHTS -> Screen.Insights
                        }
                        navController.navigate(targetRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None }
        ) {
            // Onboarding
            composable<Screen.Onboarding> {
                OnboardingScreen(
                    onComplete = {
                        navController.navigate(Screen.Today) {
                            popUpTo(Screen.Onboarding) { inclusive = true }
                        }
                    }
                )
            }

            // Top-level tabs
            composable<Screen.Today> {
                TodayScreen(
                    onNavigateToSettings = { navController.navigate(Screen.Settings) },
                    onNavigateToCalendar = { navController.navigate(Screen.Calendar) },
                    onNavigateToCreateReminder = { navController.navigate(Screen.ReminderCreate()) },
                    onNavigateToCreateTask = { navController.navigate(Screen.TaskCreate()) },
                    onNavigateToCreateHabit = { navController.navigate(Screen.HabitCreate()) },
                    onNavigateToTaskDetail = { id -> navController.navigate(Screen.TaskDetail(id)) },
                    onNavigateToPlan = { date -> navController.navigate(Screen.Plan(date.toString())) },
                    onNavigateToBriefing = { date -> navController.navigate(Screen.Briefing(date.toString())) },
                    onNavigateToReview = { date -> navController.navigate(Screen.Review(date.toString())) }
                )
            }

            composable<Screen.Reminders> {
                RemindersScreen(
                    onNavigateToCreate = { navController.navigate(Screen.ReminderCreate()) },
                    onNavigateToEdit = { reminderId -> navController.navigate(Screen.ReminderCreate(reminderId)) },
                    onNavigateToDetail = { reminderId -> navController.navigate(Screen.ReminderDetail(reminderId)) }
                )
            }

            composable<Screen.Tasks> {
                TasksScreen(
                    onNavigateToCreate = { navController.navigate(Screen.TaskCreate()) },
                    onNavigateToEdit = { id -> navController.navigate(Screen.TaskCreate(id)) },
                    onNavigateToDetail = { id -> navController.navigate(Screen.TaskDetail(id)) }
                )
            }

            composable<Screen.Habits> {
                HabitsScreen(
                    onNavigateToCreate = { navController.navigate(Screen.HabitCreate()) },
                    onNavigateToEdit = { id -> navController.navigate(Screen.HabitCreate(id)) },
                    onNavigateToDetail = { id -> navController.navigate(Screen.HabitDetail(id)) }
                )
            }

            composable<Screen.Insights> {
                InsightsScreen(onNavigateBack = { navController.popBackStack() })
            }

            // Secondary routes
            composable<Screen.Calendar> {
                CalendarScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToTaskDetail = { id -> navController.navigate(Screen.TaskDetail(id)) },
                    onNavigateToReminderDetail = { id -> navController.navigate(Screen.ReminderDetail(id)) },
                    onNavigateToHabitDetail = { id -> navController.navigate(Screen.HabitDetail(id)) },
                    onNavigateToPlan = { dateStr -> navController.navigate(Screen.Plan(dateStr)) }
                )
            }

            composable<Screen.Review> {
                ReviewScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPlan = { tomorrow -> navController.navigate(Screen.Plan(tomorrow.toString())) }
                )
            }

            composable<Screen.Briefing> {
                BriefingScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPlan = { date -> navController.navigate(Screen.Plan(date.toString())) }
                )
            }

            composable<Screen.Settings> {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToBackup = { navController.navigate(Screen.Backup) }
                )
            }

            composable<Screen.Backup> {
                BackupScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable<Screen.ReminderCreate> { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.ReminderCreate>()
                ReminderCreateScreen(
                    reminderId = route.reminderId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Screen.ReminderDetail> { backStackEntry ->
                val detail = backStackEntry.toRoute<Screen.ReminderDetail>()
                ReminderDetailScreen(
                    reminderId = detail.reminderId,
                    onNavigateToEdit = { id -> navController.navigate(Screen.ReminderCreate(id)) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Screen.TaskCreate> { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.TaskCreate>()
                TaskCreateScreen(
                    taskId = route.taskId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Screen.TaskDetail> { backStackEntry ->
                val detail = backStackEntry.toRoute<Screen.TaskDetail>()
                TaskDetailScreen(
                    taskId = detail.taskId,
                    onNavigateToEdit = { id -> navController.navigate(Screen.TaskCreate(id)) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Screen.HabitCreate> { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.HabitCreate>()
                HabitCreateScreen(
                    habitId = route.habitId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Screen.HabitDetail> { backStackEntry ->
                val detail = backStackEntry.toRoute<Screen.HabitDetail>()
                HabitDetailScreen(
                    habitId = detail.habitId,
                    onNavigateToEdit = { id -> navController.navigate(Screen.HabitCreate(id)) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Screen.Plan> {
                PlanScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
