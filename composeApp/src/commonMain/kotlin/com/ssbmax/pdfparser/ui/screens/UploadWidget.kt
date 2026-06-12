package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun UploadWidget(
    isLoading: Boolean,
    error: String?,
    success: Boolean,
    onPickPdfTrigger: (password: String) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var password by remember { mutableStateOf("535d04") }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            UploadWidgetHeader()
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(AppStrings.labelPassword) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Button(
                onClick = { onPickPdfTrigger(password) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(AppStrings.labelSelectPdf, fontSize = 16.sp)
            }
            UploadStatusSection(isLoading, error, success, onClearError)
        }
    }
}

@Composable
private fun UploadWidgetHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = AppStrings.uploadHeader,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = AppStrings.uploadDesc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun UploadStatusSection(
    isLoading: Boolean,
    error: String?,
    success: Boolean,
    onClearError: () -> Unit,
) {
    if (isLoading) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Text(AppStrings.loaderDecrypt, style = MaterialTheme.typography.labelSmall)
        }
    }
    error?.let {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onClearError) {
                Text("Dismiss", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    if (success) {
        Text(
            text = "Payslip Imported Successfully!",
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
