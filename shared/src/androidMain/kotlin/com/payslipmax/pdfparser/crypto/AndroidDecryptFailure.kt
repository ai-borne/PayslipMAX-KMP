package com.payslipmax.pdfparser.crypto

import javax.crypto.BadPaddingException

/**
 * What [CryptoHelper.decrypt] returns for a thrown [e]. A failed GCM tag check is a [BadPaddingException] (an `AEADBadTagException` on the
 * JVM and on a phone, where its text is the library's own, "error:1e000065:...BAD_DECRYPT"): that is the password or the file, so it
 * becomes the typed failure and the text is dropped. Anything else (no cipher on the device) is not about the data and is returned as it is.
 */
internal fun decryptFailure(e: Exception): Exception = if (e is BadPaddingException) DecryptionFailedException() else e
