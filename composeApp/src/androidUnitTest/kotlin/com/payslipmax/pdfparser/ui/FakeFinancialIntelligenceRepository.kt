package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.insights.EngineResult
import com.payslipmax.pdfparser.repository.FinancialIntelligenceRepository
import com.payslipmax.pdfparser.testing.FakePayslipDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Test double for [FinancialIntelligenceRepository].
 * All cloud/Gemini dependencies removed — repository is now fully offline.
 * Runs on [Dispatchers.Unconfined], not the production Default: a hop to a worker thread would come back to the
 * ViewModel's Main after a short test's resetMain() (see FakeFinancialIntelligenceRepositoryTest).
 */
class FakeFinancialIntelligenceRepository(
    val fakeDao: FakePayslipDao = FakePayslipDao(),
    dispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
) : FinancialIntelligenceRepository(payslipDao = fakeDao, dispatcher = dispatcher) {
    override suspend fun processPayslipAndRunAnalysis(payslip: ParsedPayslip): EngineResult =
        EngineResult(
            healthScore = 80,
            anomalies = emptyList(),
            monthlySavingRate = 10.0,
            taxRatio = 4.0,
        )
}
