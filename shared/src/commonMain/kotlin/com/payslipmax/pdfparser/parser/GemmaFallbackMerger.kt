package com.payslipmax.pdfparser.parser

import com.payslipmax.pdfparser.domain.ConfidenceThresholds
import com.payslipmax.pdfparser.domain.FieldSource

internal fun applyGemmaFallback(
    solved: SolvedTable,
    extractor: GemmaFallbackExtractor,
): SolvedTable {
    return try {
        val extractions =
            kotlinx.coroutines.runBlocking {
                extractor.extractFallback(solved.rawEarnings, solved.rawDeductions)
            }
        if (extractions.earnings.isEmpty() && extractions.deductions.isEmpty()) return solved

        val mergedEarnings = solved.earningsMap.toMutableMap()
        val mergedDeductions = solved.deductionsMap.toMutableMap()
        val mergedSource = solved.fieldSource.toMutableMap()
        val mergedConfidence = solved.fieldConfidence.toMutableMap()
        extractions.earnings.forEach { (k, v) ->
            mergedEarnings[k] = (mergedEarnings[k] ?: 0.0) + v
            mergedSource[k] = FieldSource.GEMMA_FALLBACK
            mergedConfidence[k] = ConfidenceThresholds.GEMMA_FALLBACK_CONFIDENCE
        }
        extractions.deductions.forEach { (k, v) ->
            mergedDeductions[k] = (mergedDeductions[k] ?: 0.0) + v
            mergedSource[k] = FieldSource.GEMMA_FALLBACK
            mergedConfidence[k] = ConfidenceThresholds.GEMMA_FALLBACK_CONFIDENCE
        }

        // Gemma resolves a whole side (earnings/deductions) at once from the complete raw map for that
        // side — its response has no back-reference to which raw label produced which key, so per-key
        // removal isn't possible. A side it returned nothing for is left untouched.
        val remainingRawEarnings = if (extractions.earnings.isNotEmpty()) emptyMap() else solved.rawEarnings
        val remainingRawDeductions = if (extractions.deductions.isNotEmpty()) emptyMap() else solved.rawDeductions

        solved.copy(
            earningsMap = mergedEarnings,
            deductionsMap = mergedDeductions,
            fieldSource = mergedSource,
            fieldConfidence = mergedConfidence,
            rawEarnings = remainingRawEarnings,
            rawDeductions = remainingRawDeductions,
        )
    } catch (e: Exception) {
        solved
    }
}
