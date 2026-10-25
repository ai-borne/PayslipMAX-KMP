package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.Part2VaultStrings

/**
 * "Use an order from my vault": the numbered orders the vault holds, newest first. Nothing is chosen for the officer (with two leaves in
 * flight the newest order may be the wrong grant); a pick calls [onPick] and closes the list. Drawn only when there is something to pick.
 */
@Composable
internal fun GrantPicker(
    choices: List<GrantChoiceUi>,
    onPick: (GrantChoiceUi) -> Unit,
) {
    if (choices.isEmpty()) return
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = !open }, modifier = Modifier.fillMaxWidth()) { Text(Part2VaultStrings.useVaultOrder) }
    if (!open) return
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny)) {
        choices.forEach { choice ->
            TextButton(
                onClick = {
                    open = false
                    onPick(choice)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(choice.label) }
        }
        TextButton(onClick = { open = false }) { Text(Part2VaultStrings.closeChoices) }
    }
}
