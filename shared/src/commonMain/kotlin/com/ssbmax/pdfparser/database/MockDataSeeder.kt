package com.ssbmax.pdfparser.database

import com.ssbmax.pdfparser.domain.*

object MockDataSeeder {
    suspend fun seedDatabase(payslipDao: PayslipDao) {
        val officer =
            Officer(
                name = "Officer Officer Officer",
                accountNo = "16/000/000000X",
                pan = "AR*****90G",
            )
        val payslips = mutableListOf<PayslipEntity>()
        var dsopBalance = 1200000.0 // Starting DSOP balance in Jan 2022

        val months =
            listOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December",
            )

        val years = listOf(2022, 2023, 2024, 2025)

        for (year in years) {
            val baseBp =
                when (year) {
                    2022 -> 118500.0
                    2023 -> 122100.0
                    2024 -> 125800.0
                    else -> 129600.0
                }

            // DA Rates: 2022 (34%-38%), 2023 (42%-46%), 2024 (50%-53%), 2025 (53%)
            val daRate =
                when (year) {
                    2022 -> 0.38
                    2023 -> 0.46
                    2024 -> 0.50
                    else -> 0.53
                }

            for (monthIndex in 0..11) {
                // Skip future months (assume current date is June 2026, so seed up to end of 2025)
                val monthName = months[monthIndex]
                val monthNum = monthIndex + 1
                val dateStr = "${monthNum.toString().padStart(2, '0')}/$year"

                // Allowances
                val basicPay = baseBp
                val msp = 15500.0
                val da = (basicPay + msp) * daRate
                val tpta = 7200.0
                val tptada = tpta * daRate
                val ration = 3450.0

                // July gets annual Dress Allowance
                val dress = if (monthNum == 7) 10000.0 else 0.0

                // Random tiny arrears or adjustments
                val arrearsDa = if (monthNum == 4) 12500.0 else 0.0

                val earnings =
                    Earnings(
                        basicPay = basicPay,
                        dearnessAllowance = da,
                        militaryServicePay = msp,
                        transportAllowance = tpta,
                        transportAllowanceDa = tptada,
                        rationMoney = ration,
                        dressAllowance = dress,
                        arrearsDa = arrearsDa,
                    )

                // Deductions
                val dsopSub = 25000.0
                val agif = 5000.0
                val itax =
                    when {
                        basicPay > 120000.0 -> 24000.0
                        basicPay > 115000.0 -> 21000.0
                        else -> 18000.0
                    }
                val cess = itax * 0.04
                val lf = 880.0
                val fur = 280.0
                val water = 140.0
                val elec = 850.0

                val deductions =
                    Deductions(
                        dsopSubscription = dsopSub,
                        agif = agif,
                        incomeTax = itax,
                        educationCess = cess,
                        licenseFee = lf,
                        furnitureRent = fur,
                        waterCharges = water,
                        electricityCharges = elec,
                    )

                // Ledger balances
                val totalCredits = basicPay + msp + da + tpta + tptada + ration + dress + arrearsDa
                val totalDebits = dsopSub + agif + itax + cess + lf + fur + water + elec
                val netRemittance = totalCredits - totalDebits

                val ledger =
                    LedgerBalances(
                        openingCreditBalance = 0.0,
                        openingDebitBalance = 0.0,
                        closingCreditBalance = 0.0,
                        closingDebitBalance = 0.0,
                    )

                val summary =
                    PayslipSummary(
                        grossPay = totalCredits,
                        totalDeductions = totalDebits,
                        netRemittance = netRemittance,
                    )

                // DSOP Fund growing
                val prevDsop = dsopBalance
                dsopBalance += dsopSub
                val dsopFund =
                    DsopFund(
                        openingBalance = prevDsop,
                        subscriptionYtd = dsopSub * monthNum,
                        refundYtd = 0.0,
                        miscAdjYtd = 0.0,
                        withdrawalYtd = 0.0,
                        closingBalance = dsopBalance,
                    )

                // Tax YTD
                val tax =
                    TaxAndSavings(
                        grossSalaryYtd = totalCredits * monthNum,
                        totalTaxableIncome = (totalCredits - dsopSub) * monthNum,
                        standardDeduction = 50000.0,
                        netTaxableIncome = ((totalCredits - dsopSub) * monthNum) - 50000.0,
                        totalTaxPayable = itax * 12,
                        taxDeductedYtd = (itax + cess) * monthNum,
                        cessDeductedYtd = cess * monthNum,
                        dsopFund = dsopFund,
                    )

                val parsed =
                    ParsedPayslip(
                        file = "payslip_${year}_${monthNum.toString().padStart(2, '0')}.pdf",
                        year = year,
                        monthNum = monthNum,
                        monthName = monthName,
                        dateStr = dateStr,
                        officer = officer,
                        earnings = earnings,
                        deductions = deductions,
                        ledgerBalances = ledger,
                        summary = summary,
                        taxAndSavings = tax,
                    )

                payslips.add(parsed.toEntity())
            }
        }

        payslipDao.insertPayslips(payslips)
    }
}
