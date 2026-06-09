package com.ssbmax.pdfparser.testing

import com.ssbmax.pdfparser.database.PayslipDao
import com.ssbmax.pdfparser.database.PayslipEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakePayslipDao : PayslipDao {
    private val database = MutableStateFlow<Map<String, PayslipEntity>>(emptyMap())

    override suspend fun insertPayslip(payslip: PayslipEntity) {
        database.value = database.value + (payslip.dateStr to payslip)
    }

    override suspend fun insertPayslips(payslips: List<PayslipEntity>) {
        database.value = database.value + payslips.associateBy { it.dateStr }
    }

    override fun getAllPayslips(): Flow<List<PayslipEntity>> {
        return database.map {
            it.values.toList().sortedWith(
                compareBy<PayslipEntity> { it.year }.thenBy { it.monthNum }
            )
        }
    }

    override suspend fun getPayslipByDate(dateStr: String): PayslipEntity? {
        return database.value[dateStr]
    }

    override suspend fun deletePayslip(dateStr: String) {
        database.value = database.value - dateStr
    }

    override suspend fun clearAll() {
        database.value = emptyMap()
    }
}
