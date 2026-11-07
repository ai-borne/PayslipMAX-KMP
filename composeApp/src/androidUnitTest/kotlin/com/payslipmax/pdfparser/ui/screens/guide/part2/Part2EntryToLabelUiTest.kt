package com.payslipmax.pdfparser.ui.screens.guide.part2

import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.model.Part2Column
import com.payslipmax.pdfparser.guide.model.Part2ColumnRow
import com.payslipmax.pdfparser.testing.SyntheticGuidePart2
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
 * WHY: the entry form's To field must say what the casualty's own Annexure III row says. A required To labelled "(optional)" (found on the
 * Pixel for a casual-leave entry) tells the officer he may skip a field the form then refuses to save without.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
class Part2EntryToLabelUiTest : Part2UiBase() {
    private val lookup =
        SyntheticGuidePart2.lookup.let { base ->
            base.copy(columns = base.columns.map { Part2ColumnRow(it.serial, column("required"), column(if (it.serial == "1.1") "required" else "optional"), it.data) })
        }
    private val data = BankTestBase(lookup)
    private val bank by lazy { data.bank() }

    private fun column(need: String) = Part2Column(need, "dd/mm/yyyy")

    /** 1.1 (FIRSTCODE) needs a To; 1.2 (SECONDCODE) may have one. */
    override fun bundle(): GuideBundle = SyntheticGuidePart2.bundle().copy(part2 = lookup)

    private fun addFrom(code: String) = show(GuideNavState(listOf(Part2Destination.Home, Part2Destination.Code(code))), part2Bank = { bank }, bankAccess = BankAccess(true) {}).also { tap(LearnStrings.addToBank) }

    @Test
    fun aRequiredToIsNotCalledOptional() {
        addFrom("FIRSTCODE")

        see("To (dd/mm/yyyy)").assertExists()
        assertEquals(0, count("To (optional)").fetchSemanticsNodes().size)
    }

    @Test
    fun anOptionalToStillSaysOptional() {
        addFrom("SECONDCODE")

        see("To (optional) (dd/mm/yyyy)").assertExists()
    }
}
