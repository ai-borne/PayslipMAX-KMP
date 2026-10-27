package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

/**
 * The box for a year-by-year list: several lines, one `YYYY D` line a year, so a full career's break-up can be read and edited without
 * scrolling a single line sideways. A semicolon still ends a line, as the reader takes it.
 */
@Composable
internal fun YearsBox(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    supporting: String?,
    error: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = false,
        minLines = MIN_LINES,
        isError = error,
        supportingText = supporting?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        modifier = Modifier.fillMaxWidth(),
    )
}

private const val MIN_LINES = 4
