package com.payslipmax.pdfparser.parser.corpus

import com.payslipmax.pdfparser.parser.ExtractedPayslipTexts
import com.payslipmax.pdfparser.parser.PositionedToken
import com.payslipmax.pdfparser.parser.cleanCommasAndWhitespace
import com.payslipmax.pdfparser.parser.negateHindiTransliterations
import com.payslipmax.pdfparser.parser.parseOfficer

/**
 * Removes personally identifiable information from captured payslip text before it is committed
 * as a regression fixture. Only non-numeric PII regions are touched (name, account number, PAN,
 * email), so the parser's numeric extraction — the behavior under test — is unaffected.
 *
 * De-identification is deterministic: the same fake identifiers are injected everywhere, so the
 * parser re-extracts stable placeholder values that ground-truth fixtures can assert against.
 */
object CorpusScrubber {
    const val FAKE_ACCOUNT = "16/000/000000X"
    const val FAKE_PAN = "AR*****90G"
    const val FAKE_EMAIL = "user@example.com"
    private const val FAKE_NAME_WORD = "Officer"
    private const val FAKE_ID_SERIAL = "000000X"

    private val accountRegex = Regex("""\b\d{2,}/\d{2,}/\d{5,}[A-Z]?\b""")
    private val panMaskedRegex = Regex("""\b[A-Z]{2}[*\d]{7}[A-Z]\b""")
    private val panPlainRegex = Regex("""\b[A-Z]{5}\d{4}[A-Z]\b""")
    private val emailRegex = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")

    // Account serials survive column-splitting as bare tails like "000000X". The trailing capital
    // letter distinguishes them from monetary amounts (pure digits), which must NOT be scrubbed.
    private val accountSerialRegex = Regex("""\b\d{5,}[A-Z]\b""")

    /** Name words + account serials to remove, derived from the officer parsed out of [fullText]. */
    data class Identifiers(val nameWords: List<String>, val idSerials: List<String>)

    fun identifiersFrom(
        fullText: String,
        monthNum: Int,
        year: Int,
    ): Identifiers {
        val officer = officerFrom(fullText, monthNum, year)
        val nameWords =
            officer.name
                .split(Regex("\\s+"))
                .map { it.trim() }
                .filter { it.length >= 3 && it.all { c -> c.isLetter() } }
                .distinct()
        // Distinctive alphanumeric pieces of the real account number (e.g. "000000X"), so that
        // column-split fragments containing only the serial are still removed.
        val idSerials =
            officer.accountNo
                .split(Regex("[^0-9A-Za-z]+"))
                .filter { token -> token.count { it.isDigit() } >= 5 }
                .distinct()
        return Identifiers(nameWords, idSerials)
    }

    fun scrub(
        texts: ExtractedPayslipTexts,
        monthNum: Int,
        year: Int,
    ): ExtractedPayslipTexts {
        val (nameWords, idSerials) = identifiersFrom(texts.fullText, monthNum, year)
        return ExtractedPayslipTexts(
            leftColumnText = scrubText(texts.leftColumnText, nameWords, idSerials),
            middleColumnText = scrubText(texts.middleColumnText, nameWords, idSerials),
            fullText = scrubText(texts.fullText, nameWords, idSerials),
            taxPageText = scrubText(texts.taxPageText, nameWords, idSerials),
            dsopPageText = scrubText(texts.dsopPageText, nameWords, idSerials),
        )
    }

    /**
     * Removes PII from token IR text (Phase 2). Each token's text is a single word, so the same
     * regex/word/serial rules apply; numeric amount tokens are pure digits and are never matched.
     */
    fun scrubTokens(
        tokens: List<PositionedToken>,
        ids: Identifiers,
    ): List<PositionedToken> = tokens.map { it.copy(text = scrubText(it.text, ids.nameWords, ids.idSerials)) }

    private fun officerFrom(
        fullText: String,
        monthNum: Int,
        year: Int,
    ) = parseOfficer(negateHindiTransliterations(cleanCommasAndWhitespace(fullText)), monthNum, year)

    private fun scrubText(
        text: String,
        nameWords: List<String>,
        idSerials: List<String>,
    ): String {
        var out = text
        out = emailRegex.replace(out, FAKE_EMAIL)
        out = accountRegex.replace(out, FAKE_ACCOUNT)
        for (serial in idSerials) {
            out = Regex("(?<![0-9A-Za-z])${Regex.escape(serial)}(?![0-9A-Za-z])").replace(out, FAKE_ID_SERIAL)
        }
        out = accountSerialRegex.replace(out, FAKE_ID_SERIAL)
        out = panMaskedRegex.replace(out, FAKE_PAN)
        out = panPlainRegex.replace(out, FAKE_PAN)
        for (word in nameWords) {
            out = Regex("(?<![A-Za-z])${Regex.escape(word)}(?![A-Za-z])", RegexOption.IGNORE_CASE).replace(out, FAKE_NAME_WORD)
        }
        return out
    }
}
