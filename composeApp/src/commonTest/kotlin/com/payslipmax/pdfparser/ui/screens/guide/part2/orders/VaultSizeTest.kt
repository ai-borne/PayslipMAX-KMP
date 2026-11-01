package com.payslipmax.pdfparser.ui.screens.guide.part2.orders

import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.testing.FakePart2Repository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** WHY: the officer must hear the vault is nearly full before a scan is refused, and the figure must never read as less than it is. */
class VaultSizeTest {
    private val mb = 1024L * 1024

    @Test
    fun theUsageLineRoundsUpSoItNeverUnderstates() {
        assertEquals("Order pages use 1 MB of 100 MB", VaultSize.usage(1))
        assertEquals("Order pages use 0 MB of 100 MB", VaultSize.usage(0))
        assertEquals("Order pages use 13 MB of 100 MB", VaultSize.usage(12 * mb + 1))
    }

    @Test
    fun noWarningUntilEightyPercentThenOneWithTheFigures() {
        assertNull(VaultSize.warning(Part2Limits.VAULT_WARN_BYTES))
        val text = VaultSize.warning(Part2Limits.VAULT_WARN_BYTES + 1)
        assertTrue(text != null && text.startsWith("Order pages use 81 MB of 100 MB"), text)
    }

    @Test
    fun theWarnThresholdIsEightyPercentOfTheCap() {
        assertEquals(80L * mb, Part2Limits.VAULT_WARN_BYTES)
    }

    @Test
    fun anUnreadableStoreReadsAsEmptyAndNeverCrashesAScreen() =
        runTest {
            val broken =
                object : Part2Repository by FakePart2Repository() {
                    override suspend fun vaultBytes(): Long = throw Error("no native library")
                }

            assertEquals(0L, broken.vaultBytesOrZero())
        }
}
