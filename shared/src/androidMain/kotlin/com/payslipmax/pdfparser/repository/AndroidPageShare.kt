package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.ContextHolder
import com.payslipmax.pdfparser.utils.shareImageFile
import java.io.File

/** The shared page in the app's private cache, under one fixed name; see [PageShare] for why it stays until the next share or launch. */
class AndroidPageShare(
    private val cacheDir: () -> File? = { ContextHolder.context?.cacheDir },
    private val share: (File) -> Unit = ::shareImageFile,
) : FilePageShare() {
    override val path: String get() = File(requireNotNull(cacheDir()), PAGE_SHARE_FILE_NAME).path

    override fun write(jpeg: ByteArray) = File(path).writeBytes(jpeg)

    override fun present(path: String) = share(File(path))

    override fun delete(path: String) {
        File(path).delete()
    }
}

actual fun providePageShare(): PageShare = AndroidPageShare()
