package com.payslipmax.pdfparser.utils

expect fun shareText(
    text: String,
    title: String,
)

/**
 * Writes [bytes] to a temporary file named [fileName] and opens the platform share sheet so the
 * user can save it (Files / Drive / iCloud / email …). Used to hand off the encrypted backup
 * archive — the app never uploads anything itself; the user chooses the destination.
 */
expect fun shareBytes(
    bytes: ByteArray,
    fileName: String,
    mimeType: String,
)
