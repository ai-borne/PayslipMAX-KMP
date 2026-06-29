package com.ssbmax.pdfparser.parser.debug

import com.ssbmax.pdfparser.parser.TableSide
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ParserDiffEngineTest {
    private fun createBaseArtifact(
        platform: String,
        needsReview: Boolean = false,
    ): ParserDebugArtifact {
        val tokens =
            listOf(
                PositionedTokenDump("BPAY", 10f, 20f, 40f, 10f, "table", 10f, false),
                PositionedTokenDump("140500", 100f, 20f, 50f, 10f, "table", 10f, false),
            )
        val s1 = Stage1TokenDump(tokens, emptyList(), emptyList())
        val s2 =
            Stage2RowDump(
                listOf(
                    RowDump(
                        0,
                        20f,
                        listOf(
                            CellDump("BPAY", 10f, 50f, 20f, listOf("BPAY")),
                            CellDump("140500", 100f, 150f, 20f, listOf("140500")),
                        ),
                    ),
                ),
            )
        val s3 =
            Stage3ColumnDump(
                creditBand = 30f,
                debitBand = 200f,
                acceptRadius = 50f,
                assignedEntries =
                    listOf(
                        EntryAssignmentDump("BPAY", 140500.0, TableSide.CREDIT, 30f, 20f, true, "basicPay"),
                    ),
                droppedEntries = emptyList(),
            )
        val s4 =
            Stage4ClassificationDump(
                listOf(
                    FieldClassificationDump("basicPay", 140500.0, FieldStatus.RECOGNIZED, "Clean match", TableSide.CREDIT, "BPAY"),
                ),
            )
        val s5 =
            Stage5ReconciliationDump(
                grossPay = 140500.0,
                totalDeductions = 0.0,
                netRemittance = 140500.0,
                earningsSum = 140500.0,
                deductionsSum = 0.0,
                netResidual = 0.0,
                needsReview = needsReview,
                reviewReasons = if (needsReview) listOf("Low confidence") else emptyList(),
                fieldConfidence = mapOf("basicPay" to 1.0f),
            )
        return ParserDebugArtifact(platform, "test.pdf", s1, s2, s3, s4, s5)
    }

    @Test
    fun testIdenticalArtifactsProduceNoDivergence() {
        val android = createBaseArtifact("Android")
        val ios = createBaseArtifact("iOS")
        val report = ParserDiffEngine.compare(android, ios)
        assertNull(report.firstDivergence, "Identical artifacts should have no first divergence")
        assertTrue(report.allDivergences.isEmpty(), "All divergences list should be empty")
    }

    @Test
    fun testReconciliationNeedsReviewMismatchDetected() {
        val android = createBaseArtifact("Android", needsReview = false)
        val ios = createBaseArtifact("iOS", needsReview = true)
        val report = ParserDiffEngine.compare(android, ios)
        assertNotNull(report.firstDivergence, "Divergence should be detected when needsReview differs")
        assertEquals("Stage 5: Reconciliation", report.firstDivergence?.stage)
    }

    @Test
    fun testColumnBandShiftDetected() {
        val android = createBaseArtifact("Android")
        val iosBase = createBaseArtifact("iOS")
        val ios = iosBase.copy(stage3 = iosBase.stage3.copy(creditBand = 45f))
        val report = ParserDiffEngine.compare(android, ios)
        assertNotNull(report.firstDivergence)
        assertEquals("Stage 3: Column Bands", report.firstDivergence?.stage)
    }
}
