package com.dayflow.app.core.designsystem.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.TabularHeadline

@Composable
fun ProgressRing(
    progress: Float, // 0.0f to 1.0f
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    strokeWidth: Dp = 10.dp,
    progressColor: Color = DayflowTheme.colors.primary,
    trackColor: Color = DayflowTheme.colors.surfaceElevated,
    centerContent: (@Composable () -> Unit)? = null
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing
        ),
        label = "ProgressRingAnimation"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = Stroke(
                width = strokeWidth.toPx(),
                cap = StrokeCap.Round
            )
            val canvasSize = this.size.minDimension
            val radius = (canvasSize - strokeWidth.toPx()) / 2f
            val topLeft = androidx.compose.ui.geometry.Offset(
                (this.size.width - radius * 2) / 2f,
                (this.size.height - radius * 2) / 2f
            )
            val arcSize = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)

            // Background Track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )

            // Animated Progress Arc
            if (animatedProgress > 0f) {
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke
                )
            }
        }

        if (centerContent != null) {
            centerContent()
        } else {
            val percentage = (clampedProgress * 100).toInt()
            Text(
                text = "$percentage%",
                style = TabularHeadline,
                color = DayflowTheme.colors.textPrimary
            )
        }
    }
}

@Preview(name = "ProgressRing Dark", showBackground = true, backgroundColor = 0xFF07120F)
@Composable
private fun ProgressRingPreview() {
    DayflowTheme {
        ProgressRing(progress = 0.68f)
    }
}
