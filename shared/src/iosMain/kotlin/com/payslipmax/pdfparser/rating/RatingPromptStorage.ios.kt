package com.payslipmax.pdfparser.rating

import platform.Foundation.NSUserDefaults

class IosRatingPromptStorage : RatingPromptStorage {
    private val keyCleanSuccessCount = "clean_success_count"
    private val keyLastPromptTimestampMs = "last_prompt_timestamp_ms"
    private val keyHasEverPrompted = "has_ever_prompted"

    private val defaults get() = NSUserDefaults.standardUserDefaults

    override fun getCleanSuccessCount(): Int = defaults.integerForKey(keyCleanSuccessCount).toInt()

    override fun saveCleanSuccessCount(count: Int) {
        defaults.setInteger(count.toLong(), forKey = keyCleanSuccessCount)
    }

    override fun getLastPromptTimestampMs(): Long? {
        if (defaults.objectForKey(keyLastPromptTimestampMs) == null) return null
        return defaults.integerForKey(keyLastPromptTimestampMs)
    }

    override fun saveLastPromptTimestampMs(timestampMs: Long) {
        defaults.setInteger(timestampMs, forKey = keyLastPromptTimestampMs)
    }

    override fun getHasEverPrompted(): Boolean = defaults.boolForKey(keyHasEverPrompted)

    override fun saveHasEverPrompted(hasPrompted: Boolean) {
        defaults.setBool(hasPrompted, forKey = keyHasEverPrompted)
    }
}

actual fun provideRatingPromptStorage(): RatingPromptStorage = IosRatingPromptStorage()
