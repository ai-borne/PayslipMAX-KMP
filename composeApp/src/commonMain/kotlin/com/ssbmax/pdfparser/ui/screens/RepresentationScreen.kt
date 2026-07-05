package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.ssbmax.pdfparser.database.RepresentationDraftEntity
import com.ssbmax.pdfparser.ui.*
import com.ssbmax.pdfparser.ui.platform.rememberClipboardCopier
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings
import com.ssbmax.pdfparser.ui.theme.AppStringsPremium
import com.ssbmax.pdfparser.utils.shareText

@Composable
fun RepresentationScreen(
    viewModel: PayslipViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val drafts by viewModel.representationDrafts.collectAsState()
    var selectedDraft by remember { mutableStateOf<RepresentationDraftEntity?>(null) }
    var editedBody by remember { mutableStateOf("") }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
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
            Column(modifier = Modifier.fillMaxSize()) {
                RepresentationHeader(onBack = onBack)
                Spacer(modifier = Modifier.height(AppDimensions.SpacingMedium))
                if (drafts.isEmpty()) {
                    RepresentationEmptyState()
                } else {
                    RepresentationList(
                        drafts = drafts,
                        onSelect = {
                            selectedDraft = it
                            editedBody = it.bodyText
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RepresentationHeader(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onBack,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
        ) {
            Text(AppStrings.btnBack)
        }
        Spacer(modifier = Modifier.width(AppDimensions.SpacingMedium))
        Column {
            Text(
                text = AppStringsPremium.representationTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = AppStringsPremium.representationSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
    ) {
        items(items = drafts, key = { it.id }) { draft ->
            RepresentationCardItem(draft = draft, onSelect = onSelect)
        }
    }
}

@Composable
private fun RepresentationCardItem(
    draft: RepresentationDraftEntity,
    onSelect: (RepresentationDraftEntity) -> Unit,
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
            RepresentationActionsRow(draft = draft, onSelect = onSelect)
        }
    }
}

@Composable
private fun RepresentationActionsRow(
    draft: RepresentationDraftEntity,
    onSelect: (RepresentationDraftEntity) -> Unit,
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
