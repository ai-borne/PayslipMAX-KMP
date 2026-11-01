package com.payslipmax.pdfparser.ui.screens.guide.part2.orders

import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.ui.theme.OrderStrings
import kotlinx.coroutines.CancellationException

/**
 * The vault's size, or 0 when the store cannot be read. The lists beside it swallow the same failures through Flow's `.catch`, which takes
 * any Throwable: a screen shows an unreadable store as empty and never crashes. Cancellation still propagates.
 */
internal suspend fun Part2Repository.vaultBytesOrZero(): Long =
    try {
        vaultBytes()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        0L
    }

/** The one place that words the vault's size, for Settings (always) and the Bank (only past the warning). Whole megabytes, rounded up. */
internal object VaultSize {
    private const val MB = 1024L * 1024

    fun usage(bytes: Long): String = OrderStrings.sizeUsage(megabytes(bytes), megabytes(Part2Limits.VAULT_BYTES_MAX))

    fun warning(bytes: Long): String? = if (bytes > Part2Limits.VAULT_WARN_BYTES) OrderStrings.sizeWarning(megabytes(bytes), megabytes(Part2Limits.VAULT_BYTES_MAX)) else null

    /** Rounded up, so a vault just over the line reads one more and never "80 MB" beside a warning. */
    private fun megabytes(bytes: Long): Long = (bytes + MB - 1) / MB
}
