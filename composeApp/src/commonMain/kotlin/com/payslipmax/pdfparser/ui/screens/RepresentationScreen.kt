package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.payslipmax.pdfparser.database.RepresentationDraftEntity
import com.payslipmax.pdfparser.subscription.FeatureGate
import com.payslipmax.pdfparser.ui.*
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.components.detailScreenSafeArea
import com.payslipmax.pdfparser.ui.platform.rememberClipboardCopier
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.AppStringsPremium
import com.payslipmax.pdfparser.utils.PdfLetterFormatter
import com.payslipmax.pdfparser.utils.sharePdf
import com.payslipmax.pdfparser.utils.shareText

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RepresentationScreen(
    viewModel: PayslipViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onUnsavedStateChanged: (Boolean) -> Unit = {},
) {
    val drafts by viewModel.representationDrafts.collectAsState()
    var selectedDraft by remember { mutableStateOf<RepresentationDraftEntity?>(null) }
    var editedBody by remember { mutableStateOf("") }
    var showUpgradeSheet by remember { mutableStateOf(false) }

    // Export to PDF is gated by CLAIM_GENERATOR (Phase 4d): unlocked users get the share sheet; locked
    // users get the upgrade sheet (D4). Everything else (edit/copy/text-share) stays free within this
    // already-premium screen.
    val hasClaimGenerator = viewModel.rememberHasAccess(FeatureGate.CLAIM_GENERATOR)
    val onExportPdf: (RepresentationDraftEntity) -> Unit = { draft ->
        if (hasClaimGenerator) {
            sharePdf(PdfLetterFormatter.fileName(draft.disputeMonth), draft.subject, draft.bodyText)
        } else {
            showUpgradeSheet = true
        }
    }

    // Nested handler: while a draft is open, back closes it and returns to the list (mirrors the
    // editor's Cancel button). Disabled at the list level, so App.kt's handler pops the screen.
    BackHandler(enabled = selectedDraft != null) { selectedDraft = null }
    LaunchedEffect(selectedDraft) { onUnsavedStateChanged(selectedDraft != null) }

    if (showUpgradeSheet) {
        PremiumUpgradeBottomSheet(
            onDismissRequest = { showUpgradeSheet = false },
            onUnlockClick = { viewModel.setPremiumEnabled(true) },
        )
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .detailScreenSafeArea()
                .padding(AppDimensions.PaddingMedium),
    ) {
        val currentSelected = selectedDraft
        if (currentSelected != null) {
            RepresentationEditor(
                draft = currentSelected,
                editedBody = editedBody,
                onBodyChange = { editedBody = it },
                onSave = {
                    viewModel.updateRepresentationDraft(currentSelected.copy(bodyText = editedBody))
                    selectedDraft = null
                },
                onCancel = { selectedDraft = null },
            )
        } else {
            RepresentationDraftList(
                drafts = drafts,
                onBack = onBack,
                onSelect = {
                    selectedDraft = it
                    editedBody = it.bodyText
                },
                onExportPdf = onExportPdf,
            )
        }
    }
}

@Composable
private fun RepresentationDraftList(
    drafts: List<RepresentationDraftEntity>,
    onBack: () -> Unit,
    onSelect: (RepresentationDraftEntity) -> Unit,
    onExportPdf: (RepresentationDraftEntity) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenBackHeader(title = AppStringsPremium.representationTitle, subtitle = AppStringsPremium.representationSubtitle, onBack = onBack)
        Spacer(modifier = Modifier.height(AppDimensions.SpacingMedium))
        if (drafts.isEmpty()) {
            RepresentationEmptyState()
        } else {
            RepresentationList(drafts = drafts, onSelect = onSelect, onExportPdf = onExportPdf)
        }
    }
}

@Composable
private fun RepresentationEmptyState(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = AppStringsPremium.representationDraftsEmpty,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(AppDimensions.PaddingLarge),
        )
    }
}

@Composable
private fun RepresentationList(
    drafts: List<RepresentationDraftEntity>,
    onSelect: (RepresentationDraftEntity) -> Unit,
    onExportPdf: (RepresentationDraftEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
    ) {
        items(items = drafts, key = { it.id }) { draft ->
            RepresentationCardItem(draft = draft, onSelect = onSelect, onExportPdf = onExportPdf)
        }
    }
}

@Composable
private fun RepresentationCardItem(
    draft: RepresentationDraftEntity,
    onSelect: (RepresentationDraftEntity) -> Unit,
    onExportPdf: (RepresentationDraftEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            Text(
                text = draft.subject,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(AppDimensions.SpacingSmall))
            Text(
                text = "${AppStringsPremium.representationMonthLabel} ${draft.disputeMonth} | ${AppStringsPremium.representationRecipientPcda}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(AppDimensions.SpacingMedium))
            RepresentationActionsRow(draft = draft, onSelect = onSelect, onExportPdf = onExportPdf)
        }
    }
}

@Composable
private fun RepresentationActionsRow(
    draft: RepresentationDraftEntity,
    onSelect: (RepresentationDraftEntity) -> Unit,
    onExportPdf: (RepresentationDraftEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val copyToClipboard = rememberClipboardCopier()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
    ) {
        Button(
            onClick = { onSelect(draft) },
            modifier = Modifier.weight(1f),
        ) {
            Text(AppStringsPremium.representationEditBtn)
        }
        OutlinedButton(
            onClick = { copyToClipboard(draft.bodyText) },
            modifier = Modifier.weight(1f),
        ) {
            Text(AppStringsPremium.representationCopyBtn)
        }
        OutlinedButton(
            onClick = { shareText(draft.bodyText, draft.subject) },
            modifier = Modifier.weight(1f),
        ) {
            Text(AppStringsPremium.representationShareBtn)
        }
        OutlinedButton(
            onClick = { onExportPdf(draft) },
            modifier = Modifier.weight(1f),
        ) {
            Text(AppStringsPremium.representationExportPdfBtn)
        }
    }
}

@Composable
private fun RepresentationEditor(
    draft: RepresentationDraftEntity,
    editedBody: String,
    onBodyChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
    ) {
        Text(
            text = "${AppStringsPremium.representationEditDraftTitle} ${draft.disputeType}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        OutlinedTextField(
            value = editedBody,
            onValueChange = onBodyChange,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            textStyle = MaterialTheme.typography.bodySmall,
            shape = RoundedCornerShape(AppDimensions.CornerRadius),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
            ) {
                Text(AppStringsPremium.representationSaveBtn)
            }
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            ) {
                Text(AppStrings.btnCancel)
            }
        }
    }
}
