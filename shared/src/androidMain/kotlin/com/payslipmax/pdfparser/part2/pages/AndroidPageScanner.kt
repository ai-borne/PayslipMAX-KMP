package com.payslipmax.pdfparser.part2.pages

import com.payslipmax.pdfparser.part2.domain.Part2Limits
import kotlinx.coroutines.CancellationException
import java.io.File
import java.io.IOException
import java.nio.file.Files

/** What the platform scanner reported: the files it wrote, that the officer closed it, or that it could not run. */
sealed interface SourceResult {
    class Files(val paths: List<String>) : SourceResult

    data object Cancelled : SourceResult

    class Failed(val reason: ScanFailure) : SourceResult
}

/** Opens the platform's scanner (ML Kit's, in production) and reports what it wrote. */
fun interface ScanSource {
    suspend fun scan(maxPages: Int): SourceResult
}

/**
 * The Android [PageScanner]: turns the files a [ScanSource] reports into pages and owns their deletion. The scanner writes plain JPEG files
 * under [scanDir] in the app's private cache and never removes them, so this class deletes every file the scanner left: each page when it
 * is done ([ScannedPage.discard]), and all of them before a scan (a killed process), on cancel, on failure and when the officer walks
 * away. A path outside [scanDir] (after resolving `..` and links) is never read or deleted, whatever the source reports.
 */
class AndroidPageScanner(
    private val scanDir: File,
    private val source: ScanSource,
) : PageScanner {
    override suspend fun scan(maxPages: Int): PageScan {
        sweep()
        var handedOver = false
        try {
            val reported =
                try {
                    source.scan(maxPages)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    return PageScan.Failed(ScanFailure.FAILED)
                }
            return when (reported) {
                SourceResult.Cancelled -> PageScan.Cancelled
                is SourceResult.Failed -> PageScan.Failed(reported.reason)
                is SourceResult.Files -> pagesOf(reported.paths, maxPages).also { handedOver = it is PageScan.Pages }
            }
        } finally {
            if (!handedOver) sweep()
        }
    }

    override fun discardLeftovers() = sweep()

    private fun pagesOf(
        paths: List<String>,
        maxPages: Int,
    ): PageScan {
        val ours = paths.mapNotNull { ownFile(it) }
        val kept = ours.take(maxPages)
        ours.drop(maxPages).forEach { it.delete() }
        // No page at all is a closed scanner; pages that were all refused (not ours to read) are a scanner we cannot use.
        if (kept.isEmpty()) return if (paths.isEmpty()) PageScan.Cancelled else PageScan.Failed(ScanFailure.FAILED)
        return PageScan.Pages(kept.map { FileScannedPage(it) }, dropped = paths.size - kept.size)
    }

    /** The file as the scanner's own, or null: it must resolve (links and `..` included) to a regular file inside [scanDir]. */
    private fun ownFile(path: String): File? {
        val folder = scanDir.canonicalFile
        val file = File(path).canonicalFile
        return file.takeIf { it.isFile && it.parentFile == folder }
    }

    /** Deletes everything in the scanner's folder; a link is removed, never followed. */
    private fun sweep() {
        for (entry in scanDir.listFiles().orEmpty()) {
            if (Files.isSymbolicLink(entry.toPath()) || entry.isFile) entry.delete() else entry.deleteRecursively()
        }
    }
}

/** A page that is a file the scanner wrote. It is not a data class, so its path and content never reach a string. */
internal class FileScannedPage(private val file: File) : ScannedPage {
    override fun read(): ByteArray? =
        try {
            if (file.length() <= Part2Limits.PAGE_RAW_MAX_BYTES) file.readBytes() else null
        } catch (e: IOException) {
            null
        }

    override fun discard() {
        file.delete()
    }
}
