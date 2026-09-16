package com.payslipmax.pdfparser.parser

/**
 * Credit / Allowance paycode mappings partitioned by functional domains.
 * Extracted from [PayslipPatternConfig] to adhere to Single Responsibility Principle (SRP)
 * and maintain strict line-count limits (<300 LOC).
 */
object AllowancePatternMappings {
    /**
     * Core pay entitlements: Basic Pay, Dearness Allowance, Military Service Pay, and NPA.
     */
    val corePayMappings: Map<String, String> =
        mapOf(
            "Basic Pay" to "basicPay",
            "BPAY" to "basicPay",
            "BPAY (12A)" to "basicPay",
            "B.PAY" to "basicPay",
            "B PAY" to "basicPay",
            "Gr. Pay" to "basicPay",
            "Gr Pay" to "basicPay",
            "Grade Pay" to "basicPay",
            "DA" to "dearnessAllowance",
            "D.A." to "dearnessAllowance",
            "D.A" to "dearnessAllowance",
            "Dearness Allowance" to "dearnessAllowance",
            "MSP" to "militaryServicePay",
            "M.S.P." to "militaryServicePay",
            "M.S.P" to "militaryServicePay",
            "Military Service Pay" to "militaryServicePay",
            "NPA" to "nonPracticingAllowance",
        )

    /**
     * House Rent Allowance (HRA) classifications and historical arrears.
     */
    val housingMappings: Map<String, String> =
        mapOf(
            "HH11" to "houseRentAllowance",
            "HH12" to "houseRentAllowance",
            "HH13" to "houseRentAllowance",
            "HH21" to "houseRentAllowance",
            "HH22" to "houseRentAllowance",
            "HH23" to "houseRentAllowance",
            "HH31" to "houseRentAllowance",
            "HH32" to "houseRentAllowance",
            "HH33" to "houseRentAllowance",
            "HRA" to "houseRentAllowance",
            "A/o HRA" to "arrearsHra",
            "ARR-HH11" to "arrearsHra",
            "ARR-HH12" to "arrearsHra",
            "ARR-HH13" to "arrearsHra",
            "ARR-HH21" to "arrearsHra",
            "ARR-HH22" to "arrearsHra",
            "ARR-HH23" to "arrearsHra",
            "ARR-HH31" to "arrearsHra",
            "ARR-HH32" to "arrearsHra",
            "ARR-HH33" to "arrearsHra",
            "ARR-HRA" to "arrearsHra",
        )

    /**
     * Field area, Risk and Hardship Matrix allowances and arrears.
     */
    val fieldAndRiskMappings: Map<String, String> =
        mapOf(
            "FD" to "fieldAllowance",
            "RH11" to "riskHardshipAllowance",
            "RH12" to "riskHardshipAllowance",
            "RH13" to "riskHardshipAllowance",
            "RH21" to "riskHardshipAllowance",
            "RH22" to "riskHardshipAllowance",
            "RH23" to "riskHardshipAllowance",
            "RH31" to "riskHardshipAllowance",
            "RH32" to "riskHardshipAllowance",
            "RH33" to "riskHardshipAllowance",
            "ARR-RH11" to "arrearsRiskHardship",
            "ARR-RH12" to "arrearsRiskHardship",
            "ARR-RH13" to "arrearsRiskHardship",
            "ARR-RH21" to "arrearsRiskHardship",
            "ARR-RH22" to "arrearsRiskHardship",
            "ARR-RH23" to "arrearsRiskHardship",
            "ARR-RH31" to "arrearsRiskHardship",
            "ARR-RH32" to "arrearsRiskHardship",
            "ARR-RH33" to "arrearsRiskHardship",
            "SICHA" to "riskHardshipAllowance",
            "ARR-SICHA" to "arrearsRiskHardship",
            "HA/UCA All" to "riskHardshipAllowance",
            "SCCI Allce" to "riskHardshipAllowance",
        )

    /**
     * General and specialized allowances: Transport, Dress, Ration, Special Forces, CEA, Medical.
     */
    val generalAllowanceMappings: Map<String, String> =
        mapOf(
            "Tpt Allc" to "transportAllowance",
            "TPTA" to "transportAllowance",
            "TRAN1" to "transportAllowance",
            "TRAN-1" to "transportAllowance",
            "Tpt. Allc" to "transportAllowance",
            "TPTADA" to "transportAllowanceDa",
            "Tpt DA" to "transportAllowanceDa",
            "DRESALW" to "dressAllowance",
            "A/o DressAllowance" to "dressAllowance",
            "K.M.A" to "dressAllowance",
            "Outfit Alc" to "dressAllowance",
            "Outfit Allowance" to "dressAllowance",
            "Of / Drs Alc" to "dressAllowance",
            "RSHNA" to "rationMoney",
            "RMONEYAllce-RA" to "rationMoney",
            "RA" to "rationMoney",
            "SpCmd Pay" to "specialForcesPay",
            "SPCDO" to "specialForcesPay",
            "SC" to "specialForcesPay",
            "CEA" to "childrenEducationAllowance",
            "C E A(NT)" to "childrenEducationAllowance",
            "C E A (T)" to "childrenEducationAllowance",
            "C E A" to "childrenEducationAllowance",
            "MEDICAL" to "medicalAllowance",
            "Reimb Med" to "medicalAllowance",
        )

    /**
     * Arrears, ledger adjustments, and carryover credit balances.
     */
    val adjustmentAndLedgerMappings: Map<String, String> =
        mapOf(
            "ARR-CEA" to "arrearsCea",
            "ARR-DA" to "arrearsDa",
            "A/o DA" to "arrearsDa",
            "ARR-RSHNA" to "arrearsRation",
            "A/o RMONEYAllce-RA" to "arrearsRation",
            "ARR-SPCDO" to "arrearsSpecialForces",
            "ARR-TPTA" to "arrearsTpta",
            "ARR-TPTADA" to "arrearsTptaDa",
            "A/o BPAY-" to "adjBasicPay",
            "A/o BPAY" to "adjBasicPay",
            "A/o DA-" to "adjDa",
            "A/o MSP-" to "adjMsp",
            "A/o MSP" to "adjMsp",
            "A/o TRAN-1" to "adjTpta",
            "A/o TRAN-2" to "adjTpta",
            "A/o Pay & Allce" to "adjPayAndAllce",
            "A/o FIELD-R1" to "adjFieldAllowance",
            "ETKT-ref" to "adjTicketRecovery",
            "Adhoc Payt" to "adjPayAndAllce",
            "TA/DA Cheq" to "adjPayAndAllce",
            "Arrs P & A" to "adjPayAndAllce",
            "Arr P & A" to "adjPayAndAllce",
            "Instr Allce" to "adjPayAndAllce",
            "Instr Allowance" to "adjPayAndAllce",
            "LTC Encash" to "adjPayAndAllce",
            "Ref.L Fee" to "adjPayAndAllce",
            "Ref.Furn." to "adjPayAndAllce",
            "Op Cr Bal" to "openingCreditBalance",
            "Cl. Dr. Bal." to "closingDebitBalance",
            "Clos Bal(-)" to "closingDebitBalance",
        )

    /**
     * Composite credit key mapping providing Single Source of Truth across parsing pipelines.
     */
    val all: Map<String, String> =
        corePayMappings +
            housingMappings +
            fieldAndRiskMappings +
            generalAllowanceMappings +
            adjustmentAndLedgerMappings
}
