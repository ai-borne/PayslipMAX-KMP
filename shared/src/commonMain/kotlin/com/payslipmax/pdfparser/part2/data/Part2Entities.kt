package com.payslipmax.pdfparser.part2.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The three Part II tables (schema 15). Only ids and the page's order link are readable in the database file; everything else is one
 * versioned AES-256-GCM ciphertext made with `CryptoHelper` (see `Part2Codec`), so later changes change the payload, never the schema.
 * Ciphertext is a BLOB, not the hex text the Guide notes use: a page is up to 500 KB and hex would double it in the file and in memory.
 * [toString] shows no bytes.
 */
@Entity(tableName = "part2_drafts")
class Part2DraftEntity(
    @PrimaryKey val id: String,
    val ciphertext: ByteArray,
) {
    override fun toString(): String = "Part2DraftEntity(${ciphertext.size} bytes)"
}

/** A published order. Its draft link, number and date are inside the ciphertext. */
@Entity(tableName = "part2_orders")
class Part2OrderEntity(
    @PrimaryKey val id: String,
    val ciphertext: ByteArray,
) {
    override fun toString(): String = "Part2OrderEntity(${ciphertext.size} bytes)"
}

/** One scanned page. [orderId] and [position] are plain so pages can be listed and deleted per order without decrypting 500 KB each. */
@Entity(tableName = "part2_pages", indices = [Index("orderId")])
class Part2PageEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val position: Int,
    val ciphertext: ByteArray,
) {
    override fun toString(): String = "Part2PageEntity(${ciphertext.size} bytes)"
}
