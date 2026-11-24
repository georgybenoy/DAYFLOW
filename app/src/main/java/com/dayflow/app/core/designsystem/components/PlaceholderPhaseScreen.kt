package com.dayflow.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.DayflowTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderPhaseScreen(
    title: String,
    phaseName: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Construction,
    onBackClick: (() -> Unit)? = null
) {
    val colors = DayflowTheme.colors

    DayflowScaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = DayflowTypography.titleLarge,
                        color = colors.textPrimary
                    )
                },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                                tint = colors.textPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.textPrimary
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(colors.background)
        ) {
            EmptyState(
                title = phaseName,
                message = description,
                icon = icon,
                actionLabel = if (onBackClick != null) "Return to Dashboard" else null,
                onActionClick = onBackClick
            )
        }
    }
}

@Preview(name = "Placeholder Screen Dark", showBackground = true, backgroundColor = 0xFF07120F)
@Composable
private fun PlaceholderPhaseScreenPreview() {
    DayflowTheme {
        PlaceholderPhaseScreen(
            title = "Reminders",
            phaseName = "Coming in Phase 5: Reminder UI",
            description = "Recurring and interval reminder management will be fully unlocked in Phase 5.",
            onBackClick = {}
        )
    }
}
