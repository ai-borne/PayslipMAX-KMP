package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.data.InMemoryGuideWhatsNewStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY: the "new" banner must call attention exactly once per change-log entry. Before the user has seen anything it is new; once seen
 * (opened or dismissed) it is quiet, and it is new again only when a NEWER entry ships. A date the user has not reached must never be
 * hidden by an older seen date, and the seen date must survive a restart.
 */
class GuideWhatsNewSeenModelTest {
    @Test
    fun beforeAnythingIsSeenTheNewestEntryIsNew() {
        assertTrue(GuideWhatsNewSeenModel(InMemoryGuideWhatsNewStorage()).isNew("2026-10-30"))
    }

    @Test
    fun onceSeenTheSameEntryIsNotNewAgainEvenAfterARestart() {
        val storage = InMemoryGuideWhatsNewStorage()
        GuideWhatsNewSeenModel(storage).markSeen("2026-10-30")

        assertFalse(GuideWhatsNewSeenModel(storage).isNew("2026-10-30"))
    }

    @Test
    fun aNewerEntryIsNewAgainAfterAnOlderOneWasSeen() {
        val model = GuideWhatsNewSeenModel(InMemoryGuideWhatsNewStorage())
        model.markSeen("2026-10-30")

        assertTrue(model.isNew("2026-11-15"))
    }

    @Test
    fun seeingAnOlderEntryNeverMovesTheSeenDateBack() {
        val storage = InMemoryGuideWhatsNewStorage()
        val model = GuideWhatsNewSeenModel(storage)
        model.markSeen("2026-11-15")
        model.markSeen("2026-10-30")

        assertEquals("2026-11-15", storage.load())
        assertFalse(model.isNew("2026-11-15"))
    }

    @Test
    fun withNoEntryNothingIsNew() {
        assertFalse(GuideWhatsNewSeenModel(InMemoryGuideWhatsNewStorage()).isNew(null))
    }
}
