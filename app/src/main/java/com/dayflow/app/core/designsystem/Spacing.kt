package com.dayflow.app.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class DayflowSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val section: Dp = 28.dp,
    val largeSection: Dp = 32.dp,
    val screenHorizontal: Dp = 16.dp,
    val cardPadding: Dp = 16.dp,
    val cardSpacing: Dp = 12.dp,
    val sectionSpacing: Dp = 24.dp
)

val LocalDayflowSpacing = staticCompositionLocalOf { DayflowSpacing() }
