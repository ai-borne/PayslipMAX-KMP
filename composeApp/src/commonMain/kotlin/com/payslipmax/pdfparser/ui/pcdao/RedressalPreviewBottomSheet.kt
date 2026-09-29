package com.payslipmax.pdfparser.ui.pcdao

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.payslipmax.pcdao.redressal.RedressalLetter
import com.payslipmax.pdfparser.ui.platform.rememberClipboardCopier

@Composable
fun RedressalPreviewBottomSheet(
    letter: RedressalLetter,
    onDismiss: () -> Unit,
    onExportPdf: (RedressalLetter) -> Unit,
    onShareText: (RedressalLetter) -> Unit,
    onEditText: (RedressalLetter) -> Unit,
    onToggleMaskPii: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboardCopier = rememberClipboardCopier()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier =
                modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                RedressalSheetHeader(onDismiss = onDismiss)

                RedressalSheetContent(
                    letter = letter,
                    onToggleMaskPii = onToggleMaskPii,
                    modifier = Modifier.weight(1f),
                )

                RedressalSheetActions(
                    letter = letter,
                    onExportPdf = { onExportPdf(letter) },
                    onShareText = { onShareText(letter) },
                    onEditText = { onEditText(letter) },
                    onCopyText = { clipboardCopier(letter.fullBodyText) },
                )
            }
        }
    }
}

@Composable
private fun RedressalSheetHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = AppStringsPcdao.letterModalTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = AppStringsPcdao.redressalDraftTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RedressalSheetContent(
    letter: RedressalLetter,
    onToggleMaskPii: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MaskPiiToggleRow(
            isMasked = letter.isPiiMasked,
            onToggle = { onToggleMaskPii(!letter.isPiiMasked) },
        )
        LetterBodyPreviewCard(bodyText = letter.fullBodyText)
    }
}

@Composable
private fun MaskPiiToggleRow(
    isMasked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(checked = isMasked, onCheckedChange = { onToggle() })
        Text(
            text = AppStringsPcdao.chkMaskPiiLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun LetterBodyPreviewCard(bodyText: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = bodyText,
            modifier = Modifier.padding(12.dp),
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RedressalSheetActions(
    letter: RedressalLetter,
    onExportPdf: () -> Unit,
    onShareText: () -> Unit,
    onEditText: () -> Unit,
    onCopyText: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        RedressalPrimaryActionRow(onExportPdf = onExportPdf, onShareText = onShareText)
        RedressalSecondaryActionRow(onCopyText = onCopyText, onEditText = onEditText)
    }
}

@Composable
private fun RedressalPrimaryActionRow(
    onExportPdf: () -> Unit,
    onShareText: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onExportPdf,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(
                text = AppStringsPcdao.btnExportPdf,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Button(
            onClick = onShareText,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
        ) {
            Text(
                text = AppStringsPcdao.btnShareText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun RedressalSecondaryActionRow(
    onCopyText: () -> Unit,
    onEditText: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = onCopyText,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(
                text = AppStringsPcdao.btnCopyText,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Button(
            onClick = onEditText,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
        ) {
            Text(
                text = AppStringsPcdao.btnEditText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
