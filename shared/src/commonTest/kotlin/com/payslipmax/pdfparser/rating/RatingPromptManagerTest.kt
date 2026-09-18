package com.payslipmax.pdfparser.rating

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RatingPromptManagerTest {
    private class FakeRatingPromptStorage(
        private var cleanSuccessCount: Int = 0,
        private var lastPromptTimestampMs: Long? = null,
        private var hasEverPrompted: Boolean = false,
    ) : RatingPromptStorage {
        override fun getCleanSuccessCount(): Int = cleanSuccessCount

        override fun saveCleanSuccessCount(count: Int) {
            cleanSuccessCount = count
        }

        override fun getLastPromptTimestampMs(): Long? = lastPromptTimestampMs

        override fun saveLastPromptTimestampMs(timestampMs: Long) {
            lastPromptTimestampMs = timestampMs
        }

        override fun getHasEverPrompted(): Boolean = hasEverPrompted

        override fun saveHasEverPrompted(hasPrompted: Boolean) {
            hasEverPrompted = hasPrompted
        }
    }

    private class FakeClock(
        var nowMs: Long = 0L,
    ) {
        fun get(): Long = nowMs
    }

    @Test
    fun noPromptBeforeThirdCleanSuccess() {
        val storage = FakeRatingPromptStorage()
        val clock = FakeClock()
        val manager = RatingPromptManager(storage, clock::get)

        assertFalse(manager.onCleanParseSuccess())
        assertFalse(manager.onCleanParseSuccess())
    }

    @Test
    fun promptsOnThirdCleanSuccess() {
        val storage = FakeRatingPromptStorage()
        val clock = FakeClock()
        val manager = RatingPromptManager(storage, clock::get)

        manager.onCleanParseSuccess()
        manager.onCleanParseSuccess()
        assertTrue(manager.onCleanParseSuccess())
    }

    @Test
    fun noRepeatPromptUntilTenMoreCleanSuccessesPastReset() {
        val storage = FakeRatingPromptStorage()
        val clock = FakeClock()
        val manager = RatingPromptManager(storage, clock::get)

        repeat(3) { manager.onCleanParseSuccess() }
        clock.nowMs += RatingPromptManager.COOLDOWN_MS + 1

        repeat(9) { assertFalse(manager.onCleanParseSuccess()) }
        assertTrue(manager.onCleanParseSuccess())
    }

    @Test
    fun cooldownBlocksPromptEvenWhenCountThresholdMet() {
        val storage = FakeRatingPromptStorage()
        val clock = FakeClock()
        val manager = RatingPromptManager(storage, clock::get)

        repeat(3) { manager.onCleanParseSuccess() }
        clock.nowMs += 1

        // 10 more calls reach the repeat-prompt count threshold, but the cooldown (only 1ms
        // elapsed since the last attempt) must still block every one of them, including the 10th.
        repeat(10) { assertFalse(manager.onCleanParseSuccess()) }
    }

    @Test
    fun cooldownExpiryAndCountThresholdTogetherAllowPrompt() {
        val storage = FakeRatingPromptStorage()
        val clock = FakeClock()
        val manager = RatingPromptManager(storage, clock::get)

        repeat(3) { manager.onCleanParseSuccess() }
        repeat(9) { manager.onCleanParseSuccess() }
        clock.nowMs += RatingPromptManager.COOLDOWN_MS + 1

        assertTrue(manager.onCleanParseSuccess())
    }

    @Test
    fun statePersistsAcrossManagerInstancesSharingTheSameStorage() {
        val storage = FakeRatingPromptStorage()
        val clock = FakeClock()
        val firstManager = RatingPromptManager(storage, clock::get)

        firstManager.onCleanParseSuccess()
        firstManager.onCleanParseSuccess()

        val secondManager = RatingPromptManager(storage, clock::get)
        assertTrue(secondManager.onCleanParseSuccess())
    }
}
