package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.guide.preclaim.CtgCase
import com.payslipmax.pdfparser.guide.preclaim.PreClaimProfile

/**
 * What the officer changed from the payslip's values. A null field was not touched and follows the payslip, an empty one was cleared
 * on purpose (the calculators then ask for it), so a late-arriving payslip never overwrites a value the officer typed.
 */
data class ProfileEdits(
    val level: String? = null,
    val basic: String? = null,
    val da: String? = null,
    val msp: String? = null,
    val npa: String? = null,
)

/**
 * Everything the officer entered on one worksheet, as sanitised text and plain choices. Held in memory only: it is never saved, so a
 * process death, a lock or leaving the screen discards it (plan 22, P6). Numbers stay text until a request is built, so a half-typed
 * value is shown as typed. Nothing here is logged or sent anywhere.
 */
data class PreClaimFormState(
    val move: PreClaimMoveType? = null,
    val edits: ProfileEdits = ProfileEdits(),
    /** Ids of the tick fields that are ticked (SPR, spouse in Army, Andaman Nicobar, ...). */
    val ticks: Set<String> = emptySet(),
    /** The CTG case; [ctgCaseChosen] says the officer set it, so the Andaman Nicobar tick no longer moves it (owner decision 2). */
    val ctgCase: CtgCase = CtgCase.NORMAL,
    val ctgCaseChosen: Boolean = false,
    val roadKm: String = "",
    val originState: String? = null,
    /** The chosen "TR Rule" option of a temporary-duty or leave-travel move. */
    val trRuleId: String? = null,
    val from: CalendarDate? = null,
    val to: CalendarDate? = null,
    val awayHours: String = "",
    val awayMinutes: String = "",
    /** Typed nights; null follows the day count (owner decision, 2026-10-11: the default is nights = days). */
    val nights: String? = null,
    val nightlyRate: String = "",
    val holdsRmaSanction: Boolean? = null,
)

/** The profile boxes as the officer sees them: an edit if there is one, else the payslip's value, else empty. */
data class PreClaimProfileText(
    val level: String,
    val basicPay: String,
    val daPercent: String,
    val mspAmount: String,
    val npaAmount: String,
)

internal fun ProfileEdits.applyTo(defaults: PreClaimProfile): PreClaimProfile =
    PreClaimProfile(
        level = if (level != null) level.ifEmpty { null } else defaults.level,
        basicPay = if (basic != null) basic.toLongOrNull() else defaults.basicPay,
        daPercent = if (da != null) da.toIntOrNull() else defaults.daPercent,
        mspAmount = if (msp != null) msp.toLongOrNull() else defaults.mspAmount,
        npaAmount = if (npa != null) npa.toLongOrNull() else defaults.npaAmount,
    )

internal fun ProfileEdits.textOver(defaults: PreClaimProfile): PreClaimProfileText =
    PreClaimProfileText(
        level = level ?: defaults.level.orEmpty(),
        basicPay = basic ?: defaults.basicPay?.toString().orEmpty(),
        daPercent = da ?: defaults.daPercent?.toString().orEmpty(),
        mspAmount = msp ?: defaults.mspAmount?.toString().orEmpty(),
        npaAmount = npa ?: defaults.npaAmount?.toString().orEmpty(),
    )
