package com.ssbmax.pdfparser.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

private val DarkColorScheme =
    darkColorScheme(
        primary = DarkColorPrimary,
        primaryContainer = DarkColorPrimaryContainer,
        secondary = DarkColorSecondary,
        tertiary = DarkColorTertiary,
        background = DarkColorBackground,
        surface = DarkColorSurface,
        onBackground = DarkColorOnSurface,
        onSurface = DarkColorOnSurface,
        onSurfaceVariant = DarkColorOnSurfaceVariant,
        error = DarkColorError,
        outlineVariant = DarkColorOutlineVariant,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = LightColorPrimary,
        primaryContainer = LightColorPrimaryContainer,
        secondary = LightColorSecondary,
        tertiary = LightColorTertiary,
        background = LightColorBackground,
        surface = LightColorSurface,
        onBackground = LightColorOnSurface,
        onSurface = LightColorOnSurface,
        onSurfaceVariant = LightColorOnSurfaceVariant,
        error = LightColorError,
        outlineVariant = LightColorOutlineVariant,
    )

// Dimension Constants (Fulfills no-hardcoded-dimensions rule)
object AppColors {
    val Warning = Color(0xFFF59E0B)
    val Caution = Color(0xFFEA580C)
}

object AppDimensions {
    val PaddingSmall = 8.dp
    val PaddingMedium = 16.dp
    val PaddingLarge = 24.dp
    val CornerRadius = 16.dp
    val CornerRadiusSmall = 2.dp
    val CornerRadiusMedium = 8.dp
    val CardElevation = 4.dp
    val DialogElevation = 6.dp
    val DialogMaxHeight = 240.dp
    val LockKeyboardWidth = 280.dp

    val SpacingTwo = 2.dp
    val SpacingTiny = 4.dp
    val SpacingSix = 6.dp
    val SpacingSmall = 8.dp
    val SpacingTen = 10.dp
    val SpacingMedium = 12.dp
    val SpacingLarge = 16.dp
    val SpacingExtraLarge = 20.dp
    val SpacingHuge = 24.dp
    val SpacingDouble = 32.dp

    val BorderHairline = 0.5.dp
    val BorderThin = 1.dp
    val BorderMedium = 2.dp

    val IconSizeSmall = 16.dp
    val IconSizeMedium = 24.dp
    val IconSizeLarge = 32.dp
    val IconSizeExtraLarge = 40.dp
    val IconSizeDouble = 48.dp
    val IconSizeHuge = 72.dp
    val LedgerCellWidth = 75.dp

    val ChartHeightMedium = 180.dp
    val ChartHeightLarge = 200.dp

    val TextSizeTiny = 9.sp
    val TextSizeSmall = 11.sp
    val TextSizeMedium = 12.sp
    val TextSizeLarge = 14.sp
    val TextSizeExtraLarge = 18.sp
    val TextSizeButton = 16.sp
    val TextSizeHuge = 24.sp
    val FontSizeEmoji = 64.sp
    val FontSizeEmojiMedium = 48.sp
}

// Typography Tokens
val AppTypography =
    Typography(
        headlineLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                letterSpacing = (-0.5).sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 28.sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                lineHeight = 24.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
    )

@Composable
fun PDFParserTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
