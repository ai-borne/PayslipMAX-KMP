package com.payslipmax.pdfparser.ui.screens.guide.part2.due

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.screens.FlatBorderedCardShape
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.Part2DueStrings

/**
 * The card on the Dashboard after a payslip import: how many reminders are due, a way to see them and "Not now". [importDone] is true while
 * an import has just succeeded; each time it turns true the model counts one import. Only an [entitled] officer sees it (reminders are
 * Premium), and only while something is due now.
 */
@Composable
internal fun DueImportCard(
    model: DueCardModel,
    entitled: Boolean,
    importDone: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(importDone) { if (importDone) model.onImportCompleted() }
    val count by model.count.collectAsState(initial = 0)
    if (entitled && count > 0) DueCardContent(count, onOpen, model::notNow, modifier)
}

@Composable
private fun DueCardContent(
    count: Int,
    onOpen: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier,
) {
    FlatBorderedCardShape(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(AppDimensions.PaddingMedium), verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
            Text(Part2DueStrings.cardTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
            Text(Part2DueStrings.cardBody(count), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
                Button(onClick = onOpen) { Text(Part2DueStrings.cardOpen) }
                TextButton(onClick = onNotNow) { Text(Part2DueStrings.cardNotNow) }
            }
        }
    }
}
