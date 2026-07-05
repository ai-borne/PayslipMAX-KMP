package com.payslipmax.pdfparser.parser.registry

import com.payslipmax.pdfparser.parser.TokenizedPayslip
import com.payslipmax.pdfparser.parser.detection.GrammarDiagnosticReport
import com.payslipmax.pdfparser.parser.detection.GrammarEraMapper
import com.payslipmax.pdfparser.parser.detection.GrammarFamily
import com.payslipmax.pdfparser.parser.detection.extractStatementPeriod

/**
 * Registry maintaining registered [GrammarDescriptor] plugins and executing deterministic grammar
 * selection across document grammars.
 *
 * The statement period printed on the document (e.g. "STATEMENT OF ACCOUNT FOR 03/2025") is the
 * primary signal: [GrammarEraMapper] maps it directly to a family, and only that family's
 * [GrammarDescriptor.verificationMatcher] runs, as a sanity check. Text-signature matching across
 * every registered descriptor ([GrammarDescriptor.detectorMatcher], priority-sorted) is the fallback,
 * used only when no period can be parsed, or when the date-mapped family fails verification.
 */
class GrammarRegistry {
    private val descriptors = mutableListOf<GrammarDescriptor>()

    fun register(descriptor: GrammarDescriptor) {
        descriptors.add(descriptor)
    }

    fun detectAndSelect(tokenized: TokenizedPayslip): Pair<GrammarDescriptor?, GrammarDiagnosticReport> {
        val period = extractStatementPeriod(tokenized.fullText)
        if (period != null) {
            val mappedFamily = GrammarEraMapper.mapToFamily(period)
            val candidate = descriptors.find { it.family == mappedFamily }
            if (candidate != null) {
                val verification = candidate.verificationMatcher(tokenized)
                if (verification.isMatch) {
                    val report =
                        GrammarDiagnosticReport(
                            selectedFamily = candidate.family,
                            selectedPriority = candidate.priority,
                            isKnownGrammar = true,
                            matchedFingerprints = verification.matchedFingerprints,
                            selectedStrategies =
                                mapOf(
                                    "HeaderStrategy" to candidate.strategySet.headerStrategy::class.simpleName.orEmpty(),
                                    "PageStrategy" to candidate.strategySet.pageStrategy::class.simpleName.orEmpty(),
                                ),
                            validationStatus = "Signature verification: Passed",
                            selectionReason = "Date mapping (${GrammarEraMapper.eraLabel(mappedFamily)})",
                        )
                    return Pair(candidate, report)
                }
                return detectByTextSignature(
                    tokenized,
                    extraWarning =
                        "Date mapped to ${mappedFamily.name} (${GrammarEraMapper.eraLabel(mappedFamily)}) but failed " +
                            "signature verification: ${verification.rejectedReasons}; used text-signature fallback",
                )
            }
        }

        return detectByTextSignature(
            tokenized,
            extraWarning = "Statement period unavailable; fallback detector used",
        )
    }

    private fun detectByTextSignature(
        tokenized: TokenizedPayslip,
        extraWarning: String,
    ): Pair<GrammarDescriptor?, GrammarDiagnosticReport> {
        val matches = mutableListOf<Pair<GrammarDescriptor, List<String>>>()
        val rejectedCandidates = mutableMapOf<String, List<String>>()

        for (descriptor in descriptors) {
            val result = descriptor.detectorMatcher(tokenized)
            if (result.isMatch) {
                matches.add(descriptor to result.matchedFingerprints)
            } else {
                rejectedCandidates[descriptor.family.name] = result.rejectedReasons
            }
        }

        if (matches.isEmpty()) {
            val unknownReport =
                GrammarDiagnosticReport(
                    selectedFamily = GrammarFamily.UNKNOWN,
                    selectedPriority = -1,
                    isKnownGrammar = false,
                    matchedFingerprints = emptyList(),
                    rejectedCandidates = rejectedCandidates,
                    warnings = listOf("No registered grammar descriptor matched the document stream", extraWarning),
                    selectionReason = extraWarning,
                )
            return Pair(null, unknownReport)
        }

        // Deterministic conflict resolution: Sort by priority descending
        val sortedMatches = matches.sortedByDescending { it.first.priority }
        val winningPair = sortedMatches.first()
        val winningDescriptor = winningPair.first
        val winningFingerprints = winningPair.second

        // Record non-winning matches into rejectedCandidates for telemetry transparency
        for (i in 1 until sortedMatches.size) {
            val nonWinner = sortedMatches[i]
            rejectedCandidates[nonWinner.first.family.name] =
                listOf(
                    "Matched rules but lost in priority resolution (${nonWinner.first.priority} < ${winningDescriptor.priority})",
                )
        }

        val report =
            GrammarDiagnosticReport(
                selectedFamily = winningDescriptor.family,
                selectedPriority = winningDescriptor.priority,
                isKnownGrammar = true,
                matchedFingerprints = winningFingerprints,
                rejectedCandidates = rejectedCandidates,
                selectedStrategies =
                    mapOf(
                        "HeaderStrategy" to winningDescriptor.strategySet.headerStrategy::class.simpleName.orEmpty(),
                        "PageStrategy" to winningDescriptor.strategySet.pageStrategy::class.simpleName.orEmpty(),
                    ),
                warnings = listOf(extraWarning),
                selectionReason = extraWarning,
            )

        return Pair(winningDescriptor, report)
    }
}
