package com.payslipmax.pcdao.reconciliation

class OperationalAllowanceResolvers {
    fun resolveSiachenEntitlement(
        context: ActiveSituationalContext,
        isEscalated: Boolean,
    ): ResolvedEntitlement {
        val rate = if (isEscalated) 53125.0 else 42500.0
        return ResolvedEntitlement(
            allowanceKey = "SIACHEN",
            allowanceName = "Siachen Glacier Allowance (RH-MAX)",
            entitledMonthly = rate,
            entitledAnnual = rate * 12.0,
            statutoryAuthority = "MoD Letter No. 1(26)/2017/D(Pay/Services) dated 18.09.2017 (RH-MAX)",
            relevantRuleId = "ALLOWANCE_SIACHEN_001",
            explanation = "Siachen Glacier Allowance at RH-MAX rate ₹${rate.toInt()}/mo.",
            isEscalated = isEscalated,
        )
    }

    fun resolveHafaaEntitlement(
        context: ActiveSituationalContext,
        isEscalated: Boolean,
    ): ResolvedEntitlement {
        val rate = if (isEscalated) 21125.0 else 16900.0
        return ResolvedEntitlement(
            allowanceKey = "HAFAA",
            allowanceName = "High Altitude Field Area Allowance (HAFAA Cell R1H2)",
            entitledMonthly = rate,
            entitledAnnual = rate * 12.0,
            statutoryAuthority = "MoD Letter No. 1(26)/2017/D(Pay/Services) dated 18.09.2017 (Cell R1H2)",
            relevantRuleId = "ALLOWANCE_HAFAA_001",
            explanation = "HAFAA at Risk & Hardship Cell R1H2 rate ₹${rate.toInt()}/mo.",
            isEscalated = isEscalated,
        )
    }

    fun resolveCfaaEntitlement(
        context: ActiveSituationalContext,
        isEscalated: Boolean,
    ): ResolvedEntitlement {
        val rate = if (isEscalated) 13125.0 else 10500.0
        return ResolvedEntitlement(
            allowanceKey = "CFAA",
            allowanceName = "Compensatory Field Area Allowance (CFAA Cell R2H2)",
            entitledMonthly = rate,
            entitledAnnual = rate * 12.0,
            statutoryAuthority = "MoD Letter No. 1(26)/2017/D(Pay/Services) dated 18.09.2017 (Cell R2H2)",
            relevantRuleId = "ALLOWANCE_CFAA_001",
            explanation = "CFAA (CI Ops/RR) at Risk & Hardship Cell R2H2 rate ₹${rate.toInt()}/mo.",
            isEscalated = isEscalated,
        )
    }

    fun resolveCmfaaEntitlement(
        context: ActiveSituationalContext,
        isEscalated: Boolean,
    ): ResolvedEntitlement {
        val rate = if (isEscalated) 7875.0 else 6300.0
        return ResolvedEntitlement(
            allowanceKey = "CMFAA",
            allowanceName = "Compensatory Modified Field Area Allowance (CMFAA Cell R3H2)",
            entitledMonthly = rate,
            entitledAnnual = rate * 12.0,
            statutoryAuthority = "MoD Letter No. 1(26)/2017/D(Pay/Services) dated 18.09.2017 (Cell R3H2)",
            relevantRuleId = "ALLOWANCE_CMFAA_001",
            explanation = "CMFAA at Risk & Hardship Cell R3H2 rate ₹${rate.toInt()}/mo.",
            isEscalated = isEscalated,
        )
    }

    fun resolveSpecialDutyAllowance(
        context: ActiveSituationalContext,
        basicPay: Double,
    ): ResolvedEntitlement {
        val sdaMonthly = basicPay * 0.10
        return ResolvedEntitlement(
            allowanceKey = "SDA",
            allowanceName = "Special Duty Allowance (10% Basic Pay)",
            entitledMonthly = sdaMonthly,
            entitledAnnual = sdaMonthly * 12.0,
            statutoryAuthority = "MoD Letter No. 1(26)/2017/D(Pay/Services) dated 18.09.2017",
            relevantRuleId = "ALLOWANCE_SDA_001",
            explanation = "10% of Basic Pay for postings in North-East and Ladakh.",
        )
    }

    fun resolveIslandSpecialDutyAllowance(
        context: ActiveSituationalContext,
        basicPay: Double,
        tierPercent: Double = 16.0,
    ): ResolvedEntitlement {
        val monthly = basicPay * (tierPercent / 100.0)
        return ResolvedEntitlement(
            allowanceKey = "ISDA",
            allowanceName = "Island Special Duty Allowance (ISDA ${tierPercent.toInt()}%)",
            entitledMonthly = monthly,
            entitledAnnual = monthly * 12.0,
            statutoryAuthority = "MoD Letter No. 1(26)/2017/D(Pay/Services) dated 18.09.2017 (ISDA)",
            relevantRuleId = "ALLOWANCE_ISDA_001",
            explanation = "${tierPercent.toInt()}% of Basic Pay for Andaman & Nicobar / Lakshadweep Islands.",
        )
    }

    fun resolveTrainingAllowance(
        context: ActiveSituationalContext,
        basicPay: Double,
        isNationalAcademy: Boolean = false,
    ): ResolvedEntitlement {
        val ratePercent = if (isNationalAcademy) 24.0 else 12.0
        val monthly = basicPay * (ratePercent / 100.0)
        val establishmentType = if (isNationalAcademy) "National Academy (24%)" else "Training Establishment (12%)"
        return ResolvedEntitlement(
            allowanceKey = "TRAINING_ALLOWANCE",
            allowanceName = "Training Allowance — $establishmentType",
            entitledMonthly = monthly,
            entitledAnnual = monthly * 12.0,
            statutoryAuthority = "MoD Order No. 1(26)/2017/D(Pay/Services); 7th CPC Para 8.7.41",
            relevantRuleId = "ALLOWANCE_TRAINING_001",
            explanation = "${ratePercent.toInt()}% of Basic Pay for instructional appointments.",
        )
    }

    fun resolveTechnicalPayEntitlement(
        context: ActiveSituationalContext,
        tier: Int = 1,
    ): ResolvedEntitlement {
        val rate = if (tier == 2) 4500.0 else 3000.0
        return ResolvedEntitlement(
            allowanceKey = "TECHNICAL_PAY",
            allowanceName = "Technical Pay (Tier $tier)",
            entitledMonthly = rate,
            entitledAnnual = rate * 12.0,
            statutoryAuthority = "MoD Letter No. 1(26)/2017/D(Pay/Services) (Technical Allowance)",
            relevantRuleId = "ALLOWANCE_TECH_PAY_001",
            explanation = "Technical Pay for qualified technical officers at Tier $tier rate ₹${rate.toInt()}/mo.",
        )
    }

    fun resolveParachuteAllowance(
        context: ActiveSituationalContext,
        isEscalated: Boolean,
    ): ResolvedEntitlement {
        val rate = if (isEscalated) 13125.0 else 10500.0
        return ResolvedEntitlement(
            allowanceKey = "PARACHUTE_ALLOWANCE",
            allowanceName = "Parachute Allowance",
            entitledMonthly = rate,
            entitledAnnual = rate * 12.0,
            statutoryAuthority = "MoD Letter No. 1(26)/2017/D(Pay/Services) (Parachute Allowance)",
            relevantRuleId = "ALLOWANCE_PARA_001",
            explanation = "Special corps parachute pay rate of ₹${rate.toInt()}/mo.",
            isEscalated = isEscalated,
        )
    }
}
