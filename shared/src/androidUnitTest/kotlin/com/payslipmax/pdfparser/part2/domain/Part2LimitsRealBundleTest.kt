package com.payslipmax.pdfparser.part2.domain

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: storage bounds a draft by counts that follow from the shipped templates (how many typed boxes one request can hold, how many rows
 * one request can print), and a bound that falls behind the content silently drops what the officer typed: phase 9 found the 16 boxes of a
 * request already full by the fifth box of the SCCIA re-grant, and the form then showed fields that could not be saved. This measures the
 * real bundle, so the next content batch that outgrows a limit fails here instead of in an officer's draft.
 */
class Part2LimitsRealBundleTest {
    private val section = RealPart2.section
    private val byCode = section.templates.associateBy { it.code }
    private val workedOut = setOf("days", "earliest", "latest", "next_day", "derived")

    /** Every template [code] can ever bring in, answers ignored. */
    private fun reachable(code: String): List<com.payslipmax.pdfparser.guide.model.Part2Template> {
        val seen = linkedSetOf<String>()
        val queue = ArrayDeque(listOf(code))
        while (queue.isNotEmpty()) {
            val next = queue.removeFirst()
            val template = byCode[next] ?: continue
            if (seen.add(next)) template.links.forEach { queue.addLast(it.code) }
        }
        return seen.map { byCode.getValue(it) }
    }

    private fun boxes(code: String): Set<String> = reachable(code).flatMap { it.slots }.filter { it.kind !in workedOut }.map { it.id }.toSet()

    @Test
    fun noRequestCanHaveMoreTypedBoxesThanOneRequestMayStoreAndTheMostFillsTheLimitExactly() {
        val most = section.templates.maxOf { boxes(it.code).size }
        assertEquals(28, most, "GTDAL with R&H, ration, HAA, SCCIA, field area and Transport Allowance")
        assertEquals(most, Part2Limits.VALUES_PER_REQUEST_MAX, "the limit is the longest real request, as the template limits are")
        for (t in section.templates) assertTrue(boxes(t.code).size <= Part2Limits.VALUES_PER_REQUEST_MAX, t.code)
    }

    @Test
    fun theAnswersOfARequestFitTheirLimitWithRoomToSpare() {
        // one per question the code can raise: ration, five areas, two NCC questions, the earlier-allowances one
        val questions = Part2Rules.shipped().questions
        assertTrue(questions.size <= Part2Limits.ANSWERS_PER_REQUEST_MAX, "even answering every question there is fits")
    }

    @Test
    fun theRowsOfAPrintedSnapshotFollowFromTheBundlesLinkLimitAndTheRequestLimit() {
        val links = checkNotNull(section.templateLimits).links
        assertEquals(Part2Limits.REQUESTS_MAX * (links + 1), Part2Limits.PRINTED_ROWS_MAX, "10 requests, each with at most the link limit of linked orders and itself")
        assertTrue(section.templates.all { 1 + it.links.size <= links + 1 })
    }
}
