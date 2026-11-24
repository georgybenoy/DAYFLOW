package com.dayflow.app.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import com.dayflow.app.domain.model.ThemePreference

private val DarkMaterialColorScheme = darkColorScheme(
    primary = DarkPrimaryMint,
    onPrimary = DarkBackground,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = DarkPrimaryMint,
    secondary = DarkSecondaryMint,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceElevated,
    onSecondaryContainer = DarkSecondaryMint,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    error = DarkError,
    onError = DarkBackground
)

private val LightMaterialColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightSurface,
    primaryContainer = LightSurfaceElevated,
    onPrimaryContainer = LightPrimary,
    secondary = LightSecondary,
    onSecondary = LightSurface,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = LightSecondary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    error = LightError,
    onError = LightSurface
)

@Composable
fun DayflowTheme(
    themePreference: ThemePreference = ThemePreference.DARK,
    content: @Composable () -> Unit
) {
    val isDark = when (themePreference) {
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }

    val dayflowColors = if (isDark) DarkDayflowColors else LightDayflowColors
    val materialColors = if (isDark) DarkMaterialColorScheme else LightMaterialColorScheme
    val spacing = DayflowSpacing()
    val shapes = DayflowShapes()

    CompositionLocalProvider(
        LocalDayflowColors provides dayflowColors,
        LocalDayflowSpacing provides spacing,
        LocalDayflowShapes provides shapes
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = DayflowTypography,
            content = content
        )
    }
}

object DayflowTheme {
    val colors: DayflowColors
        @Composable
        @ReadOnlyComposable
        get() = LocalDayflowColors.current

    val spacing: DayflowSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalDayflowSpacing.current

    val shapes: DayflowShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalDayflowShapes.current
}
