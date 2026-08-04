package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.insights.EngineResult
import com.payslipmax.pdfparser.repository.FinancialIntelligenceRepository
import com.payslipmax.pdfparser.testing.FakePayslipDao

/**
 * Test double for [FinancialIntelligenceRepository].
 * All cloud/Gemini dependencies removed — repository is now fully offline.
 */
class FakeFinancialIntelligenceRepository(
    val fakeDao: FakePayslipDao = FakePayslipDao(),
) : FinancialIntelligenceRepository(payslipDao = fakeDao) {
    override suspend fun processPayslipAndRunAnalysis(payslip: ParsedPayslip): EngineResult =
        EngineResult(
            healthScore = 80,
            anomalies = emptyList(),
            monthlySavingRate = 10.0,
            taxRatio = 4.0,
        )
}
