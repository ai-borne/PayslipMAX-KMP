package com.payslipmax.pdfparser.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.applyCorrection
import com.payslipmax.pdfparser.ui.cancelEditingSession
import com.payslipmax.pdfparser.ui.deleteDraftCorrection
import com.payslipmax.pdfparser.ui.saveEditingSession
import com.payslipmax.pdfparser.ui.startEditingSession
import com.payslipmax.pdfparser.ui.updateDraftCorrection

/**
 * Self-contained History payslip detail: resolves its own payslip from
 * [PayslipUiState.historyDetailPayslipId][com.payslipmax.pdfparser.ui.PayslipUiState] so Android
 * (inline push) and iOS (native VC) construct it identically with just [viewModel] + [onBack].
 * Cleanup of an in-flight edit session is lifecycle-guaranteed via [DisposableEffect] rather than
 * gesture-dependent, so it fires on every exit path — arrow, system back, swipe, or process death
 * — closing the cross-payslip correction-corruption gap the old arrow-bypasses-BackHandler path had.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PayslipReplicaDetailScreen(
    viewModel: PayslipViewModel,
    onBack: () -> Unit,
    onOpenOriginal: (ParsedPayslip) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val payslip = uiState.payslips.find { it.dateStr == uiState.historyDetailPayslipId }

    if (payslip == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val handleBack = { if (uiState.isEditModeActive) viewModel.cancelEditingSession() else onBack() }
    BackHandler(enabled = true) { handleBack() }
    DisposableEffect(Unit) {
        onDispose { if (viewModel.uiState.value.isEditModeActive) viewModel.cancelEditingSession() }
    }

    PayslipReplicaScreen(
        payslip = payslip,
        onBackClick = handleBack,
        onViewPdfClick = { viewModel.getPayslipPdf(it) { bytes -> if (bytes != null) onOpenOriginal(payslip) } },
        modifier = modifier,
        onCorrectField = { fieldKey, newValue -> viewModel.applyCorrection(payslip.dateStr, fieldKey, newValue) },
        isEditModeActive = uiState.isEditModeActive,
        draftCorrections = uiState.draftCorrections,
        onStartEditing = { viewModel.startEditingSession(payslip.dateStr) },
        onUpdateDraft = { viewModel.updateDraftCorrection(it) },
        onDeleteDraft = { fieldKey, codeHead, category, originalAmount ->
            viewModel.deleteDraftCorrection(fieldKey, codeHead, category, originalAmount)
        },
        onSaveSession = { viewModel.saveEditingSession(payslip.dateStr) },
        onCancelSession = { viewModel.cancelEditingSession() },
    )
}
