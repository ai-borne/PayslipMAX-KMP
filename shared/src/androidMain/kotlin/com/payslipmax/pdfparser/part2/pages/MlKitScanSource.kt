package com.payslipmax.pdfparser.part2.pages

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** What the scanner screen answered: [ok] is `RESULT_OK` (the officer confirmed), [data] carries the pages. */
class ScanActivityResult(val ok: Boolean, val data: Intent?)

/** Starts the scanner's intent sender and waits for its result: on Android only a screen can do this (`rememberPageScanner`). */
fun interface ScanLauncher {
    suspend fun launch(sender: IntentSender): ScanActivityResult
}

/**
 * ML Kit's Document Scanner (Google Play services; the scanner runs in Play services' process, so the app needs no camera permission and
 * no network once the module is on the device). Gallery import is off and pages come back as JPEG files in the app's cache, which
 * [AndroidPageScanner] reads and deletes. With no [activity] (the screen is gone) or without the Play services module it reports
 * [ScanFailure.UNAVAILABLE]. Nothing is logged. The two Play services calls are the defaults of [startIntent] and [pagesOf], so the
 * rest can be tested without Play services; the calls themselves were proved on a device (Phase 6a spike).
 */
class MlKitScanSource(
    private val activity: Activity?,
    private val launcher: ScanLauncher,
    private val startIntent: suspend (Activity, Int) -> IntentSender = { screen, maxPages -> GmsDocumentScanning.getClient(options(maxPages)).getStartScanIntent(screen).await() },
    private val pagesOf: (Intent?) -> List<Uri>? = { data -> GmsDocumentScanningResult.fromActivityResultIntent(data)?.pages?.map { it.imageUri } },
) : ScanSource {
    override suspend fun scan(maxPages: Int): SourceResult {
        val screen = activity ?: return SourceResult.Failed(ScanFailure.UNAVAILABLE)
        val sender =
            try {
                startIntent(screen, maxPages)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return SourceResult.Failed(ScanFailure.UNAVAILABLE)
            }
        val answer = launcher.launch(sender)
        if (!answer.ok) return SourceResult.Cancelled
        val uris = pagesOf(answer.data) ?: return SourceResult.Failed(ScanFailure.FAILED)
        return SourceResult.Files(pathsOf(uris))
    }

    companion object {
        /** The folder under the app's cache where ML Kit 16.0.0 writes pages (seen on the Pixel 9); a changed folder makes every scan fail, loudly. */
        const val OUTPUT_DIR = "mlkit_docscan_ui_client"

        /** Photos only (no PDF), no gallery import (DECISIONS Round 10), at most [maxPages] pages, with the scanner's cleaning on. */
        fun options(maxPages: Int): GmsDocumentScannerOptions =
            GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(false)
                .setPageLimit(maxPages)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build()

        /** Paths of the `file://` URIs, in order; any other kind of URI is left out and never read. */
        fun pathsOf(uris: List<Uri>): List<String> = uris.filter { it.scheme == "file" }.mapNotNull { it.path }
    }
}

private suspend fun <T> Task<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
    }
