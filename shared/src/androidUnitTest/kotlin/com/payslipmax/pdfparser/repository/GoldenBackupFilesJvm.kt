package com.payslipmax.pdfparser.repository

import java.io.File

internal actual fun readGoldenBackupFile(name: String): ByteArray = File(GOLDEN_BACKUP_DIR, name).readBytes()

internal actual fun goldenBackupFileNames(): List<String> = File(GOLDEN_BACKUP_DIR).list().orEmpty().sorted()
