package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

@Composable
fun PayslipReplicaScreen(
    payslip: ParsedPayslip,
    onBackClick: () -> Unit,
    onViewPdfClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onCorrectField: (fieldKey: String, newValue: Double) -> Unit = { _, _ -> },
) {
    var activeGlossaryItem by remember { mutableStateOf<Pair<String, String>?>(null) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(AppDimensions.PaddingMedium),
    ) {
        ReplicaHeader(onBackClick)
        Spacer(modifier = Modifier.height(AppDimensions.SpacingLarge))

        MetadataSection(payslip = payslip)
        Spacer(modifier = Modifier.height(AppDimensions.SpacingLarge))

        PdfDocumentCard(payslip = payslip, onViewPdfClick = onViewPdfClick)
        Spacer(modifier = Modifier.height(AppDimensions.SpacingLarge))

        LedgerSection(
            payslip = payslip,
            onItemClick = { code, desc -> activeGlossaryItem = code to desc },
            onCorrectField = onCorrectField,
        )

        Spacer(modifier = Modifier.height(AppDimensions.SpacingHuge))
        FooterSection()

        activeGlossaryItem?.let { (code, desc) ->
            GlossaryDialog(code = code, desc = desc, onDismiss = { activeGlossaryItem = null })
        }
    }
}

@Composable
private fun ReplicaHeader(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = AppStrings.replicaBackDesc,
            )
        }
        Spacer(modifier = Modifier.width(AppDimensions.SpacingSmall))
        Column {
            Text(
                text = AppStrings.explorerHeader,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = AppStrings.explorerSubheader,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MetadataSection(payslip: ParsedPayslip) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadiusMedium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            Text(
                text = "${AppStrings.replicaOfficerPrefix}${payslip.officer.name}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = AppDimensions.SpacingTiny),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "${AppStrings.replicaCdaPrefix}${payslip.officer.accountNo}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "${AppStrings.replicaPanPrefix}${payslip.officer.pan}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun FooterSection() {
    Text(
        text = AppStrings.replicaFooter,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun GlossaryDialog(
    code: String,
    desc: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = code, fontWeight = FontWeight.Bold) },
        text = { Text(text = desc, style = MaterialTheme.typography.bodyLarge) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = AppStrings.glossaryUnderstood)
            }
        },
    )
}
