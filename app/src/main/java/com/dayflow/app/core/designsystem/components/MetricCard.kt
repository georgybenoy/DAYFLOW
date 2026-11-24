package com.dayflow.app.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.TabularHeadline

@Composable
fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    isElevated: Boolean = false,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = DayflowTheme.colors
    val shapes = DayflowTheme.shapes
    val spacing = DayflowTheme.spacing

    val backgroundColor = if (isElevated) colors.surfaceElevated else colors.surface
    val shape = if (isElevated) shapes.secondaryCard else shapes.mainCard

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = colors.textPrimary
        ),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = DayflowTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(spacing.xs))
                Text(
                    text = value,
                    style = TabularHeadline,
                    color = colors.textPrimary
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(spacing.xs))
                    Text(
                        text = subtitle,
                        style = DayflowTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                }
            }

            if (trailingContent != null) {
                trailingContent()
            }
        }
    }
}

private val DayflowTheme.typography
    get() = com.dayflow.app.core.designsystem.DayflowTypography

@Preview(name = "MetricCard Preview", showBackground = true, backgroundColor = 0xFF07120F)
@Composable
private fun MetricCardPreview() {
    DayflowTheme {
        MetricCard(
            title = "Daily Execution",
            value = "8 / 12 tasks",
            subtitle = "67% completed today",
            trailingContent = {
                ProgressRing(progress = 0.67f, size = 48.dp, strokeWidth = 5.dp)
            }
        )
    }
}
