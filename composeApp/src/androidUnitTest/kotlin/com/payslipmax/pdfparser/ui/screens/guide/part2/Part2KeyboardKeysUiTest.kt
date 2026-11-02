package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.text.input.ImeAction
import com.payslipmax.pdfparser.ui.screens.guide.BankAccess
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import com.payslipmax.pdfparser.ui.screens.guide.part2.bank.BankTestBase
import com.payslipmax.pdfparser.ui.theme.LearnStrings
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: a long form typed on a phone should walk from field to field on the keyboard's own key, and end with Done to put the keyboard
 * away, instead of the officer reaching up to tap each field. Next on every text field but the last, Done on the last. The date
 * pickers are not typed, so the keys skip them.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
class Part2KeyboardKeysUiTest : Part2UiBase() {
    private val data = BankTestBase()
    private val bank by lazy { data.bank() }

    private fun typed() = composeRule.onAllNodes(hasSetTextAction())

    private fun keys(action: ImeAction) = composeRule.onAllNodes(hasSetTextAction() and hasImeAction(action)).fetchSemanticsNodes().size

    @Test
    fun theEntryFormEndsOnDoneAndEveryTextFieldBeforeItSaysNext() {
        val nav = show(GuideNavState(listOf(Part2Destination.Home, Part2Destination.Code("FIRSTCODE"))), part2Bank = { bank }, bankAccess = BankAccess(true) {})
        tap(LearnStrings.addToBank)
        assertEquals(true, nav.current is Part2Destination.Entry)

        val fields = typed().fetchSemanticsNodes().size
        assertEquals(1, keys(ImeAction.Done), "one Done, on the last field")
        assertEquals(fields - 1, keys(ImeAction.Next), "Next on all the others")
        typed()[fields - 1].assert(hasImeAction(ImeAction.Done))
    }

    @Test
    fun theAttachFormEndsOnDoneAndTheFieldsBeforeItSayNext() {
        show(GuideNavState(listOf(Part2Destination.Bank, Part2Destination.Attach())), part2Bank = { bank }, bankAccess = BankAccess(true) {})

        val fields = typed().fetchSemanticsNodes().size
        assertEquals(1, keys(ImeAction.Done))
        assertEquals(fields - 1, keys(ImeAction.Next))
    }
}
