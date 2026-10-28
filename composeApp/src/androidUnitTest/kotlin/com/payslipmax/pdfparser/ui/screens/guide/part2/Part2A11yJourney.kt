package com.payslipmax.pdfparser.ui.screens.guide.part2

import com.payslipmax.pdfparser.guide.GuideBundleContract
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.part2.domain.AreaRules
import com.payslipmax.pdfparser.part2.domain.Part2Rules
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import kotlinx.coroutines.runBlocking

/**
 * The real annual-leave journey the accessibility tests walk, on the shipped bundle and rules: the Part II home, the Leave ribbon, the
 * annual-leave cases, the empty form, the form with ration and an R&H code chosen, and the slip. Dates and station text are synthetic.
 */
abstract class Part2A11yJourney : Part2DraftUiBase() {
    private val real: GuideBundle = runBlocking { GuideBundleContract.parseShippedBundle(GuideBundleContract.readShippedBundleText()) }
    internal val checks by lazy { Part2A11yChecks(composeRule) }

    override fun bundle(): GuideBundle = real

    override fun rules(): Part2Rules = Part2Rules.shipped()

    /** Walks the screens, each reached from the one before, calling [onScreen] with the screen's name once it is drawn. */
    protected fun journey(onScreen: (String) -> Unit) {
        val nav = show()
        tap(Part2Strings.entryTitle)
        onScreen("the Part II home")
        tap("Leave")
        onScreen("the Leave ribbon")
        tap("Annual and casual leave")
        onScreen("the annual-leave cases")
        draftRow()
        onScreen("the empty annual-leave form")
        tap(Part2Strings.question("ration"))
        tap(AreaRules.rhCodes[1])
        onScreen("the form with ration and an R&H code chosen")
        val draft = model(nav)
        draft.setDate(0, "leave_from", day(7, 6))
        draft.setDate(0, "leave_to", day(8, 4))
        draft.setNumber(0, "year", "2026")
        draft.setText(0, "leave_kind", "annual leave")
        draft.setText(0, "station", "ZZ01")
        draft.setText(0, "corps", "Alpha")
        draft.setReference(0, "notif", "N-17/2026", day(3, 2))
        draft.setText(0, "serial", "12")
        settle()
        tap(Part2Strings.seeSlip)
        onScreen("the slip")
    }
}
