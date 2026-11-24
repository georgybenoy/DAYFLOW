package com.dayflow.app.core.designsystem.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.TabularHeadline

@Immutable
data class RingData(
    val progress: Float, // 0.0f to 1.0f
    val color: Color,
    val trackColor: Color? = null
)

@Composable
fun MultiProgressRing(
    rings: List<RingData>,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    strokeWidth: Dp = 8.dp,
    ringSpacing: Dp = 4.dp,
    centerContent: (@Composable () -> Unit)? = null
) {
    val animatedProgresses = rings.map { ring ->
        animateFloatAsState(
            targetValue = ring.progress.coerceIn(0f, 1f),
            animationSpec = tween(
                durationMillis = 400,
                easing = FastOutSlowInEasing
            ),
            label = "MultiRingProgress"
        )
    }

    val defaultTrackColor = DayflowTheme.colors.surfaceElevated

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val spacingPx = ringSpacing.toPx()
            val stroke = Stroke(width = strokePx, cap = StrokeCap.Round)

            rings.forEachIndexed { index, ring ->
                val progress = animatedProgresses[index].value
                val inset = index * (strokePx + spacingPx) + strokePx / 2f
                val diameter = this.size.minDimension - (inset * 2f)

                if (diameter > 0f) {
                    val topLeft = Offset(
                        (this.size.width - diameter) / 2f,
                        (this.size.height - diameter) / 2f
                    )
                    val arcSize = Size(diameter, diameter)

                    // Track
                    drawArc(
                        color = ring.trackColor ?: defaultTrackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke
                    )

                    // Animated Progress
                    if (progress > 0f) {
                        drawArc(
                            color = ring.color,
                            startAngle = -90f,
                            sweepAngle = progress * 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = stroke
                        )
                    }
                }
            }
        }

        if (centerContent != null) {
            centerContent()
        }
    }
}

@Preview(name = "MultiProgressRing Dark", showBackground = true, backgroundColor = 0xFF07120F)
@Composable
private fun MultiProgressRingPreview() {
    DayflowTheme {
        MultiProgressRing(
            rings = listOf(
                RingData(progress = 0.85f, color = DayflowTheme.colors.primary),
                RingData(progress = 0.60f, color = DayflowTheme.colors.accentYellow),
                RingData(progress = 0.40f, color = DayflowTheme.colors.accentLavender)
            ),
            centerContent = {
                Text(
                    text = "68%",
                    style = TabularHeadline,
                    color = DayflowTheme.colors.textPrimary
                )
            }
        )
    }
}
