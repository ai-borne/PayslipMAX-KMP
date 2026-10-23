package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.assertIsDisplayed
import com.payslipmax.pdfparser.guide.GuideBundleContract
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The same screens on the bundle the app ships, so a real title, a real group meaning or the blank-group wording that the invented
 * lookup does not have cannot break the UI unseen. Only what the manual itself says is asserted: GTDAL is serial 5.1, Group A, page 114.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
class Part2LookupRealBundleUiTest : Part2UiBase() {
    private val real: GuideBundle = runBlocking { GuideBundleContract.parseShippedBundle(GuideBundleContract.readShippedBundleText()) }

    override fun bundle(): GuideBundle = real

    @Test
    fun fromTheLeaveTileToTheAnnualLeaveCodePage() {
        val nav = show()
        tap(Part2Strings.entryTitle)
        tap("Leave")
        tap("Annual and casual leave")
        tap("Granted Annual Leave")
        assertEquals(Part2Destination.Code("GTDAL"), nav.current)
        see(Part2Strings.groupLabel("A")).assertIsDisplayed()
        see("Financial (other than claims)").assertIsDisplayed()
        see(Part2Strings.chapterAndPage(5, 114)).assertIsDisplayed()
    }

    @Test
    fun searchingTheSerialOrTheNameOfARecordsCodeFindsItAndTheBlankGroupIsSaid() {
        show()
        tap(Part2Strings.entryTitle)
        type("3.28")
        tap("Ceased to draw Tough Location Allowance")
        see(Part2Strings.groupNotPrinted).assertIsDisplayed()
    }

    @Test
    fun rejoinalsPageSaysTheSlipPrintsRejoin() {
        show()
        tap(Part2Strings.entryTitle)
        type("rejoinal")
        tap("Rejoined Annual Leave")
        see(Part2Strings.printedAsNote("REJOINAL", "REJOIN")).assertIsDisplayed()
    }
}
