package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.payslipmax.pdfparser.guide.preclaim.CtgCase
import com.payslipmax.pdfparser.ui.theme.PreClaimStrings

internal fun LazyListScope.preClaimJourneyItems(
    ws: PreClaimWorksheet,
    typed: PreClaimFormState,
    model: PreClaimViewModel,
    onOpenCard: (String) -> Unit,
) {
    when (typed.move) {
        PreClaimMoveType.PERMANENT_DUTY -> item(key = "journey-pd") { PreClaimPermanentDutySection(ws, typed, model) }
        PreClaimMoveType.TEMPORARY_DUTY -> item(key = "journey-td") { PreClaimTemporaryDutySection(ws, typed, model) }
        PreClaimMoveType.LEAVE_TRAVEL -> item(key = "journey-ltc") { PreClaimLeaveTravelSection(ws, typed, model, onOpenCard) }
        null -> Unit
    }
}

/** Permanent duty: the Andaman Nicobar tick (which pre-selects the island case), the CTG case, and the road journey. */
@Composable
private fun PreClaimPermanentDutySection(
    ws: PreClaimWorksheet,
    typed: PreClaimFormState,
    model: PreClaimViewModel,
) {
    PreClaimSectionFrame(PreClaimStrings.journeyTitle) {
        ws.sheet?.fields?.firstOrNull { it.id == PreClaimFieldIds.ANDAMAN_NICOBAR }?.let { tick ->
            PreClaimTickRow(tick.label, tick.id in typed.ticks) { model.setTick(tick.id, it) }
        }
        PreClaimQuietText(PreClaimStrings.ctgCaseTitle)
        PreClaimChipChoice(CtgCase.entries.map { it to PreClaimStrings.ctgCase(it) }, typed.ctgCase, model::chooseCtgCase)
        PreClaimRoadJourney(ws, typed, model)
    }
}

/** The road distance and the state it starts in: what baggage by road and own-car conveyance are worked from. */
@Composable
internal fun PreClaimRoadJourney(
    ws: PreClaimWorksheet,
    typed: PreClaimFormState,
    model: PreClaimViewModel,
) {
    PreClaimTextBox(PreClaimStrings.roadKm, typed.roadKm, model::setRoadKm)
    PreClaimChoiceBox(
        label = PreClaimStrings.originState,
        placeholder = PreClaimStrings.stateChoose,
        selected = typed.originState,
        choices = ws.stateChoices.map { it to it },
        onChoose = model::chooseOriginState,
    )
}

/** Leave travel works nothing out: the rule picker, with the card for the chosen rule, and the fare is typed on the portal. */
@Composable
private fun PreClaimLeaveTravelSection(
    ws: PreClaimWorksheet,
    typed: PreClaimFormState,
    model: PreClaimViewModel,
    onOpenCard: (String) -> Unit,
) {
    PreClaimSectionFrame(PreClaimStrings.journeyTitle) {
        PreClaimRulePicker(ws, typed, model)
        ws.ruleCardId?.let { card -> OutlinedButton(onClick = { onOpenCard(card) }) { Text(PreClaimStrings.readRuleCard) } }
    }
}

/** The move's "TR Rule" dropdown from the bundle's option group, with the one-line "use this when" of the chosen option. */
@Composable
internal fun PreClaimRulePicker(
    ws: PreClaimWorksheet,
    typed: PreClaimFormState,
    model: PreClaimViewModel,
) {
    val group = ws.sheet?.let { ws.options[PreClaimWorksheetBuilder.ruleGroup(it)] } ?: return
    val chosen = group.options.firstOrNull { it.id == typed.trRuleId }
    PreClaimChoiceBox(
        label = PreClaimStrings.trRule,
        placeholder = PreClaimStrings.trRuleChoose,
        selected = chosen?.label,
        choices = group.options.map { it.id to it.label },
        onChoose = model::chooseTrRule,
        hints = group.options.filter { it.short.isNotEmpty() }.associate { it.id to it.short },
    )
    chosen?.useWhen?.takeIf { it.isNotEmpty() }?.let { PreClaimQuietText(it) }
}
