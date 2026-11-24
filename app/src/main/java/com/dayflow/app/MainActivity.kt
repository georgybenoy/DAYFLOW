package com.dayflow.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.navigation.Screen
import com.dayflow.app.domain.model.ThemePreference
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val destinationFromIntent = parseDestinationFromIntent(intent)

        setContent {
            val uiState by viewModel.uiState.collectAsState()

            when (val state = uiState) {
                is MainActivityUiState.Loading -> {
                    DayflowTheme(themePreference = ThemePreference.DARK) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(DayflowTheme.colors.background),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = DayflowTheme.colors.primary)
                        }
                    }
                }
                is MainActivityUiState.Success -> {
                    DayflowTheme(themePreference = state.themePreference) {
                        val startDestination = if (state.isOnboardingCompleted) {
                            destinationFromIntent ?: Screen.Today
                        } else {
                            Screen.Onboarding
                        }
                        DayflowApp(startDestination = startDestination)
                    }
                }
            }
        }
    }

    private fun parseDestinationFromIntent(intent: Intent?): Screen? {
        if (intent == null) return null

        val destination = intent.getStringExtra("EXTRA_DESTINATION")
        val dateString = intent.getStringExtra("EXTRA_DATE")
        if (destination != null) {
            return when (destination) {
                "plan" -> Screen.Plan(dateString)
                "briefing" -> Screen.Briefing(dateString)
                "review" -> Screen.Review(dateString)
                else -> null
            }
        }

        intent.getStringExtra("EXTRA_REMINDER_ID")?.let { return Screen.ReminderDetail(it) }
        intent.getStringExtra("EXTRA_HABIT_ID")?.let { return Screen.HabitDetail(it) }
        intent.getStringExtra("EXTRA_TASK_ID")?.let { return Screen.TaskDetail(it) }

        return null
    }
}