package com.payslipmax.pdfparser.ui.screens.guide.part2

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY (Phase 11 item 35): with the font at twice its size on a 360dp-wide phone, no text on the order-drafting journey may run past the
 * screen's edge and no button may lose its label to an ellipsis. Real text shaping is needed (the legacy Robolectric graphics give every
 * character one pixel). The screen is 12000dp tall because the answered form is about 6700dp long at this size and the list below the
 * viewport is not composed: a shorter screen would hide the "See the slip" button, which is what the first run of this test found.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h12000dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Part2A11yLargeFontUiTest : Part2A11yJourney() {
    @Test
    fun atTwiceTheFontNothingRunsOffTheEdgeOrLosesAButtonsLabel() {
        fontScale = 2f
        val faults = mutableListOf<String>()
        journey { screen -> faults += checks.largeFontFaults().map { "$screen: $it" } }
        assertEquals(emptyList(), faults)
    }
}
