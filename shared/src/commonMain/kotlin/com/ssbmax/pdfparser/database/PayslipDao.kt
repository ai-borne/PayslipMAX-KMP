package com.ssbmax.pdfparser.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PayslipDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayslip(payslip: EncryptedPayslipEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayslips(payslips: List<EncryptedPayslipEntity>)

    @Query("SELECT * FROM encrypted_payslips ORDER BY year ASC, monthNum ASC")
    fun getAllPayslips(): Flow<List<EncryptedPayslipEntity>>

    @Query("SELECT * FROM encrypted_payslips WHERE dateStr = :dateStr LIMIT 1")
    suspend fun getPayslipByDate(dateStr: String): EncryptedPayslipEntity?

    @Query("DELETE FROM encrypted_payslips WHERE dateStr = :dateStr")
    suspend fun deletePayslip(dateStr: String)

    @Query("DELETE FROM encrypted_payslips")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayslipPdf(pdf: PayslipPdfEntity)

    @Query("SELECT * FROM payslip_pdfs WHERE dateStr = :dateStr LIMIT 1")
    suspend fun getPayslipPdfByDate(dateStr: String): PayslipPdfEntity?

    @Query("DELETE FROM payslip_pdfs WHERE dateStr = :dateStr")
    suspend fun deletePayslipPdf(dateStr: String)
}
