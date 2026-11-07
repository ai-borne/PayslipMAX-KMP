package com.payslipmax.pdfparser.ui.screens.guide.part2.bank

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.part2.domain.EntryDataRules
import com.payslipmax.pdfparser.part2.domain.EntryPosition
import com.payslipmax.pdfparser.part2.domain.EntryProblem
import com.payslipmax.pdfparser.part2.domain.Part2ColumnPosition
import com.payslipmax.pdfparser.ui.components.scrollToWhenFocused
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimDateBox
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimDates
import com.payslipmax.pdfparser.ui.theme.OrdersBankStrings

/** From, To, each Data column the casualty uses, then the note: the form the casualty's own Annexure III row makes. */
internal fun LazyListScope.entryFields(
    editor: EntryEditViewModel,
    state: EntryEditState,
    canEdit: Boolean,
    suggestedFrom: CalendarDate? = null,
) {
    val form = editor.form
    form.fields.forEach { field ->
        item(key = "field-${field.position}") {
            when (field.position) {
                Part2ColumnPosition.FROM -> {
                    val label = if (form.fromPrinted) OrdersBankStrings.fromLabel else OrdersBankStrings.dateLabel
                    DateField(dateShown(label, field.caption), state.input.from, state.problems[field.position], canEdit, editor::setFrom, suggestedFrom.takeIf { editor.isNew })
                }
                Part2ColumnPosition.TO -> DateField(dateShown(OrdersBankStrings.toLabel(field.required), field.caption), state.input.to, state.problems[field.position], canEdit, editor::setTo)
                else -> {
                    val index = field.position - Part2ColumnPosition.DATA_FIRST
                    val label = OrdersBankStrings.columnLabel(field.caption, field.required, form.manualPage)
                    if (EntryDataRules.isDate(field.caption)) {
                        // A box the manual prints as dd/mm/yyyy (prefix, suffix, ...): the same picker as From and To, kept as text.
                        DateField(label, EntryDataRules.parse(state.input.data[index]), state.problems[field.position], canEdit, onDate = { editor.setDataDate(index, it) })
                    } else {
                        TextField(label, state.input.data[index], state.problems[field.position], canEdit, last = false) { editor.setData(index, it) }
                    }
                }
            }
        }
    }
    item(key = "note") { TextField(OrdersBankStrings.noteLabel, state.input.note, state.problems[EntryPosition.NOTE], canEdit, last = true, editor::setNote) }
}

/** The manual's caption (for example dd/mm/yyyy) is the hint for a date; the picker itself needs no typing. */
private fun dateShown(
    label: String,
    caption: String,
): String = if (caption.isEmpty()) label else "$label ($caption)"

@Composable
private fun DateField(
    shown: String,
    date: CalendarDate?,
    problem: EntryProblem?,
    canEdit: Boolean,
    onDate: (CalendarDate?) -> Unit,
    suggested: CalendarDate? = null,
) {
    if (canEdit) PreClaimDateBox(shown, date, onDate, suggested) else Text("$shown: ${PreClaimDates.format(date)}")
    problem?.let { Text(OrdersBankStrings.problem(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
}

@Composable
private fun TextField(
    label: String,
    value: String,
    problem: EntryProblem?,
    canEdit: Boolean,
    last: Boolean,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        enabled = canEdit,
        isError = problem != null,
        supportingText = problem?.let { { Text(OrdersBankStrings.problem(it)) } },
        keyboardOptions = KeyboardOptions(imeAction = if (last) ImeAction.Done else ImeAction.Next),
        modifier = Modifier.fillMaxWidth().scrollToWhenFocused(),
    )
}
