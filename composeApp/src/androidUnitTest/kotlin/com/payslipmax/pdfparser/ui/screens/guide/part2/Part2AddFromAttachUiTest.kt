package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.ui.screens.guide.BankAccess
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import com.payslipmax.pdfparser.ui.screens.guide.part2.bank.BankTestBase
import com.payslipmax.pdfparser.ui.theme.LearnStrings
import com.payslipmax.pdfparser.ui.theme.OrderStrings
import com.payslipmax.pdfparser.ui.theme.OrdersBankStrings
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: found on the Pixel with MARSEROF and RETIRE on one order. "Add a casualty" on the attach form led, after Add, to a fresh Bank that
 * closed the form, so the officer had to open the entry and start "Attach an order" again. The Add must bring him back to the same form,
 * with what he typed kept and the casualty he just added already ticked, and tell him so.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
class Part2AddFromAttachUiTest : Part2UiBase() {
    private val data = BankTestBase()
    private val bank by lazy { data.bank() }

    @Test
    fun anAddFromTheAttachFormReturnsToThatFormWithTheTypingKeptAndTheNewCasualtyTicked() {
        val nav = show(GuideNavState(listOf(Part2Destination.Hub, Part2Destination.Bank, Part2Destination.Attach())), part2Bank = { bank }, bankAccess = BankAccess(true) {})
        composeRule.onNode(hasText(OrderStrings.orderNoLabel) and hasSetTextAction()).performTextInput("SYN/77")
        settle()
        tap(OrderStrings.addCasualty)
        nav.push(Part2Destination.Code("FIRSTCODE"))
        settle()
        tap(LearnStrings.addToBank)
        val id = (nav.current as Part2Destination.Entry).entryId
        runBlocking {
            bank.openEditor(id)!!.apply {
                setFrom(CalendarDate(2026, 3, 1))
                setData(1, "Ravi")
            }
        }
        composeRule.onNodeWithText(OrdersBankStrings.add).performClick()
        settle()

        assertEquals(Part2Destination.Attach(), nav.current, "back on the form he left")
        see("SYN/77").assertIsDisplayed()
        assertTrue(id in bank.attach.state.value.ticked, "the casualty just added is ticked on this order")
        see(OrdersBankStrings.noticeAddedToOrder("FIRSTCODE")).assertIsDisplayed()
    }
}
