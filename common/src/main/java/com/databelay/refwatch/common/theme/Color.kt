package com.databelay.refwatch.common.theme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// =====================================================================================
// RefWatch color tokens — direct port of the CSS custom properties used on the
// RefWatch landing page (super-yoshi6.github.io/RefWatch). The site is a
// "stadium-at-night" aesthetic with a dark green pitch background, dark navy
// surface cards, and the standard set of accent colors that the website's
// feature cards use to call out stoppage time, cards, etc.
//
// --bg:           #0a1a0a      (page background — pitch grass at night)
// --pitch-dark:   #1a3d1a      (gradient stop — dark mown grass)
// --pitch-light:  #2d5a2d      (gradient stop — lighter mown grass stripes)
// --surf:         #0d1320      (card surface — dark navy)
// --surf2:        #121a2c      (raised card surface — slightly lighter)
// --border:       #1a2438      (hairline borders / dividers)
// --blue:         #7da8e0      (pastel blue, used for secondary highlights)
// --amber:        #f59e0b      (yellow card accent)
// --red:          #ef4444      (red card / error accent)
// --text:         #e2e8f0
// --muted:        #94a3b8
// =====================================================================================

// Pitch greens (page background + decorative gradients)
val PitchBg = Color(0xFF0A1A0A)
val PitchDark = Color(0xFF1A3D1A)
val PitchLight = Color(0xFF2D5A2D)
val PitchShadow = Color(0xFF061206) // deeper variant used in feature-card vignette

// Surfaces (cards, lists, modals)
val Surface = Color(0xFF0D1320)
val Surface2 = Color(0xFF121A2C)
val Border = Color(0xFF1A2438)
val SurfaceTint = Color(0xFF1A2438)

// Accent colors (mirroring the website palette)
val AccentBlue = Color(0xFF7DA8E0)
val AccentGreen = Color(0xFF10B981)
val AccentAmber = Color(0xFFF59E0B)
val AccentRed = Color(0xFFEF4444)
val AccentGithub = Color(0xFF58A6FF) // used in the website's GitHub block

// Text
val TextPrimary = Color(0xFFE2E8F0)
val TextMuted = Color(0xFF94A3B8)
val TextInverse = Color(0xFF0A1A0A)

// =====================================================================================
// Light scheme — kept for previews / system light mode if the user enables it.
// The website itself is dark-only, but we still need a light scheme so M3's
// APIs (e.g. surfaceVariant) don't crash in light mode. We pick cool, low-saturation
// grays that visually feel like the website's "card on a green field" concept.
// =====================================================================================
val md_theme_light_primary = AccentGreen
val md_theme_light_onPrimary = TextInverse
val md_theme_light_primaryContainer = Color(0xFFD1FAE5)
val md_theme_light_onPrimaryContainer = Color(0xFF064E3B)

val md_theme_light_secondary = AccentBlue
val md_theme_light_onSecondary = Color.White
val md_theme_light_secondaryContainer = Color(0xFFDBEAFE)
val md_theme_light_onSecondaryContainer = Color(0xFF1E3A8A)

val md_theme_light_tertiary = AccentAmber
val md_theme_light_onTertiary = Color(0xFF422006)
val md_theme_light_tertiaryContainer = Color(0xFFFEF3C7)
val md_theme_light_onTertiaryContainer = Color(0xFF78350F)

val md_theme_light_error = AccentRed
val md_theme_light_onError = Color.White
val md_theme_light_errorContainer = Color(0xFFFEE2E2)
val md_theme_light_onErrorContainer = Color(0xFF7F1D1D)

val md_theme_light_background = Color(0xFFF8FAFC)
val md_theme_light_onBackground = Color(0xFF0F172A)
val md_theme_light_surface = Color(0xFFFFFFFF)
val md_theme_light_onSurface = Color(0xFF0F172A)
val md_theme_light_surfaceVariant = Color(0xFFE2E8F0)
val md_theme_light_onSurfaceVariant = Color(0xFF475569)
val md_theme_light_outline = Color(0xFFCBD5E1)
val md_theme_light_inverseOnSurface = Color(0xFFF1F5F9)
val md_theme_light_inverseSurface = Color(0xFF1E293B)
val md_theme_light_inversePrimary = Color(0xFF6EE7B7)
val md_theme_light_surfaceTint = AccentGreen
val md_theme_light_outlineVariant = Color(0xFFE2E8F0)
val md_theme_light_scrim = Color(0x99000000)

val md_theme_light_primaryDim = lerp(md_theme_light_primary, Color.Black, 0.1f)
val md_theme_light_secondaryDim = lerp(md_theme_light_secondary, Color.Black, 0.1f)
val md_theme_light_errorDim = lerp(md_theme_light_error, Color.Black, 0.1f)
val md_theme_light_surfaceContainerLow = Color(0xFFF1F5F9)
val md_theme_light_surfaceContainer = Color(0xFFE2E8F0)
val md_theme_light_surfaceContainerHigh = Color(0xFFCBD5E1)

// =====================================================================================
// Dark scheme — the default for RefWatch, matches the website pitch-dark theme.
// This is what `RefWatchMobileTheme` will use regardless of system theme.
// =====================================================================================
val md_theme_dark_primary = AccentGreen         // #10B981 — same green the site uses for stoppage
val md_theme_dark_onPrimary = TextInverse       // dark text on green
val md_theme_dark_primaryContainer = Color(0xFF064E3B) // deep emerald for chips/badges
val md_theme_dark_onPrimaryContainer = Color(0xFFD1FAE5)

val md_theme_dark_secondary = AccentBlue
val md_theme_dark_onSecondary = Color.White
val md_theme_dark_secondaryContainer = Color(0xFF1E3A8A)
val md_theme_dark_onSecondaryContainer = Color(0xFFDBEAFE)

val md_theme_dark_tertiary = AccentAmber
val md_theme_dark_onTertiary = Color(0xFF1A1A0A)
val md_theme_dark_tertiaryContainer = Color(0xFF78350F)
val md_theme_dark_onTertiaryContainer = Color(0xFFFEF3C7)

val md_theme_dark_error = AccentRed
val md_theme_dark_onError = Color.White
val md_theme_dark_errorContainer = Color(0xFF7F1D1D)
val md_theme_dark_onErrorContainer = Color(0xFFFEE2E2)

val md_theme_dark_background = PitchBg
val md_theme_dark_onBackground = TextPrimary
val md_theme_dark_surface = Surface
val md_theme_dark_onSurface = TextPrimary
val md_theme_dark_surfaceVariant = Surface2
val md_theme_dark_onSurfaceVariant = TextMuted
val md_theme_dark_outline = Border
val md_theme_dark_inverseOnSurface = TextPrimary
val md_theme_dark_inverseSurface = Color(0xFFF1F5F9)
val md_theme_dark_inversePrimary = Color(0xFF6EE7B7)
val md_theme_dark_surfaceTint = AccentGreen
val md_theme_dark_outlineVariant = Color(0xFF243049)
val md_theme_dark_scrim = Color(0xCC000000)

val md_theme_dark_primaryDim = Color(0xFF059669)
val md_theme_dark_secondaryDim = Color(0xFF2563EB)
val md_theme_dark_tertiaryDim = Color(0xFFD97706)
val md_theme_dark_errorDim = Color(0xFFDC2626)

val md_theme_dark_surfaceContainerLow = Color(0xFF0A0F1A) // a touch darker than --surf
val md_theme_dark_surfaceContainer = Surface
val md_theme_dark_surfaceContainerHigh = Surface2

// --- Default Jersey Colors (used in previews, kept the same as before) ---
val DefaultHomeJerseyColor: Color = Color.White
val DefaultAwayJerseyColor: Color = AccentRed

// --- Predefined selectable jersey colors ---
val PredefinedJerseyColors: List<Color> = listOf(
    AccentRed,
    Color(0xFFFFA500), // orange
    AccentAmber,
    Color(0xFF008000), // dark green
    Color.Cyan,
    AccentBlue,
    Color(0xFF800080), // purple
    Color.Black,
    Color.White,
    Color.Gray,
    md_theme_light_primary,
    md_theme_light_secondary,
    md_theme_light_tertiary,
    Color(0xFFF08080), // light coral
    Color(0xFFADD8E6), // light blue
    PitchLight,        // pitch green
    AccentGreen        // accent green
).distinct()
