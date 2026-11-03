package com.payslipmax.pdfparser.repository.backup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The rows a `.pcda` archive carries (F1, docs/Plan/14a P1). They are the file format and nothing else: a Room entity can gain a column
// without changing a backup, and every key is pinned with @SerialName so a Kotlin rename cannot either (BackupWireKeysTest). Keys and
// defaults are the ones the entities wrote before this split, so every file already in the wild still reads. Mappers: BackupRowMappers.kt.

/** One payslip; [ciphertext] is its JSON encrypted with the backup password, hex-encoded. */
@Serializable
data class PayslipBackupRow(
    @SerialName("dateStr") val dateStr: String,
    @SerialName("year") val year: Int,
    @SerialName("monthNum") val monthNum: Int,
    @SerialName("monthName") val monthName: String,
    @SerialName("ciphertext") val ciphertext: String,
)

/** One payslip's original PDF, as a JSON number array (the format files already in the wild use). */
@Serializable
class PdfBackupRow(
    @SerialName("dateStr") val dateStr: String,
    @SerialName("pdfData") val pdfData: ByteArray,
)

/**
 * The officer's choices about the app itself. D5: the app lock, its PIN hash, telemetry consent and Premium belong to the device, so they
 * are not on this type at all; a restore keeps the device's own. Older files that still carry them restore with those keys ignored.
 */
@Serializable
data class SettingsBackup(
    // "light", "dark", "system"
    @SerialName("appTheme") val appTheme: String = "system",
    @SerialName("profileName") val profileName: String = "",
    @SerialName("profileCdaNumber") val profileCdaNumber: String = "",
    @SerialName("profilePanNumber") val profilePanNumber: String = "",
    @SerialName("useLocalAi") val useLocalAi: Boolean = false,
)

/** One representation letter, with the officer's edits. */
@Serializable
data class DraftBackupRow(
    @SerialName("id") val id: String,
    @SerialName("disputeMonth") val disputeMonth: String,
    @SerialName("disputeType") val disputeType: String,
    @SerialName("recipient") val recipient: String,
    @SerialName("subject") val subject: String,
    @SerialName("bodyText") val bodyText: String,
    @SerialName("createdAt") val createdAt: Long,
)

/** A letter the officer deleted, so a restore does not draft it again. */
@Serializable
data class DismissedDraftBackupRow(
    @SerialName("disputeMonth") val disputeMonth: String,
    @SerialName("disputeType") val disputeType: String,
)

/** One payslip's per-field corrections; [ciphertext] is encrypted with the backup password, hex-encoded. */
@Serializable
data class CorrectionBackupRow(
    @SerialName("dateStr") val dateStr: String,
    @SerialName("ciphertext") val ciphertext: String,
)

/** One private Claim Guide note; [ciphertext] is encrypted with the backup password, hex-encoded. */
@Serializable
data class GuideNoteBackupRow(
    @SerialName("cardId") val cardId: String,
    @SerialName("ciphertext") val ciphertext: String,
)
