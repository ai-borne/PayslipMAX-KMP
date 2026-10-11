package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.payslipmax.pdfparser.ui.theme.PreClaimStrings

internal fun LazyListScope.preClaimProfileItems(
    ws: PreClaimWorksheet,
    typed: PreClaimFormState,
    model: PreClaimViewModel,
) {
    item(key = "profile") { PreClaimProfileSection(ws, typed, model) }
}

/** The five payslip-derived boxes, every one editable. A box the officer has not touched follows the payslip; a cleared one stays empty. */
@Composable
private fun PreClaimProfileSection(
    ws: PreClaimWorksheet,
    typed: PreClaimFormState,
    model: PreClaimViewModel,
) {
    val edits = typed.edits
    PreClaimSectionFrame(PreClaimStrings.profileTitle, PreClaimStrings.profileHelp) {
        PreClaimChoiceBox(
            label = PreClaimStrings.level,
            placeholder = PreClaimStrings.levelChoose,
            selected = (edits.level ?: ws.profile.level).ifEmpty { null },
            choices = ws.levelChoices.map { it to it },
            onChoose = model::chooseLevel,
        )
        PreClaimTextBox(PreClaimStrings.basicPay, edits.basic ?: ws.profile.basicPay, model::editBasicPay)
        PreClaimTextBox(PreClaimStrings.daPercent, edits.da ?: ws.profile.daPercent, model::editDaPercent)
        PreClaimTextBox(PreClaimStrings.msp, edits.msp ?: ws.profile.mspAmount, model::editMsp)
        PreClaimTextBox(PreClaimStrings.npa, edits.npa ?: ws.profile.npaAmount, model::editNpa)
        OutlinedButton(onClick = model::resetProfileToPayslip) { Text(PreClaimStrings.resetProfile) }
    }
}
