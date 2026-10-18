package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.payslipmax.pdfparser.guide.model.GuidePreClaimField
import com.payslipmax.pdfparser.ui.screens.guide.GuidePlatform
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.PreClaimStrings

internal fun LazyListScope.preClaimFieldItems(
    ws: PreClaimWorksheet,
    platform: GuidePlatform,
) {
    val fields = ws.sheet?.fields ?: return
    item(key = "fields") {
        PreClaimSectionFrame(PreClaimStrings.fieldsTitle, PreClaimStrings.fieldsHelp) {
            fields.forEach { field -> PreClaimFieldRow(field, ws.copyValues[field.id], platform.copy) }
        }
    }
}

/**
 * One portal box: its label, who fills it, the bundle's hint, and a Copy button only when the app has a plain value for exactly this
 * box. There is no copy-all, no share and no export: one tap puts one value on the clipboard (plan 22, rule 1b d).
 */
@Composable
private fun PreClaimFieldRow(
    field: GuidePreClaimField,
    value: String?,
    copy: (String) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(field.label, style = MaterialTheme.typography.bodyMedium)
            PreClaimQuietText(PreClaimStrings.fieldSource(field.source))
            if (field.hint.isNotEmpty() && !field.unverified) PreClaimQuietText(field.hint)
        }
        value?.let { PreClaimCopyButton(field.label, it, copy) }
    }
}

/** Copies [value] and says so; the label goes back to "Copy" if the value changes under it. */
@Composable
private fun PreClaimCopyButton(
    label: String,
    value: String,
    copy: (String) -> Unit,
) {
    var copied by remember(value) { mutableStateOf(false) }
    OutlinedButton(
        onClick = {
            copy(value)
            copied = true
        },
        modifier = Modifier.semantics { contentDescription = PreClaimStrings.copyField(label) },
    ) { Text(if (copied) PreClaimStrings.copied else PreClaimStrings.copy) }
}
