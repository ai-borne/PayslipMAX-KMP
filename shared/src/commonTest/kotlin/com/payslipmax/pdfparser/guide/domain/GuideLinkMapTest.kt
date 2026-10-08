package com.payslipmax.pdfparser.guide.domain

import com.payslipmax.pdfparser.insights.AnomalyTierMap
import com.payslipmax.pdfparser.insights.PayAuditFindingTypes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The owner-approved finding-to-card table (E7, 2026-10-08). The expected ids are written out here on purpose: a test that
 * read them from the map could not fail when someone changes a mapping the owner never approved.
 */
class GuideLinkMapTest {
    @Test
    fun eachApprovedFindingOpensItsApprovedCard() {
        val approved =
            listOf(
                Triple(AnomalyTierMap.MISSING_ALLOWANCE, "houseRentAllowance", "RB-SS-P114"),
                Triple(AnomalyTierMap.MISSING_ALLOWANCE, "militaryServicePay", "RB-C13-05"),
                Triple(AnomalyTierMap.TPTA_ENTITLEMENT, "transportAllowance", "RB-SS-P051-rates"),
                Triple(AnomalyTierMap.ARREARS_AUDIT, "arrearsDa", "RB-RP-052"),
                Triple(AnomalyTierMap.ARREARS_AUDIT, "arrearsTptaDa", "RB-RP-052"),
                Triple(AnomalyTierMap.INCREMENT_MISSED, "basicPay", "RB-C13-08"),
                Triple(AnomalyTierMap.MSP_SHORTFALL, "militaryServicePay", "RB-C13-05"),
                // The under-paid arrears issue: not a PayAuditFindingTypes member, but shown by Pay Audit.
                Triple(AnomalyTierMap.SALARY_LOSS, "arrearsDa", "RB-RP-052"),
                Triple(AnomalyTierMap.SALARY_LOSS, "arrearsTptaDa", "RB-RP-052"),
            )
        for ((type, field, card) in approved) assertEquals(card, GuideLinkMap.cardFor(type, field), "$type / $field")
    }

    @Test
    fun aNewPayAuditFindingTypeWithNoMappingDecisionFailsHere() {
        // Adding a type to PayAuditFindingTypes forces the owner decision: a card, or an explicit "no card fits".
        val undecided = PayAuditFindingTypes.TYPES - GuideLinkMap.decidedTypes
        assertTrue(undecided.isEmpty(), "Pay Audit finding types with no Guide link decision: $undecided")
    }

    @Test
    fun anUnapprovedPayLineOrTypeGetsNoLink() {
        assertNull(GuideLinkMap.cardFor(AnomalyTierMap.MISSING_ALLOWANCE, "transportAllowance"))
        assertNull(GuideLinkMap.cardFor(AnomalyTierMap.MISSING_ALLOWANCE, ""))
        assertNull(GuideLinkMap.cardFor("SOMETHING_NEW", "basicPay"))
        // SalaryLossAuditor's bare net-pay heuristic is not an arrears finding and has no card.
        assertNull(GuideLinkMap.cardFor(AnomalyTierMap.SALARY_LOSS, "netPay"))
    }

    @Test
    fun everyLinkedCardIdIsListedForTheBundleContract() {
        assertEquals(setOf("RB-SS-P114", "RB-C13-05", "RB-SS-P051-rates", "RB-RP-052", "RB-C13-08"), GuideLinkMap.cardIds)
    }
}
