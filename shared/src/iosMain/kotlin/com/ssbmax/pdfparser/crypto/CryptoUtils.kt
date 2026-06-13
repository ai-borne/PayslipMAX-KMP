@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.ssbmax.pdfparser.crypto

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.CoreFoundation.*
import platform.posix.memcpy

internal fun String.toCFString(): CFStringRef? {
    return CFStringCreateWithCString(null, this, kCFStringEncodingUTF8)
}

internal fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    if (length == 0) return ByteArray(0)
    val bytes = this.bytes ?: return ByteArray(0)
    val byteArray = ByteArray(length)
    byteArray.usePinned { pinned ->
        memcpy(pinned.addressOf(0), bytes, length.toULong())
    }
    return byteArray
}

internal fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData.data()
    return usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), size.toULong())
    }
}

@Suppress("UNCHECKED_CAST")
internal fun NSData.toCFData(): CFDataRef? {
    return CFBridgingRetain(this) as? CFDataRef
}

internal fun ByteArray.toHex(): String {
    val hexChars = "0123456789abcdef"
    val result = StringBuilder(size * 2)
    for (b in this) {
        val i = b.toInt() and 0xFF
        result.append(hexChars[i shr 4])
        result.append(hexChars[i and 0x0F])
    }
    return result.toString()
}
