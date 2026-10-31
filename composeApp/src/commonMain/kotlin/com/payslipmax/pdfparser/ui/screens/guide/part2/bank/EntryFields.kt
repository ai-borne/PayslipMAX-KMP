package com.payslipmax.pdfparser.ui.screens.guide.part2.bank

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.part2.domain.EntryField
import com.payslipmax.pdfparser.part2.domain.EntryPosition
import com.payslipmax.pdfparser.part2.domain.EntryProblem
import com.payslipmax.pdfparser.part2.domain.Part2ColumnPosition
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimDateBox
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimDates
import com.payslipmax.pdfparser.ui.theme.OrdersBankStrings

/** From, To, each Data column the casualty uses, then the note: the form the casualty's own Annexure III row makes. */
internal fun LazyListScope.entryFields(
    editor: EntryEditViewModel,
    state: EntryEditState,
    canEdit: Boolean,
) {
    val form = editor.form
    form.fields.forEach { field ->
        item(key = "field-${field.position}") {
            when (field.position) {
                Part2ColumnPosition.FROM -> DateField(if (form.fromPrinted) OrdersBankStrings.fromLabel else OrdersBankStrings.dateLabel, field, state.input.from, state.problems[field.position], canEdit, editor::setFrom)
                Part2ColumnPosition.TO -> DateField(OrdersBankStrings.toLabel, field, state.input.to, state.problems[field.position], canEdit, editor::setTo)
                else -> {
                    val index = field.position - Part2ColumnPosition.DATA_FIRST
                    val label = OrdersBankStrings.columnLabel(field.caption, field.required, form.manualPage)
                    TextField(label, state.input.data[index], state.problems[field.position], canEdit) { editor.setData(index, it) }
                }
            }
        }
    }
    item(key = "note") { TextField(OrdersBankStrings.noteLabel, state.input.note, state.problems[EntryPosition.NOTE], canEdit, editor::setNote) }
}

@Composable
private fun DateField(
    label: String,
    field: EntryField,
    date: CalendarDate?,
    problem: EntryProblem?,
    canEdit: Boolean,
    onDate: (CalendarDate?) -> Unit,
) {
    // The manual's caption (for example dd/mm/yyyy) is the hint for the date; the picker itself needs no typing.
    val shown = if (field.caption.isEmpty()) label else "$label (${field.caption})"
    if (canEdit) PreClaimDateBox(shown, date, onDate) else Text("$shown: ${PreClaimDates.format(date)}")
    problem?.let { Text(OrdersBankStrings.problem(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
}

@Composable
private fun TextField(
    label: String,
    value: String,
    problem: EntryProblem?,
    canEdit: Boolean,
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
        modifier = Modifier.fillMaxWidth(),
    )
}
