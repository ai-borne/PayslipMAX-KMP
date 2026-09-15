//
//  DataKotlinByteArrayTests.swift
//  PayslipMaxTests
//
//  Covers Data.toKotlinByteArray(), used by DocumentPickerDelegate/BackupPickerDelegate to hand
//  picked PDF/backup bytes to Kotlin. Kotlin's ByteArray is signed (Int8) while Swift's Data is
//  unsigned (UInt8) — a truncating or clamping conversion would corrupt any byte >= 0x80, silently
//  mangling binary PDF/backup content.
//

import XCTest
import composeApp
@testable import PayslipMax

final class DataKotlinByteArrayTests: XCTestCase {

    func test_roundTrips_emptyData() {
        let kotlinArray = Data().toKotlinByteArray()
        XCTAssertEqual(kotlinArray.size, 0)
    }

    func test_roundTrips_fullByteRange_includingHighBitSet() {
        // 0x00...0xFF covers every case the Int8 bit-pattern reinterpretation must get right,
        // including values >= 0x80 that would be corrupted by a naive Int8(byte) truncating init.
        let original = Data((0...255).map { UInt8($0) })
        let kotlinArray = original.toKotlinByteArray()

        XCTAssertEqual(Int(kotlinArray.size), original.count)
        for (i, expectedByte) in original.enumerated() {
            let actual = UInt8(bitPattern: kotlinArray.get(index: Int32(i)))
            XCTAssertEqual(actual, expectedByte, "byte at index \(i) did not round-trip")
        }
    }

    func test_preservesByteOrder() {
        let original = Data([0x01, 0x02, 0x03, 0x04])
        let kotlinArray = original.toKotlinByteArray()

        let recovered = (0..<Int(kotlinArray.size)).map {
            UInt8(bitPattern: kotlinArray.get(index: Int32($0)))
        }
        XCTAssertEqual(recovered, [0x01, 0x02, 0x03, 0x04])
    }
}
