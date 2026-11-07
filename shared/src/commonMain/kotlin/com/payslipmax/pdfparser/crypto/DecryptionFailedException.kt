package com.payslipmax.pdfparser.crypto

/**
 * The bytes did not decrypt under this password: the password is wrong, the data was altered or cut short, or it is not a PayslipMax
 * archive at all. AES-GCM cannot tell these apart, so neither can this type. Both platforms return it from [CryptoHelper.decrypt] for
 * exactly those cases, so a caller decides by type; the text is fixed and no cause is kept, so no library message can ride along to a
 * screen or a log.
 */
class DecryptionFailedException : Exception("The data could not be decrypted with this password.")
