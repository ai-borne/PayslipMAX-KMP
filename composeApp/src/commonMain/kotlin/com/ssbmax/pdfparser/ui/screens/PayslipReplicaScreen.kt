package com.ssbmax.pdfparser.ui.screens

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
import androidx.compose.ui.unit.dp
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun PayslipReplicaScreen(
    payslip: ParsedPayslip,
    onBackClick: () -> Unit,
    onViewPdfClick: (String) -> Unit,
    modifier: Modifier = Modifier,
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
        Spacer(modifier = Modifier.height(16.dp))

        MetadataSection(payslip = payslip)
        Spacer(modifier = Modifier.height(16.dp))

        PdfDocumentCard(payslip = payslip, onViewPdfClick = onViewPdfClick)
        Spacer(modifier = Modifier.height(16.dp))

        LedgerSection(payslip = payslip) { code, desc ->
            activeGlossaryItem = code to desc
        }

        Spacer(modifier = Modifier.height(24.dp))
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
                contentDescription = "Back",
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
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
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            Text(
                text = "Officer: ${payslip.officer.name}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "CDA A/C: ${payslip.officer.accountNo}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "PAN: ${payslip.officer.pan}", style = MaterialTheme.typography.bodyMedium)
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
                Text("Understood")
            }
        },
    )
}
