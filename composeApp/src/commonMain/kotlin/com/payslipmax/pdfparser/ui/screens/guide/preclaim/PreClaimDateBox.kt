package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.ui.theme.PreClaimStrings

/**
 * A date chosen on a calendar. The button shows the chosen date as DD/MM/YYYY (or asks to choose one) and opens the picker; "Set date"
 * hands back the picked day, "Clear date" hands back null, and nothing reaches the ViewModel until one of them is tapped. Whether the
 * date makes sense with the other one (reversed, too long) is the calculator's answer, shown in the estimates.
 */
@Composable
internal fun PreClaimDateBox(
    label: String,
    date: CalendarDate?,
    onDate: (CalendarDate?) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
        Text("$label: ${if (date == null) PreClaimStrings.dateChoose else PreClaimDates.format(date)}", modifier = Modifier.weight(1f))
    }
    if (open) PreClaimDateDialog(date, onDate) { open = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreClaimDateDialog(
    date: CalendarDate?,
    onDate: (CalendarDate?) -> Unit,
    onClose: () -> Unit,
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = date?.let(PreClaimDates::toUtcMillis), yearRange = PreClaimDates.YEARS)
    DatePickerDialog(
        onDismissRequest = onClose,
        confirmButton = {
            TextButton(onClick = {
                onDate(state.selectedDateMillis?.let(PreClaimDates::fromUtcMillis))
                onClose()
            }) { Text(PreClaimStrings.dateSet) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    onDate(null)
                    onClose()
                }) { Text(PreClaimStrings.dateClear) }
                TextButton(onClick = onClose) { Text(PreClaimStrings.dateCancel) }
            }
        },
    ) { DatePicker(state) }
}
