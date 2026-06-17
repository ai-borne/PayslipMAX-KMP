package com.ssbmax.pdfparser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.insights.RedactionSanitizer
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransparencyDialog(
    payslip: ParsedPayslip,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVerified by remember { mutableStateOf(false) }
    val redacted = remember(payslip) { RedactionSanitizer.redact(payslip) }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.fillMaxWidth(0.95f)
    ) {
        Surface(
            shape = CardDefaults.shape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = AppDimensions.DialogElevation
        ) {
            TransparencyDialogContent(
                raw = payslip,
                sanitized = redacted,
                isVerified = isVerified,
                onVerificationChange = { isVerified = it },
                onConfirm = onConfirm,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun TransparencyDialogContent(
    raw: ParsedPayslip,
    sanitized: ParsedPayslip,
    isVerified: Boolean,
    onVerificationChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(AppDimensions.PaddingMedium)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium)
    ) {
        Text(
            text = AppStrings.transparencyTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = AppStrings.transparencyMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        TransparencyComparisonPanels(
            raw = raw,
            sanitized = sanitized,
            modifier = Modifier
                .weight(1f, fill = false)
                .heightIn(max = AppDimensions.DialogMaxHeight)
        )

        TransparencyActionFooter(
            isVerified = isVerified,
            onVerificationChange = onVerificationChange,
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun TransparencyComparisonPanels(
    raw: ParsedPayslip,
    sanitized: ParsedPayslip,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                AppDimensions.BorderThin,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                CardDefaults.shape
            )
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), CardDefaults.shape)
            .padding(AppDimensions.PaddingSmall)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)
        ) {
            TransparencyDataColumn(
                title = AppStrings.transparencyLabelRaw,
                titleColor = MaterialTheme.colorScheme.error,
                payslip = raw,
                modifier = Modifier.weight(1f)
            )
            TransparencyDataColumn(
                title = AppStrings.transparencyLabelSanitized,
                titleColor = MaterialTheme.colorScheme.primary,
                payslip = sanitized,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TransparencyDataColumn(
    title: String,
    titleColor: Color,
    payslip: ParsedPayslip,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = titleColor
        )
        Text(
            text = "${AppStrings.labelOfficerName}${payslip.officer.name}\n" +
                   "${AppStrings.labelPanNumber}${payslip.officer.pan}\n" +
                   "${AppStrings.labelCdaAccount}${payslip.officer.accountNo}\n" +
                   "${AppStrings.labelFilename}${payslip.file}",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun TransparencyActionFooter(
    isVerified: Boolean,
    onVerificationChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = isVerified,
                onCheckedChange = onVerificationChange
            )
            Spacer(modifier = Modifier.width(AppDimensions.SpacingSmall))
            Text(
                text = AppStrings.transparencyDisclaimer,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }

        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.btnCancel)
            }
            Spacer(modifier = Modifier.width(AppDimensions.SpacingMedium))
            Button(
                onClick = onConfirm,
                enabled = isVerified
            ) {
                Text(AppStrings.transparencyActionApprove)
            }
        }
    }
}
