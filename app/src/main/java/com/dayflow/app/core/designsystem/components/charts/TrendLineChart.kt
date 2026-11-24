package com.dayflow.app.core.designsystem.components.charts

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.DayflowTypography

data class TrendPoint(
    val label: String, // e.g. "Nov 12"
    val value: Float   // 0.0f to 1.0f (or score 0 to 100 normalized)
)

@Composable
fun TrendLineChart(
    points: List<TrendPoint>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 140.dp
) {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing

    val accessibilitySummary = if (points.isEmpty() || points.all { it.value == 0f }) {
        "Trend line chart: No completion trend data recorded yet."
    } else {
        val avg = (points.map { it.value }.average() * 100).toInt()
        "Trend line chart over ${points.size} days. Average score: $avg%."
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = accessibilitySummary }
    ) {
        if (points.size < 2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Insufficient data points for trend line",
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
                val width = size.width
                val height = size.height

                // Draw background grid lines (0%, 50%, 100%)
                val gridColor = colors.surfaceElevated
                val gridStroke = Stroke(width = 1.dp.toPx())

                // 100% line (top)
                drawLine(gridColor, Offset(0f, 0f), Offset(width, 0f), strokeWidth = gridStroke.width)
                // 50% line (middle)
                drawLine(gridColor, Offset(0f, height / 2f), Offset(width, height / 2f), strokeWidth = gridStroke.width)
                // 0% line (bottom)
                drawLine(gridColor, Offset(0f, height), Offset(width, height), strokeWidth = gridStroke.width)

                val stepX = width / (points.size - 1)
                val linePath = Path()
                val fillPath = Path()

                val coords = points.mapIndexed { index, point ->
                    val x = index * stepX
                    val y = height - (point.value.coerceIn(0f, 1f) * height)
                    Offset(x, y)
                }

                linePath.moveTo(coords.first().x, coords.first().y)
                fillPath.moveTo(coords.first().x, height)
                fillPath.lineTo(coords.first().x, coords.first().y)

                for (i in 1 until coords.size) {
                    val prev = coords[i - 1]
                    val curr = coords[i]
                    val midX = (prev.x + curr.x) / 2f
                    linePath.cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                    fillPath.cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                }

                fillPath.lineTo(coords.last().x, height)
                fillPath.close()

                // Draw gradient under the curve
                val gradient = Brush.verticalGradient(
                    colors = listOf(
                        colors.primary.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = height
                )
                drawPath(path = fillPath, brush = gradient)

                // Draw the stroke line
                drawPath(
                    path = linePath,
                    color = colors.primary,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw dots at points
                coords.forEach { pt ->
                    drawCircle(
                        color = colors.background,
                        radius = 4.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = colors.primary,
                        radius = 2.5.dp.toPx(),
                        center = pt
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.xs))

            // X-axis first, mid, and last labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = points.first().label,
                    style = DayflowTypography.labelSmall,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.weight(1f))
                if (points.size > 2) {
                    Text(
                        text = points[points.size / 2].label,
                        style = DayflowTypography.labelSmall,
                        color = colors.textSecondary
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = points.last().label,
                    style = DayflowTypography.labelSmall,
                    color = colors.textSecondary
                )
            }
        }
    }
}
