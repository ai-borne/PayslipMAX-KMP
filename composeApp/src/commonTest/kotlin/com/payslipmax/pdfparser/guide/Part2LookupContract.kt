package com.payslipmax.pdfparser.guide

import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.part2.domain.Part2Rules
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Contract between the shipped Part II lookup (`part2/codes.json` and `part2/nav.json`, as bundled; Part II plan, phase 1) and the
 * app, on the real bundle. It pins what must reach the app intact: all 303 ODP-2026 code pages with their groups, the two serials
 * whose group the manual leaves blank, the 10 ribbons in build order with every serial homed once, and which tiles are built. Run on
 * each platform (`GuideBundleAndroidContractTest`, `GuideLoaderIosPerfTest`).
 */
object Part2LookupContract {
    private val serialsPerChapter = listOf(19, 16, 102, 5, 30, 15, 13, 10, 20, 10, 21, 6, 34, 2)
    private val ribbonOrder = listOf("leave", "moves", "field", "career", "exit", "family", "health", "role", "depu", "records")

    fun assertPart2LookupReachesTheApp(bundle: GuideBundle) {
        val section = assertNotNull(bundle.part2, "the bundle ships the Part II lookup")
        assertEquals(1, section.version)
        assertEquals(303, section.serials.size, "every Annexure II serial has a code page")
        assertEquals(321, section.serials.sumOf { it.codes.size }, "8 variant serials carry 321 distinct codes in all")
        assertEquals(serialsPerChapter, (1..14).map { c -> section.serials.count { it.chapter == c } })
        assertEquals(setOf("A", "AM", "B", "C", "CM"), section.groups.keys)
        val byGroup = section.serials.groupingBy { it.group }.eachCount()
        assertEquals(mapOf<String?, Int>("A" to 204, "AM" to 23, "B" to 11, "C" to 53, "CM" to 10, null to 2), byGroup)
        assertEquals(setOf("3.28", "11.8"), section.serials.filter { it.group == null }.map { it.serial }.toSet(), "no group is ever inferred")
    }

    fun assertCodePagesCarryTheManualsOwnWords(bundle: GuideBundle) {
        val section = assertNotNull(bundle.part2)
        val byCode = section.serials.flatMap { page -> page.codes.map { it to page } }.toMap()
        assertEquals(321, byCode.size, "a code names exactly one page")
        val gtdal = byCode.getValue("GTDAL")
        assertEquals(Triple("5.1", "A", 114), Triple(gtdal.serial, gtdal.group, gtdal.page))
        assertEquals(listOf("REJOINAL"), section.serials.first { it.serial == "5.23" }.codes, "the manual's code; the slip prints REJOIN later")
        assertEquals(171, byCode.getValue("WTHDPAY").page, "9.7 is in the index and in Annexure III")
        assertEquals((1..5).map { "GTDTRAN$it" }, section.serials.first { it.serial == "3.53" }.codes)
    }

    fun assertRibbonsHomeEverySerialOnce(bundle: GuideBundle) {
        val section = assertNotNull(bundle.part2)
        assertEquals(ribbonOrder, section.ribbons.map { it.id })
        assertEquals(48, section.ribbons.sumOf { it.cases.size })
        val homed = section.ribbons.flatMap { r -> r.cases.flatMap { it.serials } }
        assertEquals(section.serials.map { it.serial }.sorted(), homed.sorted(), "each of the 303 serials has exactly one home")
        val leave = section.ribbons.first { it.id == "leave" }
        assertEquals((1..30).map { "5.$it" }.sorted(), leave.cases.flatMap { it.serials }.sorted(), "Leave homes all of Chapter 5")
    }

    fun assertOnlyLeaveAndRecordsAreBuiltAndFiftyThreeSerialsAreLookupOnly(bundle: GuideBundle) {
        val section = assertNotNull(bundle.part2)
        assertEquals(setOf("leave", "records"), section.ribbons.filter { it.built }.map { it.id }.toSet(), "tiles only for built ribbons (Round 8)")
        assertEquals(53, section.ribbons.sumOf { r -> r.cases.sumOf { it.lookup.size } })
        val cancelling = section.ribbons.flatMap { it.cases }.first { it.serials == listOf("14.1", "14.2") }
        assertTrue(cancelling.lookup.isEmpty(), "CANCEL and CANCELS are drafted with Leave, so they are not lookup-only")
    }

    /**
     * The rules the real bundle declares and the rules the app implements are one list (Part II plan, phases 2a and 2b): a template
     * citing a check the app does not run would be a silent hole, and the template limits must travel with the templates they bound.
     * Content batches 1 and 2 ship 24 rules (11 checks, 13 conditions) and 26 templates (24 and the two cancellation codes); this fails the day one side gains a rule and the
     * other does not, and the limits are those confirmed against the longest real text (570 characters of narrative, 36 of label,
     * 156 of help, 14 of note; 8 slots, 16 links, 2 rules on a template).
     */
    fun assertRulesMatchTheRegistry(bundle: GuideBundle) {
        val section = assertNotNull(bundle.part2)
        assertEquals(emptyList(), Part2Rules.shipped().problemsAgainst(section.rules), "bundle rule ids = registry ids, with the same kinds")
        assertEquals(24, section.rules.size)
        assertEquals(26, section.templates.size)
        assertEquals(
            listOf(600, 40, 160, 10, 16, 4, 20),
            assertNotNull(section.templateLimits, "the limits ship with the section").let { listOf(it.narrative, it.label, it.help, it.slots, it.links, it.rules, it.note) },
            "the agreed template limits (rules.json)",
        )
        val known = Part2Rules.shipped().ids
        assertTrue(section.templates.flatMap { it.rules }.all { it in known }, "every template cites only rules the app implements")
    }
}
