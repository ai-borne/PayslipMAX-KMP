package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.payslipmax.pdfparser.ui.theme.AppDimensions

data class BackupStatus(
    val message: String,
    val isSuccess: Boolean,
)

@Composable
fun StatusMessage(status: BackupStatus) {
    Text(
        text = status.message,
        style = MaterialTheme.typography.labelSmall,
        color = if (status.isSuccess) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = AppDimensions.SpacingTiny),
    )
}
