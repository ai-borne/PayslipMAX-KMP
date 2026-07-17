package com.payslipmax.pdfparser.insights.gemma

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs

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
            val context =
                Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication")
                    .invoke(null) as? Context
            if (context != null) {
                val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                val memInfo = ActivityManager.MemoryInfo()
                actManager?.getMemoryInfo(memInfo)
                if (memInfo.totalMem > 0) {
                    return memInfo.totalMem / (1024 * 1024)
                }
            }
            Runtime.getRuntime().maxMemory() / (1024 * 1024)
        } catch (e: Throwable) {
            8000L
        }
    }

    private fun getFreeStorageMb(): Long {
        return try {
            val path = Environment.getDataDirectory().path
            val stat = StatFs(path)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            availableBytes / (1024 * 1024)
        } catch (e: Throwable) {
            10000L // Default safe fallback for tests/emulators
        }
    }
}
