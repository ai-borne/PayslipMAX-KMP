package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.assertIsDisplayed
import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.guide.GuideBundleContract
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.part2.domain.Part2Rules
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.FieldValue
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import com.payslipmax.pdfparser.ui.theme.Part2VaultStrings
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: on the real templates, the order that allowed a leave to be carried forward is a published order of the vault like a grant order, so
 * its box offers the vault too; the officer picks, and nothing is filled in on their behalf.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
class Part2VaultRealBundleUiTest : Part2VaultUiBase() {
    private val real: GuideBundle = runBlocking { GuideBundleContract.parseShippedBundle(GuideBundleContract.readShippedBundleText()) }

    override fun bundle(): GuideBundle = real

    override fun rules(): Part2Rules = Part2Rules.shipped()

    @Test
    fun theCarryForwardOrderBoxOffersTheVaultAndAPickFillsIt() {
        heldOrder("o1", no = "N/5", date = CalendarDate(2026, 3, 1))
        val nav = show()
        tap(Part2Strings.entryTitle)
        tap("Leave")
        tap("Annual and casual leave")
        draftRow(1)

        tap(Part2VaultStrings.useVaultOrder)
        tap(Part2VaultStrings.grantChoice("N/5", "01/03/2026"))

        val field = model(nav).state.value.sections[0].fields.first { it.slotId == "carry_order" }
        assertEquals(FieldValue.Reference("N/5", CalendarDate(2026, 3, 1)), field.value)
        see("${Part2Strings.orderDateLabel}: 01/03/2026").assertIsDisplayed()
    }
}
