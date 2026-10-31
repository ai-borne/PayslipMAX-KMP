package com.payslipmax.pdfparser.part2.data

import kotlinx.serialization.Serializable

/**
 * One Part II entry or order as a `.pcda` archive carries it (backup v5). [data] is the row's ciphertext, made with the backup password
 * rather than the device key, as Base64 text: a `ByteArray` would serialize as a JSON number array (about 3.5 times the size).
 */
@Serializable
class Part2RowBackup(
    val id: String,
    val data: String,
)

/**
 * A draft row from the frozen branch's v5 files. Only its existence is read: the Orders Bank has no drafts, so a restore counts each one as
 * skipped (and tells the officer) without decoding it.
 */
@Serializable
class Part2LegacyDraft(
    val id: String = "",
)
