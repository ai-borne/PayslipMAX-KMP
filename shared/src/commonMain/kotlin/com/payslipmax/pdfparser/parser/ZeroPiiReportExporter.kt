package com.payslipmax.pdfparser.parser

import com.payslipmax.pdfparser.parser.detection.GrammarDiagnosticReport
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ScrubbedDiagnosticExport(
    val selectedFamily: String,
    val isKnownGrammar: Boolean,
    val officerName: String = "Officer",
    val cdaAccountNo: String = "16/000/000000X",
    val pan: String = "AR*****90G",
    val email: String = "user@example.com",
    val validationStatus: String,
    val selectionReason: String,
    val matchedFingerprints: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
)

object ZeroPiiReportExporter {
    private val json =
        Json {
            prettyPrint = true
            encodeDefaults = true
        }
    private val accountRegex = Regex("""\b\d{2,}/\d{2,}/\d{5,}[A-Z]?\b""")
    private val panRegex = Regex("""\b[A-Z]{5}\d{4}[A-Z]\b""")
    private val emailRegex = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")

    fun exportJson(report: GrammarDiagnosticReport): String {
        val scrubbed =
            ScrubbedDiagnosticExport(
                selectedFamily = report.selectedFamily.name,
                isKnownGrammar = report.isKnownGrammar,
                validationStatus = report.validationStatus,
                selectionReason = report.selectionReason,
                matchedFingerprints = report.matchedFingerprints,
                warnings = report.warnings,
            )
        return json.encodeToString(scrubbed)
    }

    fun scrubText(rawText: String): String {
        var out = rawText
        out = emailRegex.replace(out, "user@example.com")
        out = accountRegex.replace(out, "16/000/000000X")
        out = panRegex.replace(out, "AR*****90G")
        return out
    }
}
