package com.payslipmax.pdfparser.ui.theme

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * One colour value has one name. A second private `val` holding the same hex drifts the day one of them is edited, so a
 * repeated literal in [ThemeColors.kt][AppLightColorScheme] must be an alias of the first, never a second `Color(0x...)`.
 */
class ThemeColorsSourceTest {
    private val source = File("src/commonMain/kotlin/com/payslipmax/pdfparser/ui/theme/ThemeColors.kt").readText()

    @Test
    fun noColourLiteralIsDeclaredTwice() {
        val literals = Regex("""Color\(0x([0-9A-Fa-f]{8})\)""").findAll(source).map { it.groupValues[1].uppercase() }.toList()
        val repeated = literals.groupingBy { it }.eachCount().filterValues { it > 1 }.keys

        assertTrue(repeated.isEmpty(), "declared more than once, make the later one an alias: $repeated")
    }
}
