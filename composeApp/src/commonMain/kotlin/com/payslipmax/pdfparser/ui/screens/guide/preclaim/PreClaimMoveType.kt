package com.payslipmax.pdfparser.ui.screens.guide.preclaim

/** The portal's three types of move. [key] is the move's id in the bundle's `preclaim.moves`, whose title is content, not copy here. */
enum class PreClaimMoveType(val key: String) {
    PERMANENT_DUTY("PD"),
    TEMPORARY_DUTY("TD"),
    LEAVE_TRAVEL("LTC"),
}

/**
 * Ids of the worksheet fields this flow reads by name. They are the bundle's own ids (pinned against the real bundle by a contract
 * test), so a field the worksheet adds later needs no code unless a calculation depends on it.
 */
internal object PreClaimFieldIds {
    const val SPR = "spr"
    const val RE_EMPLOYED = "re_employed"
    const val MARITAL_STATUS = "marital_status"
    const val SPOUSE_IN_ARMY = "spouse_in_army"
    const val ANDAMAN_NICOBAR = "andaman_nicobar"
    const val BASIC_PAY = "basic_pay"
    const val NPA = "npa"
    const val MSP = "msp"
    const val TR_RULE = "tr_rule"
    const val TOTAL_ABSENCE = "total_absence"
    const val FOOD_CHARGES = "food_charges"
    const val ACCOMMODATION = "accommodation"
    const val RMA = "rma"

    /** The portal's basic-information questions, in the order the move's own field list asks them (PD four, TD two, LTC four). */
    val QUESTIONS = setOf(SPR, RE_EMPLOYED, MARITAL_STATUS, SPOUSE_IN_ARMY)
    const val KIND_TICK = "tick"
}
