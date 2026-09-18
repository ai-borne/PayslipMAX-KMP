package com.payslipmax.pdfparser.rating

/**
 * Decides when a clean payslip parse should trigger an OS-native "rate this app" prompt.
 *
 * Cadence: the 3rd clean parse is eligible for the first prompt attempt; after that, every 10
 * more clean successes since the last attempt is eligible again. Both the count threshold and a
 * 90-day cooldown since the last attempt must be satisfied. The clean-success counter resets each
 * time a prompt is actually attempted (i.e. when this returns `true`), not on every call.
 */
class RatingPromptManager(
    private val storage: RatingPromptStorage = provideRatingPromptStorage(),
    private val nowMs: () -> Long = ::currentTimeMillis,
) {
    /**
     * Call once per clean (needsReview == false) parse. Returns whether the caller should invoke
     * the platform review prompt now.
     */
    fun onCleanParseSuccess(): Boolean {
        val newCount = storage.getCleanSuccessCount() + 1
        storage.saveCleanSuccessCount(newCount)

        val threshold = if (storage.getHasEverPrompted()) REPEAT_PROMPT_THRESHOLD else FIRST_PROMPT_THRESHOLD
        if (newCount < threshold) return false

        val lastPromptMs = storage.getLastPromptTimestampMs()
        val cooldownSatisfied = lastPromptMs == null || (nowMs() - lastPromptMs) >= COOLDOWN_MS
        if (!cooldownSatisfied) return false

        storage.saveCleanSuccessCount(0)
        storage.saveLastPromptTimestampMs(nowMs())
        storage.saveHasEverPrompted(true)
        return true
    }

    companion object {
        const val FIRST_PROMPT_THRESHOLD = 3
        const val REPEAT_PROMPT_THRESHOLD = 10
        const val COOLDOWN_MS = 90L * 24 * 60 * 60 * 1000
    }
}
