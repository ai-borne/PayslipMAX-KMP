package com.payslipmax.pdfparser.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Color Tokens (Harmonious Premium Dark & Light Palettes)
private val DarkColorPrimary = Color(0xFF3B82F6) // Premium Blue
private val DarkColorPrimaryContainer = Color(0xFF1E3A8A)
private val DarkColorSecondary = Color(0xFF10B981) // Emerald Success
private val DarkColorTertiary = Color(0xFF8B5CF6) // Violet Accent
private val DarkColorBackground = Color(0xFF080C14) // Deep Navy Black
private val DarkColorSurface = Color(0xFF0D1423) // Glass-Surface Opaque
private val DarkColorOnSurface = Color(0xFFF3F4F6)
private val DarkColorOnSurfaceVariant = Color(0xFF9CA3AF)
private val DarkColorError = Color(0xFFEF4444) // Red-500
private val DarkColorOutlineVariant = Color(0xFF374151) // Gray-700

private val LightColorPrimary = Color(0xFF2563EB)
private val LightColorPrimaryContainer = Color(0xFFDBEAFE)
private val LightColorSecondary = Color(0xFF059669)
private val LightColorTertiary = Color(0xFF7C3AED)
private val LightColorBackground = Color(0xFFF9FAFB)
private val LightColorSurface = Color(0xFFFFFFFF)
private val LightColorOnSurface = Color(0xFF111827)
private val LightColorOnSurfaceVariant = Color(0xFF4B5563)
private val LightColorError = Color(0xFFDC2626) // Red-600
private val LightColorOutlineVariant = Color(0xFFD1D5DB) // Gray-300

// Roles Material's stock components read (chips, nav pill, dialogs, sheets, menus, switches). Left unset, each falls
// back to Material's baseline purple; these keep them in the app's blue / emerald / violet / gray families.
private val White = Color(0xFFFFFFFF) // Text and switch thumbs on a filled brand colour, in both themes.
private val Blue100 = Color(0xFFDBEAFE)
private val Blue900 = Color(0xFF1E3A8A)
private val Emerald100 = Color(0xFFD1FAE5)
private val Emerald900 = Color(0xFF064E3B)
private val Violet100 = Color(0xFFEDE9FE)
private val Violet900 = Color(0xFF4C1D95)
private val Red100 = Color(0xFFFEE2E2)
private val Red900 = Color(0xFF7F1D1D)
private val Gray100 = Color(0xFFF3F4F6)
private val Gray200 = Color(0xFFE5E7EB)
private val Gray400 = Color(0xFF9CA3AF)
private val Gray500 = Color(0xFF6B7280)
private val Gray800 = Color(0xFF1F2937)
private val Navy850 = Color(0xFF111A2E) // Between the dark surface and Gray-800: raised containers (menus, dialogs).
private val Navy800 = Color(0xFF162036)

internal val AppDarkColorScheme =
    darkColorScheme(
        primary = DarkColorPrimary,
        onPrimary = White,
        primaryContainer = DarkColorPrimaryContainer,
        onPrimaryContainer = Blue100,
        secondary = DarkColorSecondary,
        onSecondary = White,
        secondaryContainer = Emerald900,
        onSecondaryContainer = Emerald100,
        tertiary = DarkColorTertiary,
        onTertiary = White,
        tertiaryContainer = Violet900,
        onTertiaryContainer = Violet100,
        background = DarkColorBackground,
        surface = DarkColorSurface,
        onBackground = DarkColorOnSurface,
        onSurface = DarkColorOnSurface,
        surfaceVariant = Gray800,
        onSurfaceVariant = DarkColorOnSurfaceVariant,
        surfaceContainerLowest = DarkColorBackground,
        surfaceContainerLow = DarkColorSurface,
        surfaceContainer = Navy850,
        surfaceContainerHigh = Navy800,
        surfaceContainerHighest = Gray800,
        error = DarkColorError,
        onError = White,
        errorContainer = Red900,
        onErrorContainer = Red100,
        outline = Gray500,
        outlineVariant = DarkColorOutlineVariant,
    )

internal val AppLightColorScheme =
    lightColorScheme(
        primary = LightColorPrimary,
        primaryContainer = LightColorPrimaryContainer,
        onPrimaryContainer = Blue900,
        secondary = LightColorSecondary,
        secondaryContainer = Emerald100,
        onSecondaryContainer = Emerald900,
        tertiary = LightColorTertiary,
        tertiaryContainer = Violet100,
        onTertiaryContainer = Violet900,
        background = LightColorBackground,
        surface = LightColorSurface,
        onBackground = LightColorOnSurface,
        onSurface = LightColorOnSurface,
        surfaceVariant = Gray100,
        onSurfaceVariant = LightColorOnSurfaceVariant,
        // Raised containers stay white like the app's cards; only the switch track's highest tier is gray.
        surfaceContainerLowest = LightColorSurface,
        surfaceContainerLow = LightColorSurface,
        surfaceContainer = LightColorSurface,
        surfaceContainerHigh = LightColorSurface,
        surfaceContainerHighest = Gray200,
        error = LightColorError,
        errorContainer = Red100,
        onErrorContainer = Red900,
        outline = Gray400,
        outlineVariant = LightColorOutlineVariant,
    )
