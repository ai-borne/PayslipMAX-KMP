package com.payslipmax.pdfparser.rating

import com.payslipmax.pdfparser.crypto.ContextHolder
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class RatingPromptStorageAndroidTest {
    @After
    fun tearDown() {
        ContextHolder.context = null
    }

    @Test
    fun nullContextReturnsSafeDefaults() {
        ContextHolder.context = null
        val storage = AndroidRatingPromptStorage()

        assertEquals(0, storage.getCleanSuccessCount())
        assertNull(storage.getLastPromptTimestampMs())
        assertFalse(storage.getHasEverPrompted())
    }

    @Test
    fun realContextRoundTripsSaveAndRead() {
        ContextHolder.context = RuntimeEnvironment.getApplication()
        val storage = AndroidRatingPromptStorage()

        storage.saveCleanSuccessCount(3)
        storage.saveLastPromptTimestampMs(1_000L)
        storage.saveHasEverPrompted(true)

        assertEquals(3, storage.getCleanSuccessCount())
        assertEquals(1_000L, storage.getLastPromptTimestampMs())
        assertEquals(true, storage.getHasEverPrompted())
    }
}
