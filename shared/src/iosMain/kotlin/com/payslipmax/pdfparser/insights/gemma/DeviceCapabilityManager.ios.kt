package com.payslipmax.pdfparser.insights.gemma

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
actual class DeviceCapabilityManager actual constructor() {
    actual fun checkGemmaSupport(
        requiredRamMb: Long,
        requiredStorageMb: Long,
    ): GemmaSupportStatus {
        val totalRamMb = getTotalRamMb()
        if (totalRamMb in 1 until requiredRamMb) {
            return GemmaSupportStatus.InsufficientRam(requiredRamMb, totalRamMb)
        }

        val freeStorageMb = getFreeStorageMb()
        if (freeStorageMb in 0 until requiredStorageMb) {
            return GemmaSupportStatus.InsufficientStorage(requiredStorageMb, freeStorageMb)
        }

        return GemmaSupportStatus.Supported
    }

    private fun getTotalRamMb(): Long {
        return try {
            val bytes = NSProcessInfo.processInfo.physicalMemory
            (bytes / (1024uL * 1024uL)).toLong()
        } catch (e: Throwable) {
            6000L
        }
    }

    private fun getFreeStorageMb(): Long {
        return try {
            val fileManager = NSFileManager.defaultManager
            val paths = fileManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
            val docUrl = paths.firstOrNull() as? platform.Foundation.NSURL
            val path = docUrl?.path ?: return 10000L
            val attributes = fileManager.attributesOfFileSystemForPath(path, null)
            val freeSize = attributes?.get(NSFileSystemFreeSize) as? Long ?: return 10000L
            freeSize / (1024 * 1024)
        } catch (e: Throwable) {
            10000L
        }
    }

    actual companion object {
        actual fun isRamSufficientForGemma(
            totalRamMb: Long,
            requiredRamMb: Long,
        ): Boolean {
            return totalRamMb >= requiredRamMb
        }
    }
}
