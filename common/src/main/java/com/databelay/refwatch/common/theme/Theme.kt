package com.databelay.refwatch.common.theme

import android.app.Activity
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.wear.compose.material3.MaterialTheme // Wear Material Theme

// Mobile color schemes — see Color.kt for the actual hex tokens.
val AppLightColorScheme: ColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inverseSurface = md_theme_light_inverseSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint,
    outlineVariant = md_theme_light_outlineVariant,
    scrim = md_theme_light_scrim,
)

val AppDarkColorScheme: ColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inverseSurface = md_theme_dark_inverseSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint,
    outlineVariant = md_theme_dark_outlineVariant,
    scrim = md_theme_dark_scrim,
)

// Wear OS color scheme. Mirrors the mobile dark scheme but expressed in the
// Wear OS uses a NEUTRAL dark background (pure black / dark gray), NOT the
// pitch-green / navy of the website. The watch is too small for the
// PitchBackground gradient — it would muddy the contrast of timer numerals
// and team colour chips. The accent colours (green, blue, amber, red) stay
// the same so buttons / card indicators remain readable.
private val WearNeutralBackground = Color(0xFF000000)
private val WearNeutralSurfaceLow = Color(0xFF111111)
private val WearNeutralSurface = Color(0xFF1A1A1A)
private val WearNeutralSurfaceHigh = Color(0xFF262626)
private val WearNeutralOnSurface = Color(0xFFE2E8F0)
private val WearNeutralOnSurfaceVariant = Color(0xFF94A3B8)

private val WearAppDarkColorScheme: androidx.wear.compose.material3.ColorScheme =
    androidx.wear.compose.material3.ColorScheme(
        // Accent colours — same as the phone, so brand recognition carries over.
        primary = md_theme_dark_primary,
        primaryDim = md_theme_dark_primaryDim,
        primaryContainer = md_theme_dark_primaryContainer,
        secondary = md_theme_dark_secondary,
        secondaryDim = md_theme_dark_secondaryDim,
        secondaryContainer = md_theme_dark_secondaryContainer,
        error = md_theme_dark_error,
        errorDim = md_theme_dark_errorDim,
        errorContainer = md_theme_dark_errorContainer,
        onPrimary = md_theme_dark_onPrimary,
        onPrimaryContainer = md_theme_dark_onPrimaryContainer,
        onSecondary = md_theme_dark_onSecondary,
        onSecondaryContainer = md_theme_dark_onSecondaryContainer,
        onError = md_theme_dark_onError,
        onErrorContainer = md_theme_dark_onErrorContainer,
        // Backgrounds — neutral, NOT the website's pitch green.
        background = WearNeutralBackground,
        onBackground = WearNeutralOnSurface,
        surfaceContainer = WearNeutralSurface,
        surfaceContainerLow = WearNeutralSurfaceLow,
        surfaceContainerHigh = WearNeutralSurfaceHigh,
        onSurface = WearNeutralOnSurface,
        onSurfaceVariant = WearNeutralOnSurfaceVariant
    )

val WearAppLightColorScheme: androidx.wear.compose.material3.ColorScheme =
    androidx.wear.compose.material3.ColorScheme(
        primary = md_theme_light_primary,
        primaryDim = md_theme_light_primaryDim,
        primaryContainer = md_theme_light_primaryContainer,
        secondary = md_theme_light_secondary,
        secondaryDim = md_theme_light_secondaryDim,
        secondaryContainer = md_theme_light_secondaryContainer,
        error = md_theme_light_error,
        errorDim = md_theme_light_errorDim,
        errorContainer = md_theme_light_errorContainer,
        onPrimary = md_theme_light_onPrimary,
        onPrimaryContainer = md_theme_light_onPrimaryContainer,
        onSecondary = md_theme_light_onSecondary,
        onSecondaryContainer = md_theme_light_onSecondaryContainer,
        onError = md_theme_light_onError,
        onErrorContainer = md_theme_light_onErrorContainer,
        background = Color(0xFFF8FAFC),
        onBackground = Color(0xFF0F172A),
        surfaceContainer = Color(0xFFE2E8F0),
        surfaceContainerLow = Color(0xFFF1F5F9),
        surfaceContainerHigh = Color(0xFFCBD5E1),
        onSurface = Color(0xFF0F172A),
        onSurfaceVariant = Color(0xFF475569)
    )

@Composable
fun RefWatchWearTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WearAppDarkColorScheme,
        typography = WearTypography,
        content = content
    )
}

/**
 * Phone-app theme, designed to match the RefWatch landing page
 * (super-yoshi6.github.io/RefWatch). Always dark, no dynamic color —
 * the brand needs to stay consistent across devices. The background is the
 * same dark pitch-green (#0A1A0A) that opens the website, so the app feels
 * like an extension of the same page.
 */
@Composable
fun RefWatchMobileTheme(
    darkTheme: Boolean = true,           // ignore system theme — brand is dark
    dynamicColor: Boolean = false,       // never pull wallpaper colors
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AppDarkColorScheme else AppLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Make the system bars TRANSPARENT so the PitchBackground (which
            // sits behind the Scaffold as a full-bleed Box) shows through
            // edge-to-edge. We do NOT paint them with the background colour
            // any more — that used to be correct, but with edge-to-edge
            // enabled the painted colour would clip the gradient that bleeds
            // up under the status bar.
            window.statusBarColor = AndroidColor.TRANSPARENT
            window.navigationBarColor = AndroidColor.TRANSPARENT
            // Light icons on dark — the gradient behind is always dark.
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    androidx.compose.material3.MaterialTheme(
        colorScheme = colorScheme,
        typography = MobileTypography,
        content = content
    )
}
