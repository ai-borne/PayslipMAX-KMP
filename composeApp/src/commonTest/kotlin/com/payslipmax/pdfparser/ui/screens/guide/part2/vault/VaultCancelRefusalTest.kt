package com.payslipmax.pdfparser.ui.screens.guide.part2.vault

import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.CancelRefusal
import com.payslipmax.pdfparser.ui.theme.Part2CancelStrings
import com.payslipmax.pdfparser.ui.theme.Part2VaultStrings
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: a cancel that cannot start must say which thing is wrong, in words the officer can act on, never one vague sentence: a guide that has
 * not loaded is waited out, a missing number is typed, a missing draft means a hand-made corrected draft, a full device means freeing space.
 */
class VaultCancelRefusalTest {
    @Test
    fun everyRefusalHasItsOwnWords() {
        val expected =
            mapOf(
                CancelRefusal.NOT_READY to Part2CancelStrings.notReady,
                CancelRefusal.GONE to Part2VaultStrings.orderGone,
                CancelRefusal.NOT_QUOTABLE to Part2CancelStrings.notQuotable,
                CancelRefusal.NOTHING_TO_COPY to Part2CancelStrings.nothingToCopy,
                CancelRefusal.CANCEL_SLIP to Part2CancelStrings.cancelSlipSource,
                CancelRefusal.SAVE_FAILED to Part2CancelStrings.saveFailed,
            )

        assertEquals(CancelRefusal.entries.toSet(), expected.keys, "a new refusal needs words here")
        for ((why, words) in expected) assertEquals(words, VaultNotices.cancelRefusal(why), "$why")
        assertEquals(expected.size, expected.values.toSet().size, "no two refusals share a sentence")
    }
}
