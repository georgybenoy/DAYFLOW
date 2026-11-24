package com.dayflow.app.feature.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.DayflowTypography
import com.dayflow.app.domain.model.ThemePreference
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    OnboardingContent(
        uiState = uiState,
        onNameChange = viewModel::updateName,
        onThemeChange = viewModel::updateTheme,
        onCompleteClick = { viewModel.completeOnboarding(onComplete) },
        onSkipClick = { viewModel.skipOnboarding(onComplete) },
        modifier = modifier
    )
}

@Composable
fun OnboardingContent(
    uiState: OnboardingUiState,
    onNameChange: (String) -> Unit,
    onThemeChange: (ThemePreference) -> Unit,
    onCompleteClick: () -> Unit,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing
    val shapes = DayflowTheme.shapes
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(scrollState)
            .padding(horizontal = spacing.screenHorizontal, vertical = spacing.xl)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DAYFLOW",
                style = DayflowTypography.labelSmall,
                color = colors.primary
            )
            TextButton(onClick = onSkipClick) {
                Text(
                    text = "Skip",
                    style = DayflowTypography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.sm))

        Text(
            text = "Make today count.",
            style = DayflowTypography.displayLarge,
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(spacing.xs))
        Text(
            text = "Your offline-first daily execution and habit companion.",
            style = DayflowTypography.bodyMedium,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(spacing.xl))

        // Step 1: Your Name
        Text(
            text = "1. What should we call you?",
            style = DayflowTypography.titleMedium,
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(spacing.sm))
        OutlinedTextField(
            value = uiState.name,
            onValueChange = onNameChange,
            placeholder = {
                Text("Enter your name (optional)", color = colors.textMuted)
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = shapes.input,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.border,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            )
        )

        Spacer(modifier = Modifier.height(spacing.xl))

        // Step 2: Theme Selection
        Text(
            text = "2. Appearance",
            style = DayflowTypography.titleMedium,
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(spacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            ThemeOptionCard(
                title = "Dark (Default)",
                selected = uiState.theme == ThemePreference.DARK,
                onClick = { onThemeChange(ThemePreference.DARK) },
                modifier = Modifier.weight(1f)
            )
            ThemeOptionCard(
                title = "Light",
                selected = uiState.theme == ThemePreference.LIGHT,
                onClick = { onThemeChange(ThemePreference.LIGHT) },
                modifier = Modifier.weight(1f)
            )
            ThemeOptionCard(
                title = "System",
                selected = uiState.theme == ThemePreference.SYSTEM,
                onClick = { onThemeChange(ThemePreference.SYSTEM) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(spacing.xl))

        // Step 3: Briefing & Review Schedules
        Text(
            text = "3. Daily Cadence",
            style = DayflowTypography.titleMedium,
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(spacing.sm))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = shapes.secondaryCard,
            colors = CardDefaults.cardColors(
                containerColor = colors.surface,
                contentColor = colors.textPrimary
            ),
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(modifier = Modifier.padding(spacing.cardPadding)) {
                TimeRow(
                    label = "Morning Briefing",
                    time = uiState.morningBriefingTime,
                    hint = "Prepares your day's agenda"
                )
                Spacer(modifier = Modifier.height(spacing.md))
                TimeRow(
                    label = "Evening Review",
                    time = uiState.eveningReviewTime,
                    hint = "Reviews progress and plans tomorrow"
                )
                Spacer(modifier = Modifier.height(spacing.md))
                TimeRow(
                    label = "Quiet Hours",
                    time = uiState.quietHoursStart,
                    timeEnd = uiState.quietHoursEnd,
                    hint = "Silences non-critical routine notifications"
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.xl))

        // Step 4: Notification Transparency Notice
        Text(
            text = "4. Notifications & Privacy",
            style = DayflowTypography.titleMedium,
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(spacing.sm))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = shapes.secondaryCard,
            colors = CardDefaults.cardColors(
                containerColor = colors.surfaceVariant,
                contentColor = colors.textPrimary
            ),
            border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(spacing.cardPadding),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(spacing.md))
                Column {
                    Text(
                        text = "100% Offline & Private",
                        style = DayflowTypography.titleMedium,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(spacing.xs))
                    Text(
                        text = "DAYFLOW requires exact alarms and local notifications to remind you at precise moments. No data ever leaves your device — there are no servers, no ads, and no external trackers.",
                        style = DayflowTypography.bodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(spacing.xs))
                    Text(
                        text = "Permissions will be requested contextually when you schedule your first alarm.",
                        style = DayflowTypography.labelSmall,
                        color = colors.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.largeSection))

        // Get Started Button
        Button(
            onClick = onCompleteClick,
            shape = shapes.button,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.background
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "Get Started",
                style = DayflowTypography.titleMedium
            )
        }
    }
}

@Composable
private fun ThemeOptionCard(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DayflowTheme.colors
    val shapes = DayflowTheme.shapes

    val backgroundColor = if (selected) colors.surfaceElevated else colors.surface
    val borderColor = if (selected) colors.primary else colors.border

    Box(
        modifier = modifier
            .clip(shapes.input)
            .background(backgroundColor)
            .border(1.dp, borderColor, shapes.input)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = DayflowTypography.labelSmall,
            color = if (selected) colors.primary else colors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TimeRow(
    label: String,
    time: LocalTime,
    hint: String,
    timeEnd: LocalTime? = null
) {
    val colors = DayflowTheme.colors
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

    val timeText = if (timeEnd != null) {
        "${time.format(timeFormatter)} – ${timeEnd.format(timeFormatter)}"
    } else {
        time.format(timeFormatter)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = DayflowTypography.bodyMedium,
                color = colors.textPrimary
            )
            Text(
                text = hint,
                style = DayflowTypography.labelSmall,
                color = colors.textMuted
            )
        }
        Text(
            text = timeText,
            style = DayflowTypography.titleMedium,
            color = colors.primary
        )
    }
}

@Preview(name = "OnboardingScreen Dark", showBackground = true, backgroundColor = 0xFF07120F)
@Composable
private fun OnboardingScreenPreview() {
    DayflowTheme {
        OnboardingContent(
            uiState = OnboardingUiState(),
            onNameChange = {},
            onThemeChange = {},
            onCompleteClick = {},
            onSkipClick = {}
        )
    }
}
