package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.repository.BACKUP_PASSWORD
import com.payslipmax.pdfparser.repository.PayslipBackupService
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.SyntheticPart2Drafts
import kotlinx.coroutines.Dispatchers
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.time.TimeSource

/** Told when a phase starts and ends, so a platform test can read its own memory counter around exactly that phase. */
internal interface ScaleProbe {
    fun begin(phase: String)

    fun end(phase: String)

    object None : ScaleProbe {
        override fun begin(phase: String) = Unit

        override fun end(phase: String) = Unit
    }
}

/**
 * The scenario the plan asks to be measured: 120 synthetic pages of the largest allowed size (12 orders of 10), exported and restored
 * through the real service, real crypto and JSON. The platform tests (`Part2VaultScaleJvmTest`, `Part2VaultScaleIosTest`) wrap it
 * with a peak-memory reading and check the bounds; the numbers are recorded in the Phase 3 summary.
 */
internal class VaultScale(
    val pages: Int,
    val archiveBytes: Int,
    val exportMs: Long,
    val restoreMs: Long,
)

internal const val SCALE_ORDERS = 12

internal suspend fun runVaultScale(probe: ScaleProbe = ScaleProbe.None): VaultScale {
    val source = FakePayslipDao()
    val target = FakePayslipDao()
    val bytes = Part2Limits.PAGE_MAX_BYTES
    var seed = 0
    repeat(SCALE_ORDERS) { o ->
        source.putOrder(SyntheticPart2Drafts.order("order-$o"))
        repeat(Part2Limits.PAGES_PER_ORDER_MAX) { p -> source.putPage("page-$o-$p", "order-$o", p, SyntheticPart2Drafts.jpeg(bytes, seed++)) }
    }
    val sourceService = PayslipBackupService(source, Dispatchers.Unconfined, part2Backup = Part2Backup(source.part2))
    val targetService = PayslipBackupService(target, Dispatchers.Unconfined, part2Backup = Part2Backup(target.part2))

    probe.begin("export")
    val exportStart = TimeSource.Monotonic.markNow()
    val archive = sourceService.export(BACKUP_PASSWORD).getOrThrow()
    val exportMs = exportStart.elapsedNow().inWholeMilliseconds
    probe.end("export")

    probe.begin("restore")
    val restoreStart = TimeSource.Monotonic.markNow()
    targetService.restore(archive, BACKUP_PASSWORD, RestoreMode.REPLACE).getOrThrow()
    val restoreMs = restoreStart.elapsedNow().inWholeMilliseconds
    probe.end("restore")

    assertEquals(SCALE_ORDERS * Part2Limits.PAGES_PER_ORDER_MAX, target.part2.allPageRefs().size, "every page came back")
    val last = target.part2.page("page-${SCALE_ORDERS - 1}-9")!!.openPage(deviceKey)
    assertContentEquals(SyntheticPart2Drafts.jpeg(bytes, seed - 1), last, "and the last one is byte-identical")
    return VaultScale(SCALE_ORDERS * Part2Limits.PAGES_PER_ORDER_MAX, archive.size, exportMs, restoreMs)
}
