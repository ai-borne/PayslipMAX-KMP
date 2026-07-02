package com.ssbmax.pdfparser.parser.registry

import com.ssbmax.pdfparser.parser.TokenizedPayslip
import com.ssbmax.pdfparser.parser.detection.GrammarDiagnosticReport
import com.ssbmax.pdfparser.parser.detection.GrammarFamily

/**
 * Registry maintaining registered [GrammarDescriptor] plugins and executing deterministic,
 * priority-based conflict resolution across document grammars.
 */
class GrammarRegistry {
    private val descriptors = mutableListOf<GrammarDescriptor>()

    fun register(descriptor: GrammarDescriptor) {
        descriptors.add(descriptor)
    }

    fun detectAndSelect(tokenized: TokenizedPayslip): Pair<GrammarDescriptor?, GrammarDiagnosticReport> {
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
                    warnings = listOf("No registered grammar descriptor matched the document stream"),
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
            )

        return Pair(winningDescriptor, report)
    }
}
