package com.payslipmax.pdfparser.parser

import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.IOException

open class LayoutScanner : PDFTextStripper() {
    var bpayY: Float = 250f
    var tableHeaderY: Float = 0f
    var dsopX: Float = 150f
    var totalCreditY: Float = 700f
    var detailsX: Float = 0f

    init {
        sortByPosition = true
    }

    @Throws(IOException::class)
    override fun writeString(
        text: String?,
        textPositions: MutableList<TextPosition>?,
    ) {
        if (text == null || textPositions == null || textPositions.isEmpty()) return

        val lineText = text.trim()
        val lowerText = lineText.lowercase()

        // Locate Table Header / Earnings Y coordinate
        if (tableHeaderY == 0f && (
                lowerText.contains("earnings") || lowerText.contains("aaya") ||
                    lowerText.contains("bpay") || lowerText.contains("basic pay")
            )
        ) {
            tableHeaderY = textPositions.first().yDirAdj - 5f
        }

        // Locate BPAY / Basic Pay Y coordinate — first occurrence wins
        if (bpayY == 250f && (lowerText.contains("bpay") || lowerText.contains("basic pay"))) {
            bpayY = textPositions.first().yDirAdj
        }

        // Locate DSOP / AGIF / ITAX X coordinate
        if (lowerText.contains("dsop") || lowerText.contains("agif") || lowerText.contains("itax")) {
            val y = textPositions.first().yDirAdj
            if (y in (bpayY - 10f)..(totalCreditY + 10f)) {
                val idx =
                    lowerText.indexOf("dsop").takeIf { it >= 0 }
                        ?: lowerText.indexOf("agif").takeIf { it >= 0 }
                        ?: lowerText.indexOf("itax").takeIf { it >= 0 }
                        ?: 0
                if (idx < textPositions.size) {
                    val charX = textPositions[idx].xDirAdj
                    if (dsopX == 150f || charX < dsopX) {
                        dsopX = charX
                    }
                }
            }
        }

        // Locate DETAILS OF TRANSACTIONS / DETAILS OF DO2s / DETAILS OF / laona dona / loona dona
        if (lowerText.contains("details of transactions") || lowerText.contains("details of do2s") ||
            lowerText.contains("details of") || lowerText.contains("laona dona") || lowerText.contains("loona dona")
        ) {
            val idx =
                lowerText.indexOf("details of transactions").takeIf { it >= 0 }
                    ?: lowerText.indexOf("details of do2s").takeIf { it >= 0 }
                    ?: lowerText.indexOf("details of").takeIf { it >= 0 }
                    ?: lowerText.indexOf("laona dona").takeIf { it >= 0 }
                    ?: lowerText.indexOf("loona dona").takeIf { it >= 0 }
                    ?: 0
            if (idx < textPositions.size) {
                val charX = textPositions[idx].xDirAdj
                if (detailsX == 0f || charX < detailsX) {
                    detailsX = charX
                }
            }
        }

        // Locate Remittance / Closing Balance Y coordinate to ensure table extraction stops before summary footer
        if (lowerText.contains("remittance") || lowerText.contains("closing balance") ||
            lowerText.contains("cl. cr") || lowerText.contains("cl. dr")
        ) {
            val y = textPositions.first().yDirAdj
            if (y < totalCreditY) {
                totalCreditY = y
            }
        }

        // Locate Total Credit / Gross Pay / Total Debit Y coordinate
        if (lowerText.contains("total credit") || lowerText.contains("gross pay") ||
            lowerText.contains("total debit") || lowerText.contains("total deductions")
        ) {
            val y = textPositions.first().yDirAdj
            if (y < totalCreditY) {
                totalCreditY = y
            }
        }
    }
}
