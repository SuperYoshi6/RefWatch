package com.databelay.refwatch.common.theme // Your package
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.wear.compose.material3.lazy.lerp

// Define your M3 color palette.
// You can generate these using the Material Theme Builder: https://m3.material.io/theme-builder
// Example Light Scheme Colors
val md_theme_light_primary = Color(0xFF006874)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFF9EEFFD)
val md_theme_light_onPrimaryContainer = Color(0xFF001F24)
val md_theme_light_secondary = Color(0xFF4A6267)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFCDE7ED)
val md_theme_light_onSecondaryContainer = Color(0xFF051F23)
val md_theme_light_tertiary = Color(0xFF545D7E)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFDCE1FF)
val md_theme_light_onTertiaryContainer = Color(0xFF101A37)
val md_theme_light_error = Color(0xFFBA1A1A)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onErrorContainer = Color(0xFF410002)
val md_theme_light_background = Color(0xFFFBFCFC)
val md_theme_light_onBackground = Color(0xFF191C1D)
val md_theme_light_surface = Color(0xFFFBFCFC)
val md_theme_light_onSurface = Color(0xFF191C1D)
val md_theme_light_surfaceVariant = Color(0xFFDBE4E6)
val md_theme_light_onSurfaceVariant = Color(0xFF3F484A)
val md_theme_light_outline = Color(0xFF6F797B)
val md_theme_light_inverseOnSurface = Color(0xFFEFF1F1)
val md_theme_light_inverseSurface = Color(0xFF2E3132)
val md_theme_light_inversePrimary = Color(0xFF82D3E0)
// val md_theme_light_shadow = Color(0xFF000000) // Usually not needed directly
val md_theme_light_surfaceTint = Color(0xFF006874)
val md_theme_light_outlineVariant = Color(0xFFBFC8CA)
val md_theme_light_scrim = Color(0xFF000000)


// PrimaryDim: Slightly darker/desaturated version of primary.
// We can achieve this by lerping (linear interpolation) towards black or a darker neutral.
// Let's make it 10-15% darker.
val md_theme_light_primaryDim =
    lerp(md_theme_light_primary, Color.Black, 0.1f) // Mix 10% black

// SecondaryDim: Slightly darker/desaturated version of secondary.
val md_theme_light_secondaryDim =
    lerp(md_theme_light_secondary, Color.Black, 0.1f) // Mix 10% black

// ErrorDim: Slightly darker/desaturated version of error.
val md_theme_light_errorDim =
    lerp(md_theme_light_error, Color.Black, 0.1f) // Mix 10% black

// SurfaceContainer Variants for Light Theme:
// These are typically nuances of light grays or off-whites.
// surfaceContainerLow: Lowest emphasis surface, often very close to background/surface.
val md_theme_light_surfaceContainerLow = Color(0xFFF5F6F6) // Slightly darker than FBFCTC background

// surfaceContainer: Default surface container.
// Can be the same as surface, or a step darker than Low.
val md_theme_light_surfaceContainer = Color(0xFFEFF0F0)    // A step darker than Low

// surfaceContainerHigh: Higher emphasis surface container.
// A step darker than surfaceContainer.
val md_theme_light_surfaceContainerHigh = Color(0xFFE9EAEA) // A step darker than Container




// Example Dark Scheme Colors
// --- Primary (Buttons will use this) ---
val md_theme_dark_primary = Color(0xFF00E676)      // A vibrant, clear Green (e.g., Material Green A400)
val md_theme_dark_onPrimary = Color(0xFF000000)      // Pure Black for max contrast on the vibrant green
val md_theme_dark_primaryContainer = Color(0xFF00502A)  // A darker green for container elements, if needed
val md_theme_dark_onPrimaryContainer = Color(0xFFFFFFFF) // Pure White on the darker green container

// --- Secondary (Less emphasis than primary, but still needs contrast) ---
// Let's pick a contrasting but not overly competing color, maybe a bright cyan or blue.
val md_theme_dark_secondary = Color(0xFF00E5FF)     // Vibrant Cyan/Blue (e.g., Material Cyan A400)
val md_theme_dark_onSecondary = Color(0xFF000000)     // Pure Black for max contrast
val md_theme_dark_secondaryContainer = Color(0xFF004C5A) // Darker cyan/blue container
val md_theme_dark_onSecondaryContainer = Color(0xFFFFFFFF) // Pure White

// --- Tertiary (For minor accents, if used) ---
// Could be a bright yellow or magenta for differentiation.
val md_theme_dark_tertiary = Color(0xFFFFFF00)      // Bright Yellow (e.g., Material Yellow A400)
val md_theme_dark_onTertiary = Color(0xFF000000)      // Pure Black for max contrast
val md_theme_dark_tertiaryContainer = Color(0xFF5C5C00)  // Darker yellow container
val md_theme_dark_onTertiaryContainer = Color(0xFFFFFFFF) // Pure White

// --- Error ---
// Error colors should remain distinct and alerting.
val md_theme_dark_error = Color(0xFFFF5252)        // Bright Red (e.g., Material Red A200)
val md_theme_dark_onError = Color(0xFF000000)        // Pure Black for max contrast
val md_theme_dark_errorContainer = Color(0xFFB00020)    // Standard darker M2/M3 error container red
val md_theme_dark_onErrorContainer = Color(0xFFFFFFFF)   // Pure White

// --- Background and Surface (Keep these very dark) ---
val md_theme_dark_background = Color(0xFF000000)   // Pure Black for maximum background depth
val md_theme_dark_onBackground = Color(0xFFFFFFFF)   // Pure White for text/icons on background

val md_theme_dark_surface = Color(0xFF101010)       // Very dark gray, slightly off-black for surfaces
val md_theme_dark_onSurface = Color(0xFFFFFFFF)       // Pure White for text/icons on surface
val md_theme_dark_surfaceVariant = Color(0xFF1F1F1F)  // A slightly lighter dark gray for variants
val md_theme_dark_onSurfaceVariant = Color(0xFFFAFAFA)  // Very light gray / off-white for less emphasis

// --- Outline ---
val md_theme_dark_outline = Color(0xFF9E9E9E)       // Medium-Light Gray, clearly visible
val md_theme_dark_outlineVariant = Color(0xFF424242)  // Darker Gray for subtle dividers if absolutely needed

// --- Inverse Roles (For elements that need to look like light theme on dark) ---
val md_theme_dark_inverseSurface = Color(0xFFF5F5F5)   // Very Light Gray / Off-white
val md_theme_dark_inverseOnSurface = Color(0xFF000000) // Pure Black
// Inverse Primary: Could be the dark theme's primary or a dark green
val md_theme_dark_inversePrimary = md_theme_dark_primaryContainer // Example: Darker green

// --- Other Standard M3 Roles ---
val md_theme_dark_surfaceTint = md_theme_dark_primary // Standard practice
val md_theme_dark_scrim = Color(0x99000000)           // Semi-transparent black for scrims (can be more opaque if needed)


// --- Custom Roles (Adjusted for high contrast) ---
// "Dim" variants might be less necessary if main colors are already highly saturated
// and on-colors provide the contrast. If used, they should still maintain high contrast
// with their respective "on" colors.

val md_theme_dark_primaryDim = Color(0xFF00C853)    // Slightly less intense green than primary
// onPrimary (black) should still work here.
val md_theme_dark_secondaryDim = Color(0xFF00B8D4)  // Slightly less intense cyan
// onSecondary (black) should still work here.
val md_theme_dark_tertiaryDim = Color(0xFFFFD600)   // Slightly less intense yellow
// onTertiary (black) should still work.
val md_theme_dark_errorDim = Color(0xFFFF1744)      // Slightly less intense bright red
// onError (black) should still work.

// Specific Surface Container Variants (ensure text on them is high contrast)
val md_theme_dark_surfaceContainerLow = Color(0xFF0A0A0A)  // Very, very dark gray
// onSurface (white) will be used on these.
val md_theme_dark_surfaceContainer = md_theme_dark_surface // Use standard surface
val md_theme_dark_surfaceContainerHigh = Color(0xFF1A1A1A) // Dark gray, but lighter than Low/Surface

// --- Default Jersey Colors (Conceptually part of your app's "theme") ---
// These can be some of your M3 palette colors or distinct colors.
val DefaultHomeJerseyColor: Color = Color.White // Example: Use M3 primary for home
val DefaultAwayJerseyColor: Color = Color.Red // Example: Use M3 secondary for away

// --- Predefined selectable jersey colors ---
// It's good to offer a palette that works well with your theme.
val PredefinedJerseyColors: List<Color> = listOf(
    Color.Red, // Classic Red
    Color(0xFFFFA500), // Orange
    Color.Yellow,
    Color(0xFF008000), // Green (darker than Color.Green)
    Color.Cyan,
    Color.Blue,
    Color(0xFF800080), // Purple
    Color.Black,
    Color.White,
    Color.Gray,
    md_theme_light_primary, // Your M3 Primary
    md_theme_light_secondary, // Your M3 Secondary
    md_theme_light_tertiary, // Your M3 Tertiary
    Color(0xFFF08080), // Light Coral
    Color(0xFFADD8E6)  // Light Blue
).distinct() // Ensure no duplicates if M3 colors are similar to classics