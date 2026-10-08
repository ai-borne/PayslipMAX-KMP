package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.crypto.ContextHolder
import com.payslipmax.pdfparser.guide.domain.GuidePins
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class GuidePinsStorageAndroidTest {
    @After
    fun tearDown() {
        ContextHolder.context = null
    }

    @Test
    fun pinsSurviveANewStorageObjectOnTheSameDevice() {
        ContextHolder.context = RuntimeEnvironment.getApplication()
        AndroidGuidePinsStorage().save(GuidePins.Empty.toggle("RB-A").toggle("RB-B"))

        assertEquals(listOf("RB-B", "RB-A"), AndroidGuidePinsStorage().load().newestFirst)
    }

    @Test
    fun withoutAContextThereAreNoPinsAndSavingDoesNotCrash() {
        ContextHolder.context = null
        val storage = AndroidGuidePinsStorage()

        storage.save(GuidePins.Empty.toggle("RB-A"))

        assertTrue(storage.load().newestFirst.isEmpty())
    }

    @Test
    fun theStoredValueIsPlainIdsUnderOneKeyWithNothingElse() {
        val context = RuntimeEnvironment.getApplication()
        ContextHolder.context = context
        AndroidGuidePinsStorage().save(GuidePins.Empty.toggle("RB-A"))

        val prefs = context.getSharedPreferences("payslipmax_guide_prefs", android.content.Context.MODE_PRIVATE)

        assertEquals(mapOf(GUIDE_PINS_KEY to "RB-A"), prefs.all)
    }
}
