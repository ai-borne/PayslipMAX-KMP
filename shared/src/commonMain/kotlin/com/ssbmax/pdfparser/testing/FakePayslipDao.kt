package com.ssbmax.pdfparser.testing

import com.ssbmax.pdfparser.database.PayslipDao
import com.ssbmax.pdfparser.database.EncryptedPayslipEntity
import com.ssbmax.pdfparser.database.PayslipPdfEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakePayslipDao : PayslipDao {
    private val database = MutableStateFlow<Map<String, EncryptedPayslipEntity>>(emptyMap())
    private val pdfDatabase = MutableStateFlow<Map<String, PayslipPdfEntity>>(emptyMap())

    override suspend fun insertPayslip(payslip: EncryptedPayslipEntity) {
        database.value = database.value + (payslip.dateStr to payslip)
    }

    override suspend fun insertPayslips(payslips: List<EncryptedPayslipEntity>) {
        database.value = database.value + payslips.associateBy { it.dateStr }
    }

    override fun getAllPayslips(): Flow<List<EncryptedPayslipEntity>> {
        return database.map {
            it.values.toList().sortedWith(
                compareBy<EncryptedPayslipEntity> { it.year }.thenBy { it.monthNum },
            )
        }
    }

    override suspend fun getPayslipByDate(dateStr: String): EncryptedPayslipEntity? {
        return database.value[dateStr]
    }

    override suspend fun deletePayslip(dateStr: String) {
        database.value = database.value - dateStr
    }

    override suspend fun clearAll() {
        database.value = emptyMap()
        pdfDatabase.value = emptyMap()
    }

    override suspend fun insertPayslipPdf(pdf: PayslipPdfEntity) {
        pdfDatabase.value = pdfDatabase.value + (pdf.dateStr to pdf)
    }

    override suspend fun getPayslipPdfByDate(dateStr: String): PayslipPdfEntity? {
        return pdfDatabase.value[dateStr]
    }

    override suspend fun deletePayslipPdf(dateStr: String) {
        pdfDatabase.value = pdfDatabase.value - dateStr
    }
}
