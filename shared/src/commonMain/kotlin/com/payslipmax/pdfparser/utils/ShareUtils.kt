package com.payslipmax.pdfparser.utils

expect fun shareText(
    text: String,
    title: String,
)

/**
 * Opens an email compose view addressed to [to] with [subject] pre-filled and [body] as the
 * message content. Falls back to the generic [shareText] share sheet if no mail app can handle
 * the request, so the current behavior remains a safety net (no regression).
 */
expect fun shareTextViaEmail(
    to: String,
    subject: String,
    body: String,
)
