package com.payslipmax.pdfparser.ui.screens.guide.part2.bank

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2ListPadding
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.OrdersBankStrings

/** What the entry form needs from outside. [canEdit] is false for a lapsed officer: the entry is shown and can be deleted, nothing more. */
internal class EntryActions(
    val canEdit: Boolean,
    val isNew: Boolean,
    val name: String?,
    val message: String?,
    val onSave: () -> Unit,
    val onDelete: () -> Unit,
    val onBack: () -> Unit,
)

/**
 * One entry: the casualty (code and the manual's name), then the form the casualty's own columns make, then Add or Save, and Delete for an
 * entry that exists. A lapsed officer gets the same page with the fields shown but not editable, and no Save.
 */
@Composable
internal fun EntryScreen(
    editor: EntryEditViewModel,
    state: EntryEditState,
    actions: EntryActions,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        val title =
            if (actions.isNew) {
                OrdersBankStrings.newTitle
            } else if (actions.canEdit) {
                OrdersBankStrings.editTitle
            } else {
                OrdersBankStrings.viewTitle
            }
        ScreenBackHeader(title = title, onBack = actions.onBack)
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = Part2ListPadding, verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium)) {
            item(key = "casualty") { CasualtyHeading(editor.form.code, actions.name) }
            entryFields(editor, state, actions.canEdit)
            actions.message?.let { message -> item(key = "message") { Text(message, color = MaterialTheme.colorScheme.error) } }
            item(key = "buttons") { EntryButtons(actions) }
        }
    }
}

@Composable
private fun CasualtyHeading(
    code: String,
    name: String?,
) {
    Column {
        Text(code, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        name?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun EntryButtons(actions: EntryActions) {
    var confirming by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        if (actions.canEdit) Button(onClick = actions.onSave, modifier = Modifier.fillMaxWidth()) { Text(if (actions.isNew) OrdersBankStrings.add else OrdersBankStrings.save) }
        if (!actions.isNew) OutlinedButton(onClick = { confirming = true }, modifier = Modifier.fillMaxWidth()) { Text(OrdersBankStrings.delete) }
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(OrdersBankStrings.deleteTitle) },
            text = { Text(OrdersBankStrings.deleteBody) },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    actions.onDelete()
                }) { Text(OrdersBankStrings.delete) }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text(OrdersBankStrings.cancel) } },
        )
    }
}
