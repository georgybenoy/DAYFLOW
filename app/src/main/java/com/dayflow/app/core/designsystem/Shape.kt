package com.dayflow.app.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

@Immutable
data class DayflowShapes(
    val mainCard: RoundedCornerShape = RoundedCornerShape(18.dp),
    val secondaryCard: RoundedCornerShape = RoundedCornerShape(14.dp),
    val input: RoundedCornerShape = RoundedCornerShape(12.dp),
    val button: RoundedCornerShape = RoundedCornerShape(14.dp),
    val chip: RoundedCornerShape = RoundedCornerShape(percent = 50),
    val bottomSheet: RoundedCornerShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
)

val LocalDayflowShapes = staticCompositionLocalOf { DayflowShapes() }
