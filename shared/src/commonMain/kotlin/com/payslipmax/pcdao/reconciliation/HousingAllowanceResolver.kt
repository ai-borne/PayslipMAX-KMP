package com.payslipmax.pcdao.reconciliation

class HousingAllowanceResolver {
    fun resolveHraEntitlement(
        context: ActiveSituationalContext,
        effectivePay: Double,
        da: Double,
        escalated: EscalatedRates,
    ): ResolvedEntitlement {
        val tier = context.sprCityTier.uppercase()
        val hraPercent = escalated.hraRates[tier] ?: 20.0
        val entitledMonthly = effectivePay * (hraPercent / 100.0)
        val entitledAnnual = entitledMonthly * 12.0

        return ResolvedEntitlement(
            allowanceKey = "HRA_SPR",
            allowanceName = "Selected Place of Residence (SPR) HRA ($tier-City)",
            entitledMonthly = entitledMonthly,
            entitledAnnual = entitledAnnual,
            statutoryAuthority = "Handbook of Pay & Allowances 2023, Chapter 10, Para 5",
            relevantRuleId = "HRA_SPR_001",
            explanation = "Family at SPR qualifies for ${hraPercent.toInt()}% HRA on effective base ₹${effectivePay.toInt()}.",
            isEscalated = escalated.isEscalated,
        )
    }

    fun resolveTwoLocationConcessionEntitlement(
        context: ActiveSituationalContext,
        effectivePay: Double,
        da: Double,
        escalated: EscalatedRates,
    ): ResolvedEntitlement {
        val tier = context.sprCityTier.uppercase()
        val tiles = context.activeTileIds
        val isPeaceRetention = tiles.contains(SituationalTileKeys.HOUSE_PEACE_RETENTION)
        val isSfAccomm = tiles.contains(SituationalTileKeys.HOUSE_SF_ACCOMMODATION)

        return when {
            isPeaceRetention ->
                ResolvedEntitlement(
                    allowanceKey = "TLC_PEACE_RETENTION",
                    allowanceName = "Two-Location Concession (Govt Peace Accomm Retention)",
                    entitledMonthly = 0.0,
                    entitledAnnual = 0.0,
                    statutoryAuthority = "Handbook of Pay & Allowances 2023, Chapter 10, Para 5",
                    relevantRuleId = "HOUSING_TLC_001",
                    explanation = "Authorized retention of Govt Married Accommodation at peace station under TLC.",
                )
            isSfAccomm ->
                ResolvedEntitlement(
                    allowanceKey = "TLC_SF_ACCOMMODATION",
                    allowanceName = "Two-Location Concession (Separated Family Accomm)",
                    entitledMonthly = 0.0,
                    entitledAnnual = 0.0,
                    statutoryAuthority = "Handbook of Pay & Allowances 2023, Chapter 10, Para 5",
                    relevantRuleId = "HOUSING_TLC_001",
                    explanation = "Authorized Separated Family (SF) accommodation under TLC.",
                )
            else -> {
                val hraPercent = escalated.hraRates[tier] ?: 20.0
                val entitledMonthly = effectivePay * (hraPercent / 100.0)
                ResolvedEntitlement(
                    allowanceKey = "HRA_SPR",
                    allowanceName = "Two-Location Concession (Family SPR $tier-City HRA)",
                    entitledMonthly = entitledMonthly,
                    entitledAnnual = entitledMonthly * 12.0,
                    statutoryAuthority = "Handbook of Pay & Allowances 2023, Chapter 10, Para 5",
                    relevantRuleId = "HOUSING_TLC_001",
                    explanation = "TLC authorizes ${hraPercent.toInt()}% HRA for family at SPR on ₹${effectivePay.toInt()} with field deployment.",
                    isEscalated = escalated.isEscalated,
                )
            }
        }
    }
}
