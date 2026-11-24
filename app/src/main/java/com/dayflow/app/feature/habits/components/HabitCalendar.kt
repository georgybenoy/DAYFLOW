package com.dayflow.app.feature.habits.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.DayflowTypography
import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.HabitLog
import com.dayflow.app.domain.recurrence.HabitDayStatus
import com.dayflow.app.domain.recurrence.StreakCalculator
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HabitCalendar(
    habit: Habit,
    logs: List<HabitLog>,
    modifier: Modifier = Modifier,
    initialMonth: YearMonth = YearMonth.now(),
    onDayClick: ((LocalDate) -> Unit)? = null
) {
    var currentMonth by remember { mutableStateOf(initialMonth) }
    val monthlyHistory = remember(habit, logs, currentMonth) {
        StreakCalculator.getMonthlyHistory(habit, logs, currentMonth)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DayflowTheme.colors.surface,
        shape = DayflowTheme.shapes.mainCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Month Switcher Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                    style = DayflowTypography.titleMedium,
                    color = DayflowTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )

                Row {
                    IconButton(
                        onClick = { currentMonth = currentMonth.minusMonths(1) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Previous Month",
                            tint = DayflowTheme.colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { currentMonth = currentMonth.plusMonths(1) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = "Next Month",
                            tint = DayflowTheme.colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Day of week labels
            val dayHeaders = listOf("M", "T", "W", "T", "F", "S", "S")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayHeaders.forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        style = DayflowTypography.labelSmall,
                        color = DayflowTheme.colors.textMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Days Grid
            val firstDayOfMonth = currentMonth.atDay(1)
            // Monday is 1, Sunday is 7 -> offset is dayOfWeek.value - 1
            val leadingEmptyDays = firstDayOfMonth.dayOfWeek.value - 1
            val daysInMonth = currentMonth.lengthOfMonth()
            val totalCells = leadingEmptyDays + daysInMonth
            val totalRows = (totalCells + 6) / 7

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (rowIndex in 0 until totalRows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (colIndex in 0 until 7) {
                            val cellIndex = rowIndex * 7 + colIndex
                            val dayNumber = cellIndex - leadingEmptyDays + 1

                            if (dayNumber in 1..daysInMonth) {
                                val status = monthlyHistory.getOrNull(dayNumber - 1)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                ) {
                                    if (status != null) {
                                        HeatmapDayCell(
                                            status = status,
                                            dayNumber = dayNumber,
                                            onClick = { onDayClick?.invoke(status.date) }
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Heatmap Legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Less",
                    style = DayflowTypography.labelSmall.copy(fontSize = 10.sp),
                    color = DayflowTheme.colors.textMuted,
                    modifier = Modifier.padding(end = 4.dp)
                )

                listOf(0f, 0.25f, 0.5f, 0.75f, 1.0f).forEach { intensity ->
                    val color = when {
                        intensity == 0f -> DayflowTheme.colors.surfaceElevated
                        intensity <= 0.25f -> DayflowTheme.colors.primary.copy(alpha = 0.25f)
                        intensity <= 0.5f -> DayflowTheme.colors.primary.copy(alpha = 0.5f)
                        intensity <= 0.75f -> DayflowTheme.colors.primary.copy(alpha = 0.75f)
                        else -> DayflowTheme.colors.primary
                    }
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color)
                    )
                }

                Text(
                    text = "More",
                    style = DayflowTypography.labelSmall.copy(fontSize = 10.sp),
                    color = DayflowTheme.colors.textMuted,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
fun HeatmapDayCell(
    status: HabitDayStatus,
    dayNumber: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cellColor = when {
        status.isCompleted -> DayflowTheme.colors.primary
        status.progressRatio >= 0.75f -> DayflowTheme.colors.primary.copy(alpha = 0.75f)
        status.progressRatio >= 0.5f -> DayflowTheme.colors.primary.copy(alpha = 0.50f)
        status.progressRatio > 0.0f -> DayflowTheme.colors.primary.copy(alpha = 0.25f)
        status.isScheduled -> DayflowTheme.colors.surfaceElevated
        else -> Color.Transparent
    }

    val contentColor = when {
        status.isCompleted -> DayflowTheme.colors.background
        status.progressRatio > 0.5f -> DayflowTheme.colors.background
        status.isScheduled -> DayflowTheme.colors.textSecondary
        else -> DayflowTheme.colors.textMuted.copy(alpha = 0.5f)
    }

    val borderModifier = if (status.isScheduled && !status.isCompleted && status.progressRatio == 0f) {
        Modifier.border(0.5.dp, DayflowTheme.colors.border, RoundedCornerShape(6.dp))
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(cellColor)
            .then(borderModifier)
            .clickable(onClick = onClick)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (status.isCompleted) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = DayflowTheme.colors.background,
                modifier = Modifier.size(12.dp)
            )
        } else {
            Text(
                text = dayNumber.toString(),
                style = DayflowTypography.labelSmall.copy(fontSize = 10.sp),
                color = contentColor,
                fontWeight = if (status.isScheduled) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
