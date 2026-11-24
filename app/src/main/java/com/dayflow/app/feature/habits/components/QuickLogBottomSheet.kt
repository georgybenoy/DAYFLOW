package com.dayflow.app.feature.habits.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.core.designsystem.DayflowTypography
import com.dayflow.app.domain.model.Habit
import com.dayflow.app.domain.model.HabitLog
import com.dayflow.app.domain.model.HabitTrackingType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickLogBottomSheet(
    habit: Habit,
    currentLog: HabitLog?,
    onSave: (value: Double) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val initialVal = currentLog?.completedValue ?: 0.0
    var currentValue by remember(currentLog) { mutableDoubleStateOf(initialVal) }
    var textInput by remember(currentLog) {
        mutableStateOf(if (initialVal % 1.0 == 0.0) initialVal.toLong().toString() else initialVal.toString())
    }

    val stepValues = when (habit.trackingType) {
        HabitTrackingType.DURATION -> listOf(-15.0, -5.0, 5.0, 15.0)
        HabitTrackingType.QUANTITY -> {
            if (habit.targetValue >= 100) listOf(-50.0, -10.0, 10.0, 50.0)
            else listOf(-1.0, -0.5, 0.5, 1.0)
        }
        HabitTrackingType.REPETITION -> listOf(-5.0, -1.0, 1.0, 5.0)
        HabitTrackingType.BINARY -> listOf(-1.0, 1.0)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DayflowTheme.colors.surfaceElevated,
        shape = DayflowTheme.shapes.bottomSheet,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Log ${habit.name}",
                        style = DayflowTypography.titleLarge,
                        color = DayflowTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    val unitStr = if (!habit.unit.isNullOrBlank()) " ${habit.unit}" else ""
                    Text(
                        text = "Target: ${habit.targetValue}$unitStr",
                        style = DayflowTypography.bodyMedium,
                        color = DayflowTheme.colors.textSecondary
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = DayflowTheme.colors.textSecondary
                    )
                }
            }

            // Quick increment/decrement buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                stepValues.forEach { step ->
                    OutlinedButton(
                        onClick = {
                            val nextVal = (currentValue + step).coerceAtLeast(0.0)
                            currentValue = nextVal
                            textInput = if (nextVal % 1.0 == 0.0) nextVal.toLong().toString() else String.format(java.util.Locale.US, "%.1f", nextVal)
                        },
                        modifier = Modifier.weight(1f),
                        shape = DayflowTheme.shapes.button,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = DayflowTheme.colors.textPrimary
                        )
                    ) {
                        val sign = if (step > 0) "+$step" else "$step"
                        val formatted = if (step % 1.0 == 0.0) {
                            if (step > 0) "+${step.toLong()}" else "${step.toLong()}"
                        } else sign
                        Text(text = formatted, style = DayflowTypography.labelMedium)
                    }
                }
            }

            // Custom Value input
            OutlinedTextField(
                value = textInput,
                onValueChange = { input ->
                    textInput = input
                    val parsed = input.toDoubleOrNull()
                    if (parsed != null && parsed >= 0.0) {
                        currentValue = parsed
                    }
                },
                label = { Text("Value (${habit.unit ?: "units"})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = DayflowTheme.shapes.input,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DayflowTheme.colors.primary,
                    unfocusedBorderColor = DayflowTheme.colors.border,
                    focusedLabelColor = DayflowTheme.colors.primary,
                    unfocusedLabelColor = DayflowTheme.colors.textSecondary,
                    focusedTextColor = DayflowTheme.colors.textPrimary,
                    unfocusedTextColor = DayflowTheme.colors.textPrimary
                )
            )

            // Quick Complete button
            OutlinedButton(
                onClick = {
                    currentValue = habit.targetValue
                    textInput = if (habit.targetValue % 1.0 == 0.0) habit.targetValue.toLong().toString() else habit.targetValue.toString()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = DayflowTheme.shapes.button,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = DayflowTheme.colors.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Mark as Target Reached (${habit.targetValue})",
                    style = DayflowTypography.labelLarge
                )
            }

            // Save Action
            Button(
                onClick = {
                    onSave(currentValue)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = DayflowTheme.shapes.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DayflowTheme.colors.primary,
                    contentColor = DayflowTheme.colors.background
                )
            ) {
                Text(
                    text = "Save Progress",
                    style = DayflowTypography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
