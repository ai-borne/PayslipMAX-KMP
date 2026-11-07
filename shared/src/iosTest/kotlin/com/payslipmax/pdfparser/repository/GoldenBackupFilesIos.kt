package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.toByteArray
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.dataWithContentsOfFile

// The simulator test binary runs on the host, so it reads the committed files at the path the build generated.
internal actual fun readGoldenBackupFile(name: String): ByteArray =
    (NSData.dataWithContentsOfFile("$GOLDEN_BACKUP_DIR/$name") ?: error("No golden backup file named $name")).toByteArray()

internal actual fun goldenBackupFileNames(): List<String> =
    (NSFileManager.defaultManager.contentsOfDirectoryAtPath(GOLDEN_BACKUP_DIR, null) as List<*>?).orEmpty().map { it as String }.sorted()
