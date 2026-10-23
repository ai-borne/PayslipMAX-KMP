package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.model.Part2Case
import com.payslipmax.pdfparser.guide.model.Part2CodePage
import com.payslipmax.pdfparser.guide.model.Part2Lookup
import com.payslipmax.pdfparser.guide.model.Part2Ribbon

/**
 * An invented Part II lookup shaped for the browse screens (Part II plan, phase 4a): a built Leave ribbon, a built Records ribbon (lookup
 * by design), and a Career ribbon that is NOT built, so its tiles stay hidden while its codes stay searchable. It reuses
 * [SyntheticGuidePart2]'s groups and limits and [SyntheticPart2Templates]' REJOINAL template (printed as REJOIN), so an edit to the real
 * manual data never breaks a screen test. Fixed points the tests lean on:
 * - [VARIANT_SERIAL] carries two codes; [NO_GROUP_SERIAL] prints no group; [REJOIN_SERIAL] is the one code the slip prints differently.
 * - `leave_one` homes 1.1 and 1.2 and lists 3.1 under "also"; `leave_two` homes 2.1, 3.1 and 5.23, and marks 3.1 lookup-only.
 * - [LATER_CODE] is homed in the unbuilt `career_one`.
 */
object SyntheticPart2Lookup {
    const val VARIANT_SERIAL = SyntheticGuidePart2.VARIANT_SERIAL
    const val NO_GROUP_SERIAL = SyntheticGuidePart2.NO_GROUP_SERIAL
    const val REJOIN_SERIAL = "5.23"
    const val LATER_SERIAL = "4.1"
    const val LATER_CODE = "LATERCODE"

    private val extraPages =
        listOf(
            Part2CodePage(REJOIN_SERIAL, 5, listOf(SyntheticPart2Templates.REJOIN_ALL), "C", "Rejoined from the invented leave", 131),
            Part2CodePage(LATER_SERIAL, 4, listOf(LATER_CODE), "C", "Granted a later thing", 109),
            Part2CodePage("4.2", 4, listOf("LATERTWO"), "A", "Stopped a later thing", 110),
        )

    private val base = SyntheticGuidePart2.lookup

    val lookup: Part2Lookup =
        base.copy(
            serials = base.serials + extraPages,
            ribbons =
                listOf(
                    Part2Ribbon(
                        "leave",
                        "The first ribbon",
                        true,
                        listOf(
                            Part2Case("leave_one", "First case", listOf("1.1", "1.2"), also = listOf("3.1")),
                            Part2Case("leave_two", "Second case", listOf("2.1", "3.1", REJOIN_SERIAL), lookup = listOf("3.1")),
                        ),
                    ),
                    Part2Ribbon(
                        "records",
                        "The second ribbon",
                        true,
                        listOf(Part2Case("records_one", "Third case", listOf(VARIANT_SERIAL, NO_GROUP_SERIAL), also = listOf("1.1"), lookup = listOf(VARIANT_SERIAL, NO_GROUP_SERIAL))),
                    ),
                    Part2Ribbon("career", "The third ribbon", false, listOf(Part2Case("career_one", "Fourth case", listOf(LATER_SERIAL, "4.2")))),
                ),
            templateLimits = SyntheticPart2Templates.limits,
            rules = SyntheticPart2Templates.rules,
            templates = SyntheticPart2Templates.templates.filter { it.code == SyntheticPart2Templates.REJOIN_ALL },
        )

    /** [SyntheticGuidePart2.bundle] with this lookup attached. */
    fun bundle(): GuideBundle = SyntheticGuidePart2.bundle().copy(part2 = lookup)
}
