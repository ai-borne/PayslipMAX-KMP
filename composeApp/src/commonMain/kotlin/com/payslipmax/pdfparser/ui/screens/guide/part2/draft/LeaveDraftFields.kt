package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.payslipmax.pdfparser.part2.domain.LeaveRules
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimDateBox
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimQuietText
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimTextBox
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import com.payslipmax.pdfparser.ui.theme.Part2VaultStrings

/** One input, drawn by what it holds. The label and help are the bundle's; a refused answer is named under the box, in words the model already chose. */
@Composable
internal fun FieldView(
    index: Int,
    field: FieldUi,
    actions: LeaveDraftActions,
    grants: List<GrantChoiceUi>,
) {
    val hint = Part2Strings.fieldHelp(field.help, field.required)
    val slot = field.slotId
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny)) {
        when (val value = field.value) {
            is FieldValue.Date -> PreClaimDateBox(field.label, value.date) { actions.onDate(index, slot, it) }
            is FieldValue.Digits -> PreClaimTextBox(field.label, value.text, { actions.onNumber(index, slot, it) }, supporting = field.error ?: hint, error = field.error != null)
            is FieldValue.Words -> PreClaimTextBox(field.label, value.text, { actions.onText(index, slot, it) }, numeric = false, supporting = field.error ?: hint, error = field.error != null)
            is FieldValue.Reference -> ReferenceFields(index, field, value, actions, grants.takeIf { field.slotId in LeaveRules.VAULT_ORDER_SLOTS }.orEmpty())
        }
        if (field.value is FieldValue.Date) PreClaimQuietText(hint)
        if (field.value is FieldValue.Date && field.error != null) Text(field.error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
}

/**
 * An order's number and its date, set together; "Clear" removes both (an order reference is never required). The grant order may also be
 * picked from the vault ([grants], newest first), which fills both boxes and leaves them editable.
 */
@Composable
private fun ReferenceFields(
    index: Int,
    field: FieldUi,
    value: FieldValue.Reference,
    actions: LeaveDraftActions,
    grants: List<GrantChoiceUi>,
) {
    val slot = field.slotId
    val hint = Part2Strings.fieldHelp(field.help, field.required)
    PreClaimTextBox(field.label, value.number, { actions.onReference(index, slot, it, value.date) }, numeric = false, supporting = field.error ?: hint, error = field.error != null)
    PreClaimDateBox(Part2Strings.orderDateLabel, value.date) { actions.onReference(index, slot, value.number, it) }
    if (value.number.isNotEmpty() || value.date != null) TextButton(onClick = { actions.onReference(index, slot, "", null) }) { Text(Part2Strings.clearAnswer) }
    ChoicePicker(Part2VaultStrings.useVaultOrder, grants, { it.label }) { actions.onReference(index, slot, it.orderNo, it.date) }
}
