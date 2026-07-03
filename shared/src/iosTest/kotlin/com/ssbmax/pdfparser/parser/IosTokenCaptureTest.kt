@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.ssbmax.pdfparser.parser

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.create
import platform.Foundation.writeToFile
import kotlin.test.Test

/**
 * Opt-in developer utility: extracts the iOS PDFKit token IR from real PDFs and writes
 * `<id>.tokens.json` files using the same schema as the committed Android corpus fixtures.
 *
 * Purpose: produces the real-device token dumps committed at
 * `shared/src/androidUnitTest/resources/corpus_ios_tokens/` (one file per Android corpus id),
 * which [TokenParityDiffTest] asserts token-content parity against on every CI run. Re-run this
 * and recommit the output only when Tier-1 token extraction changes on either platform.
 *
 * Skips entirely when PAYSLIP_LOCAL_CORPUS is absent (CI-safe).
 *
 * Usage — set these env vars in the Xcode scheme (Product → Scheme → Edit Scheme → Test →
 * Arguments → Environment Variables) or export them before running via Gradle:
 *
 *   PAYSLIP_LOCAL_CORPUS      /path/to/Pay Slip Elements   (dir with <year>/<file>.pdf)
 *   PAYSLIP_LOCAL_CORPUS_TOKENS_OUT  /tmp/ios_corpus_tokens  (optional; output dir)
 *
 * Run:
 *   ./gradlew :shared:iosSimulatorArm64Test --tests "*IosTokenCaptureTest*"
 *
 * Then, for each id present in the committed Android corpus, scrub (already done inline here)
 * and copy into the repo:
 *   cp /tmp/ios_corpus_tokens/04_april_2022.tokens.json \
 *      shared/src/androidUnitTest/resources/corpus_ios_tokens/04_april_2022.tokens.json
 */
class IosTokenCaptureTest {
    @Test
    fun dumpIosTokens() {
        val basePath = platform.posix.getenv("PAYSLIP_LOCAL_CORPUS")?.toKString()
        if (basePath.isNullOrBlank()) {
            println("[IosTokenCapture] PAYSLIP_LOCAL_CORPUS not set; skipping (expected in CI).")
            return
        }
        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(basePath)) {
            println("[IosTokenCapture] Path does not exist: $basePath; skipping.")
            return
        }

        val outDir =
            platform.posix.getenv("PAYSLIP_LOCAL_CORPUS_TOKENS_OUT")?.toKString()
                ?.takeIf { it.isNotBlank() } ?: "/tmp/ios_corpus_tokens"
        fileManager.createDirectoryAtPath(outDir, withIntermediateDirectories = true, attributes = null, error = null)

        val password = "535d04"
        val parser = PlatformPdfParser()
        val years = listOf("2022", "2023", "2024", "2025", "2026")
        var total = 0
        var written = 0
        val errors = mutableListOf<String>()

        for (year in years) {
            val yearPath = "$basePath/$year"
            if (!fileManager.fileExistsAtPath(yearPath)) continue
            val contents = fileManager.contentsOfDirectoryAtPath(yearPath, null) as? List<*> ?: continue
            val pdfs =
                contents.mapNotNull { it as? String }
                    .filter { it.endsWith(".pdf", ignoreCase = true) }
                    .sorted()

            for (filename in pdfs) {
                total++
                val data = NSData.create(contentsOfFile = "$yearPath/$filename")
                if (data == null) {
                    errors += "$filename: could not read file"
                    continue
                }
                val tokenResult = parser.extractTokens(data.toByteArray(), password, filename)
                if (tokenResult.isFailure) {
                    errors += "$filename: extractTokens failed — ${tokenResult.exceptionOrNull()?.message}"
                    continue
                }
                val tokenized = tokenResult.getOrThrow()

                val id = idFor(filename)
                val yearInt = year.toInt()
                val monthNum = filename.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
                val nameWords = nameWordsFrom(tokenized.fullText, monthNum, yearInt)

                val json = buildTokenJson(id, filename, yearInt, tokenized, nameWords)
                val dest = "$outDir/$id.tokens.json"
                if (writeUtf8(json, dest)) {
                    written++
                    println("[IosTokenCapture] ✅ $filename → $id.tokens.json")
                } else {
                    errors += "$filename: failed to write $dest"
                }
            }
        }

        println("\n[IosTokenCapture] ===== Summary =====")
        println("[IosTokenCapture] PDFs processed: $total | Written: $written | Errors: ${errors.size}")
        errors.forEach { println("[IosTokenCapture] ❌ $it") }
        println("[IosTokenCapture] Token files written to: $outDir")
    }

    // ── Serialization ────────────────────────────────────────────────────────

    private val pretty =
        Json {
            prettyPrint = true
            encodeDefaults = true
        }
    private val tokenListSer = ListSerializer(PositionedToken.serializer())

    private fun buildTokenJson(
        id: String,
        filename: String,
        year: Int,
        tokenized: TokenizedPayslip,
        nameWords: List<String>,
    ): String {
        val obj: JsonObject =
            buildJsonObject {
                put("id", id)
                put("filename", filename)
                put("year", year)
                put("tableTokens", pretty.encodeToJsonElement(tokenListSer, scrubTokens(tokenized.tableTokens, nameWords)))
                put("taxTokens", pretty.encodeToJsonElement(tokenListSer, scrubTokens(tokenized.taxTokens, nameWords)))
                put("dsopTokens", pretty.encodeToJsonElement(tokenListSer, scrubTokens(tokenized.dsopTokens, nameWords)))
            }
        return pretty.encodeToString(JsonElement.serializer(), obj)
    }

    // ── PII scrubbing (mirrors CorpusScrubber — numeric amounts untouched) ──

    private fun nameWordsFrom(
        fullText: String,
        monthNum: Int,
        year: Int,
    ): List<String> {
        val cleaned = negateHindiTransliterations(cleanCommasAndWhitespace(fullText))
        return parseOfficer(cleaned, monthNum, year).name
            .split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.length >= 3 && it.all { c -> c.isLetter() } }
            .distinct()
    }

    private fun scrubTokens(
        tokens: List<PositionedToken>,
        nameWords: List<String>,
    ): List<PositionedToken> =
        tokens.map { it.copy(text = scrubWord(it.text, nameWords)) }

    private fun scrubWord(
        text: String,
        nameWords: List<String>,
    ): String {
        var s = emailRegex.replace(text, FAKE_EMAIL)
        s = accountRegex.replace(s, FAKE_ACCOUNT)
        s = accountSerialRegex.replace(s, "000000X")
        s = panMaskedRegex.replace(s, FAKE_PAN)
        s = panPlainRegex.replace(s, FAKE_PAN)
        for (word in nameWords) {
            s =
                Regex("(?<![A-Za-z])${Regex.escape(word)}(?![A-Za-z])", RegexOption.IGNORE_CASE)
                    .replace(s, FAKE_NAME_WORD)
        }
        return s
    }

    // ── iOS filesystem helpers ───────────────────────────────────────────────

    private fun idFor(filename: String): String =
        filename.removeSuffix(".pdf").removeSuffix(".PDF")
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')

    private fun writeUtf8(
        content: String,
        path: String,
    ): Boolean {
        val bytes = content.encodeToByteArray()
        return bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
                .writeToFile(path, atomically = true)
        }
    }

    private fun NSData.toByteArray(): ByteArray {
        val size = length.toInt()
        val out = ByteArray(size)
        if (size > 0) {
            out.usePinned { pinned ->
                platform.posix.memcpy(pinned.addressOf(0), bytes, length)
            }
        }
        return out
    }

    companion object {
        private const val FAKE_ACCOUNT = "16/000/000000X"
        private const val FAKE_PAN = "AR*****90G"
        private const val FAKE_EMAIL = "user@example.com"
        private const val FAKE_NAME_WORD = "Officer"
        private val accountRegex = Regex("""\b\d{2,}/\d{2,}/\d{5,}[A-Z]?\b""")
        private val panMaskedRegex = Regex("""\b[A-Z]{2}[*\d]{7}[A-Z]\b""")
        private val panPlainRegex = Regex("""\b[A-Z]{5}\d{4}[A-Z]\b""")
        private val emailRegex = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")
        private val accountSerialRegex = Regex("""\b\d{5,}[A-Z]\b""")
    }
}
