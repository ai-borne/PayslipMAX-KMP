package com.payslipmax.pdfparser.repository

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The bytes of one committed file in [GOLDEN_BACKUP_DIR] (a golden `.pcda` or the manifest), read from the host disk on every target. */
internal expect fun readGoldenBackupFile(name: String): ByteArray

/** Every file name in [GOLDEN_BACKUP_DIR], sorted. */
internal expect fun goldenBackupFileNames(): List<String>

/**
 * One committed golden backup and what restoring it must give. A golden is a file an app version really wrote (exported by that
 * version's own code from the synthetic data set, or the exact bytes of an older committed one), so the counts are what was seeded,
 * not what today's code happens to produce. Never regenerate a golden: add a new one (docs/Plan/14a, section 6).
 */
@Serializable
internal data class GoldenBackup(
    val file: String,
    val origin: String,
    val format: Int,
    val password: String,
    val payslips: Int,
    val pdfs: Int,
    val corrections: Int,
    val guideNotes: Int,
    val drafts: Int,
    val dismissedDrafts: Int,
    // The theme the restored settings carry: "system" (the default) when the file has no settings.
    val theme: String,
    val part2Entries: Int = 0,
    val part2Orders: Int = 0,
    val part2Pages: Int = 0,
)

internal object GoldenBackupCatalog {
    const val MANIFEST = "manifest.json"

    val goldens: List<GoldenBackup> by lazy { Json.decodeFromString(readGoldenBackupFile(MANIFEST).decodeToString()) }

    fun bytes(file: String): ByteArray = readGoldenBackupFile(file)
}
