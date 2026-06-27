package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.parser.corpus.CorpusExpected
import com.ssbmax.pdfparser.parser.corpus.CorpusFixtures
import com.ssbmax.pdfparser.parser.corpus.CorpusInput
import com.ssbmax.pdfparser.parser.corpus.CorpusScrubber
import com.ssbmax.pdfparser.parser.corpus.CorpusTokens
import com.ssbmax.pdfparser.parser.corpus.StandardizedGroundTruth
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import org.junit.Test
import java.io.File

/**
 * Opt-in developer utility that regenerates the committed regression corpus from real PDFs.
 * It is NOT a behavioral test — it is skipped entirely unless the local-corpus path is supplied.
 *
 * Usage:
 * ```
 * ./gradlew :shared:testDebugUnitTest --tests "*CorpusCaptureTest" \
 *   -Dpayslip.localCorpus="/Users/sunil/Desktop/Pay Slip Elements" \
 *   -Dpayslip.localCorpus.json="/Users/sunil/Downloads/PDFParser/payslips_data_standardized.json"
 * ```
 *
 * Optional properties: `.out` (output dir, default the committed resources dir),
 * `.password` (default 535d04), `.minYear` (default 2022).
 */
class CorpusCaptureTest {
    @Test
    fun captureCorpus() {
        val baseDirPath = System.getProperty("payslip.localCorpus")
        if (baseDirPath.isNullOrBlank()) {
            println("[CorpusCapture] -Dpayslip.localCorpus not set; skipping capture (this is expected in CI).")
            return
        }
        val baseDir = File(baseDirPath)
        require(baseDir.isDirectory) { "payslip.localCorpus is not a directory: $baseDirPath" }

        val password = System.getProperty("payslip.localCorpus.password") ?: "535d04"
        val minYear = System.getProperty("payslip.localCorpus.minYear")?.toIntOrNull() ?: 2022
        val outDir = File(System.getProperty("payslip.localCorpus.out") ?: "src/androidUnitTest/resources/corpus")
        outDir.mkdirs()

        val groundTruth =
            System.getProperty("payslip.localCorpus.json")
                ?.let { File(it) }
                ?.takeIf { it.exists() }
                ?.let { StandardizedGroundTruth(it.readText()) }
        println("[CorpusCapture] groundTruth loaded: ${groundTruth != null}; output dir: ${outDir.absolutePath}")

        val parser = PlatformPdfParser()
        val capturedIds = sortedSetOf<String>()
        val noTruth = mutableListOf<String>()
        val scrubChangedNumbers = mutableListOf<String>()
        val mismatches = mutableListOf<String>()

        baseDir.listFiles { f -> f.isDirectory && f.name.toIntOrNull()?.let { it >= minYear } == true }
            ?.sortedBy { it.name }
            ?.forEach { yearDir ->
                val year = yearDir.name.toInt()
                yearDir.listFiles { _, name -> name.endsWith(".pdf", ignoreCase = true) }
                    ?.sortedBy { it.name }
                    ?.forEach { pdf ->
                        captureOne(parser, pdf, year, password, groundTruth, outDir, noTruth, scrubChangedNumbers, mismatches)
                        capturedIds += CorpusFixtures.idFor(pdf.name)
                    }
            }

        File(outDir, "index.json").writeText(
            CorpusFixtures.json.encodeToString(
                ListSerializer(String.serializer()),
                capturedIds.toList(),
            ),
        )

        println("\n[CorpusCapture] ===== Summary =====")
        println("[CorpusCapture] Payslips captured: ${capturedIds.size}")
        println("[CorpusCapture] Without ground-truth JSON (expected=current behavior): ${noTruth.size} -> $noTruth")
        println("[CorpusCapture] WARNING scrub altered numbers: ${scrubChangedNumbers.size} -> $scrubChangedNumbers")
        println("[CorpusCapture] Quarantine candidates (parser != ground truth): ${mismatches.size}")
        mismatches.forEach { println("[CorpusCapture]   $it") }
    }

    private fun captureOne(
        parser: PlatformPdfParser,
        pdf: File,
        year: Int,
        password: String,
        groundTruth: StandardizedGroundTruth?,
        outDir: File,
        noTruth: MutableList<String>,
        scrubChangedNumbers: MutableList<String>,
        mismatches: MutableList<String>,
    ) {
        val id = CorpusFixtures.idFor(pdf.name)
        val texts =
            parser.decryptAndExtractTexts(pdf.readBytes(), password).getOrElse {
                mismatches += "$id: EXTRACTION FAILED: ${it.message}"
                return
            }
        val monthNum = pdf.name.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
        val scrubbed = CorpusScrubber.scrub(texts, monthNum, year)

        val parsedReal = parseTexts(texts, pdf.name)
        val parsedScrubbed = parseTexts(scrubbed, pdf.name)
        if (parsedReal != null && parsedScrubbed != null && parsedReal.earnings != parsedScrubbed.earnings) {
            scrubChangedNumbers += id
        }
        val parsed =
            parsedScrubbed ?: run {
                mismatches += "$id: PARSE FAILED on scrubbed text"
                return
            }

        val input =
            CorpusInput(
                id = id,
                filename = pdf.name,
                year = year,
                monthNum = parsed.monthNum,
                leftColumnText = scrubbed.leftColumnText,
                middleColumnText = scrubbed.middleColumnText,
                fullText = scrubbed.fullText,
                taxPageText = scrubbed.taxPageText,
                dsopPageText = scrubbed.dsopPageText,
            )

        val truthFound = groundTruth?.hasFile(pdf.name) == true
        if (!truthFound) noTruth += id
        val expected =
            CorpusExpected(
                id = id,
                filename = pdf.name,
                year = year,
                monthNum = parsed.monthNum,
                officer = parsed.officer,
                summary = groundTruth?.summary(pdf.name) ?: parsed.summary,
                earnings = groundTruth?.earnings(pdf.name) ?: parsed.earnings,
                deductions = groundTruth?.deductions(pdf.name) ?: parsed.deductions,
                taxAndSavings = groundTruth?.taxAndSavings(pdf.name) ?: parsed.taxAndSavings,
            )

        File(outDir, "$id.input.json").writeText(CorpusFixtures.json.encodeToString(CorpusInput.serializer(), input))
        File(outDir, "$id.expected.json").writeText(CorpusFixtures.json.encodeToString(CorpusExpected.serializer(), expected))
        captureTokens(parser, pdf, id, year, password, texts.fullText, parsed.monthNum, outDir, mismatches)

        val diff = CorpusFixtures.diff(expected, parsed)
        if (diff.isNotEmpty()) mismatches += "$id: ${diff.size} field(s) -> ${diff.take(4)}"
    }

    /**
     * Captures the Phase 2 token IR via the new [PlatformPdfParser.extractTokens] path, scrubs PII
     * from each token's text (numeric amounts are untouched), and commits it as `<id>.tokens.json`.
     */
    private fun captureTokens(
        parser: PlatformPdfParser,
        pdf: File,
        id: String,
        year: Int,
        password: String,
        fullText: String,
        monthNum: Int,
        outDir: File,
        mismatches: MutableList<String>,
    ) {
        val tokenized =
            parser.extractTokens(pdf.readBytes(), password).getOrElse {
                mismatches += "$id: TOKEN EXTRACTION FAILED: ${it.message}"
                return
            }
        val ids = CorpusScrubber.identifiersFrom(fullText, monthNum, year)
        val tokens =
            CorpusTokens(
                id = id,
                filename = pdf.name,
                year = year,
                tableTokens = CorpusScrubber.scrubTokens(tokenized.tableTokens, ids),
                taxTokens = CorpusScrubber.scrubTokens(tokenized.taxTokens, ids),
                dsopTokens = CorpusScrubber.scrubTokens(tokenized.dsopTokens, ids),
            )
        File(outDir, "$id.tokens.json").writeText(CorpusFixtures.json.encodeToString(CorpusTokens.serializer(), tokens))
    }

    private fun parseTexts(
        texts: ExtractedPayslipTexts,
        filename: String,
    ) = PayslipTextParser.parse(
        leftColumnText = texts.leftColumnText,
        middleColumnText = texts.middleColumnText,
        fullText = texts.fullText,
        taxPageText = texts.taxPageText,
        dsopPageText = texts.dsopPageText,
        filename = filename,
    ).getOrNull()
}
