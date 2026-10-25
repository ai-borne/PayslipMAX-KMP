package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.data.SealedSyntheticPart2.sealed
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.model.Part2Column
import com.payslipmax.pdfparser.guide.model.Part2Link
import com.payslipmax.pdfparser.guide.model.Part2Lookup
import com.payslipmax.pdfparser.guide.model.Part2RuleDecl
import com.payslipmax.pdfparser.guide.model.Part2Slot
import com.payslipmax.pdfparser.guide.model.Part2Template
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.CLAIM
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.GRANT
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.NOTE
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.REJOIN_ALL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A template is what turns an officer's answers into the rows a clerk types into HRMS, so a damaged one must refuse to load rather than
 * print a wrong or uncited row. These rechecks mirror `part2_templates.py` and `part2_rules.py`, which enforced the same rules when the
 * bundle was built; the messages name ids and fields only, never a narrative.
 */
class GuidePart2TemplateValidatorTest {
    private val section: Part2Lookup = SealedSyntheticPart2.lookup
    private val valid: GuideBundle = SealedSyntheticPart2.bundle()

    private fun problems(part: Part2Lookup?) = GuideBundleValidator.validate(valid.copy(part2 = part))

    /** Edits one template and recomputes its revision, so the edit is judged on its own merits; [keepRev] leaves the revision as the edit made it. */
    private fun Part2Lookup.editTemplate(
        code: String,
        keepRev: Boolean = false,
        edit: (Part2Template) -> Part2Template,
    ) = copy(templates = templates.map { if (it.code == code) edit(it).let { edited -> if (keepRev) edited else edited.sealed() } else it })

    private fun Part2Lookup.editRule(
        id: String,
        edit: (Part2RuleDecl) -> Part2RuleDecl,
    ) = copy(rules = rules.map { if (it.id == id) edit(it) else it })

    private fun assertProblem(
        edited: Part2Lookup,
        fragment: String,
    ) {
        val found = problems(edited)
        assertTrue(found.any { fragment in it }, "expected a problem containing '$fragment', got $found")
    }

    private fun Part2Template.withSlot(slot: Part2Slot) = copy(slots = slots + slot)

    @Test
    fun theSyntheticTemplatesAreValidAndASectionWithNoTemplatesIsToo() {
        assertEquals(emptyList(), problems(section))
        assertEquals(emptyList(), problems(section.copy(rules = emptyList(), templates = emptyList(), templateLimits = null)))
        assertEquals(emptyList(), problems(null))
    }

    @Test
    fun aBundleBuiltBeforeTemplatesExistedStillLoads() {
        // why: the templates are additive (BUNDLE_VERSION stays 1), so a section with only the lookup must keep opening the Guide.
        val old = GuideJson.encodeToString(GuideBundle.serializer(), SyntheticGuideBundle.withPart2())
        val loaded = assertIs<GuideLoadResult.Loaded>(GuideBundleParser.parse(old))
        assertEquals(emptyList(), loaded.bundle.part2?.templates)
        assertNull(loaded.bundle.part2?.templateLimits)
    }

    @Test
    fun aBundleCarryingTemplatesLoadsAndKeepsThemInOrder() {
        val loaded = assertIs<GuideLoadResult.Loaded>(GuideBundleParser.parse(GuideJson.encodeToString(GuideBundle.serializer(), valid)))
        assertEquals(SyntheticPart2Templates.templates.map { it.code }, loaded.bundle.part2?.templates?.map { it.code })
    }

    @Test
    fun templatesNeedTheLimitsTheyAreCheckedAgainst() {
        // why: the limits ship in the section so Kotlin holds no second copy; templates without them cannot be checked, so they are refused.
        assertProblem(section.copy(templateLimits = null), "part2 templates: template_limits are missing")
        val limits = SyntheticPart2Templates.limits
        assertProblem(section.copy(templateLimits = limits.copy(help = 0)), "part2 templates: limits must be above zero")
    }

    @Test
    fun aTemplateForAnUnknownCodeOrASecondTemplateForOneCodeIsRefused() {
        assertProblem(section.editTemplate(NOTE) { it.copy(code = "NOSUCHCODE") }, "template NOSUCHCODE: unknown code")
        assertProblem(section.copy(templates = section.templates + section.templates.first { it.code == NOTE }), "template $NOTE: duplicate template")
    }

    @Test
    fun aPrintedCodeOtherThanRejoinalToRejoinIsRefused() {
        assertProblem(section.editTemplate(NOTE) { it.copy(print = "NOTE") }, "template $NOTE: print override must be REJOINAL to REJOIN")
        assertProblem(section.editTemplate(REJOIN_ALL) { it.copy(print = "REJOINING") }, "template $REJOIN_ALL: print override must be REJOINAL to REJOIN")
        assertEquals(emptyList(), problems(section.editTemplate(REJOIN_ALL) { it.copy(print = null) }), "the override is optional")
    }

    @Test
    fun aMissingOrImpossiblePageCitationIsRefused() {
        assertProblem(section.editTemplate(NOTE) { it.copy(page = 0) }, "template $NOTE: page must be a whole number from 1 to 400")
        assertProblem(section.editTemplate(NOTE) { it.copy(page = 401) }, "template $NOTE: page must be a whole number from 1 to 400")
    }

    @Test
    fun aSlotUsedButNotDeclaredIsRefusedWhereverItIsUsed() {
        val fragment = "template $NOTE: slot ghost used but not declared"
        assertProblem(section.editTemplate(NOTE) { it.copy(narrative = "Noted on {noted_on} as {ghost}") }, fragment)
        assertProblem(section.editTemplate(NOTE) { it.copy(data = listOf(Part2Column("required", "ghost")) + it.data.drop(1)) }, fragment)
        assertProblem(section.editTemplate(NOTE) { it.copy(authority = "By {ghost}") }, fragment)
        assertProblem(section.editTemplate(NOTE) { it.copy(certificate = "Seen {ghost}") }, fragment)
    }

    @Test
    fun aDeclaredSlotNothingUsesIsRefused() {
        assertProblem(section.editTemplate(NOTE) { it.withSlot(Part2Slot("spare", "text", "Spare", "Not used anywhere.")) }, "template $NOTE: slot spare declared but never used")
    }

    @Test
    fun aFilledNotRequiredColumnIsRefused() {
        // why: Annexure III marks columns the unit must leave empty; a value there is a row HRMS rejects.
        val filled = section.editTemplate(NOTE) { it.copy(data = it.data.toMutableList().also { cols -> cols[1] = Part2Column("none", "remark") }) }
        assertProblem(filled, "template $NOTE: column data2 is not required but names a slot")
    }

    @Test
    fun aRequiredOrOptionalColumnMustNameASlotAndANeedMustBeKnown() {
        assertProblem(section.editTemplate(NOTE) { it.copy(from = Part2Column("required")) }, "template $NOTE: column from needs a slot")
        assertProblem(section.editTemplate(NOTE) { it.copy(data = listOf(Part2Column("optional")) + it.data.drop(1)) }, "template $NOTE: column data1 needs a slot")
        assertProblem(section.editTemplate(NOTE) { it.copy(data = listOf(Part2Column("maybe", "remark")) + it.data.drop(1)) }, "template $NOTE: column data1 need must be required, optional or none")
    }

    @Test
    fun dataIsExactlyFourColumnsAndFromAndToAreDates() {
        assertProblem(section.editTemplate(NOTE) { it.copy(data = it.data.dropLast(1)) }, "template $NOTE: data must have exactly 4 columns")
        assertProblem(section.editTemplate(NOTE) { it.copy(from = Part2Column("required", "remark")) }, "template $NOTE: column from must name a date slot")
    }

    @Test
    fun aSlotIdIsWellFormedAndUniqueAndItsKindIsKnown() {
        assertProblem(section.editTemplate(NOTE) { it.copy(slots = listOf(it.slots[0], it.slots[1].copy(id = "Remark"))) }, "template $NOTE: slot Remark: id must be lowercase letters, digits and underscores")
        assertProblem(section.editTemplate(NOTE) { it.copy(slots = listOf(it.slots[0].copy(id = "remark"), it.slots[1])) }, "template $NOTE: duplicate slot id remark")
        assertProblem(section.editTemplate(NOTE) { it.copy(slots = listOf(it.slots[0], it.slots[1].copy(kind = "money"))) }, "template $NOTE: slot remark: kind must be date, number, text, days, ref, earliest, latest or next_day")
    }

    @Test
    fun aDaysSlotCountsBetweenTwoDeclaredDateSlots() {
        val between = "template $GRANT: slot days: between must name two different date slots"
        assertProblem(section.editTemplate(GRANT) { it.copy(slots = it.slots.map { s -> if (s.id == "days") s.copy(between = listOf("leave_from")) else s }) }, between)
        assertProblem(section.editTemplate(GRANT) { it.copy(slots = it.slots.map { s -> if (s.id == "days") s.copy(between = listOf("leave_from", "year")) else s }) }, between)
        assertProblem(section.editTemplate(NOTE) { it.copy(slots = listOf(it.slots[0], it.slots[1].copy(between = listOf("noted_on", "noted_on")))) }, "template $NOTE: slot remark: only a days or computed slot has between")
    }

    @Test
    fun anOrderReferenceIsUsedByItsNumberOrItsDateAndNothingElseIs() {
        assertProblem(section.editTemplate(REJOIN_ALL) { it.copy(narrative = "Rejoined {rejoin_on} vide {grant}") }, "template $REJOIN_ALL: slot grant is a reference, so use grant.no or grant.date")
        assertProblem(section.editTemplate(NOTE) { it.copy(narrative = "Noted {noted_on} as {remark.no}") }, "template $NOTE: slot remark is not a reference, so it has no .no")
    }

    @Test
    fun oneSlotIdMeansTheSameKindInEveryTemplate() {
        val clash = section.editTemplate(NOTE) { it.withSlot(Part2Slot("leave_from", "text", "Other", "Same id as a date elsewhere.")).copy(narrative = it.narrative + " {leave_from}") }
        assertProblem(clash, "slot leave_from: declared with different kinds (date, text)")
    }

    @Test
    fun anUnknownRuleIdIsRefusedInAChecksListAndInALinkCondition() {
        assertProblem(section.editTemplate(GRANT) { it.copy(rules = listOf("nosuchrule")) }, "template $GRANT: rule nosuchrule is unknown")
        assertProblem(section.editTemplate(GRANT) { it.copy(links = listOf(it.links[0].copy(condition = "nosuchrule")) + it.links.drop(1)) }, "template $GRANT: link to ${SyntheticPart2Templates.RATION}: condition nosuchrule is unknown")
    }

    @Test
    fun aCheckCannotStandInForAConditionOrTheOtherWayRound() {
        assertProblem(section.editTemplate(GRANT) { it.copy(rules = listOf(SyntheticPart2Templates.ON_RATION)) }, "template $GRANT: rule on_ration is not a check")
        assertProblem(section.editTemplate(GRANT) { it.copy(links = listOf(it.links[0].copy(condition = SyntheticPart2Templates.CAP_DAYS)) + it.links.drop(1)) }, "condition cap_days is not a condition")
    }

    @Test
    fun aTemplateCannotRepeatARuleOrExceedTheRuleLimit() {
        assertProblem(section.editTemplate(GRANT) { it.copy(rules = listOf("cap_days", "cap_days")) }, "template $GRANT: rule cap_days repeats")
        assertProblem(section.editTemplate(GRANT) { it.copy(rules = listOf("cap_days", "grant_quoted", "a", "b", "c")) }, "template $GRANT: has 5 rules, max 4")
    }

    @Test
    fun aLinkNeedsAManualPageOrThePracticeLabelAndNeverBoth() {
        fun link(edit: (Part2Link) -> Part2Link) = section.editTemplate(GRANT) { it.copy(links = it.links.mapIndexed { i, l -> if (i == 0) edit(l) else l }) }
        assertProblem(link { it.copy(basis = "hearsay") }, "link to ${SyntheticPart2Templates.RATION}: basis must be manual or practice")
        assertProblem(link { it.copy(page = null) }, "link to ${SyntheticPart2Templates.RATION}: a manual link needs a page")
        assertProblem(link { it.copy(basis = "practice", page = 12) }, "link to ${SyntheticPart2Templates.RATION}: a practice link cites no page")
    }

    @Test
    fun aLinkToAnUnknownCodeOrToItselfIsRefused() {
        fun link(code: String) = section.editTemplate(GRANT) { it.copy(links = listOf(it.links[1].copy(code = code))) }
        assertProblem(link("NOSUCHCODE"), "template $GRANT: link to NOSUCHCODE: unknown code")
        assertProblem(link(GRANT), "template $GRANT: link to $GRANT: a template cannot link to itself")
    }

    @Test
    fun angleBracketsAreRefusedInEveryShippedText() {
        for (field in listOf("narrative", "authority", "certificate")) {
            val edited =
                section.editTemplate(NOTE) {
                    val bad = "Noted <b>{noted_on}</b> {remark}"
                    when (field) {
                        "narrative" -> it.copy(narrative = bad)
                        "authority" -> it.copy(authority = bad)
                        else -> it.copy(certificate = bad)
                    }
                }
            assertProblem(edited, "template $NOTE: $field has angle brackets (plain text only)")
        }
        assertProblem(section.editTemplate(NOTE) { it.copy(slots = listOf(it.slots[0], it.slots[1].copy(label = "<i>Remark</i>"))) }, "slot remark: label has angle brackets")
        assertProblem(section.editTemplate(NOTE) { it.copy(slots = listOf(it.slots[0], it.slots[1].copy(help = "<script>"))) }, "slot remark: help has angle brackets")
    }

    @Test
    fun anEmptyOrOverLongTextIsRefusedUsingTheShippedLimits() {
        val limits = SyntheticPart2Templates.limits
        assertProblem(section.editTemplate(NOTE) { it.copy(narrative = " ") }, "template $NOTE: narrative is empty")
        assertProblem(section.editTemplate(NOTE) { it.copy(narrative = "{noted_on} {remark} " + "x".repeat(limits.narrative)) }, "template $NOTE: narrative is over ${limits.narrative} characters")
        assertProblem(section.editTemplate(NOTE) { it.copy(slots = listOf(it.slots[0], it.slots[1].copy(help = "x".repeat(limits.help + 1)))) }, "slot remark: help is over ${limits.help} characters")
        assertProblem(section.editTemplate(NOTE) { it.copy(slots = listOf(it.slots[0], it.slots[1].copy(label = ""))) }, "slot remark: label is empty")
    }

    @Test
    fun aPlaceholderMustBeClosedAndNamed() {
        for (bad in listOf("Noted {noted_on", "Noted noted_on} {remark}", "Noted {} {remark}", "Noted {{noted_on}} {remark}")) {
            assertProblem(section.editTemplate(NOTE) { it.copy(narrative = bad) }, "template $NOTE: narrative has a malformed placeholder")
        }
    }

    @Test
    fun aTemplateCannotExceedTheSlotAndLinkLimits() {
        val limits = SyntheticPart2Templates.limits
        val many = List(limits.slots + 1) { Part2Slot("s$it", "text", "L", "H") }
        assertProblem(section.editTemplate(NOTE) { it.copy(slots = many) }, "template $NOTE: has ${limits.slots + 1} slots, max ${limits.slots}")
        val links = List(limits.links + 1) { Part2Link(SyntheticPart2Templates.STOP, "practice") }
        assertProblem(section.editTemplate(NOTE) { it.copy(links = links) }, "template $NOTE: has ${limits.links + 1} links, max ${limits.links}")
    }

    @Test
    fun aTemplateWhoseWordsChangedWithoutItsRevisionChangingIsRefused() {
        val edited = section.editTemplate(NOTE, keepRev = true) { it.copy(narrative = it.narrative + " and more") }
        assertProblem(edited, "template $NOTE: rev is not the revision of this template's words")
        assertEquals(emptyList(), problems(section.editTemplate(NOTE) { it.copy(narrative = it.narrative + " and more") }), "with its revision recomputed the same edit is fine")
        assertTrue(problems(edited).none { "and more" in it }, "the problem names the code, never the words")
    }

    @Test
    fun aRevisionOfTheWrongShapeIsReportedOnceAsAShapeProblem() {
        val found = problems(section.editTemplate(NOTE, keepRev = true) { it.copy(rev = "zz") })
        assertEquals(1, found.count { "template $NOTE: rev" in it }, "$found")
    }

    @Test
    fun theRevisionIsEightHexCharacters() {
        for (bad in listOf("", "a000000", "a00000001", "A0000001", "g0000001")) {
            assertProblem(section.editTemplate(NOTE, keepRev = true) { it.copy(rev = bad) }, "template $NOTE: rev must be 8 lowercase hex characters")
        }
    }

    @Test
    fun bothSeveritiesAreAcceptedAndNothingElse() {
        assertEquals(emptyList(), problems(section.editRule(SyntheticPart2Templates.GRANT_QUOTED) { it.copy(severity = "warn") }))
        assertProblem(section.editRule(SyntheticPart2Templates.CAP_DAYS) { it.copy(severity = "error") }, "rule cap_days: a check needs severity warn or ack")
        assertProblem(section.editRule(SyntheticPart2Templates.CAP_DAYS) { it.copy(severity = null) }, "rule cap_days: a check needs severity warn or ack")
        assertProblem(section.editRule(SyntheticPart2Templates.ON_RATION) { it.copy(severity = "warn") }, "rule on_ration: a condition has no severity")
    }

    @Test
    fun aManualRuleCitesAPageAndAPracticeRuleCitesNone() {
        assertProblem(section.editRule(SyntheticPart2Templates.CAP_DAYS) { it.copy(page = null) }, "rule cap_days: a manual rule needs a page")
        assertProblem(section.editRule(SyntheticPart2Templates.IN_HILL_STATION) { it.copy(page = 9) }, "rule in_hill_station: a practice rule cites no page")
        assertProblem(section.editRule(SyntheticPart2Templates.CAP_DAYS) { it.copy(basis = "hearsay") }, "rule cap_days: basis must be manual or practice")
    }

    @Test
    fun ruleIdsAreUniqueAndWellFormedAndTheKindIsKnown() {
        assertProblem(section.editRule(SyntheticPart2Templates.GRANT_QUOTED) { it.copy(id = "cap_days") }, "duplicate rule id cap_days")
        assertProblem(section.editRule(SyntheticPart2Templates.GRANT_QUOTED) { it.copy(id = "Bad-Id") }, "rule Bad-Id: id must be lowercase letters, digits and underscores")
        assertProblem(section.editRule(SyntheticPart2Templates.CAP_DAYS) { it.copy(kind = "hint") }, "rule cap_days: kind must be check or condition")
    }

    @Test
    fun aNoteIsPlainBoundedText() {
        val limit = SyntheticPart2Templates.limits.note
        assertProblem(section.editRule(SyntheticPart2Templates.CAP_DAYS) { it.copy(note = "x".repeat(limit + 1)) }, "rule cap_days: note is over $limit characters")
        assertProblem(section.editRule(SyntheticPart2Templates.CAP_DAYS) { it.copy(note = "<2>") }, "rule cap_days: note has angle brackets")
    }

    @Test
    fun aProblemNeverQuotesTheTemplatesWords() {
        // why: the messages are for tests and debugging and must never be logged or reported, so they must not carry content.
        val found = problems(section.editTemplate(CLAIM) { it.copy(narrative = "Secret words <b>{claim_on}</b>") })
        assertTrue(found.isNotEmpty() && found.none { "Secret" in it }, "$found")
    }
}
