package com.dayflow.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.DayflowTypography
import com.dayflow.app.core.designsystem.TabularHeadline
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DateSelector(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    baseDate: LocalDate = LocalDate.now(),
    daysToShow: Int = 7
) {
    val colors = DayflowTheme.colors
    val spacing = DayflowTheme.spacing

    // Generate surrounding days (e.g., 3 days before today, today, 3 days after today)
    val days = (-3..3).map { baseDate.plusDays(it.toLong()) }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        contentPadding = PaddingValues(horizontal = spacing.screenHorizontal)
    ) {
        items(days, key = { it.toString() }) { date ->
            val isSelected = date.isEqual(selectedDate)
            val isToday = date.isEqual(baseDate)

            DateChip(
                date = date,
                isSelected = isSelected,
                isToday = isToday,
                onClick = { onDateSelected(date) }
            )
        }
    }
}

@Composable
private fun DateChip(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val colors = DayflowTheme.colors
    val shapes = DayflowTheme.shapes

    val dayName = date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())).uppercase(Locale.getDefault())
    val dayNumber = date.dayOfMonth.toString()

    val backgroundColor = when {
        isSelected -> colors.primary
        isToday -> colors.surfaceElevated
        else -> colors.surface
    }

    val contentColor = when {
        isSelected -> colors.background
        else -> colors.textPrimary
    }

    val subTextColor = when {
        isSelected -> colors.background.copy(alpha = 0.8f)
        isToday -> colors.primary
        else -> colors.textSecondary
    }

    val borderModifier = if (!isSelected && isToday) {
        Modifier.border(1.dp, colors.primary, shapes.input)
    } else {
        Modifier.border(1.dp, colors.border, shapes.input)
    }

    Column(
        modifier = Modifier
            .width(52.dp)
            .height(68.dp)
            .clip(shapes.input)
            .background(backgroundColor)
            .then(borderModifier)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = dayName,
            style = DayflowTypography.labelSmall,
            color = subTextColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = dayNumber,
            style = TabularHeadline,
            color = contentColor
        )
    }
}

@Preview(name = "DateSelector Dark", showBackground = true, backgroundColor = 0xFF07120F)
@Composable
private fun DateSelectorPreview() {
    DayflowTheme {
        DateSelector(
            selectedDate = LocalDate.now(),
            onDateSelected = {}
        )
    }
}
