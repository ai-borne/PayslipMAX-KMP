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
        // Date-primary path verifies structurally (BPAY), not via the incidental ARR- marker.
        assertTrue(report.matchedFingerprints.any { it.contains("BPAY") })
        assertTrue(report.selectionReason.contains("Date mapping"))
    }

    @Test
    fun testDeterministicConflictResolutionWithoutDate() {
        // No parseable statement period -> falls back to text-signature priority arbitration.
        // Mock stream that matches BOTH Modern Grid (Priority 40) AND Extended Grid (Priority 50).
        val mockTokenized =
            TokenizedPayslip(
                tableTokens = emptyList(),
                taxTokens = emptyList(),
                dsopTokens = emptyList(),
                fullText = "BPAY 149000 Gross Pay 301828 ARR-DA 9870",
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
        assertTrue(report.selectionReason.contains("Statement period unavailable"))
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

    // --- Date-primary era boundary coverage -------------------------------------------------

    @Test
    fun testPreOct2023ResolvesTransitional() {
        val mockTokenized =
            TokenizedPayslip(
                tableTokens = emptyList(),
                taxTokens = emptyList(),
                dsopTokens = emptyList(),
                fullText = "STATEMENT OF ACCOUNT FOR 09/2023\nCDA A/C NO : 16/000/000000X\nBasic Pay 130000 DA 44720",
            )
        val (descriptor, report) = registry.detectAndSelect(mockTokenized)

        assertEquals(GrammarFamily.PCDA_TRANSITIONAL_7TH_CPC, descriptor?.family)
        assertTrue(report.selectionReason.contains("Date mapping"))
    }

    @Test
    fun testOct2023BoundaryResolvesTransitional() {
        val mockTokenized =
            TokenizedPayslip(
                tableTokens = emptyList(),
                taxTokens = emptyList(),
                dsopTokens = emptyList(),
                fullText = "STATEMENT OF ACCOUNT FOR 10/2023\nBasic Pay 130000 DA 44720",
            )
        val (descriptor, _) = registry.detectAndSelect(mockTokenized)

        assertEquals(GrammarFamily.PCDA_TRANSITIONAL_7TH_CPC, descriptor?.family)
    }

    @Test
    fun testNov2023BoundaryResolvesModernGrid() {
        val mockTokenized =
            TokenizedPayslip(
                tableTokens = emptyList(),
                taxTokens = emptyList(),
                dsopTokens = emptyList(),
                fullText = "STATEMENT OF ACCOUNT FOR 11/2023\nBPAY 132400 DA 45849",
            )
        val (descriptor, _) = registry.detectAndSelect(mockTokenized)

        assertEquals(GrammarFamily.PCDA_MODERN_GRID, descriptor?.family)
    }

    @Test
    fun testJan2024ResolvesModernGrid() {
        val mockTokenized =
            TokenizedPayslip(
                tableTokens = emptyList(),
                taxTokens = emptyList(),
                dsopTokens = emptyList(),
                fullText = "STATEMENT OF ACCOUNT FOR 01/2024\nBPAY 140500 DA 71760",
            )
        val (descriptor, _) = registry.detectAndSelect(mockTokenized)

        assertEquals(GrammarFamily.PCDA_MODERN_GRID, descriptor?.family)
    }

    @Test
    fun testFeb2025BoundaryResolvesModernGrid() {
        val mockTokenized =
            TokenizedPayslip(
                tableTokens = emptyList(),
                taxTokens = emptyList(),
                dsopTokens = emptyList(),
                fullText = "STATEMENT OF ACCOUNT FOR 02/2025\nBPAY 144700 DA 84906",
            )
        val (descriptor, _) = registry.detectAndSelect(mockTokenized)

        assertEquals(GrammarFamily.PCDA_MODERN_GRID, descriptor?.family)
    }

    @Test
    fun testMar2025BoundaryResolvesExtendedGrid() {
        val mockTokenized =
            TokenizedPayslip(
                tableTokens = emptyList(),
                taxTokens = emptyList(),
                dsopTokens = emptyList(),
                fullText = "STATEMENT OF ACCOUNT FOR 03/2025\nBPAY 144700 ARR-DA 9870",
            )
        val (descriptor, _) = registry.detectAndSelect(mockTokenized)

        assertEquals(GrammarFamily.PCDA_EXTENDED_GRID, descriptor?.family)
    }

    @Test
    fun testMar2025WithoutArrearsStillResolvesExtendedGrid() {
        // Regression test for the real-world bug: a Mar-2025+ payslip with no arrears line item that
        // month must not fall back to Modern Grid just because the incidental "ARR-" text is absent.
        val mockTokenized =
            TokenizedPayslip(
                tableTokens = emptyList(),
                taxTokens = emptyList(),
                dsopTokens = emptyList(),
                fullText =
                    "STATEMENT OF ACCOUNT FOR 03/2025\nEARNINGS DEDUCTIONS\nBPAY 144700 DSOP 40000\n" +
                        "DA 84906 AGIF 10000\nMSP 15500\nGross Pay 271739 Total Deductions 96432",
            )
        val (descriptor, report) = registry.detectAndSelect(mockTokenized)

        assertEquals(GrammarFamily.PCDA_EXTENDED_GRID, descriptor?.family)
        assertTrue(report.selectionReason.contains("Date mapping"))
        assertTrue(report.validationStatus.contains("Passed"))
    }

    @Test
    fun testFutureMonthResolvesExtendedGrid() {
        val mockTokenized =
            TokenizedPayslip(
                tableTokens = emptyList(),
                taxTokens = emptyList(),
                dsopTokens = emptyList(),
                fullText = "STATEMENT OF ACCOUNT FOR 04/2025\nBPAY 150000 DA 87000",
            )
        val (descriptor, _) = registry.detectAndSelect(mockTokenized)

        assertEquals(GrammarFamily.PCDA_EXTENDED_GRID, descriptor?.family)
    }

    @Test
    fun testFarFutureMonthResolvesExtendedGrid() {
        val mockTokenized =
            TokenizedPayslip(
                tableTokens = emptyList(),
                taxTokens = emptyList(),
                dsopTokens = emptyList(),
                fullText = "STATEMENT OF ACCOUNT FOR 12/2030\nBPAY 200000 DA 120000",
            )
        val (descriptor, _) = registry.detectAndSelect(mockTokenized)

        assertEquals(GrammarFamily.PCDA_EXTENDED_GRID, descriptor?.family)
    }
}
