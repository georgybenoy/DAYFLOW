package com.dayflow.app.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.dayflow.app.core.designsystem.components.DayflowScaffold
import com.dayflow.app.domain.model.DateFormatPreference
import com.dayflow.app.domain.model.ReminderPrecision
import com.dayflow.app.domain.model.ThemePreference
import com.dayflow.app.domain.model.TimeFormatPreference
import com.dayflow.app.domain.model.UserProfile
import com.dayflow.app.domain.model.WeekStartDay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBackup: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val userProfile by viewModel.userProfile.collectAsState()

    SettingsContent(
        userProfile = userProfile,
        onThemeChange = viewModel::setTheme,
        onWeekStartChange = viewModel::setWeekStartDay,
        onDateFormatChange = viewModel::setDateFormatPreference,
        onTimeFormatChange = viewModel::setTimeFormatPreference,
        onSoundChange = viewModel::setNotificationSoundEnabled,
        onVibrationChange = viewModel::setNotificationVibrationEnabled,
        onPrecisionChange = viewModel::setDefaultReminderPrecision,
        onNavigateBack = onNavigateBack,
        onNavigateToBackup = onNavigateToBackup,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    userProfile: UserProfile,
    onThemeChange: (ThemePreference) -> Unit,
    onWeekStartChange: (WeekStartDay) -> Unit,
    onDateFormatChange: (DateFormatPreference) -> Unit,
    onTimeFormatChange: (TimeFormatPreference) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onVibrationChange: (Boolean) -> Unit,
    onPrecisionChange: (ReminderPrecision) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing
    val shapes = DayflowTheme.shapes
    val scrollState = rememberScrollState()

    DayflowScaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = DayflowTypography.titleLarge,
                        color = colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.textPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(colors.background)
                .verticalScroll(scrollState)
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.md)
        ) {
            // Theme Section
            Text(
                text = "Theme",
                style = DayflowTypography.titleMedium,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(spacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                SettingOptionButton(
                    title = "Dark (Default)",
                    selected = userProfile.themePreference == ThemePreference.DARK,
                    onClick = { onThemeChange(ThemePreference.DARK) },
                    modifier = Modifier.weight(1f)
                )
                SettingOptionButton(
                    title = "Light",
                    selected = userProfile.themePreference == ThemePreference.LIGHT,
                    onClick = { onThemeChange(ThemePreference.LIGHT) },
                    modifier = Modifier.weight(1f)
                )
                SettingOptionButton(
                    title = "System",
                    selected = userProfile.themePreference == ThemePreference.SYSTEM,
                    onClick = { onThemeChange(ThemePreference.SYSTEM) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(spacing.sectionSpacing))

            // Calendar & Formatting Section
            Text(
                text = "Calendar & Formatting",
                style = DayflowTypography.titleMedium,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(spacing.sm))

            Text(
                text = "Week Start Day",
                style = DayflowTypography.bodySmall,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(spacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                WeekStartDay.entries.forEach { option ->
                    SettingOptionButton(
                        title = option.displayName,
                        selected = userProfile.weekStartDay == option,
                        onClick = { onWeekStartChange(option) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.md))

            Text(
                text = "Time Format",
                style = DayflowTypography.bodySmall,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(spacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                SettingOptionButton(
                    title = "12-hour (AM/PM)",
                    selected = userProfile.timeFormatPreference == TimeFormatPreference.H12,
                    onClick = { onTimeFormatChange(TimeFormatPreference.H12) },
                    modifier = Modifier.weight(1f)
                )
                SettingOptionButton(
                    title = "24-hour",
                    selected = userProfile.timeFormatPreference == TimeFormatPreference.H24,
                    onClick = { onTimeFormatChange(TimeFormatPreference.H24) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(spacing.md))

            Text(
                text = "Date Format",
                style = DayflowTypography.bodySmall,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(spacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                SettingOptionButton(
                    title = "System",
                    selected = userProfile.dateFormatPreference == DateFormatPreference.SYSTEM,
                    onClick = { onDateFormatChange(DateFormatPreference.SYSTEM) },
                    modifier = Modifier.weight(1f)
                )
                SettingOptionButton(
                    title = "YYYY-MM-DD",
                    selected = userProfile.dateFormatPreference == DateFormatPreference.ISO_YYYY_MM_DD,
                    onClick = { onDateFormatChange(DateFormatPreference.ISO_YYYY_MM_DD) },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(spacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                SettingOptionButton(
                    title = "MM/DD/YYYY",
                    selected = userProfile.dateFormatPreference == DateFormatPreference.MM_DD_YYYY,
                    onClick = { onDateFormatChange(DateFormatPreference.MM_DD_YYYY) },
                    modifier = Modifier.weight(1f)
                )
                SettingOptionButton(
                    title = "DD/MM/YYYY",
                    selected = userProfile.dateFormatPreference == DateFormatPreference.DD_MM_YYYY,
                    onClick = { onDateFormatChange(DateFormatPreference.DD_MM_YYYY) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(spacing.sectionSpacing))

            // Notifications & Precision Section
            Text(
                text = "Notifications & Reminders",
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notification Sound",
                                style = DayflowTypography.titleSmall,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Play audio cue when notifications trigger",
                                style = DayflowTypography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                        Switch(
                            checked = userProfile.isSoundEnabled,
                            onCheckedChange = onSoundChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.background,
                                checkedTrackColor = colors.primary,
                                uncheckedThumbColor = colors.textSecondary,
                                uncheckedTrackColor = colors.surfaceElevated
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(spacing.md))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Vibration",
                                style = DayflowTypography.titleSmall,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Vibrate device for upcoming reminders and deadlines",
                                style = DayflowTypography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                        Switch(
                            checked = userProfile.isVibrationEnabled,
                            onCheckedChange = onVibrationChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.background,
                                checkedTrackColor = colors.primary,
                                uncheckedThumbColor = colors.textSecondary,
                                uncheckedTrackColor = colors.surfaceElevated
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(spacing.md))

                    Text(
                        text = "Default Reminder Precision",
                        style = DayflowTypography.titleSmall,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(spacing.xs))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        SettingOptionButton(
                            title = "Approximate",
                            selected = userProfile.defaultReminderPrecision == ReminderPrecision.APPROXIMATE,
                            onClick = { onPrecisionChange(ReminderPrecision.APPROXIMATE) },
                            modifier = Modifier.weight(1f)
                        )
                        SettingOptionButton(
                            title = "Exact Alarm",
                            selected = userProfile.defaultReminderPrecision == ReminderPrecision.EXACT,
                            onClick = { onPrecisionChange(ReminderPrecision.EXACT) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.sectionSpacing))

            // Data & Storage Section
            Text(
                text = "Data & Storage",
                style = DayflowTypography.titleMedium,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(spacing.sm))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.secondaryCard)
                    .clickable(onClick = onNavigateToBackup),
                shape = shapes.secondaryCard,
                colors = CardDefaults.cardColors(
                    containerColor = colors.surface,
                    contentColor = colors.textPrimary
                ),
                border = BorderStroke(1.dp, colors.border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.cardPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Backup & Restore",
                            style = DayflowTypography.titleSmall,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(spacing.xs))
                        Text(
                            text = "Automated 24h snapshots, manual export & import",
                            style = DayflowTypography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.Backup,
                        contentDescription = "Backup & Restore",
                        tint = colors.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.sectionSpacing))

            // About Section
            Text(
                text = "About DAYFLOW",
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
                    Text(
                        text = "DAYFLOW v1.0.0",
                        style = DayflowTypography.titleMedium,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(spacing.xs))
                    Text(
                        text = "Offline-first personal productivity & daily execution system.",
                        style = DayflowTypography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingOptionButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DayflowTheme.colors
    val shapes = DayflowTheme.shapes

    Box(
        modifier = modifier
            .clip(shapes.input)
            .background(if (selected) colors.surfaceElevated else colors.surface)
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

@Preview(name = "SettingsScreen Preview", showBackground = true, backgroundColor = 0xFF07120F)
@Composable
private fun SettingsScreenPreview() {
    DayflowTheme {
        SettingsContent(
            userProfile = UserProfile(),
            onThemeChange = {},
            onWeekStartChange = {},
            onDateFormatChange = {},
            onTimeFormatChange = {},
            onSoundChange = {},
            onVibrationChange = {},
            onPrecisionChange = {},
            onNavigateBack = {},
            onNavigateToBackup = {}
        )
    }
}
