package com.payslipmax.pdfparser.part2.pages

import com.payslipmax.pdfparser.part2.domain.Part2Limits
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: the system scanner leaves each page as a plain file in the app's cache, and it never cleans up after itself (seen on the Pixel 9:
 * the page was still there after the scan ended). This class turns those files into pages and owns their deletion: the file goes when the
 * page is done, and whatever else the scanner left goes on cancel, on failure and before the next scan. It also never touches a path
 * outside the scanner's own folder, whatever the scanner reports.
 */
class AndroidPageScannerTest : ScanDirFixture() {
    @Test
    fun theFilesTheScannerReportsBecomePagesInOrder() =
        runTest {
            val result = scanner(writing("a.jpg" to byteArrayOf(1, 2), "b.jpg" to byteArrayOf(3))).scan(10)

            val pages = assertIs<PageScan.Pages>(result)
            assertContentEquals(byteArrayOf(1, 2), pages.pages[0].read())
            assertContentEquals(byteArrayOf(3), pages.pages[1].read())
            assertEquals(0, pages.dropped)
        }

    @Test
    fun discardingAPageDeletesItsFileOnlyAndTwiceIsHarmless() =
        runTest {
            val pages = assertIs<PageScan.Pages>(scanner(writing("a.jpg" to byteArrayOf(1), "b.jpg" to byteArrayOf(2))).scan(10))

            pages.pages[0].discard()
            pages.pages[0].discard()

            assertEquals(listOf("b.jpg"), left())
        }

    @Test
    fun aPageWhoseFileIsGoneReadsAsNull() =
        runTest {
            val pages = assertIs<PageScan.Pages>(scanner(writing("a.jpg" to byteArrayOf(1))).scan(10))
            file("a.jpg").delete()

            assertNull(pages.pages.single().read())
        }

    @Test
    fun aFileLargerThanTheReadCapIsNotReadIntoMemory() =
        runTest {
            val pages = assertIs<PageScan.Pages>(scanner(sized("big.jpg", Part2Limits.PAGE_RAW_MAX_BYTES + 1L)).scan(10))

            assertNull(pages.pages.single().read())
            pages.pages.single().discard()
            assertEquals(emptyList(), left())
        }

    @Test
    fun aFileExactlyAtTheReadCapIsRead() =
        runTest {
            val pages = assertIs<PageScan.Pages>(scanner(sized("cap.jpg", Part2Limits.PAGE_RAW_MAX_BYTES.toLong())).scan(10))

            assertEquals(Part2Limits.PAGE_RAW_MAX_BYTES, pages.pages.single().read()?.size)
        }

    @Test
    fun aPathOutsideTheScannersFolderIsNeverReadOrDeleted() =
        runTest {
            val outside = File(root, "precious.txt").also { it.writeBytes(byteArrayOf(9)) }
            val sibling = File(root, "mlkit_docscan_ui_client_evil").also { it.mkdirs() }.let { File(it, "x.jpg").also { f -> f.writeBytes(byteArrayOf(8)) } }

            val result = scanner { SourceResult.Files(listOf(outside.path, sibling.path, scanned("a.jpg", 1).path)) }.scan(10)

            val pages = assertIs<PageScan.Pages>(result)
            assertEquals(1, pages.pages.size)
            assertEquals(2, pages.dropped, "the scanner reported two pages we will not touch")
            pages.pages.forEach { it.discard() }
            assertTrue(outside.exists() && sibling.exists(), "files outside the scanner's folder survive a scan and its cleanup")
            scanner { SourceResult.Cancelled }.discardLeftovers()
            assertTrue(outside.exists() && sibling.exists())
        }

    @Test
    fun aPathThatClimbsOutOfTheFolderIsRefused() =
        runTest {
            val outside = File(root, "precious.txt").also { it.writeBytes(byteArrayOf(9)) }
            scanDir.mkdirs()

            val result = scanner { SourceResult.Files(listOf("${scanDir.path}/../precious.txt")) }.scan(10)

            assertEquals(ScanFailure.FAILED, assertIs<PageScan.Failed>(result, "pages came back but none was ours to read: that is a failure, not a closed scanner").reason)
            assertTrue(outside.exists())
        }

    @Test
    fun aSymlinkInTheFolderThatPointsOutsideIsRefusedAndTheTargetSurvives() =
        runTest {
            val outside = File(root, "precious.txt").also { it.writeBytes(byteArrayOf(9)) }

            val result =
                scanner {
                    scanDir.mkdirs()
                    val link = file("link.jpg")
                    Files.createSymbolicLink(link.toPath(), outside.toPath())
                    SourceResult.Files(listOf(link.path))
                }.scan(10)

            assertEquals(ScanFailure.FAILED, assertIs<PageScan.Failed>(result).reason)
            assertTrue(outside.exists(), "a link must not make us read or delete the file it points to")
            assertEquals(emptyList(), left(), "the link itself is removed")
        }

    @Test
    fun aDirectoryOrAMissingPathIsNotAPage() =
        runTest {
            val result =
                scanner {
                    scanDir.mkdirs()
                    SourceResult.Files(listOf(file("sub").also { it.mkdirs() }.path, file("nope.jpg").path))
                }.scan(10)

            assertEquals(ScanFailure.FAILED, assertIs<PageScan.Failed>(result).reason)
            assertEquals(emptyList(), left())
        }

    @Test
    fun morePagesThanAskedForAreDeletedAndCounted() =
        runTest {
            val pages = assertIs<PageScan.Pages>(scanner(writing(*(1..4).map { "p$it.jpg" to byteArrayOf(it.toByte()) }.toTypedArray())).scan(2))

            assertEquals(2, pages.pages.size)
            assertEquals(2, pages.dropped)
            assertEquals(listOf("p1.jpg", "p2.jpg"), left(), "the extras are deleted at once, the kept pages when they are done")
        }

    @Test
    fun noFilesMeansTheOfficerClosedTheScanner() =
        runTest {
            assertEquals(PageScan.Cancelled, scanner { SourceResult.Files(emptyList()) }.scan(10))
        }

    @Test
    fun aPageNeverPrintsItsPathOrItsContent() =
        runTest {
            val pages = assertIs<PageScan.Pages>(scanner(writing("MARKER_name.jpg" to byteArrayOf(1))).scan(10))

            assertFalse("MARKER" in pages.toString() + pages.pages.single().toString())
        }
}
