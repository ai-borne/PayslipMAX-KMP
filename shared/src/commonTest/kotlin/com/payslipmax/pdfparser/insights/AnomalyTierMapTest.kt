package com.payslipmax.pdfparser.insights

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnomalyTierMapTest {
    /** D6: exactly these two anomaly types are FREE; everything else is PRO. */
    @Test
    fun onlySalaryLossAndDeductionSpikeAreFree() {
        val free = AnomalyTierMap.tiers.filterValues { it == AnomalyTier.FREE }.keys
        assertEquals(setOf("SALARY_LOSS", "DEDUCTION_SPIKE"), free)
    }

    /** Every anomaly type an auditor emits today must be classified (loop guards against forgetting). */
    @Test
    fun allKnownAuditorTypesAreClassified() {
        val knownAuditorTypes =
            setOf(
                "SALARY_LOSS",
                "DEDUCTION_SPIKE",
                "MISSING_ALLOWANCE",
                "TPTA_ENTITLEMENT",
                "ARREARS_AUDIT",
                "DSOP_COMPLIANCE",
                "DSOP_MILESTONE",
                "TAX_PROJECTION",
                "RENT_RECOVERY_RISK",
                "DEBIT_RECOVERY",
            )
        knownAuditorTypes.forEach { type ->
            assertTrue(AnomalyTierMap.tiers.containsKey(type), "Unclassified anomaly type: $type")
        }
        assertEquals(knownAuditorTypes, AnomalyTierMap.tiers.keys)
    }

    /** Fail-closed: an unclassified type is locked (PRO), never leaked as free. */
    @Test
    fun unknownTypeDefaultsToPro() {
        assertEquals(AnomalyTier.PRO, AnomalyTierMap.tierFor("SOME_FUTURE_AUDITOR"))
    }

    @Test
    fun tierForResolvesKnownTypes() {
        assertEquals(AnomalyTier.FREE, AnomalyTierMap.tierFor("SALARY_LOSS"))
        assertEquals(AnomalyTier.PRO, AnomalyTierMap.tierFor("DSOP_COMPLIANCE"))
    }
}
