package com.payslipmax.pdfparser.part2.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The three Part II tables (schema 16). Only ids and a page's order link are readable in the database file; everything else is one versioned
 * AES-256-GCM ciphertext made with `CryptoHelper`, so later changes change the payload, never the schema. Ciphertext is a BLOB, not the hex
 * text the Guide notes use: a page is up to 500 KB and hex would double it in the file and in memory. [toString] shows no bytes.
 *
 * [Part2OrderEntity] and [Part2PageEntity] are the schema-15 tables of the frozen feature/part2-orders branch, unchanged: a phone that ran
 * that branch keeps its scanned orders. Schema 16 drops that branch's drafts table and adds [Part2EntryEntity].
 */
@Entity(tableName = "part2_entries")
class Part2EntryEntity(
    @PrimaryKey val id: String,
    val ciphertext: ByteArray,
) {
    override fun toString(): String = "Part2EntryEntity(${ciphertext.size} bytes)"
}

/** A published order the officer scanned. Its number, date and the entries it covers are inside the ciphertext. */
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
