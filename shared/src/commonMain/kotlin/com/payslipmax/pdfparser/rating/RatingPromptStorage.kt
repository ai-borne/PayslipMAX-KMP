package com.payslipmax.pdfparser.rating

/**
 * Persistence abstraction for in-app rating prompt eligibility state.
 */
interface RatingPromptStorage {
    fun getCleanSuccessCount(): Int

    fun saveCleanSuccessCount(count: Int)

    fun getLastPromptTimestampMs(): Long?

    fun saveLastPromptTimestampMs(timestampMs: Long)

    fun getHasEverPrompted(): Boolean

    fun saveHasEverPrompted(hasPrompted: Boolean)
}

/**
 * Expect function providing platform-specific persistent storage for [RatingPromptStorage].
 */
expect fun provideRatingPromptStorage(): RatingPromptStorage
