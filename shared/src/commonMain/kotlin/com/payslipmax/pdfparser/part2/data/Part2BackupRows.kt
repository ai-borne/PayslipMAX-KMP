package com.payslipmax.pdfparser.part2.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One Part II entry or order as a `.pcda` archive carries it (backup v5). [data] is the row's ciphertext, sealed with the backup's key
 * (the password up to format 5, the archive key from 6) rather than the device key, as Base64 text: a `ByteArray` would serialize as a JSON number array (about 3.5 times the size). Keys are
 * pinned with @SerialName like the other backup rows (`repository/backup`, BackupWireKeysTest).
 */
@Serializable
class Part2RowBackup(
    @SerialName("id") val id: String,
    @SerialName("data") val data: String,
)

/**
 * A draft row from the frozen branch's v5 files. Only its existence is read: the Orders Bank has no drafts, so a restore counts each one as
 * skipped (and tells the officer) without decoding it.
 */
@Serializable
class Part2LegacyDraft(
    @SerialName("id") val id: String = "",
)
