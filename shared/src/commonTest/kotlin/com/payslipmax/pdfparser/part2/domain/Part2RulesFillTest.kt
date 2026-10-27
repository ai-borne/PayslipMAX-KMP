package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.domain.LeaveEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

/** The fills are registered per code beside the questions and read through the registry, so a screen never names a code (plan, phase 8b). */
class Part2RulesFillTest {
    private class Fake(override val code: String) : LeaveBankFill {
        override val slots: Set<String> = emptySet()

        override fun fill(
            request: OccurrenceRequest,
            inputs: LeaveBankInputs,
        ) = FillOutcome.NothingKnown

        override fun notes(
            request: OccurrenceRequest,
            rows: List<LeaveEntry>,
        ) = emptyList<FillNote>()
    }

    @Test
    fun theShippedRegistryFillsTheYearEndCodeAndTheEncashmentAndNoOther() {
        val shipped = Part2Rules.shipped()
        assertEquals("ACCLVE", shipped.fill("ACCLVE")?.code)
        assertEquals("ENCASH", shipped.fill("ENCASH")?.code)
        assertNull(shipped.fill("GTDAL"))
        assertNull(shipped.fill(""))
    }

    @Test
    fun aFillIsFoundByItsCodeAndTwoForOneCodeAreRefused() {
        val one = Fake("AAA")
        assertSame(one, Part2Rules(emptyList(), emptyList(), listOf(one, Fake("BBB"))).fill("AAA"))
        assertFailsWith<IllegalArgumentException> { Part2Rules(emptyList(), emptyList(), listOf(Fake("AAA"), Fake("AAA"))) }
        assertNull(Part2Rules(emptyList()).fill("AAA"), "a registry built without fills has none")
    }
}
