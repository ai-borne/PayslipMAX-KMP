package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun HistoryDeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = AppStrings.historyConfirmDeleteTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(
                text = AppStrings.historyConfirmDeleteMessage,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
            ) {
                Text(AppStrings.historyActionDelete)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.btnCancel)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryActionBottomSheet(
    payslip: ParsedPayslip,
    onDismissRequest: () -> Unit,
    onViewReplica: () -> Unit,
    onViewOriginal: () -> Unit,
    onShareSummary: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        HistoryActionSheetContent(
            payslip = payslip,
            onDismissRequest = onDismissRequest,
            onViewReplica = onViewReplica,
            onViewOriginal = onViewOriginal,
            onShareSummary = onShareSummary,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun HistoryActionSheetContent(
    payslip: ParsedPayslip,
    onDismissRequest: () -> Unit,
    onViewReplica: () -> Unit,
    onViewOriginal: () -> Unit,
    onShareSummary: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = AppDimensions.PaddingMedium)
                .padding(bottom = AppDimensions.PaddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
    ) {
        Text(
            text = "${payslip.monthName} ${payslip.year}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = AppDimensions.SpacingSmall),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        Spacer(modifier = Modifier.height(AppDimensions.SpacingSmall))

        BottomSheetActionList(
            onDismissRequest = onDismissRequest,
            onViewReplica = onViewReplica,
            onViewOriginal = onViewOriginal,
            onShareSummary = onShareSummary,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun BottomSheetActionList(
    onDismissRequest: () -> Unit,
    onViewReplica: () -> Unit,
    onViewOriginal: () -> Unit,
    onShareSummary: () -> Unit,
    onDelete: () -> Unit,
) {
    BottomSheetActionItem(
        icon = Icons.AutoMirrored.Filled.List,
        label = AppStrings.historyActionViewReplica,
        onClick = {
            onViewReplica()
            onDismissRequest()
        },
    )
    BottomSheetActionItem(
        icon = Icons.Default.PlayArrow,
        label = AppStrings.historyActionViewOriginal,
        onClick = {
            onViewOriginal()
            onDismissRequest()
        },
    )
    BottomSheetActionItem(
        icon = Icons.Default.Share,
        label = AppStrings.historyActionShareSummary,
        onClick = {
            onShareSummary()
            onDismissRequest()
        },
    )
    BottomSheetActionItem(
        icon = Icons.Default.Delete,
        label = AppStrings.historyActionDelete,
        isDestructive = true,
        onClick = {
            onDelete()
        },
    )
}

@Composable
private fun BottomSheetActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
) {
    val tintColor =
        if (isDestructive) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = AppDimensions.PaddingMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tintColor,
            modifier = Modifier.size(AppDimensions.IconSizeMedium),
        )
        Spacer(modifier = Modifier.width(AppDimensions.SpacingLarge))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = tintColor,
        )
    }
}
