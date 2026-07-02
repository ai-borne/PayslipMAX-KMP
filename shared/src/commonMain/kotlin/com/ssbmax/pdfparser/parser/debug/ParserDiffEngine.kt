package com.ssbmax.pdfparser.parser.debug

import kotlin.math.abs

/** Point of divergence between Android and iOS execution. */
data class DivergencePoint(
    val stage: String,
    val description: String,
    val androidValue: String,
    val iosValue: String,
)

/** Summary report comparing Android and iOS debug artifacts. */
data class ParserDiffReport(
    val filename: String,
    val firstDivergence: DivergencePoint?,
    val allDivergences: List<DivergencePoint>,
    val tokenCountDiff: String,
    val tokenTextDiff: String,
    val boundingBoxDiff: String,
    val rowGroupingDiff: String,
    val columnAssignmentDiff: String,
    val classifiedFieldsDiff: String,
    val finalLedgerDiff: String,
)

/** Parser Diff Mode engine comparing Android and iOS debug dumps across 7 metrics. */
object ParserDiffEngine {
    fun compare(
        androidArtifact: ParserDebugArtifact,
        iosArtifact: ParserDebugArtifact,
    ): ParserDiffReport {
        val divergences = mutableListOf<DivergencePoint>()
        compareStage1(androidArtifact.stage1, iosArtifact.stage1, divergences)
        compareStage2(androidArtifact.stage2, iosArtifact.stage2, divergences)
        compareStage3(androidArtifact.stage3, iosArtifact.stage3, divergences)
        compareStage4(androidArtifact.stage4, iosArtifact.stage4, divergences)
        compareStage5(androidArtifact.stage5, iosArtifact.stage5, divergences)

        val aTokens = androidArtifact.stage1.tableTokens
        val iTokens = iosArtifact.stage1.tableTokens
        val aCol = androidArtifact.stage3
        val iCol = iosArtifact.stage3

        return ParserDiffReport(
            filename = androidArtifact.filename,
            firstDivergence = divergences.firstOrNull(),
            allDivergences = divergences,
            tokenCountDiff = "Android: ${aTokens.size}, iOS: ${iTokens.size}",
            tokenTextDiff = "Text mismatches in sequence: ${countTextMismatches(aTokens, iTokens)}",
            boundingBoxDiff = "BBox shifts (>1pt): ${countBboxShifts(aTokens, iTokens)}",
            rowGroupingDiff = "Android rows: ${androidArtifact.stage2.rows.size}, iOS rows: ${iosArtifact.stage2.rows.size}",
            columnAssignmentDiff = "Android: (${aCol.creditBand}, ${aCol.debitBand}) vs iOS: (${iCol.creditBand}, ${iCol.debitBand})",
            classifiedFieldsDiff = "Android fields: ${androidArtifact.stage4.fields.size}, iOS fields: ${iosArtifact.stage4.fields.size}",
            finalLedgerDiff = "Android review: ${androidArtifact.stage5.needsReview}, iOS review: ${iosArtifact.stage5.needsReview}",
        )
    }

    private fun compareStage1(
        aStage: Stage1TokenDump,
        iStage: Stage1TokenDump,
        out: MutableList<DivergencePoint>,
    ) {
        val aTokens = aStage.tableTokens
        val iTokens = iStage.tableTokens
        if (aTokens.size != iTokens.size) {
            out.add(DivergencePoint("Stage 1: Token Count", "Token count mismatch", "${aTokens.size}", "${iTokens.size}"))
        }
        val maxLen = maxOf(aTokens.size, iTokens.size)
        for (idx in 0 until maxLen) {
            val a = aTokens.getOrNull(idx)
            val i = iTokens.getOrNull(idx)
            if (a != null && i != null) {
                if (a.text != i.text && out.count { it.stage == "Stage 1: Token Text" } < 3) {
                    out.add(DivergencePoint("Stage 1: Token Text", "Text mismatch at $idx", a.text, i.text))
                }
                val dx = abs(a.x - i.x)
                val dy = abs(a.y - i.y)
                if ((dx > 1f || dy > 1f) && out.count { it.stage == "Stage 1: Bounding Box" } < 3) {
                    out.add(DivergencePoint("Stage 1: Bounding Box", "BBox shift for '${a.text}'", "x=${a.x}, y=${a.y}", "x=${i.x}, y=${i.y}"))
                }
            }
        }
    }

    private fun compareStage2(
        aStage: Stage2RowDump,
        iStage: Stage2RowDump,
        out: MutableList<DivergencePoint>,
    ) {
        if (aStage.rows.size != iStage.rows.size) {
            out.add(DivergencePoint("Stage 2: Row Groupings", "Row count mismatch", "${aStage.rows.size} rows", "${iStage.rows.size} rows"))
        }
    }

    private fun compareStage3(
        aStage: Stage3ColumnDump,
        iStage: Stage3ColumnDump,
        out: MutableList<DivergencePoint>,
    ) {
        if (aStage.creditBand != iStage.creditBand || aStage.debitBand != iStage.debitBand) {
            out.add(
                DivergencePoint(
                    stage = "Stage 3: Column Bands",
                    description = "Credit/debit band shift",
                    androidValue = "credit=${aStage.creditBand}, debit=${aStage.debitBand}",
                    iosValue = "credit=${iStage.creditBand}, debit=${iStage.debitBand}",
                ),
            )
        }
    }

    private fun compareStage4(
        aStage: Stage4ClassificationDump,
        iStage: Stage4ClassificationDump,
        out: MutableList<DivergencePoint>,
    ) {
        val aRec = aStage.fields.filter { it.status == FieldStatus.RECOGNIZED }.map { it.fieldKeyOrLabel }.toSet()
        val iRec = iStage.fields.filter { it.status == FieldStatus.RECOGNIZED }.map { it.fieldKeyOrLabel }.toSet()
        if (aRec != iRec) {
            out.add(DivergencePoint("Stage 4: Classified Fields", "Recognized field set mismatch", aRec.joinToString(","), iRec.joinToString(",")))
        }
    }

    private fun compareStage5(
        aStage: Stage5ReconciliationDump,
        iStage: Stage5ReconciliationDump,
        out: MutableList<DivergencePoint>,
    ) {
        if (aStage.needsReview != iStage.needsReview) {
            out.add(
                DivergencePoint(
                    stage = "Stage 5: Reconciliation",
                    description = "needsReview mismatch",
                    androidValue = "needsReview=${aStage.needsReview} (${aStage.reviewReasons.joinToString()})",
                    iosValue = "needsReview=${iStage.needsReview} (${iStage.reviewReasons.joinToString()})",
                ),
            )
        }
    }

    private fun countTextMismatches(
        a: List<PositionedTokenDump>,
        b: List<PositionedTokenDump>,
    ): Int {
        var count = 0
        for (i in 0 until minOf(a.size, b.size)) if (a[i].text != b[i].text) count++
        return count
    }

    private fun countBboxShifts(
        a: List<PositionedTokenDump>,
        b: List<PositionedTokenDump>,
    ): Int {
        var count = 0
        for (i in 0 until minOf(a.size, b.size)) {
            if (abs(a[i].x - b[i].x) > 1f || abs(a[i].y - b[i].y) > 1f) count++
        }
        return count
    }
}
