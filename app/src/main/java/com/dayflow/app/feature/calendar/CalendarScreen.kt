package com.dayflow.app.feature.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.DayflowTypography
import com.dayflow.app.core.designsystem.components.charts.DailyTimeline
import com.dayflow.app.domain.calendar.CalendarEvent
import com.dayflow.app.domain.calendar.CalendarEventType
import com.dayflow.app.domain.model.WeekStartDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@Composable
fun CalendarScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTaskDetail: (String) -> Unit = {},
    onNavigateToReminderDetail: (String) -> Unit = {},
    onNavigateToHabitDetail: (String) -> Unit = {},
    onNavigateToPlan: (String?) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    CalendarContent(
        uiState = uiState,
        onViewModeChanged = viewModel::setViewMode,
        onDateSelected = viewModel::selectDate,
        onFilterSelected = viewModel::setFilter,
        onNavigatePrevious = viewModel::navigatePrevious,
        onNavigateNext = viewModel::navigateNext,
        onGoToToday = viewModel::goToToday,
        onEventClick = { event ->
            when (event.type) {
                CalendarEventType.TASK,
                CalendarEventType.DEADLINE -> onNavigateToTaskDetail(event.referenceId)
                CalendarEventType.REMINDER -> onNavigateToReminderDetail(event.referenceId)
                CalendarEventType.HABIT -> onNavigateToHabitDetail(event.referenceId)
                CalendarEventType.PLAN_ITEM -> onNavigateToPlan(event.referenceId)
            }
        },
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarContent(
    uiState: CalendarUiState,
    onViewModeChanged: (CalendarViewMode) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onFilterSelected: (CalendarEventType?) -> Unit,
    onNavigatePrevious: () -> Unit,
    onNavigateNext: () -> Unit,
    onGoToToday: () -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Calendar",
                        style = DayflowTypography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onGoToToday) {
                        Icon(
                            imageVector = Icons.Outlined.Today,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Today",
                            style = DayflowTypography.labelMedium,
                            color = colors.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // View Mode Selector (Day, Week, Month)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.screenHorizontal, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalendarViewMode.entries.forEach { mode ->
                    FilterChip(
                        selected = uiState.viewMode == mode,
                        onClick = { onViewModeChanged(mode) },
                        label = { Text(text = mode.label, style = DayflowTypography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primary,
                            selectedLabelColor = colors.background,
                            containerColor = colors.surfaceElevated,
                            labelColor = colors.textSecondary
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (uiState.viewMode == mode) colors.primary else colors.border
                        )
                    )
                }
            }

            // Date Navigation Header
            DateNavigationHeader(
                selectedDate = uiState.selectedDate,
                viewMode = uiState.viewMode,
                weekStartDay = uiState.weekStartDay,
                onPrevious = onNavigatePrevious,
                onNext = onNavigateNext
            )

            // Event Type Filter Chips
            EventTypeFilterRow(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = onFilterSelected
            )

            // Legend Row (Zero color-only encoding)
            CalendarLegendRow()

            // Main View Content
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.primary)
                }
            } else {
                when (uiState.viewMode) {
                    CalendarViewMode.DAY -> {
                        DailyTimeline(
                            events = uiState.events,
                            onEventClick = onEventClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    CalendarViewMode.WEEK -> {
                        WeekCalendarView(
                            selectedDate = uiState.selectedDate,
                            weekStartDay = uiState.weekStartDay,
                            events = uiState.events,
                            onDateSelected = onDateSelected,
                            onEventClick = onEventClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    CalendarViewMode.MONTH -> {
                        MonthCalendarView(
                            selectedDate = uiState.selectedDate,
                            weekStartDay = uiState.weekStartDay,
                            events = uiState.events,
                            onDateSelected = onDateSelected,
                            onEventClick = onEventClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateNavigationHeader(
    selectedDate: LocalDate,
    viewMode: CalendarViewMode,
    weekStartDay: WeekStartDay,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing

    val title = remember(selectedDate, viewMode, weekStartDay) {
        when (viewMode) {
            CalendarViewMode.DAY -> {
                selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault()))
            }
            CalendarViewMode.WEEK -> {
                val firstDayOfWeek = when (weekStartDay) {
                    WeekStartDay.MONDAY -> DayOfWeek.MONDAY
                    WeekStartDay.SUNDAY -> DayOfWeek.SUNDAY
                    WeekStartDay.SATURDAY -> DayOfWeek.SATURDAY
                }
                val start = selectedDate.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
                val end = start.plusDays(6)
                val startFmt = start.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
                val endFmt = end.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))
                "$startFmt – $endFmt"
            }
            CalendarViewMode.MONTH -> {
                selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.screenHorizontal, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Previous period",
                tint = colors.textPrimary
            )
        }

        Text(
            text = title,
            style = DayflowTypography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary
        )

        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = "Next period",
                tint = colors.textPrimary
            )
        }
    }
}

@Composable
private fun EventTypeFilterRow(
    selectedFilter: CalendarEventType?,
    onFilterSelected: (CalendarEventType?) -> Unit
) {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = spacing.screenHorizontal, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterChip(
            selected = selectedFilter == null,
            onClick = { onFilterSelected(null) },
            label = { Text("All", style = DayflowTypography.labelSmall) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = colors.primary,
                selectedLabelColor = colors.background,
                containerColor = colors.surfaceElevated,
                labelColor = colors.textSecondary
            )
        )

        CalendarEventType.entries.forEach { type ->
            FilterChip(
                selected = selectedFilter == type,
                onClick = { onFilterSelected(type) },
                label = { Text(type.displayName, style = DayflowTypography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.primary,
                    selectedLabelColor = colors.background,
                    containerColor = colors.surfaceElevated,
                    labelColor = colors.textSecondary
                )
            )
        }
    }
}

@Composable
private fun CalendarLegendRow() {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = spacing.screenHorizontal, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendChip(label = "Task", color = colors.primary, icon = Icons.Outlined.CheckCircleOutline)
        LegendChip(label = "Deadline", color = colors.error, icon = Icons.Outlined.Schedule)
        LegendChip(label = "Reminder", color = colors.secondary, icon = Icons.Outlined.Notifications)
        LegendChip(label = "Habit", color = colors.accentLavender, icon = Icons.Outlined.Refresh)
        LegendChip(label = "Plan", color = colors.accentYellow, icon = Icons.Outlined.Event)
    }
}

@Composable
private fun LegendChip(
    label: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val colors = DayflowTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(colors.surfaceElevated)
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = DayflowTypography.labelSmall,
            color = colors.textSecondary
        )
    }
}

@Composable
private fun WeekCalendarView(
    selectedDate: LocalDate,
    weekStartDay: WeekStartDay,
    events: List<CalendarEvent>,
    onDateSelected: (LocalDate) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing
    val shapes = DayflowTheme.shapes

    val firstDayOfWeek = when (weekStartDay) {
        WeekStartDay.MONDAY -> DayOfWeek.MONDAY
        WeekStartDay.SUNDAY -> DayOfWeek.SUNDAY
        WeekStartDay.SATURDAY -> DayOfWeek.SATURDAY
    }

    val weekDays = remember(selectedDate, weekStartDay) {
        val start = selectedDate.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        (0..6).map { start.plusDays(it.toLong()) }
    }

    val today = remember { LocalDate.now() }
    val dayEvents = remember(events, selectedDate) {
        events.filter { it.date == selectedDate }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // 7-day horizontal selector strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.screenHorizontal, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            weekDays.forEach { date ->
                val isSelected = date == selectedDate
                val isToday = date == today
                val dateEvents = events.filter { it.date == date }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                        .clickable { onDateSelected(date) },
                    shape = shapes.input,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) colors.primary else colors.surfaceElevated
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = when {
                            isSelected -> colors.primary
                            isToday -> colors.primary.copy(alpha = 0.5f)
                            else -> colors.border
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3),
                            style = DayflowTypography.labelSmall,
                            color = if (isSelected) colors.background else colors.textMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = date.dayOfMonth.toString(),
                            style = DayflowTypography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) colors.background else colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Event indicator dot
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        dateEvents.isEmpty() -> Color.Transparent
                                        isSelected -> colors.background
                                        else -> colors.primary
                                    }
                                )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Selected Day Timeline
        DailyTimeline(
            events = dayEvents,
            onEventClick = onEventClick,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}

@Composable
private fun MonthCalendarView(
    selectedDate: LocalDate,
    weekStartDay: WeekStartDay,
    events: List<CalendarEvent>,
    onDateSelected: (LocalDate) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing
    val shapes = DayflowTheme.shapes
    val today = remember { LocalDate.now() }

    val ym = remember(selectedDate) { YearMonth.from(selectedDate) }
    val firstDayOfWeek = when (weekStartDay) {
        WeekStartDay.MONDAY -> DayOfWeek.MONDAY
        WeekStartDay.SUNDAY -> DayOfWeek.SUNDAY
        WeekStartDay.SATURDAY -> DayOfWeek.SATURDAY
    }

    val gridDays = remember(ym, weekStartDay) {
        val monthStart = ym.atDay(1)
        val monthEnd = ym.atEndOfMonth()
        val gridStart = monthStart.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        val lastDayOfWeek = firstDayOfWeek.plus(6)
        val gridEnd = monthEnd.with(TemporalAdjusters.nextOrSame(lastDayOfWeek))

        val days = mutableListOf<LocalDate>()
        var cur = gridStart
        while (!cur.isAfter(gridEnd)) {
            days.add(cur)
            cur = cur.plusDays(1)
        }
        days
    }

    val dayEvents = remember(events, selectedDate) {
        events.filter { it.date == selectedDate }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // Weekday header labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.screenHorizontal, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            for (i in 0..6) {
                val dow = firstDayOfWeek.plus(i.toLong())
                Text(
                    text = dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2),
                    style = DayflowTypography.labelSmall,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Calendar Grid
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.screenHorizontal),
            shape = shapes.mainCard,
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                val weeks = gridDays.chunked(7)
                weeks.forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        week.forEach { date ->
                            val isCurrentMonth = date.month == ym.month
                            val isSelected = date == selectedDate
                            val isToday = date == today
                            val dateEvents = events.filter { it.date == date }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(shapes.input)
                                    .background(
                                        when {
                                            isSelected -> colors.primary
                                            else -> Color.Transparent
                                        }
                                    )
                                    .then(
                                        if (isToday && !isSelected) {
                                            Modifier.border(1.dp, colors.primary, shapes.input)
                                        } else Modifier
                                    )
                                    .clickable { onDateSelected(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = date.dayOfMonth.toString(),
                                        style = DayflowTypography.bodySmall,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> colors.background
                                            !isCurrentMonth -> colors.textMuted.copy(alpha = 0.4f)
                                            else -> colors.textPrimary
                                        }
                                    )

                                    if (dateEvents.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val distinctTypes = dateEvents.map { it.type }.distinct().take(3)
                                            distinctTypes.forEach { type ->
                                                val dotColor = when {
                                                    isSelected -> colors.background
                                                    type == CalendarEventType.TASK -> colors.primary
                                                    type == CalendarEventType.DEADLINE -> colors.error
                                                    type == CalendarEventType.REMINDER -> colors.secondary
                                                    type == CalendarEventType.HABIT -> colors.accentLavender
                                                    type == CalendarEventType.PLAN_ITEM -> colors.accentYellow
                                                    else -> colors.primary
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(dotColor)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Selected Date Agenda Section
        Text(
            text = "AGENDA · ${selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))}",
            style = DayflowTypography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = colors.primary,
            modifier = Modifier.padding(horizontal = spacing.screenHorizontal)
        )

        Spacer(modifier = Modifier.height(6.dp))

        DailyTimeline(
            events = dayEvents,
            onEventClick = onEventClick,
            modifier = Modifier.fillMaxWidth(),
            scrollable = false
        )

        Spacer(modifier = Modifier.height(spacing.largeSection))
    }
}
