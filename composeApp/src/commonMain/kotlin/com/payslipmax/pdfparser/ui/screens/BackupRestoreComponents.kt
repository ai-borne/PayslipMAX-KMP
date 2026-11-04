package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.BackupRestoreStrings

@Composable
fun BackupRestoreHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = AppStrings.settingsBackupHeader,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = AppStrings.settingsBackupDesc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

const val BackupRestorePasswordFieldTestTag = "backup_restore_password_field"

@Composable
fun BackupRestorePasswordField(
    password: String,
    onPasswordChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    BackupSecretField(
        value = password,
        onValueChange = onPasswordChange,
        label = AppStrings.settingsBackupPasswordLabel,
        hint = AppStrings.settingsBackupPasswordHint,
        placeholder = AppStrings.settingsBackupPasswordPlaceholder,
        modifier = modifier.testTag(BackupRestorePasswordFieldTestTag),
        enabled = enabled,
    )
}

/** Back Up asks for the password twice (14a P8): a mismatch shows [BackupRestoreStrings.passwordMismatch] and the sheet disables Back Up. */
@Composable
internal fun BackupConfirmPasswordField(
    confirm: String,
    mismatch: Boolean,
    onConfirmChange: (String) -> Unit,
    enabled: Boolean,
) {
    BackupSecretField(
        value = confirm,
        onValueChange = onConfirmChange,
        label = BackupRestoreStrings.confirmLabel,
        hint = if (mismatch) BackupRestoreStrings.passwordMismatch else BackupRestoreStrings.confirmHint,
        isError = mismatch,
        modifier = Modifier.testTag(BackupConfirmFieldTag),
        enabled = enabled,
    )
}

/**
 * One masked field for both password entries. The text is pinned to the end on every change (the iOS cursor race, see the test beside it).
 * The value lives in the caller's `remember` state, never `rememberSaveable`, so the password is not saved with the screen's state.
 */
@Composable
private fun BackupSecretField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    hint: String,
    modifier: Modifier,
    enabled: Boolean,
    placeholder: String? = null,
    isError: Boolean = false,
) {
    var visible by remember { mutableStateOf(false) }
    var fieldValue by remember(value) { mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length))) }
    OutlinedTextField(
        value = fieldValue,
        onValueChange = {
            fieldValue = it.copy(selection = TextRange(it.text.length))
            onValueChange(it.text)
        },
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = { Text(hint) },
        isError = isError,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = { IconButton(onClick = { visible = !visible }, enabled = enabled) { Text(if (visible) "👁️" else "🙈") } },
    )
}
