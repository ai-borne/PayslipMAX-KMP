package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import com.payslipmax.pdfparser.ui.theme.AppDimensions

/**
 * One text or number box, controlled by [value]. It binds to the ViewModel's `typed` form (updated in the same step as the setter),
 * so what the officer sees is exactly what the ViewModel kept: a digit it refuses never shows. [error] turns the box red and shows
 * [supporting] as the reason; otherwise [supporting] is a quiet hint.
 */
@Composable
internal fun PreClaimTextBox(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    numeric: Boolean = true,
    supporting: String? = null,
    error: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        isError = error,
        supportingText = supporting?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
        modifier = modifier.fillMaxWidth(),
    )
}

/** A labelled tick, read by screen readers as one checkbox row. */
@Composable
internal fun PreClaimTickRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

/** A short single choice shown as chips (two to four options, such as Yes/No or the CTG case). [answer] null means not chosen yet. */
@Composable
internal fun <T> PreClaimChipChoice(
    options: List<Pair<T, String>>,
    answer: T?,
    onChoose: (T) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall), modifier = Modifier.semantics { role = Role.RadioButton }) {
        options.forEach { (option, label) ->
            FilterChip(
                selected = option == answer,
                onClick = { onChoose(option) },
                label = { Text(label) },
                modifier = Modifier.semantics { if (option == answer) stateDescription = label },
            )
        }
    }
}

/** A long single choice (levels, states, rules) as a button that opens a menu. [selected] is the chosen text or null. */
@Composable
internal fun PreClaimChoiceBox(
    label: String,
    placeholder: String,
    selected: String?,
    choices: List<Pair<String, String>>,
    onChoose: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$label: ${selected ?: placeholder}", modifier = Modifier.weight(1f))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            choices.forEach { (id, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = {
                    open = false
                    onChoose(id)
                })
            }
        }
    }
}
