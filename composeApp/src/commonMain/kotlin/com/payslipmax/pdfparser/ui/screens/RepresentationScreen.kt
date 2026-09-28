package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import com.payslipmax.pcdao.redressal.RedressalLetter
import com.payslipmax.pdfparser.database.RepresentationDraftEntity
import com.payslipmax.pdfparser.subscription.FeatureGate
import com.payslipmax.pdfparser.ui.*
import com.payslipmax.pdfparser.ui.components.detailScreenSafeArea
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.utils.PdfLetterFormatter
import com.payslipmax.pdfparser.utils.sharePdf

@OptIn(ExperimentalComposeUiApi::class)
@Suppress("DEPRECATION")
@Composable
fun RepresentationScreen(
    viewModel: PayslipViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onUnsavedStateChanged: (Boolean) -> Unit = {},
    initialLetter: RedressalLetter? = null,
) {
    val drafts by viewModel.representationDrafts.collectAsState()
    val initialDraft = remember(initialLetter) { initialLetter?.toRepresentationDraftEntity() }
    var selectedDraft by remember { mutableStateOf<RepresentationDraftEntity?>(initialDraft) }
    var editedBody by remember { mutableStateOf(initialDraft?.bodyText ?: "") }
    var showUpgradeSheet by remember { mutableStateOf(false) }

    LaunchedEffect(initialDraft) {
        if (initialDraft != null) viewModel.updateRepresentationDraft(initialDraft)
    }

    BackHandler(enabled = selectedDraft != null) { selectedDraft = null }
    LaunchedEffect(selectedDraft) { onUnsavedStateChanged(selectedDraft != null) }

    if (showUpgradeSheet) {
        RepresentationUpgradeDialog(viewModel = viewModel, onDismiss = { showUpgradeSheet = false })
    }

    val onExportPdf = rememberRepresentationPdfExporter(viewModel) { showUpgradeSheet = true }

    Box(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).detailScreenSafeArea().padding(AppDimensions.PaddingMedium),
    ) {
        RepresentationBody(
            selectedDraft = selectedDraft,
            editedBody = editedBody,
            drafts = drafts,
            onBack = onBack,
            onBodyChange = { editedBody = it },
            onSave = { draft, body ->
                viewModel.updateRepresentationDraft(draft.copy(bodyText = body))
                selectedDraft = null
            },
            onCancel = { selectedDraft = null },
            onSelect = { draft ->
                selectedDraft = draft
                editedBody = draft.bodyText
            },
            onExportPdf = onExportPdf,
        )
    }
}

@Composable
private fun RepresentationBody(
    selectedDraft: RepresentationDraftEntity?,
    editedBody: String,
    drafts: List<RepresentationDraftEntity>,
    onBack: () -> Unit,
    onBodyChange: (String) -> Unit,
    onSave: (RepresentationDraftEntity, String) -> Unit,
    onCancel: () -> Unit,
    onSelect: (RepresentationDraftEntity) -> Unit,
    onExportPdf: (RepresentationDraftEntity) -> Unit,
) {
    if (selectedDraft != null) {
        RepresentationEditor(
            draft = selectedDraft,
            editedBody = editedBody,
            onBodyChange = onBodyChange,
            onSave = { onSave(selectedDraft, editedBody) },
            onCancel = onCancel,
        )
    } else {
        RepresentationDraftList(
            drafts = drafts,
            onBack = onBack,
            onSelect = onSelect,
            onExportPdf = onExportPdf,
        )
    }
}

@Composable
private fun rememberRepresentationPdfExporter(
    viewModel: PayslipViewModel,
    onShowUpgrade: () -> Unit,
): (RepresentationDraftEntity) -> Unit {
    val hasClaimGenerator = viewModel.rememberHasAccess(FeatureGate.CLAIM_GENERATOR)
    return { draft ->
        if (hasClaimGenerator) {
            sharePdf(PdfLetterFormatter.fileName(draft.disputeMonth), draft.subject, draft.bodyText)
        } else {
            onShowUpgrade()
        }
    }
}

@Composable
private fun RepresentationUpgradeDialog(
    viewModel: PayslipViewModel,
    onDismiss: () -> Unit,
) {
    val premiumPrice by viewModel.premiumPriceState.collectAsState()
    PremiumUpgradeBottomSheet(
        onDismissRequest = onDismiss,
        onUnlockClick = { onResult -> viewModel.launchPurchaseFlow(onResult) },
        onRestoreClick = { onResult -> viewModel.restorePurchases(onResult) },
        price = premiumPrice,
        onPresented = viewModel::refreshPremiumPrice,
    )
}
