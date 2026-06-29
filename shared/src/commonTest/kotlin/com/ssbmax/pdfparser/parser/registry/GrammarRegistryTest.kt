package com.ssbmax.pdfparser.parser.registry

import com.ssbmax.pdfparser.parser.TokenizedPayslip
import com.ssbmax.pdfparser.parser.detection.GrammarFamily
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GrammarRegistryTest {
    private lateinit var registry: GrammarRegistry

    @BeforeTest
    fun setUp() {
        registry = GrammarRegistry()
        DefaultGrammarDescriptors.registerAll(registry)
    }

    @Test
    fun testDetect2014LegacyStatement() {
        val mockTokenized =
            TokenizedPayslip(
                tableTokens = emptyList(),
                taxTokens = emptyList(),
                dsopTokens = emptyList(),
                fullText = "STATEMENT OF ACCOUNT FOR 01/2014\nNAME : OFFICER",
            )
        val (descriptor, report) = registry.detectAndSelect(mockTokenized)

        assertNotNull(descriptor)
        assertEquals(GrammarFamily.PCDA_LEGACY_STATEMENT, descriptor.family)
        assertEquals(GrammarFamily.PCDA_LEGACY_STATEMENT, report.selectedFamily)
        assertTrue(report.isKnownGrammar)
        assertTrue(report.matchedFingerprints.any { it.contains("STATEMENT OF ACCOUNT") })
    }

    @Test
    fun testDetect2016EarlyDualCol() {
        val mockTokenized =
            TokenizedPayslip(
                tableTokens = emptyList(),
                taxTokens = emptyList(),
                dsopTokens = emptyList(),
                fullText = "STATEMENT OF ACCOUNT FOR 05/2016\nCDA A/C NO : 16/000/000000X\nBand Pay 45000 Grade Pay 10000",
            )
        val (descriptor, report) = registry.detectAndSelect(mockTokenized)

        assertNotNull(descriptor)
        assertEquals(GrammarFamily.PCDA_EARLY_DUAL_COL, descriptor.family)
        assertEquals(GrammarFamily.PCDA_EARLY_DUAL_COL, report.selectedFamily)
        assertTrue(report.isKnownGrammar)
        assertTrue(report.matchedFingerprints.any { it.contains("Band Pay") })
    }

    @Test
    fun testDetect2022Transitional7thCPC() {
        val mockTokenized =
            TokenizedPayslip(
                tableTokens = emptyList(),
                taxTokens = emptyList(),
                dsopTokens = emptyList(),
                fullText = "STATEMENT OF ACCOUNT FOR 01/2022\nCDA A/C NO : 16/000/000000X\nBasic Pay 132400 DA 45849 MSP 15500",
            )
        val (descriptor, report) = registry.detectAndSelect(mockTokenized)

        assertNotNull(descriptor)
        assertEquals(GrammarFamily.PCDA_TRANSITIONAL_7TH_CPC, descriptor.family)
        assertEquals(GrammarFamily.PCDA_TRANSITIONAL_7TH_CPC, report.selectedFamily)
        assertTrue(report.isKnownGrammar)
        assertTrue(report.matchedFingerprints.any { it.contains("Basic Pay") })
    }

    @Test
    fun testDetect2024ModernGrid() {
        val mockTokenized =
            TokenizedPayslip(
                tableTokens = emptyList(),
                taxTokens = emptyList(),
                dsopTokens = emptyList(),
                fullText = "STATEMENT OF ACCOUNT FOR 01/2024\nName: Officer A/C No: 16/000/000000X\nBPAY 140500 DA 71760\nGross Pay 233016 Total Deductions 93412\nDO2 Details Page 4",
            )
        val (descriptor, report) = registry.detectAndSelect(mockTokenized)

        assertNotNull(descriptor)
        assertEquals(GrammarFamily.PCDA_MODERN_GRID, descriptor.family)
        assertEquals(GrammarFamily.PCDA_MODERN_GRID, report.selectedFamily)
        assertTrue(report.isKnownGrammar)
        assertTrue(report.matchedFingerprints.any { it.contains("BPAY") })
    }

    @Test
    fun testDetect2026ExtendedGrid() {
        val mockTokenized =
            TokenizedPayslip(
                tableTokens = emptyList(),
                taxTokens = emptyList(),
                dsopTokens = emptyList(),
                fullText = "STATEMENT OF ACCOUNT FOR 04/2026\nBPAY 149000 ARR-DA 9870 ARR-TPTADA 216\nGross Pay 301828 Total Deductions 107998",
            )
        val (descriptor, report) = registry.detectAndSelect(mockTokenized)

        assertNotNull(descriptor)
        assertEquals(GrammarFamily.PCDA_EXTENDED_GRID, descriptor.family)
        assertEquals(GrammarFamily.PCDA_EXTENDED_GRID, report.selectedFamily)
        assertTrue(report.isKnownGrammar)
        assertTrue(report.matchedFingerprints.any { it.contains("ARR-") })
    }

    @Test
    fun testDeterministicConflictResolution() {
        // Mock stream that matches BOTH Modern Grid (Priority 40) AND Extended Grid (Priority 50)
        val mockTokenized =
            TokenizedPayslip(
                tableTokens = emptyList(),
                taxTokens = emptyList(),
                dsopTokens = emptyList(),
                fullText = "STATEMENT OF ACCOUNT FOR 04/2026\nBPAY 149000 Gross Pay 301828 ARR-DA 9870",
            )
        val (descriptor, report) = registry.detectAndSelect(mockTokenized)

        assertNotNull(descriptor)
        // Must resolve to highest priority (Extended Grid: 50)
        assertEquals(GrammarFamily.PCDA_EXTENDED_GRID, descriptor.family)
        assertEquals(50, report.selectedPriority)
        assertTrue(report.rejectedCandidates.containsKey(GrammarFamily.PCDA_MODERN_GRID.name))
        val rejectedReason = report.rejectedCandidates[GrammarFamily.PCDA_MODERN_GRID.name]?.firstOrNull()
        assertNotNull(rejectedReason)
        assertTrue(rejectedReason.contains("lost in priority resolution"))
    }

    @Test
    fun testUnknownFallback() {
        val mockTokenized =
            TokenizedPayslip(
                tableTokens = emptyList(),
                taxTokens = emptyList(),
                dsopTokens = emptyList(),
                fullText = "Unrecognized document structure without any matching keywords",
            )
        val (descriptor, report) = registry.detectAndSelect(mockTokenized)

        assertNull(descriptor)
        assertEquals(GrammarFamily.UNKNOWN, report.selectedFamily)
        assertFalse(report.isKnownGrammar)
        assertEquals(-1, report.selectedPriority)
    }
}
