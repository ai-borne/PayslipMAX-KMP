package com.payslipmax.pdfparser.ui.screens.guide.part2

import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.testing.SyntheticGuidePart2
import com.payslipmax.pdfparser.ui.screens.guide.GuideViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * WHY: Learn is free for everyone and absent while the dark-launch flag is off. So the entry has two states and no Premium one, and a
 * flag-off build must neither show the row nor ask Koin for the model.
 */
class Part2EntryTest {
    private val shipped = SyntheticGuidePart2.bundle()
    private val old = SyntheticGuideBundle.withPart2().copy(part2 = null)

    @Test
    fun theEntryIsHiddenWhileTheFeatureIsOffWhateverTheBundleSays() {
        assertEquals(Part2Entry.HIDDEN, part2Entry(enabled = false, bundle = shipped))
        assertEquals(Part2Entry.HIDDEN, part2Entry(enabled = false, bundle = old))
    }

    @Test
    fun withTheFeatureOnLearnIsOpenToEveryone() {
        assertEquals(Part2Entry.OPEN, part2Entry(enabled = true, bundle = shipped))
        assertEquals(listOf(Part2Entry.HIDDEN, Part2Entry.OPEN), Part2Entry.entries, "no LOCKED state: Learn is free (Premium starts at the Orders Bank)")
    }

    @Test
    fun aBundleWithNoPartTwoSectionLeavesNoDeadRow() {
        assertEquals(Part2Entry.HIDDEN, part2Entry(enabled = true, bundle = old))
    }

    private fun model() = Part2LookupViewModel(GuideViewModel(FakeGuideRepository(), FakeCrashReporter()))

    @Test
    fun theFactoryIsNullWhenOffAndNeverCalledThen() {
        var calls = 0
        assertNull(part2FactoryOrNull(enabled = false) { model().also { calls++ } })
        assertEquals(0, calls)
        val byDefault = part2FactoryOrNull { model().also { calls++ } }
        if (isPart2Enabled()) assertNotNull(byDefault, "the shipped default follows the flag") else assertNull(byDefault, "the shipped default follows the flag")
        assertEquals(0, calls, "so Koin is never asked for the model until the factory is called")
    }

    @Test
    fun theFactoryMakesTheModelOnlyWhenCalled() {
        var calls = 0
        val factory = assertNotNull(part2FactoryOrNull(enabled = true) { model().also { calls++ } })
        assertEquals(0, calls)
        factory()
        assertEquals(1, calls)
    }
}
