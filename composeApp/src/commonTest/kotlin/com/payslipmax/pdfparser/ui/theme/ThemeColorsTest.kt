package com.payslipmax.pdfparser.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Every colour role a stock Material component reads must come from the app's palette. A role left unset falls
 * back to Material's baseline purple, which is how the Guide's chips, the bottom-bar pill, dialogs and sheets ended
 * up lavender in an otherwise blue-and-grey app.
 */
class ThemeColorsTest {
    private fun roles(scheme: ColorScheme): Map<String, Color> =
        mapOf(
            "onPrimary" to scheme.onPrimary,
            "onSecondary" to scheme.onSecondary,
            "onTertiary" to scheme.onTertiary,
            "onError" to scheme.onError,
            "primaryContainer" to scheme.primaryContainer,
            "onPrimaryContainer" to scheme.onPrimaryContainer,
            "secondaryContainer" to scheme.secondaryContainer,
            "onSecondaryContainer" to scheme.onSecondaryContainer,
            "tertiaryContainer" to scheme.tertiaryContainer,
            "onTertiaryContainer" to scheme.onTertiaryContainer,
            "surfaceVariant" to scheme.surfaceVariant,
            "surfaceContainerLowest" to scheme.surfaceContainerLowest,
            "surfaceContainerLow" to scheme.surfaceContainerLow,
            "surfaceContainer" to scheme.surfaceContainer,
            "surfaceContainerHigh" to scheme.surfaceContainerHigh,
            "surfaceContainerHighest" to scheme.surfaceContainerHighest,
            "outline" to scheme.outline,
            "errorContainer" to scheme.errorContainer,
            "onErrorContainer" to scheme.onErrorContainer,
        )

    private fun assertNoBaselineRole(
        app: ColorScheme,
        baseline: ColorScheme,
        sameByDesign: Set<String> = emptySet(),
    ) {
        val baselineRoles = roles(baseline)
        val leaked = roles(app).filter { (role, color) -> role !in sameByDesign && color == baselineRoles.getValue(role) }.keys
        assertTrue(leaked.isEmpty(), "roles still on Material's baseline palette: $leaked")
    }

    // Material's light baseline is already pure white for these, which is the app's card surface and its text on colour.
    private val lightWhiteByDesign = setOf("surfaceContainerLowest", "onPrimary", "onSecondary", "onTertiary", "onError")

    @Test
    fun lightSchemeDefinesEveryRoleTheComponentsRead() = assertNoBaselineRole(AppLightColorScheme, lightColorScheme(), lightWhiteByDesign)

    @Test
    fun darkSchemeDefinesEveryRoleTheComponentsRead() = assertNoBaselineRole(AppDarkColorScheme, darkColorScheme())
}
