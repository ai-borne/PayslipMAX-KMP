package com.payslipmax.pdfparser.ui.screens.guide.part2

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY (Phase 11 item 35): an officer with a screen reader or big fingers must be able to use the screens that draft an order. They were
 * walked only at the default font and in dark theme. On the real annual-leave journey (see [Part2A11yJourney]) every control must have a
 * name a screen reader can say, and no two touch areas may overlap (Compose itself widens every touch area to 48dp). The large-font half is [Part2A11yLargeFontUiTest].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Part2A11yUiTest : Part2A11yJourney() {
    @Test
    fun everyControlOnTheJourneyHasANameAndNoTwoTouchAreasOverlap() {
        val faults = mutableListOf<String>()
        journey { screen -> faults += (checks.unnamedActions() + checks.crowdedTargets()).map { "$screen: $it" } }
        assertEquals(emptyList(), faults)
    }
}
