package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.domain.Officer

/**
 * Standardized display details for officer identity headers across Dashboard and Replica.
 */
data class OfficerDisplayDetails(
    val name: String,
    val cda: String,
    val pan: String,
)

/**
 * Resolves effective officer display details using single-source-of-truth precedence:
 * If an override is non-blank, use it; otherwise fall back to the parsed payslip officer data.
 */
fun resolveOfficerDisplayDetails(
    parsedOfficer: Officer,
    overrideName: String = "",
    overrideCda: String = "",
    overridePan: String = "",
): OfficerDisplayDetails =
    OfficerDisplayDetails(
        name = if (overrideName.isNotBlank()) overrideName else parsedOfficer.name,
        cda = if (overrideCda.isNotBlank()) overrideCda else parsedOfficer.accountNo,
        pan = if (overridePan.isNotBlank()) overridePan else parsedOfficer.pan,
    )
