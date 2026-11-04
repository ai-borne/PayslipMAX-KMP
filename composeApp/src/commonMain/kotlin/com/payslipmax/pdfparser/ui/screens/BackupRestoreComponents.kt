package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.editableText
import androidx.compose.ui.semantics.inputText
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
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

/** The password entry. [password] is the field's whole state: it is never copied into a `String` or a saved state here (docs/Plan/14a P10). */
@Composable
internal fun BackupRestorePasswordField(
    password: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    BackupSecretField(
        state = password,
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
    confirm: TextFieldState,
    mismatch: Boolean,
    enabled: Boolean,
) {
    BackupSecretField(
        state = confirm,
        label = BackupRestoreStrings.confirmLabel,
        hint = if (mismatch) BackupRestoreStrings.passwordMismatch else BackupRestoreStrings.confirmHint,
        isError = mismatch,
        modifier = Modifier.testTag(BackupConfirmFieldTag),
        enabled = enabled,
    )
}

/**
 * One masked field for both password entries, on a `TextFieldState` (a secure text field, not a `String` value). The caret is pinned to the
 * end after every edit (the iOS cursor race, see the test beside it). Compose's semantics would hand a screen reader the typed text
 * (`EditableText` and `InputText`), so both are replaced by bullets here; the spike showed an outer `semantics` wins over the field's own.
 * The state is the caller's `remember`, never `rememberSaveable`, so the password is not saved with the screen's state.
 */
@Composable
private fun BackupSecretField(
    state: TextFieldState,
    label: String,
    hint: String,
    modifier: Modifier,
    enabled: Boolean,
    placeholder: String? = null,
    isError: Boolean = false,
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedSecureTextField(
        state = state,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = { Text(hint) },
        isError = isError,
        enabled = enabled,
        modifier =
            modifier.fillMaxWidth().semantics {
                val masked = AnnotatedString(BackupRestoreStrings.maskCharacter.toString().repeat(state.text.length))
                editableText = masked
                inputText = masked
            },
        inputTransformation = { placeCursorAtEnd() },
        textObfuscationMode = if (visible) TextObfuscationMode.Visible else TextObfuscationMode.Hidden,
        trailingIcon = { IconButton(onClick = { visible = !visible }, enabled = enabled) { Text(if (visible) "👁️" else "🙈") } },
    )
}
