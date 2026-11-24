package com.dayflow.app.core.designsystem.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.DayflowTypography

data class BarChartItem(
    val label: String, // e.g. "Mon"
    val value: Float,  // 0.0f to 1.0f
    val isHighlighted: Boolean = false,
    val displayValue: String? = null // e.g. "85%"
)

@Composable
fun WeeklyBarChart(
    items: List<BarChartItem>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 140.dp,
    barSpacing: Dp = 12.dp
) {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing

    val accessibilitySummary = if (items.isEmpty() || items.all { it.value == 0f }) {
        "Weekly completion chart: No activity recorded for this week."
    } else {
        "Weekly completion chart: " + items.joinToString(", ") { "${it.label} ${(it.value * 100).toInt()}%" }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = accessibilitySummary }
    ) {
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No weekly activity data available",
                    style = DayflowTypography.bodySmall,
                    color = colors.textSecondary
                )
            }
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
            ) {
                val totalBars = items.size
                val spacingPx = barSpacing.toPx()
                val availableWidth = size.width - (spacingPx * (totalBars - 1))
                val barWidth = (availableWidth / totalBars).coerceAtLeast(8.dp.toPx())
                val maxBarHeight = size.height

                items.forEachIndexed { index, item ->
                    val xOffset = index * (barWidth + spacingPx)

                    // Draw track (full height)
                    drawRoundRect(
                        color = colors.surfaceElevated,
                        topLeft = Offset(xOffset, 0f),
                        size = Size(barWidth, maxBarHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    // Draw active bar
                    val activeHeight = (maxBarHeight * item.value.coerceIn(0f, 1f)).coerceAtLeast(0f)
                    if (activeHeight > 0f) {
                        val barColor = if (item.isHighlighted) colors.primary else colors.secondary
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(xOffset, maxBarHeight - activeHeight),
                            size = Size(barWidth, activeHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.xs))

            // Day labels below each bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                items.forEach { item ->
                    Text(
                        text = item.label,
                        style = DayflowTypography.labelSmall,
                        color = if (item.isHighlighted) colors.primary else colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
