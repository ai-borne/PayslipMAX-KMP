package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.assertIsDisplayed
import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.part2.domain.HubStatus
import com.payslipmax.pdfparser.testing.SyntheticBank
import com.payslipmax.pdfparser.ui.screens.guide.BankAccess
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import com.payslipmax.pdfparser.ui.screens.guide.part2.bank.BankTestBase
import com.payslipmax.pdfparser.ui.theme.LearnStrings
import com.payslipmax.pdfparser.ui.theme.OrdersBankStrings
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: users could not tell that "My Part II Orders" is their own file, and a tile that never changes gives no reason to open it. Home's
 * Part II tile now says where the file stands (how many casualties are noted, how many order copies are missing), the same numbers the hub
 * shows. It must say nothing it does not know: with no Bank, or an empty or not-yet-read file, it keeps the plain line that explains what is
 * inside, and never claims progress. Numbers only: no code, date or note ever reaches Home.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
class Part2HomeTileStatusUiTest : Part2UiBase() {
    private val data = BankTestBase()
    private val bank by lazy { data.bank() }

    private fun home(withBank: Boolean = true): GuideNavState = show(part2Bank = if (withBank) ({ bank }) else null, bankAccess = BankAccess(canAdd = true) {})

    private fun seedMixed() =
        runBlocking {
            data.repository.saveOrder(SyntheticBank.order("o1"))
            data.repository.saveEntry(SyntheticBank.entry("waiting", code = "FIRSTCODE", from = CalendarDate(2026, 4, 1)))
            data.repository.saveEntry(SyntheticBank.entry("done", code = "SECONDCODE", orderId = "o1", from = CalendarDate(2025, 12, 5)))
        }

    @Test
    fun withTheBankOffTheTileKeepsThePlainLineThatSaysWhatIsInside() {
        home(withBank = false)

        see(LearnStrings.hubTileSubtitle).assertIsDisplayed()
        assertEquals(0, count(" noted").fetchSemanticsNodes().size, "no Bank, so no counts")
    }

    @Test
    fun anEmptyFileKeepsThePlainLineAndClaimsNoProgress() {
        home()

        see(LearnStrings.hubTileSubtitle).assertIsDisplayed()
        assertEquals(0, count(" noted").fetchSemanticsNodes().size, "an empty file claims no counts")
    }

    @Test
    fun aFileInProgressSaysHowManyAreNotedAndHowManyCopiesAreMissing() {
        seedMixed()
        home()

        see(OrdersBankStrings.hubTileLine(total = 2, notAttached = 1)).assertIsDisplayed()
        assertEquals(0, count(LearnStrings.hubTileSubtitle).fetchSemanticsNodes().size, "the counts replace the plain line, they do not stack on it")
    }

    @Test
    fun withEveryCopyAttachedTheTileSaysSoAndFlagsNothingMissing() {
        runBlocking {
            data.repository.saveOrder(SyntheticBank.order("o1"))
            data.repository.saveEntry(SyntheticBank.entry("done", orderId = "o1", from = CalendarDate(2026, 4, 1)))
        }
        home()

        see(OrdersBankStrings.hubTileLine(total = 1, notAttached = 0)).assertIsDisplayed()
        assertEquals(0, count(" not attached").fetchSemanticsNodes().size, "nothing is missing, so nothing is flagged")
    }

    @Test
    fun nothingIsClaimedBeforeTheFileIsKnownOrWhileItIsEmpty() {
        // WHY: the hub never says "empty" before its first read finishes, and Home must not say anything the hub would not.
        assertEquals(LearnStrings.hubTileSubtitle, OrdersBankStrings.homeTileSubtitle(HubStatus.Unknown))
        assertEquals(LearnStrings.hubTileSubtitle, OrdersBankStrings.homeTileSubtitle(HubStatus.Empty))
        assertEquals(OrdersBankStrings.hubTileLine(3, 1), OrdersBankStrings.homeTileSubtitle(HubStatus.Filing(total = 3, attached = 2, notAttached = 1)))
    }

    @Test
    fun theTileStillOpensTheHubWhateverItsStatus() {
        seedMixed()
        val nav = home()

        tap(LearnStrings.hubTitle)

        assertEquals(Part2Destination.Hub, nav.current)
    }
}
