package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.assertIsDisplayed
import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.domain.OrderRef
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.FieldValue
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import com.payslipmax.pdfparser.ui.theme.Part2VaultStrings
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY: a follow-up order (an extension, a rejoining, a recall) must quote the order that granted the leave (ODP-2026 p.115 note 7). The
 * officer picks it from their vault, newest first; nothing is filled in silently (Round 20), because with two leaves in flight the newest
 * order may not be the right one. The boxes stay editable, and a number that was never typed is not offered.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
class Part2VaultGrantUiTest : Part2VaultUiBase() {
    private fun extensionForm() =
        show().also {
            path()
            draftRow(1)
        }

    private fun grantOf(nav: com.payslipmax.pdfparser.ui.screens.guide.GuideNavState): FieldValue.Reference =
        model(nav).state.value.sections[0].fields.first { it.slotId == "grant" }.value as FieldValue.Reference

    @Test
    fun theFormOffersTheVaultsNumberedOrdersNewestFirstAndAPickFillsBothBoxes() {
        heldOrder("o1", no = "N/5", date = CalendarDate(2026, 3, 1))
        heldOrder("o2", no = "N/9", date = CalendarDate(2026, 6, 1))
        val nav = extensionForm()

        tap(Part2VaultStrings.useVaultOrder)
        see(Part2VaultStrings.grantChoice("N/9", "01/06/2026")).assertIsDisplayed()
        see(Part2VaultStrings.grantChoice("N/5", "01/03/2026")).assertIsDisplayed()

        tap(Part2VaultStrings.grantChoice("N/5", "01/03/2026"))

        assertEquals(FieldValue.Reference("N/5", CalendarDate(2026, 3, 1)), grantOf(nav))
        assertFalse(has(Part2VaultStrings.grantChoice("N/9", "01/06/2026")), "the list closes after a pick")
        see("${Part2Strings.orderDateLabel}: 01/03/2026").assertIsDisplayed()
    }

    @Test
    fun nothingIsFilledInUntilTheOfficerPicks() {
        heldOrder("o1", no = "N/5", date = CalendarDate(2026, 3, 1))
        val nav = extensionForm()
        assertEquals(FieldValue.Reference("", null), grantOf(nav))
        assertTrue(has(Part2VaultStrings.useVaultOrder))
    }

    @Test
    fun anOrderWithoutItsNumberOrDateIsNotOffered() {
        heldOrder("o1", no = null, date = null)
        heldOrder("o2", no = "N/2", date = null)
        extensionForm()
        assertFalse(has(Part2VaultStrings.useVaultOrder), "no order the slip could quote, so no button")
    }

    @Test
    fun theOtherReferenceBoxesOfTheFormGetNoVaultButton() {
        // why: only the grant order is a Part II order of the vault; a "Sanction" signal or a corps notification is not.
        heldOrder("o1", no = "N/5", date = CalendarDate(2026, 3, 1))
        val nav = extensionForm()
        val refSlots = model(nav).state.value.sections[0].fields.filter { it.value is FieldValue.Reference }.map { it.slotId }
        assertTrue("grant" in refSlots)
        assertEquals(1, count(Part2VaultStrings.useVaultOrder).fetchSemanticsNodes().size, "one button, for the grant box only")
    }

    @Test
    fun aPickedOrderIsSavedWithTheDraft() {
        heldOrder("o1", no = "N/5", date = CalendarDate(2026, 3, 1))
        val nav = extensionForm()
        tap(Part2VaultStrings.useVaultOrder)
        tap(Part2VaultStrings.grantChoice("N/5", "01/03/2026"))
        settle()

        val saved = kotlinx.coroutines.runBlocking { repository.draft(model(nav).id) }
        assertEquals(SlotValue.RefValue(OrderRef("N/5", CalendarDate(2026, 3, 1))), saved?.event?.requests?.first()?.values?.get("grant"))
    }

    @Test
    fun theListCanBeClosedWithoutPicking() {
        heldOrder("o1", no = "N/5", date = CalendarDate(2026, 3, 1))
        val nav = extensionForm()
        tap(Part2VaultStrings.useVaultOrder)
        assertTrue(has(Part2VaultStrings.grantChoice("N/5", "01/03/2026")))

        tap(Part2VaultStrings.closeChoices)

        assertFalse(has(Part2VaultStrings.grantChoice("N/5", "01/03/2026")))
        assertEquals(FieldValue.Reference("", null), grantOf(nav))
    }
}
