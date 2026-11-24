package com.dayflow.app.core.designsystem.components.charts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.DayflowTypography
import com.dayflow.app.domain.model.WeekStartDay
import java.time.LocalDate
import java.time.YearMonth

data class HeatmapDayData(
    val date: LocalDate,
    val score: Int, // 0 to 100
    val hasActivity: Boolean = false,
    val isToday: Boolean = false
)

@Composable
fun CalendarHeatmap(
    yearMonth: YearMonth,
    days: List<HeatmapDayData>,
    modifier: Modifier = Modifier,
    weekStartDay: WeekStartDay = WeekStartDay.MONDAY,
    onDayClick: ((LocalDate) -> Unit)? = null
) {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing

    val daysMap = days.associateBy { it.date }
    val firstOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()

    // Align starting offset based on weekStartDay
    val dayOfWeekVal = firstOfMonth.dayOfWeek.value // 1 (Mon) .. 7 (Sun)
    val startDayVal = weekStartDay.dayOfWeek.value
    val offset = (dayOfWeekVal - startDayVal + 7) % 7

    val dayHeaders = when (weekStartDay) {
        WeekStartDay.MONDAY -> listOf("M", "T", "W", "T", "F", "S", "S")
        WeekStartDay.SUNDAY -> listOf("S", "M", "T", "W", "T", "F", "S")
        WeekStartDay.SATURDAY -> listOf("S", "S", "M", "T", "W", "T", "F")
    }

    val totalCells = offset + daysInMonth
    val rowCount = (totalCells + 6) / 7

    val activeCount = days.count { it.hasActivity }
    val accessibilitySummary = "Monthly execution heatmap for ${yearMonth.month.name} ${yearMonth.year}. $activeCount active days recorded."

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = accessibilitySummary }
    ) {
        // Weekday headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            dayHeaders.forEach { header ->
                Text(
                    text = header,
                    style = DayflowTypography.labelSmall,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.xs))

        // Grid rows
        for (row in 0 until rowCount) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - offset + 1

                    if (dayNumber in 1..daysInMonth) {
                        val date = yearMonth.atDay(dayNumber)
                        val dayData = daysMap[date] ?: HeatmapDayData(date = date, score = 0)
                        val cellColor = getIntensityColor(dayData.score, dayData.hasActivity, colors.surfaceElevated, colors.primary)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(cellColor)
                                .then(
                                    if (dayData.isToday) {
                                        Modifier.border(BorderStroke(1.5.dp, colors.primary), RoundedCornerShape(4.dp))
                                    } else {
                                        Modifier
                                    }
                                )
                                .then(
                                    if (onDayClick != null) {
                                        Modifier.clickable { onDayClick(date) }
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayNumber.toString(),
                                style = DayflowTypography.labelSmall,
                                color = if (dayData.score > 50) colors.background else colors.textPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Empty padding cell
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.sm))

        // Heatmap Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Less",
                style = DayflowTypography.labelSmall,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.width(spacing.xs))

            val tierColors = listOf(
                colors.surfaceElevated,
                colors.primary.copy(alpha = 0.25f),
                colors.primary.copy(alpha = 0.50f),
                colors.primary.copy(alpha = 0.75f),
                colors.primary
            )

            tierColors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }

            Spacer(modifier = Modifier.width(spacing.xs))
            Text(
                text = "More",
                style = DayflowTypography.labelSmall,
                color = colors.textSecondary
            )
        }
    }
}

private fun getIntensityColor(score: Int, hasActivity: Boolean, emptyColor: Color, primaryColor: Color): Color {
    if (!hasActivity || score <= 0) return emptyColor
    return when {
        score <= 25 -> primaryColor.copy(alpha = 0.25f)
        score <= 50 -> primaryColor.copy(alpha = 0.50f)
        score <= 75 -> primaryColor.copy(alpha = 0.75f)
        else -> primaryColor
    }
}
