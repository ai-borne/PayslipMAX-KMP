package com.payslipmax.pdfparser.utils

/**
 * Generates an A4 PDF of a PCDA(O) representation letter ([title] as a bold heading, [bodyText] as the
 * paginated formal-letter body) and hands it to the OS share sheet. Runs entirely on-device; nothing
 * leaves the device except via the user's explicit share action.
 *
 * Gated by [com.payslipmax.pdfparser.subscription.FeatureGate.CLAIM_GENERATOR] at every call site.
 */
expect fun sharePdf(
    fileName: String,
    title: String,
    bodyText: String,
)
