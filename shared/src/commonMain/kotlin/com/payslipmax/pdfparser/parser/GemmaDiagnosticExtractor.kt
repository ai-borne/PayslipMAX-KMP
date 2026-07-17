package com.payslipmax.pdfparser.parser

import com.payslipmax.pdfparser.domain.DiagnosticSuggestion
import com.payslipmax.pdfparser.insights.gemma.DeviceCapabilityManager
import com.payslipmax.pdfparser.insights.gemma.GemmaEngine
import com.payslipmax.pdfparser.insights.gemma.MockGemmaEngine

/**
 * Orchestrates the Tier 6 diagnostic pass, run only after Tier 7 [SchemaValidator] fails
 * arithmetic reconciliation. Diagnosis only — the returned [DiagnosticSuggestion] is never
 * merged into `earnings`/`deductions`.
 */
class GemmaDiagnosticExtractor(
    private val gemmaEngine: GemmaEngine? = null,
    private val mockEngine: MockGemmaEngine? = null,
    private val capabilityManager: DeviceCapabilityManager = DeviceCapabilityManager(),
) {
    suspend fun suggestDiagnostic(
        earnings: Map<String, Double>,
        deductions: Map<String, Double>,
        grossPay: Double,
        totalDeductions: Double,
        netRemittance: Double,
        residual: Double,
    ): DiagnosticSuggestion? {
        val supportStatus = capabilityManager.checkGemmaSupport()
        if (!supportStatus.isSupported) {
            return null
        }

        val prompt =
            GemmaDiagnosticPromptBuilder.buildPrompt(
                earnings = earnings,
                deductions = deductions,
                grossPay = grossPay,
                totalDeductions = totalDeductions,
                netRemittance = netRemittance,
                residual = residual,
            )
        val responseText = generateResponse(prompt).getOrNull() ?: return null

        val allowedFieldKeys = PayslipPatternConfig.creditKeysMapping.values.toSet() + PayslipPatternConfig.debitKeysMapping.values.toSet()
        return GemmaDiagnosticResponseParser.parse(responseText, allowedFieldKeys)
    }

    private suspend fun generateResponse(prompt: String): Result<String> {
        return GemmaEngineInvoker.generateResponse(prompt, gemmaEngine, mockEngine)
    }
}
