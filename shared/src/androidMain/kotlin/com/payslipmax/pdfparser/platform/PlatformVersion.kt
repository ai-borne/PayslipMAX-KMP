package com.payslipmax.pdfparser.platform

import android.content.pm.PackageManager
import com.payslipmax.pdfparser.crypto.ContextHolder

actual fun platformAppVersion(): String {
    val ctx = ContextHolder.context ?: return "unknown"
    return try {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "unknown"
    } catch (e: PackageManager.NameNotFoundException) {
        "unknown"
    }
}
