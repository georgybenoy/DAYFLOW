package com.dayflow.app.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.DayflowTypography

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Inbox,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing
    val shapes = DayflowTheme.shapes

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = colors.textMuted
        )
        Spacer(modifier = Modifier.height(spacing.md))
        Text(
            text = title,
            style = DayflowTypography.titleMedium,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(spacing.xs))
        Text(
            text = message,
            style = DayflowTypography.bodySmall,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )

        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(spacing.lg))
            Button(
                onClick = onActionClick,
                shape = shapes.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.background
                ),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = actionLabel,
                    style = DayflowTypography.titleMedium
                )
            }
        }
    }
}

@Preview(name = "EmptyState Dark", showBackground = true, backgroundColor = 0xFF07120F)
@Composable
private fun EmptyStatePreview() {
    DayflowTheme {
        EmptyState(
            title = "No upcoming reminders",
            message = "You're all caught up for the rest of today. Add a new reminder whenever you're ready.",
            actionLabel = "Add Reminder",
            onActionClick = {}
        )
    }
}
