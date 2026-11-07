package com.payslipmax.pdfparser.repository

import kotlin.coroutines.cancellation.CancellationException

/**
 * Hands one scanned order page, as the officer asked, to the platform share sheet so they can open it in the gallery or save it to Files or
 * Drive. Only the officer's own tap on a page reaches this; the app sends nothing anywhere itself. The file is a plain JPEG in the app's
 * private cache or temporary directory under one fixed name (the share sheet cannot say when its target has finished reading on Android,
 * so it stays until the next share or launch, as a backup export does). A failure removes it at once.
 */
interface PageShare {
    /** Writes [jpeg] to the private file and opens the share sheet on it. The caller zeroes [jpeg] afterwards; nothing here keeps it. */
    fun share(jpeg: ByteArray): Result<Unit>

    /** Deletes a page file a share sheet or a killed process left behind. */
    fun sweep()
}

/** The one name the shared page has on every platform; it is also the name the share target sees. */
const val PAGE_SHARE_FILE_NAME = "PayslipMax-order-page.jpg"

/** Any trouble with the page file or the share sheet; the cause is dropped on purpose because an I/O message can carry a path. */
class PageShareException : Exception("The page could not be shared.")

expect fun providePageShare(): PageShare

/** The rules every platform shares; a platform supplies only where the file is and how to write, present and delete it. */
abstract class FilePageShare : PageShare {
    protected abstract val path: String

    protected abstract fun write(jpeg: ByteArray)

    protected abstract fun present(path: String)

    protected abstract fun delete(path: String)

    override fun share(jpeg: ByteArray): Result<Unit> {
        sweep()
        return try {
            write(jpeg)
            present(path)
            Result.success(Unit)
        } catch (e: CancellationException) {
            sweep()
            throw e
        } catch (e: Exception) {
            sweep()
            Result.failure(PageShareException())
        }
    }

    override fun sweep() {
        runCatching { delete(path) }
    }
}
