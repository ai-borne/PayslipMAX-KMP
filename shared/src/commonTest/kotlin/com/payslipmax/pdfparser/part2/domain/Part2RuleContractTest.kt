package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.guide.model.Part2RuleDecl
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The bundle declares each rule's id, kind, severity and citation; Kotlin implements the logic. They are two lists that must stay one,
 * or a template cites a check the app does not run (a silent hole) or the app runs a check no template can cite. The same comparison
 * runs on the real bundle in composeApp (`Part2LookupContract`); here it is pinned on the synthetic rules, both ways round.
 */
class Part2RuleContractTest {
    private val declared = SyntheticPart2Templates.rules
    private val registry = SyntheticPart2Templates.registry()

    @Test
    fun theSyntheticRegistryImplementsExactlyTheRulesTheBundleDeclares() {
        assertEquals(emptyList(), registry.problemsAgainst(declared))
        assertEquals(declared.map { it.id }.toSet(), registry.ids)
    }

    @Test
    fun aDeclaredRuleTheAppDoesNotImplementIsReported() {
        val extra = declared + Part2RuleDecl("brand_new", "check", "ack", "manual", 120)
        assertEquals(listOf("rule brand_new is declared in the bundle but the app does not implement it"), registry.problemsAgainst(extra))
    }

    @Test
    fun anImplementedRuleNoBundleDeclaresIsReported() {
        val fewer = declared.filterNot { it.id == SyntheticPart2Templates.CAP_DAYS }
        assertEquals(listOf("rule cap_days is implemented in the app but the bundle does not declare it"), registry.problemsAgainst(fewer))
    }

    @Test
    fun aCheckDeclaredAsAConditionIsReported() {
        val swapped = declared.map { if (it.id == SyntheticPart2Templates.CAP_DAYS) it.copy(kind = "condition", severity = null) else it }
        assertEquals(listOf("rule cap_days is a condition in the bundle but a check in the app"), registry.problemsAgainst(swapped))
    }

    @Test
    fun lookupByKindFindsTheRightRuleAndNeverTheOtherKind() {
        assertNotNull(registry.check(SyntheticPart2Templates.CAP_DAYS))
        assertNotNull(registry.condition(SyntheticPart2Templates.ON_RATION))
        assertNull(registry.condition(SyntheticPart2Templates.CAP_DAYS))
        assertNull(registry.check(SyntheticPart2Templates.ON_RATION))
        assertNull(registry.check("nosuchrule"))
    }

    @Test
    fun twoRulesWithOneIdAreRefusedWhenTheRegistryIsBuilt() {
        val twin = SyntheticPart2Templates.registry()
        val error = assertFailsWith<IllegalArgumentException> { Part2Rules(listOf(twin.check(SyntheticPart2Templates.CAP_DAYS)!!, twin.check(SyntheticPart2Templates.CAP_DAYS)!!)) }
        assertEquals("duplicate Part II rule id", error.message)
    }

    @Test
    fun theShippedRegistryStartsEmptyBecauseNoRealRuleExistsUntilPhase2b() {
        assertEquals(emptySet(), Part2Rules.shipped().ids)
    }
}
