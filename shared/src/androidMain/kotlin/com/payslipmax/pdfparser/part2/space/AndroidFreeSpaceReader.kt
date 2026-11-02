package com.payslipmax.pdfparser.part2.space

import com.payslipmax.pdfparser.crypto.ContextHolder
import java.io.File

/**
 * Free bytes the app may use on the volume of [dir] (its private files). `File.usableSpace` is the platform's own statvfs figure for this app;
 * a directory that is not there reads as unknown, not as zero, so a missing folder never locks the officer out.
 */
class AndroidFreeSpaceReader(
    private val dir: () -> File? = { ContextHolder.context?.filesDir },
) : FreeSpaceReader {
    override fun freeBytes(): Long? = dir()?.takeIf { it.exists() }?.usableSpace
}

actual fun provideFreeSpaceReader(): FreeSpaceReader = AndroidFreeSpaceReader()
