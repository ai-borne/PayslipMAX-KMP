package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2ListPadding
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2ListSpacing
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimQuietText
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.Part2Strings

/**
 * The optional "Check the HRMS draft" step (Round 7): the app's slip, one row at a time and one field at a time in the order HRMS
 * shows its columns, so the officer can hold it against the clerk's draft on the HRMS screen. A column the manual leaves empty is not
 * listed. Nothing here is saved or ticked, and the flow never depends on it.
 */
@Composable
internal fun HrmsCheckScreen(
    state: LeaveDraftUiState,
    onBack: () -> Unit,
    listState: LazyListState,
) {
    val rows = state.slip?.orders.orEmpty().flatMap { it.rows }
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenBackHeader(title = Part2Strings.hrmsTitle, subtitle = Part2Strings.hrmsSubtitle, onBack = onBack)
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = Part2ListPadding, verticalArrangement = Part2ListSpacing) {
            if (state.slip == null) item(key = "unfinished") { PreClaimQuietText(Part2Strings.unfinishedLine(state.missing)) }
            itemsIndexed(rows) { i, row -> HrmsRow(i + 1, rows.size, row) }
        }
    }
}

/** One row: its heading ("Row 2 of 7") and every filled field with its label, in HRMS column order. */
@Composable
private fun HrmsRow(
    number: Int,
    total: Int,
    row: SlipRowUi,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(AppDimensions.SpacingMedium), verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
            Text(Part2Strings.hrmsRowHeading(number, total), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
            hrmsFields(row).forEach { (label, value) -> HrmsField(label, value) }
            // The certificate is the manual's own text, which starts with its name, so it has no label of ours (Round 18).
            row.certificate?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

@Composable
private fun HrmsField(
    label: String,
    value: String,
) {
    Column {
        PreClaimQuietText(label)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

/** The filled fields of a row as (label, value), in HRMS column order: code, narrative, From, To, Data 1-4, authority. The certificate follows, unlabelled. */
private fun hrmsFields(row: SlipRowUi): List<Pair<String, String>> =
    (
        listOf(Part2Strings.hrmsCodeLabel to row.code, Part2Strings.hrmsNarrativeLabel to row.narrative, Part2Strings.fromLabel to row.from, Part2Strings.toLabel to row.to) +
            row.data.mapIndexed { i, text -> Part2Strings.dataLabel(i + 1) to text } +
            listOf(Part2Strings.authorityLabel to row.authority.orEmpty())
    ).filter { it.second.isNotEmpty() }
