package com.ssbmax.pdfparser.repository

import com.ssbmax.pdfparser.database.AppSettingsEntity
import com.ssbmax.pdfparser.database.EncryptedPayslipEntity
import com.ssbmax.pdfparser.database.PayslipPdfEntity
import kotlinx.serialization.Serializable

@Serializable
data class PortableBackup(
    val version: Int = 1,
    val encryptedPayslips: List<EncryptedPayslipEntity>,
    val pdfs: List<PayslipPdfEntity>,
    val settings: AppSettingsEntity?,
)
