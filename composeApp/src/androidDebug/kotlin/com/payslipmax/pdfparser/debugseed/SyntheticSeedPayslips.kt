package com.payslipmax.pdfparser.debugseed

import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import com.payslipmax.pdfparser.insights.timeline.PayLevel
import com.payslipmax.pdfparser.insights.timeline.PayMatrix
import com.payslipmax.pdfparser.parser.PayslipPatternConfig
import kotlin.math.roundToInt

/**
 * Invented payslips, one block of months per [SeedStep], each block years apart so no scenario can explain
 * or disturb another. Every number is made up and the officer is a placeholder; the only thing that matches
 * a real payslip is the pay structure the auditors read. Every payslip's `file` starts with [FILE_PREFIX],
 * the marker "Remove seed data" deletes by. All months are 2018-2023, before any real payslip era on a
 * device, and the seeder additionally refuses to touch a month that already exists.
 */
object SyntheticSeedPayslips {
    const val FILE_PREFIX = "debug-seed:"

    private val officer = Officer("Seed Officer", "00/000/000000X", "AAAAA0000A")
    private const val BASIC = 85300.0
    private const val MSP = 15500.0
    private const val DA_PERCENT = 17.0
    private const val DA_FACTOR = 1.17
    private const val OTHER_CITY_TPTA = 3600.0 * DA_FACTOR
    private const val HIGHER_CITY_TPTA = 7200.0 * DA_FACTOR

    fun payslips(step: SeedStep): List<ParsedPayslip> =
        when (step) {
            SeedStep.PROVEN_TPTA -> tptaGap(2023, resolvingMonthTpta = OTHER_CITY_TPTA, step = step, withFollowUp = true)
            SeedStep.HELD_TPTA -> tptaGap(2018, step = step)
            SeedStep.HELD_FOR_RESOLVE -> tptaGap(2019, step = step)
            SeedStep.RESOLVE_BY_RELOCATION -> listOf(slip(2019, 4, BASIC, HIGHER_CITY_TPTA, step))
            SeedStep.HELD_FOR_SURFACE -> tptaGap(2020, step = step)
            SeedStep.SURFACE_BY_SAME_CITY -> listOf(slip(2020, 4, BASIC, OTHER_CITY_TPTA, step))
            SeedStep.INCREMENT_MISS -> incrementMiss(step)
        }

    /** Jan and Feb pay TPTA at the other-places rate, March pays none; an April payslip can settle it. */
    private fun tptaGap(
        year: Int,
        resolvingMonthTpta: Double = OTHER_CITY_TPTA,
        step: SeedStep,
        withFollowUp: Boolean = false,
    ): List<ParsedPayslip> =
        listOf(slip(year, 1, BASIC, OTHER_CITY_TPTA, step), slip(year, 2, BASIC, OTHER_CITY_TPTA, step), slip(year, 3, BASIC, 0.0, step)) +
            if (withFollowUp) listOf(slip(year, 4, BASIC, resolvingMonthTpta, step)) else emptyList()

    /** Level 11: stage 7 until June 2021, stage 8 from the July 2021 increment, and still stage 8 in July 2022. */
    private fun incrementMiss(step: SeedStep): List<ParsedPayslip> =
        (0 until INCREMENT_MONTHS).map { offset ->
            val index = 2021 * MONTHS_PER_YEAR + offset
            val year = index / MONTHS_PER_YEAR
            val month = index % MONTHS_PER_YEAR + 1
            val stage = if (year == 2021 && month < 7) 7 else 8
            slip(year, month, PayMatrix.payAt(PayLevel.L11, stage)!!.toDouble(), OTHER_CITY_TPTA, step)
        }

    private fun slip(
        year: Int,
        month: Int,
        basic: Double,
        tpta: Double,
        step: SeedStep,
    ): ParsedPayslip {
        val da = ((basic + MSP) * DA_PERCENT / PERCENT).roundToInt().toDouble()
        val gross = basic + MSP + da + tpta
        return ParsedPayslip(
            file = "$FILE_PREFIX${step.name.lowercase()}",
            year = year,
            monthNum = month,
            monthName = PayslipPatternConfig.monthNames[month],
            dateStr = "${month.toString().padStart(2, '0')}/$year",
            officer = officer,
            earnings = Earnings(basicPay = basic, militaryServicePay = MSP, dearnessAllowance = da, transportAllowance = tpta),
            deductions = Deductions(),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(grossPay = gross, totalDeductions = 0.0, netRemittance = gross),
            taxAndSavings = null,
        )
    }

    private const val PERCENT = 100.0
    private const val MONTHS_PER_YEAR = 12
    private const val INCREMENT_MONTHS = 19
}
