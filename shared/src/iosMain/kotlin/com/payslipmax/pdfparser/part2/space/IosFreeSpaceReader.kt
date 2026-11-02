package com.payslipmax.pdfparser.part2.space

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSNumber
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/**
 * Free bytes on the volume that holds the app's documents (where the encrypted store lives), from the file manager's file-system
 * attributes: the disk-space API the privacy manifest declares (`NSPrivacyAccessedAPICategoryDiskSpace`, reason E174.1, "check there is
 * enough space before writing a file"). An answer that is missing reads as unknown, not as zero.
 */
@OptIn(ExperimentalForeignApi::class)
class IosFreeSpaceReader(
    private val path: () -> String? = { NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).firstOrNull() as? String },
) : FreeSpaceReader {
    override fun freeBytes(): Long? {
        val where = path() ?: return null
        val attributes = NSFileManager.defaultManager.attributesOfFileSystemForPath(where, null) ?: return null
        return (attributes[NSFileSystemFreeSize] as? NSNumber)?.longLongValue
    }
}

actual fun provideFreeSpaceReader(): FreeSpaceReader = IosFreeSpaceReader()
