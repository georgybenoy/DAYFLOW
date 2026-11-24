package com.dayflow.app.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.DayflowTypography
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun GreetingHeader(
    userName: String,
    modifier: Modifier = Modifier,
    date: LocalDate = LocalDate.now(),
    onSettingsClick: (() -> Unit)? = null,
    onCalendarClick: (() -> Unit)? = null
) {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing

    val formattedDate = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))
    val greeting = getGreeting(userName)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.screenHorizontal, vertical = spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = formattedDate.uppercase(Locale.getDefault()),
                style = DayflowTypography.labelSmall,
                color = colors.primary
            )
            Spacer(modifier = Modifier.height(spacing.xs))
            Text(
                text = greeting,
                style = DayflowTypography.headlineLarge,
                color = colors.textPrimary
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onCalendarClick != null) {
                IconButton(
                    onClick = onCalendarClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = "Open Calendar",
                        tint = colors.textSecondary
                    )
                }
            }
            if (onSettingsClick != null) {
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = colors.textSecondary
                    )
                }
            }
        }
    }
}

private fun getGreeting(name: String): String {
    val hour = java.time.LocalTime.now().hour
    val base = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }
    return if (name.isNotBlank()) "$base, $name" else base
}

@Preview(name = "GreetingHeader Dark", showBackground = true, backgroundColor = 0xFF07120F)
@Composable
private fun GreetingHeaderPreview() {
    DayflowTheme {
        GreetingHeader(userName = "Alex")
    }
}
