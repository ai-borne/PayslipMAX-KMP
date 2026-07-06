package com.payslipmax.pdfparser.insights.gemma

import java.io.File

actual fun gemmaModelStorageDir(): String {
    return try {
        val context =
            Class.forName("android.app.ActivityThread")
                .getMethod("currentApplication")
                .invoke(null) as? android.content.Context
        context?.filesDir?.absolutePath ?: ""
    } catch (e: Throwable) {
        ""
    }
}

actual fun fileExistsAt(path: String): Boolean = path.isNotEmpty() && File(path).exists()

actual fun resolveInstalledGemmaModelPath(): String? = null
