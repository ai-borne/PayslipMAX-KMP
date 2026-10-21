package com.payslipmax.pdfparser.parser

import com.payslipmax.pdfparser.domain.LeaveCasCode
import com.payslipmax.pdfparser.domain.LeaveEntry
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.insights.LeaveBankCalculator
import com.payslipmax.pdfparser.parser.corpus.CorpusFixtures
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Drives the production [GrammarAwareParser] over every committed fixture, the same way TokenParseCorpusRegressionTest does,
 * and pins the leave table each December statement prints. The 135 fixtures without the page must come back null: a false
 * match would put a made-up leave balance into the retirement calculator.
 */
class LeaveAccumulationCorpusTest {
    private data class Expected(val rows: Int, val balance: Int, val ltcDays: Int, val lastRow: LeaveEntry)

    private val expected =
        mapOf(
            "12_dec_2022" to Expected(15, 222, 30, LeaveEntry("2021-12-31", LeaveCasCode.ACCRUED, 30, 222, "0156/2022", "2022-03-17")),
            "12_dec_2023" to Expected(13, 252, 0, LeaveEntry("2022-12-31", LeaveCasCode.ACCRUED, 30, 252, "72", "2023-03-14")),
            "12_dec_2024" to Expected(16, 282, 20, LeaveEntry("2023-12-31", LeaveCasCode.ACCRUED, 30, 282, "0058", "2024-03-01")),
            "12_dec_2025" to Expected(17, 301, 20, LeaveEntry("2024-12-31", LeaveCasCode.ACCRUED, 19, 301, "381", "2025-05-02")),
        )

    private fun parse(id: String): ParsedPayslip {
        val tokens = CorpusFixtures.loadTokens(id)
        val input = CorpusFixtures.loadInput(id)
        val tokenized = TokenizedPayslip(tokens.tableTokens, tokens.taxTokens, tokens.dsopTokens, input.fullText)
        val result = runBlocking { GrammarAwareParser.parse(tokenized, input.filename) }
        return assertNotNull(result.getOrNull(), "$id failed to parse: ${result.exceptionOrNull()?.message}")
    }

    @Test
    fun decemberStatementsYieldThePrintedLeaveTable() {
        for ((id, want) in expected) {
            val payslip = parse(id)
            val leave = assertNotNull(payslip.leaveAccumulation, "$id: leave table not found")
            val summary = assertNotNull(LeaveBankCalculator.summarize(listOf(payslip)), "$id: no leave summary")

            assertEquals(want.rows, leave.entries.size, "$id: row count")
            assertEquals(want.lastRow, leave.entries.last(), "$id: last row")
            assertEquals(want.balance, summary.balanceDays, "$id: balance")
            assertEquals(want.ltcDays, summary.ltcDaysUsed, "$id: LTC days")
        }
    }

    @Test
    fun statementsWithoutTheLeavePageYieldNoTable() {
        val ids = CorpusFixtures.loadIndex()
        assertTrue(ids.size > expected.size, "Corpus index is missing fixtures")

        val falseMatches = (ids - expected.keys).filter { parse(it).leaveAccumulation != null }

        assertTrue(falseMatches.isEmpty(), "Leave table found where the page is absent: $falseMatches")
    }
}
