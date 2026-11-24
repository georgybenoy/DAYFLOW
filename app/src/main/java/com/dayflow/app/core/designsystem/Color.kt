package com.dayflow.app.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Dark Palette (Default)
val DarkBackground = Color(0xFF07120F)
val DarkSurface = Color(0xFF101D18)
val DarkSurfaceElevated = Color(0xFF172720)
val DarkSurfaceVariant = Color(0xFF20342B)
val DarkBorder = Color(0xFF263A32)
val DarkPrimaryMint = Color(0xFFA5E6D6)
val DarkSecondaryMint = Color(0xFF76CDBB)
val DarkSoftYellow = Color(0xFFF0E99B)
val DarkSoftLavender = Color(0xFFDCC3F5)
val DarkTextPrimary = Color(0xFFF1F5EF)
val DarkTextSecondary = Color(0xFFA5B6AD)
val DarkTextMuted = Color(0xFF71847A)
val DarkError = Color(0xFFF28D91)
val DarkWarning = Color(0xFFF1C879)
val DarkSuccess = Color(0xFFA5E6D6)

// Light Palette
val LightBackground = Color(0xFFF3F7F3)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFEAF1EC)
val LightSurfaceVariant = Color(0xFFDFE9E2)
val LightBorder = Color(0xFFDCE6DF)
val LightPrimary = Color(0xFF286B5B)
val LightSecondary = Color(0xFF7D987F)
val LightAccentYellow = Color(0xFFE7D978)
val LightAccentLavender = Color(0xFFB99DD8)
val LightTextPrimary = Color(0xFF17241D)
val LightTextSecondary = Color(0xFF66756B)
val LightTextMuted = Color(0xFF88998E)
val LightError = Color(0xFFBA1A1A)
val LightWarning = Color(0xFF946200)
val LightSuccess = Color(0xFF286B5B)

@Immutable
data class DayflowColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceVariant: Color,
    val border: Color,
    val primary: Color,
    val secondary: Color,
    val accentYellow: Color,
    val accentLavender: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val error: Color,
    val warning: Color,
    val success: Color,
    val isDark: Boolean
)

val DarkDayflowColors = DayflowColors(
    background = DarkBackground,
    surface = DarkSurface,
    surfaceElevated = DarkSurfaceElevated,
    surfaceVariant = DarkSurfaceVariant,
    border = DarkBorder,
    primary = DarkPrimaryMint,
    secondary = DarkSecondaryMint,
    accentYellow = DarkSoftYellow,
    accentLavender = DarkSoftLavender,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textMuted = DarkTextMuted,
    error = DarkError,
    warning = DarkWarning,
    success = DarkSuccess,
    isDark = true
)

val LightDayflowColors = DayflowColors(
    background = LightBackground,
    surface = LightSurface,
    surfaceElevated = LightSurfaceElevated,
    surfaceVariant = LightSurfaceVariant,
    border = LightBorder,
    primary = LightPrimary,
    secondary = LightSecondary,
    accentYellow = LightAccentYellow,
    accentLavender = LightAccentLavender,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textMuted = LightTextMuted,
    error = LightError,
    warning = LightWarning,
    success = LightSuccess,
    isDark = false
)

val LocalDayflowColors = staticCompositionLocalOf { DarkDayflowColors }
