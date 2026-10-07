package com.payslipmax.pdfparser.guide.domain

/**
 * Word and rule-number scanning for Claim Guide search. Plain character scans, no regex, so it stays linear on
 * Kotlin/Native (the lookaround-regex stall in `docs/AI_INSIGHTS_PIPELINE.md` came from `commonMain` regex).
 *
 * A rule number is digits with at most two trailing letters ("114", "177B", "85A"). Only words right after
 * "Rule" or "Rules" count, joined by "and", "or" and "to" ("Rules 94, 95 and 100 to 101" gives 94, 95, 100, 101),
 * so the dates, letter numbers and amounts in a cite ("1(16)/2017/D", "18-09-2017") are never read as rules. A range
 * of plain numbers ("88 to 91") gives every rule in it, unless it spans more than [MAX_RANGE_SPAN] rules, which
 * would be a typo, not a rule range; lettered ends ("177A to 177C") give just the two ends.
 */
object GuideRuleNumberParser {
    private const val MAX_RULE_SUFFIX_LETTERS = 2
    const val MAX_RANGE_SPAN = 50

    /** The words that introduce rule numbers; search drops one at the start of a query too. */
    val RULE_KEYWORDS = setOf("rule", "rules")
    private const val RANGE_WORD = "to"
    private val CONNECTORS = setOf("and", "or", RANGE_WORD)

    /**
     * The words of [text]: lower case, split at anything that is not a letter or digit. Apostrophes are dropped
     * rather than split on, so "Family's" is "familys" and a query typed the same way finds it.
     */
    fun words(text: String): List<String> {
        val words = mutableListOf<String>()
        val current = StringBuilder()
        for (char in text) {
            when {
                char.isLetterOrDigit() -> current.append(char.lowercaseChar())
                char == '\'' || char == '’' -> Unit
                current.isNotEmpty() -> {
                    words += current.toString()
                    current.clear()
                }
            }
        }
        if (current.isNotEmpty()) words += current.toString()
        return words
    }

    /** True for a word shaped like a rule number: digits first, then at most two letters ("177b"). */
    fun isRuleNumber(word: String): Boolean {
        val digits = word.takeWhile(Char::isDigit)
        val suffix = word.drop(digits.length)
        return digits.isNotEmpty() && suffix.length <= MAX_RULE_SUFFIX_LETTERS && suffix.all(Char::isLetter)
    }

    /** The rule numbers [text] names after "Rule" or "Rules", lower case ("Rule 85A, TR 2014" gives `85a`). */
    fun ruleNumbers(text: String): Set<String> {
        val found = linkedSetOf<String>()
        val words = words(text)
        var at = 0
        while (at < words.size) {
            if (words[at++] in RULE_KEYWORDS) {
                var last: String? = null
                var inRange = false
                while (at < words.size && (isRuleNumber(words[at]) || words[at] in CONNECTORS)) {
                    val word = words[at++]
                    when {
                        word == RANGE_WORD -> inRange = last != null
                        isRuleNumber(word) -> {
                            if (inRange) found += between(last.orEmpty(), word)
                            found += word
                            last = word
                            inRange = false
                        }
                        else -> inRange = false // "and" or "or" ends a range
                    }
                }
            }
        }
        return found
    }

    /** The whole numbers strictly between two plain numbers, or none when either has letters or the span is too wide. */
    private fun between(
        from: String,
        to: String,
    ): List<String> {
        val low = from.toIntOrNull() ?: return emptyList()
        val high = to.toIntOrNull() ?: return emptyList()
        return if (high - low in 2..MAX_RANGE_SPAN) (low + 1 until high).map(Int::toString) else emptyList()
    }

    /**
     * Whether [word] answers a query word that starts with a digit. The digits must match as a whole: "177" is not
     * "1770". A bare number also finds its lettered rules ("177" finds "177b"), because those are one rule family;
     * a lettered query ("177b") finds only itself.
     */
    fun numberMatches(
        queryWord: String,
        word: String,
    ): Boolean =
        word == queryWord ||
            (queryWord.all(Char::isDigit) && word.length == queryWord.length + 1 && word.startsWith(queryWord) && word.last().isLetter())
}
