@file:OptIn(ExperimentalMaterial3Api::class)

package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.*
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

fun formatProfileSubtitle(
    profileName: String,
    profileCda: String,
): String {
    return if (profileName.isNotBlank()) {
        if (profileCda.isNotBlank()) {
            "$profileName • CDA: $profileCda"
        } else {
            profileName
        }
    } else {
        AppStrings.settingsProfileRowSubtitleUnconfigured
    }
}

@Composable
fun ProfileOverridesCard(
    viewModel: PayslipViewModel,
    profileName: String,
    profileCda: String,
    profilePan: String,
    modifier: Modifier = Modifier,
) {
    var showSheet by remember { mutableStateOf(false) }
    val subtitleText = formatProfileSubtitle(profileName, profileCda)

    SettingsRow(
        icon = "👤",
        title = AppStrings.settingsRowProfileLabel,
        subtitle = subtitleText,
        onClick = { showSheet = true },
        modifier = modifier,
    )

    if (showSheet) {
        ProfileOverridesBottomSheet(
            profileName = profileName,
            profileCda = profileCda,
            profilePan = profilePan,
            onSave = { name, cda, pan ->
                viewModel.updateProfileOverrides(name, cda, pan)
                showSheet = false
            },
            onDismissRequest = { showSheet = false },
        )
    }
}

@Composable
private fun ProfileOverridesBottomSheet(
    profileName: String,
    profileCda: String,
    profilePan: String,
    onSave: (String, String, String) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        ProfileOverridesSheetContent(
            profileName = profileName,
            profileCda = profileCda,
            profilePan = profilePan,
            onSave = onSave,
            onCloseClick = onDismissRequest,
        )
    }
}

@Composable
private fun ProfileSheetHeader() {
    Text(
        text = AppStrings.settingsProfileHeader,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
    Text(
        text = AppStrings.settingsProfileDesc,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ProfileInfoBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny),
        ) {
            Text(
                text = AppStrings.settingsProfileInfoBannerHeader,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = AppStrings.settingsProfileInfoBannerBullet1,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = AppStrings.settingsProfileInfoBannerBullet2,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = AppStrings.settingsProfileInfoBannerBullet3,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProfileOverridesSheetContent(
    profileName: String,
    profileCda: String,
    profilePan: String,
    onSave: (String, String, String) -> Unit,
    onCloseClick: () -> Unit,
) {
    var name by remember { mutableStateOf(profileName) }
    var cda by remember { mutableStateOf(profileCda) }
    var pan by remember { mutableStateOf(profilePan) }

    LaunchedEffect(profileName, profileCda, profilePan) {
        name = profileName
        cda = profileCda
        pan = profilePan
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = AppDimensions.PaddingMedium)
                .padding(bottom = AppDimensions.PaddingLarge),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
    ) {
        ProfileSheetHeader()
        ProfileInfoBanner()
        ProfileInputFields(
            name = name,
            onNameChange = { name = it },
            cda = cda,
            onCdaChange = { cda = it },
            pan = pan,
            onPanChange = { pan = it },
        )
        ProfileActionsRow(
            onSaveClick = { onSave(name, cda, pan) },
            onCancelClick = onCloseClick,
        )
    }
}

@Composable
private fun ProfileInputFields(
    name: String,
    onNameChange: (String) -> Unit,
    cda: String,
    onCdaChange: (String) -> Unit,
    pan: String,
    onPanChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text(AppStrings.settingsProfileName) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
    OutlinedTextField(
        value = cda,
        onValueChange = onCdaChange,
        label = { Text(AppStrings.settingsProfileCda) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
    OutlinedTextField(
        value = pan,
        onValueChange = onPanChange,
        label = { Text(AppStrings.settingsProfilePan) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}

@Composable
private fun ProfileActionsRow(
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = onCancelClick,
            modifier = Modifier.weight(1f),
        ) {
            Text(AppStrings.btnCancel)
        }
        Button(
            onClick = onSaveClick,
            modifier = Modifier.weight(1f),
        ) {
            Text(AppStrings.settingsProfileSaveBtn)
        }
    }
}
