@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.payslipmax.pdfparser.insights.gemma

import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual fun gemmaModelStorageDir(): String = documentDirectory()

actual fun fileExistsAt(path: String): Boolean = path.isNotEmpty() && NSFileManager.defaultManager.fileExistsAtPath(path)

actual fun resolveInstalledGemmaModelPath(): String? = null

private fun documentDirectory(): String {
    val documentDirectory =
        NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
    return documentDirectory?.path ?: ""
}
