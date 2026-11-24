package com.dayflow.app.feature.insights

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.DayflowTypography
import com.dayflow.app.core.designsystem.TabularHeadline
import com.dayflow.app.core.designsystem.components.EmptyState
import com.dayflow.app.core.designsystem.components.MetricCard
import com.dayflow.app.core.designsystem.components.MultiProgressRing
import com.dayflow.app.core.designsystem.components.RingData
import com.dayflow.app.core.designsystem.components.charts.BarChartItem
import com.dayflow.app.core.designsystem.components.charts.CalendarHeatmap
import com.dayflow.app.core.designsystem.components.charts.HeatmapDayData
import com.dayflow.app.core.designsystem.components.charts.TrendLineChart
import com.dayflow.app.core.designsystem.components.charts.TrendPoint
import com.dayflow.app.core.designsystem.components.charts.WeeklyBarChart
import com.dayflow.app.domain.analytics.DailyAnalytics
import com.dayflow.app.domain.analytics.InsightsData
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun InsightsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    InsightsContent(
        uiState = uiState,
        onFilterSelected = viewModel::setFilter,
        onRefresh = viewModel::refresh,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsContent(
    uiState: InsightsUiState,
    onFilterSelected: (InsightsViewFilter) -> Unit,
    onRefresh: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Productivity Insights",
                        style = DayflowTypography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh insights",
                            tint = colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background
                )
            )
        }
    ) { innerPadding ->
        when (uiState) {
            is InsightsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.primary)
                }
            }
            is InsightsUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        title = "Could not load insights",
                        message = uiState.message,
                        icon = Icons.Outlined.Info
                    )
                }
            }
            is InsightsUiState.Success -> {
                InsightsSuccessBody(
                    data = uiState.data,
                    selectedFilter = uiState.selectedFilter,
                    weekStartDay = uiState.weekStartDay,
                    onFilterSelected = onFilterSelected,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun InsightsSuccessBody(
    data: InsightsData,
    selectedFilter: InsightsViewFilter,
    weekStartDay: com.dayflow.app.domain.model.WeekStartDay,
    onFilterSelected: (InsightsViewFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Filter selector chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InsightsViewFilter.entries.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterSelected(filter) },
                    label = {
                        Text(
                            text = filter.label,
                            style = DayflowTypography.labelMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.primary,
                        selectedLabelColor = colors.background,
                        containerColor = colors.surfaceElevated,
                        labelColor = colors.textSecondary
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (selectedFilter == filter) colors.primary else colors.border
                    )
                )
            }
        }

        // Hero: Execution Score Card with concentric rings
        ExecutionScoreHeroCard(today = data.today)

        // Summary Metric Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Tasks Completed",
                value = data.weekly.totalTasksCompleted.toString(),
                subtitle = "This week",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Habit Consistency",
                value = "${data.weekly.habitConsistencyPercentage}%",
                subtitle = "Scheduled days",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Current Streak",
                value = "${data.streakSummary.currentStreakDays}d",
                subtitle = "Best: ${data.streakSummary.longestStreakDays}d",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Reminders",
                value = "${data.weekly.completedRemindersCount}",
                subtitle = "${data.weekly.missedRemindersCount} missed",
                modifier = Modifier.weight(1f)
            )
        }

        // Visualizations based on selected filter
        when (selectedFilter) {
            InsightsViewFilter.WEEK -> {
                WeeklySection(data = data)
                TrendSection(data = data)
            }
            InsightsViewFilter.MONTH -> {
                MonthlySection(data = data, weekStartDay = weekStartDay)
            }
            InsightsViewFilter.ALL_TIME -> {
                WeeklySection(data = data)
                TrendSection(data = data)
                MonthlySection(data = data, weekStartDay = weekStartDay)
            }
        }

        Spacer(modifier = Modifier.height(spacing.largeSection))
    }
}

@Composable
private fun ExecutionScoreHeroCard(today: DailyAnalytics) {
    val colors = DayflowTheme.colors
    val shapes = DayflowTheme.shapes
    val spacing = DayflowTheme.spacing

    val ringData = remember(today) {
        listOf(
            RingData(progress = today.taskRatio, color = colors.primary),
            RingData(progress = today.habitRatio, color = colors.accentLavender),
            RingData(progress = today.reminderRatio, color = colors.secondary)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shapes.mainCard,
        colors = CardDefaults.cardColors(
            containerColor = colors.surface,
            contentColor = colors.textPrimary
        ),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.cardPadding)
        ) {
            // Header with App-Defined Metric badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "DAILY EXECUTION SCORE",
                    style = DayflowTypography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.surfaceElevated)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "APP-DEFINED METRIC",
                        style = DayflowTypography.labelSmall,
                        color = colors.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Center area with Concentric MultiProgressRing and Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MultiProgressRing(
                    rings = ringData,
                    size = 130.dp,
                    strokeWidth = 9.dp,
                    ringSpacing = 4.dp,
                    centerContent = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${today.executionScore}",
                                style = TabularHeadline,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "/100",
                                style = DayflowTypography.labelSmall,
                                color = colors.textMuted
                            )
                        }
                    }
                )

                // Breakdown legend
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LegendItem(
                        label = "Tasks",
                        value = "${(today.taskRatio * 100).toInt()}%",
                        color = colors.primary
                    )
                    LegendItem(
                        label = "Habits",
                        value = "${(today.habitRatio * 100).toInt()}%",
                        color = colors.accentLavender
                    )
                    LegendItem(
                        label = "Reminders",
                        value = "${(today.reminderRatio * 100).toInt()}%",
                        color = colors.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Non-medical disclaimer
            Text(
                text = "Deterministic execution score derived from task, habit, and reminder completion rates. Not a medical or health assessment.",
                style = DayflowTypography.labelSmall,
                color = colors.textMuted
            )
        }
    }
}

@Composable
private fun LegendItem(label: String, value: String, color: Color) {
    val colors = DayflowTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$label: ",
            style = DayflowTypography.bodySmall,
            color = colors.textSecondary
        )
        Text(
            text = value,
            style = DayflowTypography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )
    }
}

@Composable
private fun WeeklySection(data: InsightsData) {
    val colors = DayflowTheme.colors
    val shapes = DayflowTheme.shapes
    val spacing = DayflowTheme.spacing

    val barItems = remember(data.weekly.days) {
        data.weekly.days.map { day ->
            val dayLabel = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            BarChartItem(
                label = dayLabel,
                value = day.executionScore / 100f,
                isHighlighted = (day.date == data.today.date),
                displayValue = "${day.executionScore}%"
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shapes.mainCard,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.padding(spacing.cardPadding)) {
            Text(
                text = "WEEKLY COMPLETION",
                style = DayflowTypography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Average score: ${data.weekly.averageScore}%",
                style = DayflowTypography.bodySmall,
                color = colors.textMuted
            )
            Spacer(modifier = Modifier.height(16.dp))
            WeeklyBarChart(items = barItems)
        }
    }
}

@Composable
private fun TrendSection(data: InsightsData) {
    val colors = DayflowTheme.colors
    val shapes = DayflowTheme.shapes
    val spacing = DayflowTheme.spacing

    val trendPoints = remember(data.trendDays) {
        data.trendDays.map { day ->
            val label = day.date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
            TrendPoint(
                label = label,
                value = day.executionScore / 100f
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shapes.mainCard,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.padding(spacing.cardPadding)) {
            Text(
                text = "14-DAY PERFORMANCE TREND",
                style = DayflowTypography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Daily execution score trajectory",
                style = DayflowTypography.bodySmall,
                color = colors.textMuted
            )
            Spacer(modifier = Modifier.height(16.dp))
            TrendLineChart(points = trendPoints)
        }
    }
}

@Composable
private fun MonthlySection(
    data: InsightsData,
    weekStartDay: com.dayflow.app.domain.model.WeekStartDay
) {
    val colors = DayflowTheme.colors
    val shapes = DayflowTheme.shapes
    val spacing = DayflowTheme.spacing

    val heatmapData = remember(data.monthly.days) {
        data.monthly.days.map { day ->
            HeatmapDayData(
                date = day.date,
                score = day.executionScore,
                hasActivity = day.hasActivity,
                isToday = (day.date == data.today.date)
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shapes.mainCard,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.padding(spacing.cardPadding)) {
            Text(
                text = "MONTHLY CONSISTENCY",
                style = DayflowTypography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${data.monthly.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${data.monthly.yearMonth.year} · Avg ${data.monthly.averageScore}%",
                style = DayflowTypography.bodySmall,
                color = colors.textMuted
            )
            Spacer(modifier = Modifier.height(16.dp))
            CalendarHeatmap(
                yearMonth = data.monthly.yearMonth,
                days = heatmapData,
                weekStartDay = weekStartDay
            )
        }
    }
}
