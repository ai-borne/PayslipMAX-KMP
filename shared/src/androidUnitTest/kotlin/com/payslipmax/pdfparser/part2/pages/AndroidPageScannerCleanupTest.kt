package com.payslipmax.pdfparser.part2.pages

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * WHY: the scanner never cleans up after itself, so this class must leave its folder empty at every exit: before a scan (a killed
 * process), on a cancel or a failure, when the officer walks away, and without ever following a link out of the folder.
 */
class AndroidPageScannerCleanupTest : ScanDirFixture() {
    @Test
    fun staleFilesFromAKilledScanAreDeletedBeforeTheNextScanStarts() =
        runTest {
            scanned("old1.jpg", 1)
            scanned("old2.jpg", 2)
            var leftWhenScannerOpened = emptyList<String>()

            scanner {
                leftWhenScannerOpened = left()
                SourceResult.Cancelled
            }.scan(10)

            assertEquals(emptyList(), leftWhenScannerOpened)
        }

    @Test
    fun aClosedScannerLeavesNothingBehind() =
        runTest {
            val result =
                scanner {
                    scanned("half.jpg", 1)
                    SourceResult.Cancelled
                }.scan(10)

            assertEquals(PageScan.Cancelled, result)
            assertEquals(emptyList(), left())
        }

    @Test
    fun aFailedScannerLeavesNothingBehindAndSaysWhy() =
        runTest {
            for (reason in ScanFailure.entries) {
                val result =
                    scanner {
                        scanned("half.jpg", 1)
                        SourceResult.Failed(reason)
                    }.scan(10)

                assertEquals(reason, assertIs<PageScan.Failed>(result).reason)
                assertEquals(emptyList(), left())
            }
        }

    @Test
    fun aScannerThatThrowsIsAFailureWithNoFileLeftAndNoMessageKept() =
        runTest {
            val result =
                scanner {
                    scanned("half.jpg", 1)
                    error("MARKER_BOOM with a path ${scanDir.path}")
                }.scan(10)

            val failed = assertIs<PageScan.Failed>(result)
            assertEquals(ScanFailure.FAILED, failed.reason)
            assertEquals(emptyList(), left())
        }

    @Test
    fun walkingAwayWhileTheScannerIsOpenStillCleansTheFolder() =
        runTest {
            val opened = CompletableDeferred<Unit>()
            val never = CompletableDeferred<Unit>()
            var outcome: Result<PageScan>? = null
            val job =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    outcome =
                        runCatching {
                            scanner {
                                scanned("half.jpg", 1)
                                opened.complete(Unit)
                                never.await()
                                SourceResult.Cancelled
                            }.scan(10)
                        }
                }
            opened.await()

            job.cancelAndJoin()

            assertEquals(emptyList(), left())
            assertTrue(outcome!!.exceptionOrNull() is kotlinx.coroutines.CancellationException, "a cancelled scan ends cancelled; it is not reported as a failed scanner: $outcome")
        }

    @Test
    fun cleaningUpNeverFollowsALinkToADirectoryOutsideTheFolder() =
        runTest {
            val outsideDir = File(root, "elsewhere").also { it.mkdirs() }
            val precious = File(outsideDir, "precious.txt").also { it.writeBytes(byteArrayOf(9)) }

            scanner {
                scanDir.mkdirs()
                Files.createSymbolicLink(File(scanDir, "dirlink").toPath(), outsideDir.toPath())
                SourceResult.Cancelled
            }.scan(10)
            assertEquals(emptyList(), left(), "the link itself is removed after the scan")
            assertTrue(precious.exists(), "a link to a directory must not make the cleanup walk into it")

            Files.createSymbolicLink(File(scanDir, "dirlink").toPath(), outsideDir.toPath())
            scanner { SourceResult.Cancelled }.discardLeftovers()
            assertTrue(precious.exists())
        }

    @Test
    fun discardLeftoversEmptiesTheFolderAndToleratesItNotExisting() {
        scanner { SourceResult.Cancelled }.discardLeftovers()
        scanned("a.jpg", 1)
        scanned("b.jpg", 2)

        scanner { SourceResult.Cancelled }.discardLeftovers()

        assertFalse(scanDir.listFiles().orEmpty().isNotEmpty())
    }
}
