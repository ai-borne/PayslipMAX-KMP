package com.payslipmax.pdfparser.part2.data

import kotlinx.serialization.Serializable

/**
 * Part II drafts and orders as a `.pcda` archive carries them (backup v5). [data] is the row's ciphertext, made with the backup password rather than
 * the device key, as Base64 text: a `ByteArray` would serialize as a JSON number array (about 3.5 times the size).
 */
@Serializable
class Part2DraftBackup(
    val id: String,
    val data: String,
)

@Serializable
class Part2OrderBackup(
    val id: String,
    val data: String,
)
