package com.payslipmax.pdfparser.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payslipmax.pdfparser.insights.InsightSeverity
import com.payslipmax.pdfparser.ui.platform.ApplyStatusBarIconStyle
import com.payslipmax.pdfparser.ui.platform.statusBarIconStyleFor

// Dimension Constants (Fulfills no-hardcoded-dimensions rule)
object AppColors {
    val Warning = Color(0xFFF59E0B)
    val Caution = Color(0xFFEA580C)

    /** Distinct from [Warning]/[Caution] — marks a value inferred by the on-device Gemma fallback, not a solver warning. */
    val AiInferred = Color(0xFF8B5CF6)
}

/**
 * Single color mapping for [InsightSeverity], so no Smart Insights card hardcodes a color. INFO/
 * IMPORTANT/OPPORTUNITY resolve through [MaterialTheme.colorScheme] (theme-aware, already used
 * everywhere else); WARNING reuses the existing fixed [AppColors.Warning] amber rather than adding a
 * second, possibly-drifting amber constant.
 */
@Composable
fun severityColor(severity: InsightSeverity): Color =
    when (severity) {
        InsightSeverity.INFO -> MaterialTheme.colorScheme.primary
        InsightSeverity.WARNING -> AppColors.Warning
        InsightSeverity.IMPORTANT -> MaterialTheme.colorScheme.error
        InsightSeverity.OPPORTUNITY -> MaterialTheme.colorScheme.secondary
    }

/** Claim Guide colours with no Material role: a light and a dark value, picked by the active scheme. */
object GuideColors {
    private val WatchOutLight = Color(0xFFB45309)
    private val WatchOutDark = Color(0xFFFBBF24)

    /** The card's "Watch out" heading: the [AppColors.Warning] amber, dark enough to read as text on a light surface. */
    @Composable
    fun watchOut(): Color = if (MaterialTheme.colorScheme.surface.luminance() < HALF_LUMINANCE) WatchOutDark else WatchOutLight

    private const val HALF_LUMINANCE = 0.5f
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
    val OnboardingCardPagerHeight = 320.dp
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

    /** Default M3 FAB (56dp) + its edge margin (16dp) + breathing room (8dp) so scrolled content clears it. */
    val FabClearanceHeight = 80.dp

    // Bottom padding that keeps scrolling content clear of the floating edit-session confirmation banner.
    val BannerClearance = 80.dp

    val TextSizeTiny = 9.sp
    val TextSizeSmall = 11.sp
    val TextSizeMedium = 12.sp
    val TextSizeLarge = 14.sp
    val TextSizeExtraLarge = 18.sp
    val TextSizeButton = 16.sp
    val TextSizeHuge = 24.sp
    val FontSizeEmoji = 64.sp
    val FontSizeEmojiMedium = 48.sp
    val ProgressTrackHeight = 6.dp
    val ProgressCornerRadius = 3.dp
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
    val colorScheme = if (darkTheme) AppDarkColorScheme else AppLightColorScheme

    ApplyStatusBarIconStyle(statusBarIconStyleFor(darkTheme))
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}

/**
 * Resolves the effective dark/light mode from the persisted [appTheme] preference
 * ("light" / "dark" / anything else = follow the system). Single source of truth so every
 * Compose tree — the Android/root [App] tree and each iOS detail `ComposeUIViewController`,
 * which are independent trees — computes the same theme identically.
 */
@Composable
fun resolveDarkTheme(appTheme: String): Boolean =
    when (appTheme) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
