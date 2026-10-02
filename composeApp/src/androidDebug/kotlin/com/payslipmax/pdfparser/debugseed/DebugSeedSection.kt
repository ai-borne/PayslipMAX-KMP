package com.payslipmax.pdfparser.debugseed

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.payslipmax.pdfparser.database.PayslipDao
import com.payslipmax.pdfparser.repository.FinancialIntelligenceRepository
import org.koin.compose.koinInject

/** The Settings section: Koin-backed entry used by the registry. */
@Composable
fun DebugSeedSection() {
    val dao = koinInject<PayslipDao>()
    val intelligence = koinInject<FinancialIntelligenceRepository>()
    val viewModel = remember { DebugSeedViewModel(DebugSeeder(dao, intelligence)) }
    DisposableEffect(viewModel) { onDispose { viewModel.dispose() } }
    DebugSeedSection(viewModel)
}

@Composable
fun DebugSeedSection(viewModel: DebugSeedViewModel) {
    val state by viewModel.uiState.collectAsState()
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(DebugSeedStrings.TITLE, style = MaterialTheme.typography.titleMedium)
        Text(DebugSeedStrings.DESCRIPTION, style = MaterialTheme.typography.bodySmall)
        SeedStep.entries.forEach { step ->
            Spacer(Modifier.height(8.dp))
            Button(onClick = { viewModel.seed(step) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                Text(DebugSeedStrings.label(step))
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { viewModel.remove() }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
            Text(DebugSeedStrings.REMOVE)
        }
        state.message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
