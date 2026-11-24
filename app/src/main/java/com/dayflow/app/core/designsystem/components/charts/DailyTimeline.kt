package com.dayflow.app.core.designsystem.components.charts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.DayflowTypography
import com.dayflow.app.core.designsystem.components.EmptyState
import com.dayflow.app.core.time.DateTimeFormatters
import com.dayflow.app.domain.calendar.CalendarEvent
import com.dayflow.app.domain.calendar.CalendarEventType
import java.time.LocalTime

/**
 * 24-hour timeline displaying color-coded commitment blocks for tasks, deadlines,
 * reminders, habits, and approved plan items.
 *
 * Implements accessible semantic descriptions, text labels alongside color accents (zero color-only encoding),
 * and handles empty states cleanly.
 */
@Composable
fun DailyTimeline(
    events: List<CalendarEvent>,
    onEventClick: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    scrollable: Boolean = true
) {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing

    if (events.isEmpty()) {
        EmptyState(
            title = "No commitments scheduled",
            message = "Your schedule is clear for this day.",
            icon = Icons.Outlined.Event,
            modifier = modifier.padding(spacing.cardPadding)
        )
        return
    }

    // Split events into untimed (all-day / habits) and timed
    val (timedEvents, untimedEvents) = remember(events) {
        events.partition { it.startTime != null }
    }

    val totalCount = events.size
    val completedCount = events.count { it.isCompleted }
    val accessibilitySummary = "Daily timeline with $totalCount commitments, $completedCount completed."

    if (scrollable) {
        LazyColumn(
            modifier = modifier
                .fillMaxWidth()
                .semantics { contentDescription = accessibilitySummary },
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Untimed / All-Day commitments section
            if (untimedEvents.isNotEmpty()) {
                item(key = "untimed_header") {
                    Text(
                        text = "ALL-DAY / UNTIMED",
                        style = DayflowTypography.labelSmall,
                        color = colors.textMuted,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                items(untimedEvents, key = { it.id }) { event ->
                    TimelineEventCard(
                        event = event,
                        onClick = { onEventClick(event) }
                    )
                }

                item(key = "timed_divider") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SCHEDULED TIMELINE",
                        style = DayflowTypography.labelSmall,
                        color = colors.textMuted,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            // Timed commitments
            items(timedEvents, key = { it.id }) { event ->
                TimedEventRow(
                    event = event,
                    onEventClick = onEventClick,
                    colors = colors
                )
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(contentPadding)
                .semantics { contentDescription = accessibilitySummary },
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Untimed / All-Day commitments section
            if (untimedEvents.isNotEmpty()) {
                Text(
                    text = "ALL-DAY / UNTIMED",
                    style = DayflowTypography.labelSmall,
                    color = colors.textMuted,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                untimedEvents.forEach { event ->
                    TimelineEventCard(
                        event = event,
                        onClick = { onEventClick(event) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "SCHEDULED TIMELINE",
                    style = DayflowTypography.labelSmall,
                    color = colors.textMuted,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Timed commitments
            timedEvents.forEach { event ->
                TimedEventRow(
                    event = event,
                    onEventClick = onEventClick,
                    colors = colors
                )
            }
        }
    }
}

@Composable
private fun TimedEventRow(
    event: CalendarEvent,
    onEventClick: (CalendarEvent) -> Unit,
    colors: com.dayflow.app.core.designsystem.DayflowColors,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Time column
        Column(
            modifier = Modifier.width(56.dp),
            horizontalAlignment = Alignment.End
        ) {
            val timeStr = event.startTime?.let { DateTimeFormatters.formatTime(it) } ?: ""
            Text(
                text = timeStr,
                style = DayflowTypography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            if (event.endTime != null) {
                Text(
                    text = DateTimeFormatters.formatTime(event.endTime),
                    style = DayflowTypography.labelSmall,
                    color = colors.textMuted
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Timeline node with vertical line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            val accentColor = getEventColor(event.type, colors)
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Event card
        TimelineEventCard(
            event = event,
            onClick = { onEventClick(event) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TimelineEventCard(
    event: CalendarEvent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DayflowTheme.colors
    val shapes = DayflowTheme.shapes
    val accentColor = getEventColor(event.type, colors)
    val (icon, typeLabel) = getEventMeta(event.type)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = shapes.secondaryCard,
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceElevated,
            contentColor = colors.textPrimary
        ),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Type Badge row (Zero color-only encoding: Icon + Text badge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = typeLabel,
                        style = DayflowTypography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }

                if (event.priorityName != null) {
                    Text(
                        text = event.priorityName,
                        style = DayflowTypography.labelSmall,
                        color = colors.textMuted
                    )
                }

                if (event.isCompleted) {
                    Text(
                        text = "Completed",
                        style = DayflowTypography.labelSmall,
                        color = colors.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title
            Text(
                text = event.title,
                style = DayflowTypography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (event.isCompleted) colors.textMuted else colors.textPrimary,
                textDecoration = if (event.isCompleted) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Optional description
            if (!event.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.description,
                    style = DayflowTypography.bodySmall,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun getEventColor(type: CalendarEventType, colors: com.dayflow.app.core.designsystem.DayflowColors): Color {
    return when (type) {
        CalendarEventType.TASK -> colors.primary
        CalendarEventType.DEADLINE -> colors.error
        CalendarEventType.REMINDER -> colors.secondary
        CalendarEventType.HABIT -> colors.accentLavender
        CalendarEventType.PLAN_ITEM -> colors.accentYellow
    }
}

private fun getEventMeta(type: CalendarEventType): Pair<ImageVector, String> {
    return when (type) {
        CalendarEventType.TASK -> Pair(Icons.Outlined.CheckCircleOutline, "Task")
        CalendarEventType.DEADLINE -> Pair(Icons.Outlined.Schedule, "Deadline")
        CalendarEventType.REMINDER -> Pair(Icons.Outlined.Notifications, "Reminder")
        CalendarEventType.HABIT -> Pair(Icons.Outlined.Refresh, "Habit")
        CalendarEventType.PLAN_ITEM -> Pair(Icons.Outlined.Event, "Plan")
    }
}
