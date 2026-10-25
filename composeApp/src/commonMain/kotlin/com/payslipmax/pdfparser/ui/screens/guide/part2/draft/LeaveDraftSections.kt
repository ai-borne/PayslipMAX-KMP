package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimSectionFrame
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimTickRow
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.Part2Strings

/** One occurrence the officer asked for: the questions that decide its linked orders, then the inputs its orders print, then (with more than one) remove. */
@Composable
internal fun RequestSectionView(
    section: RequestSection,
    actions: LeaveDraftActions,
    grants: List<GrantChoiceUi>,
) {
    PreClaimSectionFrame(title = section.title, help = section.name) {
        section.questions.forEach { QuestionView(section.index, it, actions) }
        section.fields.forEach { FieldView(section.index, it, actions, grants) }
        if (section.removable) TextButton(onClick = { actions.onRemove(section.index) }) { Text(Part2Strings.removeOccurrence) }
    }
}

/** A yes/no is one tick row; a choice among codes is a row of chips with "None" first, so the officer can also say they draw none. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuestionView(
    index: Int,
    question: QuestionUi,
    actions: LeaveDraftActions,
) {
    if (question.options.isEmpty()) {
        PreClaimTickRow(question.text, question.yes) { actions.onYesNo(index, question.id, it) }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        Text(question.text, style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
            FilterChip(selected = question.chosen == null, onClick = { actions.onChoose(index, question.id, null) }, label = { Text(Part2Strings.noOptionChoice) })
            question.options.forEach { option ->
                FilterChip(selected = question.chosen == option.code, onClick = { actions.onChoose(index, question.id, option.code) }, label = { Text(option.code) })
            }
        }
    }
}

/** Another occurrence of this event (a second leave year, an extension, the rejoining), offered from the codes this version can draft. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AddOccurrenceView(
    options: List<AddOption>,
    onAdd: (String) -> Unit,
) {
    if (options.isEmpty()) return
    PreClaimSectionFrame(title = Part2Strings.addAnotherHeading) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
            options.forEach { OutlinedButton(onClick = { onAdd(it.code) }) { Text(it.code) } }
        }
    }
}
