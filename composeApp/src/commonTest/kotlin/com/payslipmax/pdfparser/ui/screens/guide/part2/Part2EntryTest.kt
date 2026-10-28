package com.payslipmax.pdfparser.ui.screens.guide.part2

import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.testing.SyntheticPart2Lookup
import com.payslipmax.pdfparser.ui.screens.guide.GuideViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * WHY: the lookup is free for everyone (Round 5, 10) and absent while the dark-launch flag is off. So the entry has two states and no
 * Premium one, and a flag-off build must neither show the row nor ask Koin for the ViewModel.
 */
class Part2EntryTest {
    private val shipped = SyntheticPart2Lookup.bundle()
    private val old = SyntheticGuideBundle.withPart2().copy(part2 = null)

    @Test
    fun theEntryIsHiddenWhileTheFeatureIsOffWhateverTheBundleSays() {
        assertEquals(Part2Entry.HIDDEN, part2Entry(enabled = false, bundle = shipped))
        assertEquals(Part2Entry.HIDDEN, part2Entry(enabled = false, bundle = old))
    }

    @Test
    fun withTheFeatureOnTheLookupIsOpenToEveryone() {
        assertEquals(Part2Entry.OPEN, part2Entry(enabled = true, bundle = shipped))
        assertEquals(listOf(Part2Entry.HIDDEN, Part2Entry.OPEN), Part2Entry.entries, "no LOCKED state: free users read the lookup (Premium starts at drafting, 4b)")
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
        val shipped = part2FactoryOrNull { model().also { calls++ } }
        if (isPart2Enabled()) assertNotNull(shipped, "the shipped default follows the flag") else assertNull(shipped, "the shipped default follows the flag")
        assertEquals(0, calls, "so Koin is never asked for the ViewModel until the factory is called")
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
