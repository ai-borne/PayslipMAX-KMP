package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

@Composable
fun UploadWidget(
    isLoading: Boolean,
    error: String?,
    success: Boolean,
    onPickPdfTrigger: (password: String) -> Unit,
    onClearError: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            UploadWidgetHeader(onDismiss = onDismiss)
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(AppStrings.labelPassword) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(if (passwordVisible) AppStrings.hidePasswordToggle else AppStrings.showPasswordToggle)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Button(
                onClick = { onPickPdfTrigger(password) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppDimensions.CornerRadiusMedium),
            ) {
                Text(AppStrings.labelSelectPdf, fontSize = AppDimensions.TextSizeButton)
            }
            UploadStatusSection(isLoading, error, success, onClearError)
        }
    }
}

@Composable
private fun UploadWidgetHeader(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.padding(end = AppDimensions.IconSizeExtraLarge, start = AppDimensions.PaddingSmall),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = AppStrings.uploadHeader,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(AppDimensions.SpacingTiny))
            Text(
                text = AppStrings.uploadDesc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close Dialog",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(AppDimensions.IconSizeMedium))
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
                Text(AppStrings.uploadDismiss, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    if (success) {
        Text(
            text = AppStrings.uploadSuccess,
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
