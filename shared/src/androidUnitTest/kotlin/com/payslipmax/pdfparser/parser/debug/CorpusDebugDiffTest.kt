package com.payslipmax.pdfparser.parser.debug

import com.payslipmax.pdfparser.parser.GrammarAwareParser
import com.payslipmax.pdfparser.parser.PositionedToken
import com.payslipmax.pdfparser.parser.TokenizedPayslip
import com.payslipmax.pdfparser.parser.corpus.CorpusFixtures
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CorpusDebugDiffTest {
    @Test
    fun runMarch2025DebugInstrumentationAndDiff() {
        val id = "03_mar_2025"
        val tokens = CorpusFixtures.loadTokens(id)
        val input = CorpusFixtures.loadInput(id)

        val androidTokenized =
            TokenizedPayslip(
                tableTokens = tokens.tableTokens,
                taxTokens = tokens.taxTokens,
                dsopTokens = tokens.dsopTokens,
                fullText = input.fullText,
            )

        val androidCollector = ParserDebugCollector(platform = "Android (PDFBox)", filename = input.filename)
        val androidResult = kotlinx.coroutines.runBlocking { GrammarAwareParser.parse(androidTokenized, input.filename, debugCollector = androidCollector) }
        assertTrue(androidResult.isSuccess, "Android parse should succeed for $id")
        val androidArtifact = androidCollector.buildArtifact()

        // Simulate PDFKit layout divergence on iOS (where bounding boxes or token breaks shift slightly on revised layout)
        val iosTokens = simulateIosPdfKitTokens(tokens.tableTokens)
        val iosTokenized =
            TokenizedPayslip(
                tableTokens = iosTokens,
                taxTokens = tokens.taxTokens,
                dsopTokens = tokens.dsopTokens,
                fullText = input.fullText,
            )

        val iosCollector = ParserDebugCollector(platform = "iOS (PDFKit)", filename = input.filename)
        val iosResult = kotlinx.coroutines.runBlocking { GrammarAwareParser.parse(iosTokenized, input.filename, debugCollector = iosCollector) }
        val iosArtifact = iosCollector.buildArtifact()

        val diffReport = ParserDiffEngine.compare(androidArtifact, iosArtifact)

        println("======================================================================")
        println("PARSER DIFF REPORT: ${diffReport.filename}")
        println("First Divergence: ${diffReport.firstDivergence?.stage} - ${diffReport.firstDivergence?.description}")
        println("  Android: ${diffReport.firstDivergence?.androidValue}")
        println("  iOS:     ${diffReport.firstDivergence?.iosValue}")
        println("----------------------------------------------------------------------")
        println("Metric Summaries:")
        println("  1. Token Count:        ${diffReport.tokenCountDiff}")
        println("  2. Token Text:         ${diffReport.tokenTextDiff}")
        println("  3. Bounding Boxes:     ${diffReport.boundingBoxDiff}")
        println("  4. Row Groupings:      ${diffReport.rowGroupingDiff}")
        println("  5. Column Assignments: ${diffReport.columnAssignmentDiff}")
        println("  6. Classified Fields:  ${diffReport.classifiedFieldsDiff}")
        println("  7. Final Ledger:       ${diffReport.finalLedgerDiff}")
        println("======================================================================")

        assertNotNull(androidArtifact.stage1)
        assertNotNull(androidArtifact.stage2)
        assertNotNull(androidArtifact.stage3)
        assertNotNull(androidArtifact.stage4)
        assertNotNull(androidArtifact.stage5)
    }

    @Test
    fun runFeb2025DebugInstrumentationAndDiff() {
        runDiffForId("02_feb_2025")
    }

    private fun runDiffForId(id: String) {
        val tokens = CorpusFixtures.loadTokens(id)
        val input = CorpusFixtures.loadInput(id)

        val androidTokenized =
            TokenizedPayslip(
                tableTokens = tokens.tableTokens,
                taxTokens = tokens.taxTokens,
                dsopTokens = tokens.dsopTokens,
                fullText = input.fullText,
            )

        val androidCollector = ParserDebugCollector(platform = "Android (PDFBox)", filename = input.filename)
        val androidResult = kotlinx.coroutines.runBlocking { GrammarAwareParser.parse(androidTokenized, input.filename, debugCollector = androidCollector) }
        assertTrue(androidResult.isSuccess, "Android parse should succeed for $id")
        val androidArtifact = androidCollector.buildArtifact()

        val iosTokens = simulateIosPdfKitTokens(tokens.tableTokens)
        val iosTokenized =
            TokenizedPayslip(
                tableTokens = iosTokens,
                taxTokens = tokens.taxTokens,
                dsopTokens = tokens.dsopTokens,
                fullText = input.fullText,
            )

        val iosCollector = ParserDebugCollector(platform = "iOS (PDFKit)", filename = input.filename)
        val iosResult = kotlinx.coroutines.runBlocking { GrammarAwareParser.parse(iosTokenized, input.filename, debugCollector = iosCollector) }
        val iosArtifact = iosCollector.buildArtifact()

        val diffReport = ParserDiffEngine.compare(androidArtifact, iosArtifact)

        println("======================================================================")
        println("PARSER DIFF REPORT: ${diffReport.filename}")
        println("First Divergence: ${diffReport.firstDivergence?.stage} - ${diffReport.firstDivergence?.description}")
        println("  Android: ${diffReport.firstDivergence?.androidValue}")
        println("  iOS:     ${diffReport.firstDivergence?.iosValue}")
        println("----------------------------------------------------------------------")
        println("Metric Summaries:")
        println("  1. Token Count:        ${diffReport.tokenCountDiff}")
        println("  2. Token Text:         ${diffReport.tokenTextDiff}")
        println("  3. Bounding Boxes:     ${diffReport.boundingBoxDiff}")
        println("  4. Row Groupings:      ${diffReport.rowGroupingDiff}")
        println("  5. Column Assignments: ${diffReport.columnAssignmentDiff}")
        println("  6. Classified Fields:  ${diffReport.classifiedFieldsDiff}")
        println("  7. Final Ledger:       ${diffReport.finalLedgerDiff}")
        println("======================================================================")

        assertNotNull(androidArtifact.stage1)
    }

    private fun simulateIosPdfKitTokens(tokens: List<PositionedToken>): List<PositionedToken> {
        return tokens.mapIndexed { idx, token ->
            if (idx % 10 == 0) {
                token.copy(y = token.y + 1.5f)
            } else {
                token
            }
        }
    }
}
