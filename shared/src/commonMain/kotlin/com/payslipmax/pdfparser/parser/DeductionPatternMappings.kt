package com.payslipmax.pdfparser.parser

/**
 * Debit / Deduction paycode mappings partitioned by functional domains.
 * Extracted from [PayslipPatternConfig] to adhere to Single Responsibility Principle (SRP)
 * and maintain strict line-count limits (<300 LOC).
 */
object DeductionPatternMappings {
    /**
     * Statutory and tax deductions: Provident fund (DSOPF), Insurance (AGIF), Income tax, and Cess.
     */
    val statutoryMappings: Map<String, String> =
        mapOf(
            "DSOPF Subn" to "dsopSubscription",
            "DSOPF" to "dsopSubscription",
            "DSOP" to "dsopSubscription",
            "D.S.O.P.F." to "dsopSubscription",
            "D.S.O.P.F" to "dsopSubscription",
            "DSOP Subn" to "dsopSubscription",
            "AGIF" to "agif",
            "A.G.I.F." to "agif",
            "A.G.I.F" to "agif",
            "AGIF Subn" to "agif",
            "AGIF Subscription" to "agif",
            "Incm Tax" to "incomeTax",
            "Income Tax" to "incomeTax",
            "ITAX" to "incomeTax",
            "Inc Tax" to "incomeTax",
            "Incm. Tax" to "incomeTax",
            "I Tax" to "incomeTax",
            "Educ Cess" to "educationCess",
            "E Cess" to "educationCess",
            "EHCESS" to "educationCess",
            "Educ. Cess" to "educationCess",
        )

    /**
     * Government accommodation and utility recoveries: Licence Fee, Furniture, Water, Electricity, Barrack Damage.
     */
    val accommodationMappings: Map<String, String> =
        mapOf(
            "L Fee" to "licenseFee",
            "LF" to "licenseFee",
            "Dr L Fee" to "licenseFee",
            "Dr. L. fee" to "licenseFee",
            "1.  Recovery of LF  from" to "licenseFee",
            "2.  Recovery of LF  from" to "licenseFee",
            "3.  Recovery of LF  from" to "licenseFee",
            "4.  Recovery of LF  from" to "licenseFee",
            "Fur" to "furnitureRent",
            "FUR" to "furnitureRent",
            "Dr Furn." to "furnitureRent",
            "Dr. fur" to "furnitureRent",
            "Furn." to "furnitureRent",
            "2.  Recovery of FUR  from" to "furnitureRent",
            "3.  Recovery of FUR  from" to "furnitureRent",
            "4.  Recovery of FUR  from" to "furnitureRent",
            "5.  Recovery of FUR  from" to "furnitureRent",
            "Water" to "waterCharges",
            "WATER" to "waterCharges",
            "Dr Water" to "waterCharges",
            "Elec" to "electricityCharges",
            "Dr Elec." to "electricityCharges",
            "Barrack Damage" to "barrackDamage",
            "Dr Barrack Damage" to "barrackDamage",
        )

    /**
     * Specific recoveries, loan advances, and miscellaneous debit adjustments.
     */
    val recoveryAndLoanMappings: Map<String, String> =
        mapOf(
            "ETKT" to "ticketRecovery",
            "R/o Etkt" to "ticketRecovery",
            "Rec CIA-FD" to "recFieldAllowance",
            "Dr CIA-FD" to "recFieldAllowance",
            "Rec PARA-SC" to "recSpecialForces",
            "Dr PARA-SC" to "recSpecialForces",
            "Rec INSTR-" to "recoveryOfDebits",
            "Dr INSTR-" to "recoveryOfDebits",
            "R/o Of /Drs" to "recoveryOfDebits",
            "R/o P & A" to "recoveryOfDebits",
            "Recv P & A" to "recoveryOfDebits",
            "CC to bankers" to "recoveryOfDebits",
            "AOBF" to "aobf",
            "AFMSOF" to "aobf",
            "DLIS" to "recoveryOfDebits",
            "HBA" to "recoveryOfDebits",
            "PCA" to "recoveryOfDebits",
            "COURT" to "recoveryOfDebits",
            "MAINT" to "recoveryOfDebits",
            "AGIF-CAR" to "agifLoanRecovery",
            "AGIF-MCA" to "agifLoanRecovery",
        )

    /**
     * Ledger carryover balances (debit side).
     */
    val ledgerMappings: Map<String, String> =
        mapOf(
            "Op Dr Bal" to "openingDebitBalance",
            "OP Bal(-)" to "openingDebitBalance",
            "Cl. Cr. Bal." to "closingCreditBalance",
            "Clos Bal(+)" to "closingCreditBalance",
        )

    /**
     * Composite debit key mapping providing Single Source of Truth across parsing pipelines.
     */
    val all: Map<String, String> =
        statutoryMappings +
            accommodationMappings +
            recoveryAndLoanMappings +
            ledgerMappings
}
