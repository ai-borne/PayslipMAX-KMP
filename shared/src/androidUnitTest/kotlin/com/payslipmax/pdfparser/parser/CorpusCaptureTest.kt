package com.payslipmax.pdfparser.parser

import com.payslipmax.pdfparser.parser.corpus.CorpusExpected
import com.payslipmax.pdfparser.parser.corpus.CorpusFixtures
import com.payslipmax.pdfparser.parser.corpus.CorpusInput
import com.payslipmax.pdfparser.parser.corpus.CorpusScrubber
import com.payslipmax.pdfparser.parser.corpus.CorpusTokens
import com.payslipmax.pdfparser.parser.corpus.StandardizedGroundTruth
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
 *   -Dpayslip.localCorpus="/Users/test/Desktop/Pay Slip Elements" \
 *   -Dpayslip.localCorpus.json="/Users/test/Downloads/PDFParser/payslips_data_standardized.json"
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

        val tokenizedReal =
            parser.extractTokens(pdf.readBytes(), password).getOrElse {
                mismatches += "$id: TOKEN EXTRACTION FAILED: ${it.message}"
                return
            }
        val ids = CorpusScrubber.identifiersFrom(tokenizedReal.fullText, monthNum, year)
        val tokenizedScrubbed =
            TokenizedPayslip(
                tableTokens = CorpusScrubber.scrubTokens(tokenizedReal.tableTokens, ids),
                taxTokens = CorpusScrubber.scrubTokens(tokenizedReal.taxTokens, ids),
                dsopTokens = CorpusScrubber.scrubTokens(tokenizedReal.dsopTokens, ids),
                fullText = scrubbed.fullText,
            )

        val parsedReal = GrammarAwareParser.parse(tokenizedReal, pdf.name).getOrNull()
        val parsedScrubbed = GrammarAwareParser.parse(tokenizedScrubbed, pdf.name).getOrNull()

        if (parsedReal != null && parsedScrubbed != null && parsedReal.earnings != parsedScrubbed.earnings) {
            scrubChangedNumbers += id
        }
        val parsed =
            parsedScrubbed ?: run {
                mismatches += "$id: PARSE FAILED on scrubbed tokens"
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

        val tokens =
            CorpusTokens(
                id = id,
                filename = pdf.name,
                year = year,
                tableTokens = tokenizedScrubbed.tableTokens,
                taxTokens = tokenizedScrubbed.taxTokens,
                dsopTokens = tokenizedScrubbed.dsopTokens,
            )
        File(outDir, "$id.tokens.json").writeText(CorpusFixtures.json.encodeToString(CorpusTokens.serializer(), tokens))

        val diff = CorpusFixtures.diff(expected, parsed)
        if (diff.isNotEmpty()) mismatches += "$id: ${diff.size} field(s) -> ${diff.take(4)}"
    }
}
