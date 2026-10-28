package com.payslipmax.pdfparser.crypto

import platform.CoreCrypto.kCCDecodeError
import platform.CoreCrypto.kCCUnspecifiedError

/**
 * The exception for a non-success [status] of the GCM decrypt, or null when the status says nothing about the data. iOS raises no exception
 * with text, unlike Android: CommonCrypto's one-shot GCM returns `kCCUnspecifiedError` (-4308, seen on the simulator) when the tag does not
 * match, and `kCCDecodeError` is the status its header documents for data that does not decode. Both mean the password or the file. The call
 * is always made with a 32-byte key, a 12-byte IV and a 16-byte tag, so no other status can come from the data; a status that says the call
 * itself was wrong, or -1 (the symbol was not found), stays a generic failure.
 */
internal fun decryptStatusFailure(status: Int): Exception? =
    if (status == kCCUnspecifiedError || status == kCCDecodeError) DecryptionFailedException() else null
