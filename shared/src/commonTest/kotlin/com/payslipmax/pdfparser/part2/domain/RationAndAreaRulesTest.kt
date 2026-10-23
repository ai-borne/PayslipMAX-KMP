package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.part2.domain.RuleContexts.context
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Which linked orders apply: the ration four for an officer on ration strength (manual, ration notes 1.1, 1.5, 3, 4) and the R&H stop
 * and re-grant for an officer in an R&H station, with the officer's own rate code (seen in practice). Conditions only.
 */
class RationAndAreaRulesTest {
    private fun condition(
        family: List<Part2Rule>,
        id: String,
    ) = assertNotNull(family.filterIsInstance<ConditionRule>().firstOrNull { it.id == id }, "condition $id")

    @Test
    fun theRationFourApplyOnlyToAnOfficerOnRationStrength() {
        assertEquals(listOf("on_ration"), RationRules.all.map { it.id })
        val rule = condition(RationRules.all, RationRules.ON_RATION)
        assertTrue(rule.holds(context("X", answers = setOf(RationRules.ANSWER_ON_RATION))))
        assertFalse(rule.holds(context("X")), "not on ration strength: no ration four")
        assertFalse(rule.holds(context("X", answers = setOf("hill"))), "another answer says nothing about ration")
    }

    @Test
    fun theRhFamilyKnowsTheElevenGrantCodesAndOneConditionForEachPlusTheStation() {
        assertEquals(listOf("GTDRH11", "GTDRH12", "GTDRH13", "GTDRH21", "GTDRH22", "GTDRH23", "GTDRH31", "GTDRH32", "GTDRH33", "GTDRH77", "GTDRH60"), AreaRules.rhCodes)
        assertEquals(setOf("rh_station") + AreaRules.rhCodes.map { "rh_" + it.lowercase() }, AreaRules.all.map { it.id }.toSet())
        assertTrue(AreaRules.all.all { it is ConditionRule }, "the area family holds conditions only")
    }

    @Test
    fun anOfficerWhoNamesAnRhRateIsInAnRhStationAndOnlyThatRateIsRegranted() {
        // why: the stop applies to any R&H officer; the re-grant is under the officer's own code, never another rate.
        val station = condition(AreaRules.all, AreaRules.RH_STATION)
        val own = setOf(AreaRules.ANSWER_RH_PREFIX + "GTDRH12")
        assertTrue(station.holds(context("X", answers = own)))
        assertFalse(station.holds(context("X")), "no R&H rate named: no R&H rows")
        for (code in AreaRules.rhCodes) {
            val regrant = condition(AreaRules.all, "rh_" + code.lowercase())
            assertEquals(code == "GTDRH12", regrant.holds(context("X", answers = own)), code)
        }
    }

    @Test
    fun anUnknownRateCodeIsNeitherAStationNorARegrant() {
        // why: an answer the form never offers (or a damaged draft) must not draft an order for a code the family does not know.
        val bogus = setOf(AreaRules.ANSWER_RH_PREFIX + "GTDRH99", AreaRules.ANSWER_RH_PREFIX)
        assertFalse(condition(AreaRules.all, AreaRules.RH_STATION).holds(context("X", answers = bogus)))
    }
}
