package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import com.payslipmax.pdfparser.guide.data.GuideJson
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.model.Part2Lookup
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * The whole chain on the JSON the shipping tools write: invented templates go through the real Python tools (`part2_synthetic.py`
 * calls `part2_templates.for_bundle`), into a bundle, through [GuideBundleParser] and its validators, and into the engine, which
 * produces a slip. A second test proves the Kotlin fixture the unit tests use is the same content, so the two cannot drift.
 */
class Part2EngineIntegrationTest {
    private fun sectionText(): String =
        requireNotNull(javaClass.getResourceAsStream("/part2/synthetic_section.json")) { "run part2_synthetic.py" }.bufferedReader().readText()

    private fun bundleWithSection(): String {
        val synthetic = GuideJson.parseToJsonElement(SyntheticGuideBundle.JSON).jsonObject
        val section = GuideJson.parseToJsonElement(sectionText())
        val merged: JsonObject =
            buildJsonObject {
                synthetic.forEach { (k, v) -> put(k, v) }
                put("part2", section)
            }
        return merged.toString()
    }

    private fun loaded(): GuideBundle = assertIs<GuideLoadResult.Loaded>(GuideBundleParser.parse(bundleWithSection())).bundle

    @Test
    fun theBundleTheToolsWroteLoadsAndPassesEveryValidator() {
        val part2 = assertNotNull(loaded().part2)
        assertEquals(8, part2.templates.size)
        assertEquals(4, part2.rules.size)
        assertNotNull(part2.templateLimits)
    }

    @Test
    fun theEngineDraftsASlipFromTheShippedJson() {
        val engine = Part2DraftEngine(assertNotNull(loaded().part2), SyntheticPart2Templates.registry())

        fun day(
            m: Int,
            d: Int,
        ) = SlotValue.DateValue(CalendarDate(2026, m, d))

        val values = mapOf("leave_from" to day(7, 5), "leave_to" to day(9, 4), "year" to SlotValue.NumberValue(2026), "away_from" to day(7, 1))
        val request = OccurrenceRequest(SyntheticPart2Templates.GRANT, values, setOf(SyntheticPart2Templates.ANSWER_RATION))
        val slip = assertIs<DraftResult.Drafted>(engine.draft(LeaveEventInput(listOf(request)))).slip
        val rows = slip.orders.single().occurrences
        assertEquals(listOf(SyntheticPart2Templates.RATION, SyntheticPart2Templates.GRANT), rows.map { it.code })
        assertEquals("Granted the first thing for 62 days in 2026 from 05/07/2026 to 04/09/2026", rows[1].narrative)
        assertEquals("Certified that the second thing is in order", rows[0].certificate)
        assertEquals(listOf(SyntheticPart2Templates.CAP_DAYS), slip.findings.map { it.ruleId })
    }

    @Test
    fun theKotlinFixtureTheUnitTestsUseIsTheSameContentAsTheShippedJson() {
        val shipped: Part2Lookup = assertNotNull(loaded().part2)
        val fixture = SyntheticPart2Templates.lookup
        assertEquals(fixture.templateLimits, shipped.templateLimits)
        assertEquals(fixture.rules, shipped.rules)
        assertEquals(fixture.serials, shipped.serials)
        assertEquals(fixture.templates.map { it.copy(rev = "") }, shipped.templates.map { it.copy(rev = "") }, "same templates, ignoring the revision the tool computes")
    }

    @Test
    fun theShippedRevisionsAreEightHexCharactersAndDistinct() {
        val revs = assertNotNull(loaded().part2).templates.map { it.rev }
        assertEquals(revs.size, revs.toSet().size)
        assert(revs.all { it.length == 8 && it.all { c -> c in '0'..'9' || c in 'a'..'f' } })
    }
}
