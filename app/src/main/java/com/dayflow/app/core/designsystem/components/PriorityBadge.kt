package com.dayflow.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowUp
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.DayflowTypography
import com.dayflow.app.domain.model.Priority

@Composable
fun PriorityBadge(
    priority: Priority,
    modifier: Modifier = Modifier
) {
    val colors = DayflowTheme.colors
    val shapes = DayflowTheme.shapes

    val (badgeBg, badgeText, badgeBorder, icon) = when (priority) {
        Priority.URGENT -> Quad(
            colors.error.copy(alpha = 0.15f),
            colors.error,
            colors.error.copy(alpha = 0.4f),
            Icons.Outlined.PriorityHigh
        )
        Priority.HIGH -> Quad(
            colors.warning.copy(alpha = 0.15f),
            colors.warning,
            colors.warning.copy(alpha = 0.4f),
            Icons.Outlined.KeyboardDoubleArrowUp
        )
        Priority.MEDIUM -> Quad(
            colors.primary.copy(alpha = 0.15f),
            colors.primary,
            colors.primary.copy(alpha = 0.4f),
            Icons.Outlined.Remove
        )
        Priority.LOW -> Quad(
            colors.surfaceElevated,
            colors.textSecondary,
            colors.border,
            Icons.Outlined.KeyboardArrowDown
        )
    }

    Box(
        modifier = modifier
            .background(color = badgeBg, shape = shapes.chip)
            .border(width = 1.dp, color = badgeBorder, shape = shapes.chip)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = badgeText,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = priority.name,
                style = DayflowTypography.labelSmall,
                color = badgeText
            )
        }
    }
}

private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

@Preview(name = "Priority Badges", showBackground = true, backgroundColor = 0xFF07120F)
@Composable
private fun PriorityBadgePreview() {
    DayflowTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PriorityBadge(priority = Priority.URGENT)
            PriorityBadge(priority = Priority.HIGH)
            PriorityBadge(priority = Priority.MEDIUM)
            PriorityBadge(priority = Priority.LOW)
        }
    }
}
