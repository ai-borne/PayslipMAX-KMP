package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.database.AppSettingsEntity
import com.payslipmax.pdfparser.database.EncryptedPayslipEntity
import com.payslipmax.pdfparser.database.PayslipPdfEntity
import kotlinx.serialization.Serializable

@Serializable
data class PortableBackup(
    val version: Int = 1,
    val encryptedPayslips: List<EncryptedPayslipEntity>,
    val pdfs: List<PayslipPdfEntity>,
    val settings: AppSettingsEntity?,
)
