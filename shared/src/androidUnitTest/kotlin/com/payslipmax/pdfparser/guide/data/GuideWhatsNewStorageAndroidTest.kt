package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.crypto.ContextHolder
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class GuideWhatsNewStorageAndroidTest {
    @After
    fun tearDown() {
        ContextHolder.context = null
    }

    @Test
    fun theSeenDateSurvivesANewStorageObjectOnTheSameDevice() {
        ContextHolder.context = RuntimeEnvironment.getApplication()
        AndroidGuideWhatsNewStorage().save("2026-10-30")

        assertEquals("2026-10-30", AndroidGuideWhatsNewStorage().load())
    }

    @Test
    fun withoutAContextNothingIsSeenAndSavingDoesNotCrash() {
        ContextHolder.context = null
        val storage = AndroidGuideWhatsNewStorage()

        storage.save("2026-10-30")

        assertNull(storage.load())
    }

    @Test
    fun theStoredValueIsOneDateUnderOneKeyWithNothingElse() {
        val context = RuntimeEnvironment.getApplication()
        ContextHolder.context = context
        AndroidGuideWhatsNewStorage().save("2026-10-30")

        val prefs = context.getSharedPreferences("payslipmax_guide_prefs", android.content.Context.MODE_PRIVATE)

        assertEquals(mapOf(GUIDE_WHATS_NEW_SEEN_KEY to "2026-10-30"), prefs.all)
    }
}
