package com.dayflow.app.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.DayflowTypography

@Composable
fun PermissionBanner(
    title: String,
    description: String,
    actionLabel: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.NotificationsActive
) {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing
    val shapes = DayflowTheme.shapes

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shapes.secondaryCard,
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceVariant,
            contentColor = colors.textPrimary
        ),
        border = BorderStroke(1.dp, colors.warning.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.cardPadding)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.warning,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(spacing.sm))
                Text(
                    text = title,
                    style = DayflowTypography.titleMedium,
                    color = colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.height(spacing.xs))
            Text(
                text = description,
                style = DayflowTypography.bodySmall,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(spacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onActionClick,
                    shape = shapes.button,
                    border = BorderStroke(1.dp, colors.primary)
                ) {
                    Text(
                        text = actionLabel,
                        style = DayflowTypography.labelSmall,
                        color = colors.primary
                    )
                }
            }
        }
    }
}

@Preview(name = "PermissionBanner Dark", showBackground = true, backgroundColor = 0xFF07120F)
@Composable
private fun PermissionBannerPreview() {
    DayflowTheme {
        PermissionBanner(
            title = "Exact Alarms Permission",
            description = "DAYFLOW requires exact alarm permission to deliver time-critical reminders reliably when the phone is asleep.",
            actionLabel = "Enable in Settings",
            onActionClick = {}
        )
    }
}
