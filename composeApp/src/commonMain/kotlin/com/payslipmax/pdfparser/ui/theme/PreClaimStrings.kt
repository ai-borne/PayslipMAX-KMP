package com.payslipmax.pdfparser.ui.theme

import com.payslipmax.pdfparser.guide.preclaim.CtgCase
import com.payslipmax.pdfparser.guide.preclaim.NilReason
import com.payslipmax.pdfparser.guide.preclaim.PreClaimElement
import com.payslipmax.pdfparser.guide.preclaim.PreClaimNote

/**
 * App chrome copy for the TA/DA pre-claim worksheet (docs/Plan/rule_cards/22_ta_da_preclaim_execution_plan.md, P7). Field labels,
 * documents, move titles and the rule-picker lines are bundle content and never appear here. The wording about money always says
 * "estimate": the app never shows a total or an advance (plan 21, F1).
 */
object PreClaimStrings {
    // Entry and screen.
    const val entryEmoji = "🧮"
    const val entryTitle = "TA/DA claim worksheet"
    const val entrySubtitle = "Get your advance claim ready for the portal"
    const val entryPremium = "Premium"
    const val screenTitle = "TA/DA claim worksheet"
    const val screenSubtitle = "You still file on the portal. This gets the form ready."
    const val prepareThisClaim = "Prepare this claim"
    const val chooseMove = "What kind of move is it?"
    const val changeMove = "Change move type"
    const val readRuleCard = "Read the rule card"

    // Locked and unavailable.
    const val lockedTitle = "The claim worksheet is part of Premium"
    const val lockedBody = "Premium fills your pay details from your payslip, works out the amounts the rules allow and lists the papers to attach."
    const val unavailableTitle = "The worksheet could not open"
    const val unavailableBody = "Your payslips are not affected. Try again. If it keeps failing, update the app."

    // Your pay details.
    const val profileTitle = "Your pay details"
    const val profileHelp = "Taken from your latest payslip when one is saved. Check each box and fix any that is wrong."
    const val level = "Pay level"
    const val levelChoose = "Choose your level"
    const val basicPay = "Basic pay (Rs)"
    const val daPercent = "Dearness allowance (%)"
    const val msp = "Military service pay (Rs)"
    const val npa = "Non-practising allowance (Rs)"
    const val resetProfile = "Use my payslip values"

    // Questions and the journey.
    const val questionsTitle = "About you"
    const val journeyTitle = "Your journey"
    const val ctgCaseTitle = "Which transfer grant case applies?"
    const val roadKm = "Road distance (km)"
    const val originState = "State the journey starts in"
    const val stateChoose = "Choose a state"
    const val trRule = "TR Rule"
    const val trRuleChoose = "Choose a rule"
    const val fromDate = "From date"
    const val toDate = "To date"
    const val dateChoose = "Choose a date"
    const val dateSet = "Set date"
    const val dateClear = "Clear date"
    const val dateCancel = "Cancel"
    const val awayHours = "Hours away"
    const val awayMinutes = "Minutes away"
    const val awayHelp = "A trip within one day: the food rate depends on how long you are away."
    const val nights = "Nights in a hotel"
    const val nightlyRate = "Hotel bill per night (Rs)"
    const val sanctionQuestion = "Do you hold a Rule 40 Note 2 sanction to use your own car?"
    const val yes = "Yes"
    const val no = "No"
    const val daysAway = "Days away (counting both dates)"

    fun ctgCase(case: CtgCase): String =
        when (case) {
            CtgCase.NORMAL -> "Normal move"
            CtgCase.LOCAL_MOVE -> "Local move"
            CtgCase.ISLAND -> "Island posting"
        }

    fun daysCount(days: Int): String = if (days == 1) "1 day" else "$days days"

    // Estimates.
    const val estimatesTitle = "Estimates"
    const val estimatesNote = "Every amount here is an estimate from the published rule. The portal sets the final advance and adjusts it at the final claim."
    const val estimatesNone = "Fill in the boxes above and the amounts you can claim appear here."
    const val estimateAmountMissing = "No estimate yet"

    fun estimateSource(
        rule: String,
        asOf: String,
    ): String = "Estimate. Rule: ${rule.trimEnd('.', ' ')}. As of $asOf."

    fun element(element: PreClaimElement): String =
        when (element) {
            PreClaimElement.CTG -> "Composite transfer grant"
            PreClaimElement.BAGGAGE_ROAD -> "Baggage by road"
            PreClaimElement.CAR_CONVEYANCE -> "Own-car conveyance (RMA)"
            PreClaimElement.FOOD -> "Food"
            PreClaimElement.LODGING -> "Hotel"
            PreClaimElement.LOCAL_TAXI -> "Local taxi"
        }

    fun ctgDetail(
        percent: Int,
        basic: String,
        divisor: Int,
    ): String = if (divisor > 1) "$percent% of basic pay $basic, divided by $divisor" else "$percent% of basic pay $basic"

    fun perKmDetail(
        km: Int,
        rate: String,
    ): String = "$km km at $rate a km"

    fun perDayDetail(
        days: Int,
        rate: String,
        sharePercent: Int,
    ): String = if (sharePercent < FULL_SHARE) "${daysCount(days)} at $rate a day, $sharePercent% of the day's rate for a short trip" else "${daysCount(days)} at $rate a day"

    fun lodgingDetail(
        nights: Int,
        billed: String,
        ceiling: String,
        capped: Boolean,
    ): String {
        val base = "$nights nights, your bill $billed a night, rule allows at most $ceiling a night"
        return if (capped) "$base. Your bill is over the limit, so the limit is used." else base
    }

    fun nilReason(reason: NilReason): String =
        when (reason) {
            NilReason.RULE_124_NO_DA -> "Nil. Under Rule 124 daily allowance, hotel and local taxi are not admissible."
            NilReason.NO_RULE_40_SANCTION -> "Nil. Own-car conveyance needs a Rule 40 Note 2 sanction."
        }

    fun limitNote(text: String): String = "No rupee amount in the rule. $text"

    const val nilAmount = "Nil"

    fun note(note: PreClaimNote): String =
        when (note) {
            PreClaimNote.OVER_180_DAYS -> "Daily allowance stops after 180 days at one place (Rules 123 and 114 Note 4). Check how this applies to your orders."
        }

    // The portal form, documents and notes.
    const val fieldsTitle = "The portal form, in order"
    const val fieldsHelp = "Copy one box at a time. The portal fills its locked boxes itself."
    const val sourcePortal = "The portal fills this"
    const val sourceOfficer = "You type this"
    const val sourceApp = "Suggested from the estimate"
    const val unverified = "Not confirmed on the portal"
    const val copy = "Copy"
    const val copied = "Copied"
    const val documentsTitle = "Scan and attach"
    const val notesTitle = "Good to know"

    fun copyField(label: String): String = "Copy $label"

    fun uploadRule(
        files: Int,
        format: String,
        moreThanKb: Int,
        lessThanMb: Int,
    ): String = "Upload $files $format file, more than $moreThanKb KB and less than $lessThanMb MB."

    fun fieldSource(source: String): String =
        when (source) {
            "portal_filled" -> sourcePortal
            "app_suggests" -> sourceApp
            else -> sourceOfficer
        }

    private const val FULL_SHARE = 100
}
