package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.pdfparser.ui.PayslipViewModel
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun ProfileOverridesCard(
    viewModel: PayslipViewModel,
    profileName: String,
    profileCda: String,
    profilePan: String,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf(profileName) }
    var cda by remember { mutableStateOf(profileCda) }
    var pan by remember { mutableStateOf(profilePan) }

    LaunchedEffect(profileName, profileCda, profilePan) {
        name = profileName
        cda = profileCda
        pan = profilePan
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(AppStrings.settingsProfileHeader, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(AppStrings.settingsProfileDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(AppStrings.settingsProfileName) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = cda, onValueChange = { cda = it }, label = { Text(AppStrings.settingsProfileCda) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = pan, onValueChange = { pan = it }, label = { Text(AppStrings.settingsProfilePan) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Button(
                onClick = { viewModel.updateProfileOverrides(name, cda, pan) },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(AppStrings.settingsProfileSaveBtn)
            }
        }
    }
}
