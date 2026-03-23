package com.devora.devicemanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

// ══════════════════════════════════════
// COLOR SCHEMES
// ══════════════════════════════════════

private val LightColorScheme = lightColorScheme(
    primary = PurpleCore,
    onPrimary = BgSurface,
    primaryContainer = PurpleDim,
    onPrimaryContainer = PurpleDeep,
    secondary = PurpleBright,
    onSecondary = BgSurface,
    secondaryContainer = BgElevated,
    onSecondaryContainer = TextSecondary,
    tertiary = PurpleDeep,
    onTertiary = BgSurface,
    background = BgBase,
    onBackground = TextPrimary,
    surface = BgSurface,
    onSurface = TextPrimary,
    surfaceVariant = BgElevated,
    onSurfaceVariant = TextSecondary,
    outline = PurpleBorder,
    outlineVariant = PurpleDim,
    error = Danger,
    onError = BgSurface
)

private val DarkColorScheme = darkColorScheme(
    primary = PurpleCore,
    onPrimary = DarkTextPrimary,
    primaryContainer = PurpleDim,
    onPrimaryContainer = PurpleBright,
    secondary = PurpleBright,
    onSecondary = DarkBgBase,
    secondaryContainer = DarkBgElevated,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = PurpleDeep,
    onTertiary = DarkTextPrimary,
    background = DarkBgBase,
    onBackground = DarkTextPrimary,
    surface = DarkBgSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkBgElevated,
    onSurfaceVariant = DarkTextMuted,
    outline = PurpleBorder,
    outlineVariant = PurpleDim,
    error = Danger,
    onError = DarkTextPrimary
)

// ══════════════════════════════════════
// THEME VIEWMODEL
// ══════════════════════════════════════

class ThemeViewModel : ViewModel() {
    var isDark by mutableStateOf(false)
        private set

    fun toggle() {
        isDark = !isDark
    }
}

// ══════════════════════════════════════
// DEVORA THEME COMPOSABLE
// ══════════════════════════════════════

@Composable
fun DevoraTheme(
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DevoraTypography,
        shapes = DevoraShapes,
        content = content
    )
}
