package com.payslipmax.pdfparser.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import com.payslipmax.pdfparser.Screen
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
 * [onOpenPdf] receives the original's bytes, fetched once here; [onNavigateTo] pushes a detail on top (the Leave bank teaser).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Suppress("DEPRECATION")
@Composable
fun PayslipReplicaDetailScreen(
    viewModel: PayslipViewModel,
    onBack: () -> Unit,
    onOpenPdf: (pdfBytes: ByteArray, filename: String) -> Unit = { _, _ -> },
    onNavigateTo: (Screen) -> Unit = {},
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
        onViewPdfClick = { viewModel.getPayslipPdf(it) { bytes -> if (bytes != null) onOpenPdf(bytes, payslip.file) } },
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
        profileName = uiState.profileName,
        profileCda = uiState.profileCdaNumber,
        profilePan = uiState.profilePanNumber,
        leaveSection = payslipLeaveTable(payslip)?.let { table -> { PayslipLeaveSection(viewModel, table, uiState.isEditModeActive, onNavigateTo) } },
    )
}
