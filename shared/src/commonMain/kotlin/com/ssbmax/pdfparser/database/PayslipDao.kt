package com.ssbmax.pdfparser.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PayslipDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayslip(payslip: PayslipEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayslips(payslips: List<PayslipEntity>)

    @Query("SELECT * FROM payslips ORDER BY year ASC, monthNum ASC")
    fun getAllPayslips(): Flow<List<PayslipEntity>>

    @Query("SELECT * FROM payslips WHERE dateStr = :dateStr LIMIT 1")
    suspend fun getPayslipByDate(dateStr: String): PayslipEntity?

    @Query("DELETE FROM payslips WHERE dateStr = :dateStr")
    suspend fun deletePayslip(dateStr: String)

    @Query("DELETE FROM payslips")
    suspend fun clearAll()
}
