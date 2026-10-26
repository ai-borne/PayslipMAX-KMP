package com.payslipmax.pdfparser.part2.domain

/**
 * The rule family for annual leave encashed on LTC (Part II plan, phase 8; ODP-2026 pp.134-140): the advance (LVELTCAD), the encashment
 * after the journey (LVELTC) and the cancellation of an advance (CLVLTCAD). The ids are the ones `part2/rules.json` declares and
 * `part2/templates/ch5_yearend.json` cite; severity and citation live in the declaration.
 */
object LtcRules {
    const val DAYS_OVER_10 = "ltc_days_over_10"
    const val TOTAL_OVER_60 = "ltc_total_over_60"
    const val CREDIT_UNDER_30 = "ltc_credit_under_30"
    const val ADVANCE_QUOTED = "advance_quoted"

    /** The order of the advance, which the cancellation quotes, and the order that last encashed leave on LTC, which the encashment quotes. */
    const val ADVANCE = "advance"
    const val LAST_LTC = "last_ltc"

    /** The reference boxes that quote a published Part II order of the officer's vault, so the form offers the vault for them. */
    val VAULT_ORDER_SLOTS: Set<String> = setOf(ADVANCE, LAST_LTC)

    private const val LTC_DAYS = "ltc_days"
    private const val ACCUMULATION = "accumulation"
    private const val TOTAL_LTC = "total_ltc"

    /** p.135 note 1: ten days of annual leave are encashed at a time, sixty in a whole career. p.138 (2.4): at least thirty days must stay at credit. */
    private const val DAYS_AT_A_TIME = 10
    private const val DAYS_IN_A_CAREER = 60
    private const val DAYS_LEFT_AT_CREDIT = 30

    val all: List<Part2Rule> =
        listOf(
            checkRule(DAYS_OVER_10) { (number(it, LTC_DAYS) ?: 0) > DAYS_AT_A_TIME },
            checkRule(TOTAL_OVER_60) { (number(it, TOTAL_LTC) ?: 0) > DAYS_IN_A_CAREER },
            checkRule(CREDIT_UNDER_30) { creditLeftUnder30(it) },
            checkRule(ADVANCE_QUOTED) { it.request.values[ADVANCE] !is SlotValue.RefValue },
        )

    private fun number(
        context: RuleContext,
        slot: String,
    ): Int? = (context.request.values[slot] as? SlotValue.NumberValue)?.number

    /** What stays at credit after this encashment is under 30 days; nothing judged until both figures are typed. */
    private fun creditLeftUnder30(context: RuleContext): Boolean {
        val accumulation = number(context, ACCUMULATION) ?: return false
        val days = number(context, LTC_DAYS) ?: return false
        return accumulation - days < DAYS_LEFT_AT_CREDIT
    }
}
