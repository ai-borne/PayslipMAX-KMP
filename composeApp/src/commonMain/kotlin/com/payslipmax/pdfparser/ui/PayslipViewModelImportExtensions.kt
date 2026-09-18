package com.payslipmax.pdfparser.ui

import androidx.lifecycle.viewModelScope
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.insights.WealthOptimizationEngine
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.ImportStrings
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> {
            val mb = bytes.toDouble() / (1024 * 1024)
            val rounded = (mb * 10).toLong() / 10.0
            "$rounded MB"
        }
    }
}

fun isValidPdfBytes(bytes: ByteArray): Boolean {
    if (bytes.size < 4) return false
    return bytes[0] == 0x25.toByte() && // %
        bytes[1] == 0x50.toByte() && // P
        bytes[2] == 0x44.toByte() && // D
        bytes[3] == 0x46.toByte() // F
}

private fun isPasswordError(message: String): Boolean {
    val lower = message.lowercase()
    return lower.contains("password") ||
        lower.contains("encrypt") ||
        message.contains("InvalidPasswordException")
}

fun PayslipViewModel.onFilePicked(
    bytes: ByteArray,
    filename: String,
) {
    if (!isValidPdfBytes(bytes)) {
        _uiState.update { it.copy(importUiState = ImportUiState.Failure(ImportStrings.errInvalidPdf)) }
        return
    }
    pendingImportPdfBytes = bytes
    pendingImportFilename = filename
    _uiState.update { it.copy(importUiState = ImportUiState.InspectingFile(filename)) }

    viewModelScope.launch {
        val result = repository.importPayslip(bytes, "", filename)
        if (result.isSuccess) {
            handleSuccessfulImport(result.getOrThrow())
        } else {
            val rawMessage = result.exceptionOrNull()?.message.orEmpty()
            if (isPasswordError(rawMessage)) {
                _uiState.update {
                    it.copy(
                        importUiState =
                            ImportUiState.PasswordRequired(
                                fileName = filename,
                                formattedFileSize = formatFileSize(bytes.size.toLong()),
                            ),
                    )
                }
            } else {
                _uiState.update { it.copy(importUiState = ImportUiState.Failure(resolveFriendlyError(rawMessage))) }
            }
        }
    }
}

fun PayslipViewModel.onSubmitImportPassword() {
    val currentState = _uiState.value.importUiState as? ImportUiState.PasswordRequired ?: return
    val password = currentState.passwordInput
    if (password.isBlank()) return

    val bytes = pendingImportPdfBytes ?: return
    val filename = pendingImportFilename ?: "payslip.pdf"

    _uiState.update { it.copy(importUiState = currentState.copy(isDecrypting = true, errorMessage = null)) }

    viewModelScope.launch {
        val result = repository.importPayslip(bytes, password, filename)
        if (result.isSuccess) {
            handleSuccessfulImport(result.getOrThrow())
        } else {
            val rawMessage = result.exceptionOrNull()?.message.orEmpty()
            val errorMessage =
                if (isPasswordError(rawMessage)) {
                    ImportStrings.errIncorrectPassword
                } else {
                    resolveFriendlyError(rawMessage)
                }
            _uiState.update {
                (it.importUiState as? ImportUiState.PasswordRequired)?.let { req ->
                    it.copy(importUiState = req.copy(isDecrypting = false, errorMessage = errorMessage))
                } ?: it
            }
        }
    }
}

fun PayslipViewModel.onImportPasswordChanged(password: String) {
    _uiState.update { state ->
        (state.importUiState as? ImportUiState.PasswordRequired)?.let { req ->
            state.copy(importUiState = req.copy(passwordInput = password, errorMessage = null))
        } ?: state
    }
}

fun PayslipViewModel.onToggleImportPasswordVisibility() {
    _uiState.update { state ->
        (state.importUiState as? ImportUiState.PasswordRequired)?.let { req ->
            state.copy(importUiState = req.copy(isPasswordVisible = !req.isPasswordVisible))
        } ?: state
    }
}

fun PayslipViewModel.startImport() {
    pendingImportPdfBytes = null
    pendingImportFilename = null
    _uiState.update { it.copy(importUiState = ImportUiState.Idle, importError = null, importSuccess = false) }
}

fun PayslipViewModel.onDismissImport() = startImport()

private suspend fun PayslipViewModel.handleSuccessfulImport(parsed: ParsedPayslip) {
    pendingImportPdfBytes = null
    pendingImportFilename = null
    financialIntelligenceRepository?.processPayslipAndRunAnalysis(parsed)
    _uiState.update { state ->
        val isNew = state.payslips.none { it.dateStr == parsed.dateStr }
        lastImportWasNewPayslip = isNew
        val updated = if (isNew) state.payslips + parsed else state.payslips
        state.copy(
            payslips = updated,
            selectedPayslip = parsed,
            taxOptimizationResult = WealthOptimizationEngine.analyzeLedger(updated, parsed),
            importSuccess = true,
            importUiState = ImportUiState.Success(parsed),
        )
    }
}

/**
 * Called from the UI once the import-success state has actually rendered (not synchronously from
 * the import handler), so the native review prompt never stacks on the import dialog's own
 * transition. Both gates run before touching [RatingPromptManager.onCleanParseSuccess] so neither a
 * duplicate re-import nor a dirty parse ever counts toward the clean-success tally.
 */
internal fun PayslipViewModel.maybePromptForRating(parsed: ParsedPayslip) {
    if (!lastImportWasNewPayslip || parsed.needsReview) return
    if (ratingPromptManager.onCleanParseSuccess()) {
        reviewRequester()
    }
}

private fun resolveFriendlyError(rawMessage: String): String {
    return when {
        rawMessage.contains("UNRECOGNIZED_GRAMMAR") || rawMessage.contains("PdfPreFlightValidationFailed") ->
            AppStrings.errorUnrecognizedPdf
        rawMessage.contains("NO_TEXT_TOKENS") ->
            AppStrings.errorZeroTokensDesc
        rawMessage.isNotBlank() && !rawMessage.contains("Exception") && !rawMessage.contains(":") ->
            rawMessage
        else ->
            AppStrings.errorUnrecognizedPdf
    }
}
