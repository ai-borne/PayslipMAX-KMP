package com.payslipmax.pdfparser.guide.domain

import com.payslipmax.pdfparser.guide.model.GuideCard
import com.payslipmax.pdfparser.guide.model.GuideChip

/**
 * The trust chips of one card, each with a single source (owner decisions 2026-10-06/07). [ratesAsOf] is the bundle's
 * `rates_as_of` ("YYYY-MM") and is set only on a card with the RATES chip, so a card never shows a date it does not
 * depend on. Carries flags and a date, never card text, so a locked card may hold it.
 */
data class GuideTrust(
    val ratesAsOf: String?,
    val amended: Boolean,
    val unverified: Boolean,
    val noOfficialSource: Boolean,
) {
    val hasAny: Boolean get() = ratesAsOf != null || amended || unverified || noOfficialSource

    companion object {
        fun of(
            card: GuideCard,
            ratesAsOf: String,
        ): GuideTrust {
            val chips = card.trustChips
            return GuideTrust(
                ratesAsOf = if (GuideChip.RATES in chips) ratesAsOf else null,
                amended = GuideChip.AMENDED in chips,
                unverified = card.unverified,
                noOfficialSource = card.hasNoOfficialSource,
            )
        }
    }
}
