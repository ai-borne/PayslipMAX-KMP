package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.domain.ParsedPayslip

data class PayslipUiState(
    val payslips: List<ParsedPayslip> = emptyList(),
    val selectedPayslip: ParsedPayslip? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val importError: String? = null,
    val importSuccess: Boolean = false,
    val isPremiumEnabled: Boolean = false,
    val aiInsights: String? = null,
    val isAiLoading: Boolean = false,
    val aiError: String? = null,
    val appTheme: String = "system",
    val isLockEnabled: Boolean = false,
    val appPinHash: String = "",
    val profileName: String = "",
    val profileCdaNumber: String = "",
    val profilePanNumber: String = "",
    val isAppLocked: Boolean = false,
    val useLocalAi: Boolean = false,
    val isGemmaSupported: Boolean = true,
    val gemmaSupportReason: String? = null,
    val isDownloadingModel: Boolean = false,
    val modelDownloadProgress: Float = 0f,
    val modelDownloadError: String? = null,
    val modelDownloadNotice: String? = null,
)
