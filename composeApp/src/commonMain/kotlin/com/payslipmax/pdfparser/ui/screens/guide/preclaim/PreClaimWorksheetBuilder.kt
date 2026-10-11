package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.model.GuidePreClaim
import com.payslipmax.pdfparser.guide.model.GuidePreClaimMove
import com.payslipmax.pdfparser.guide.preclaim.PreClaimCalculator
import com.payslipmax.pdfparser.guide.preclaim.PreClaimChoices
import com.payslipmax.pdfparser.guide.preclaim.PreClaimNote
import com.payslipmax.pdfparser.guide.preclaim.PreClaimOutcome
import com.payslipmax.pdfparser.guide.preclaim.PreClaimProfile

/** Builds the worksheet frame from the loaded bundle, the typed form and the payslip's defaults. Pure: the calculators do all the sums. */
internal object PreClaimWorksheetBuilder {
    private class Estimate(
        val outcomes: List<PreClaimOutcome> = emptyList(),
        val days: Int? = null,
        val notes: Set<PreClaimNote> = emptySet(),
    )

    /** Null when the bundle ships no pre-claim worksheet. */
    fun build(
        bundle: GuideBundle,
        form: PreClaimFormState,
        defaults: PreClaimProfile,
    ): PreClaimWorksheet? {
        val section = bundle.preclaim ?: return null
        val sheet = form.move?.let { section.moves[it.key] }
        val profile = form.edits.applyTo(defaults)
        val estimate = estimate(bundle, form, profile)
        return PreClaimWorksheet(
            availableMoves = PreClaimMoveType.entries.filter { it.key in section.moves },
            moveTitles = section.moves.mapValues { it.value.title },
            form = form,
            sheet = sheet,
            questions = sheet?.fields?.filter { it.id in PreClaimFieldIds.QUESTIONS }.orEmpty(),
            options = section.options,
            upload = section.upload,
            limits = section.limits,
            profile = form.edits.textOver(defaults),
            levelChoices = PreClaimChoices.levels(bundle.figures),
            stateChoices = PreClaimChoices.states(bundle.figures),
            estimates = estimate.outcomes,
            days = estimate.days,
            nightsText = form.nights ?: estimate.days?.toString().orEmpty(),
            notes = estimate.notes,
            copyValues = PreClaimFieldValues.of(sheet, form, profile, estimate.days, estimate.outcomes),
            ruleCardId = ruleCard(section, sheet, form.trRuleId),
        )
    }

    /** The option group the move's "TR Rule" field draws from (empty when the move has none). */
    fun ruleGroup(sheet: GuidePreClaimMove): String = sheet.fields.firstOrNull { it.id == PreClaimFieldIds.TR_RULE }?.options.orEmpty()

    private fun ruleCard(
        section: GuidePreClaim,
        sheet: GuidePreClaimMove?,
        ruleId: String?,
    ): String? {
        if (sheet == null || ruleId == null) return null
        return section.options[ruleGroup(sheet)]?.options?.firstOrNull { it.id == ruleId }?.card?.ifEmpty { null }
    }

    private fun estimate(
        bundle: GuideBundle,
        form: PreClaimFormState,
        profile: PreClaimProfile,
    ): Estimate =
        when (form.move) {
            PreClaimMoveType.PERMANENT_DUTY -> Estimate(PreClaimCalculator.permanentDuty(bundle.figures, PreClaimRequests.permanentDuty(form, profile)))
            PreClaimMoveType.TEMPORARY_DUTY -> {
                val days = PreClaimCalculator.absenceDays(form.from, form.to)
                val result = PreClaimCalculator.temporaryDuty(bundle.figures, PreClaimRequests.temporaryDuty(form, profile, days))
                Estimate(result.outcomes, result.days, result.notes)
            }
            // Leave travel computes nothing (plan 21): the officer types the fare, the app shows the rule picker and the documents.
            PreClaimMoveType.LEAVE_TRAVEL, null -> Estimate()
        }
}
