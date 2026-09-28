package com.payslipmax.pcdao.reconciliation

class SituationalRuleResolver {
    fun calculateEscalatedRates(daPercent: Double): EscalatedRates {
        val isEscalated = daPercent >= 50.0
        val multiplier = if (isEscalated) 1.25 else 1.0

        val ceaMonthly = 2250.0 * multiplier
        val hostelMonthly = 6750.0 * multiplier
        val dressAnnual = 20000.0 * multiplier

        val hraRates =
            when {
                daPercent >= 50.0 -> mapOf("X" to 30.0, "Y" to 20.0, "Z" to 10.0)
                daPercent >= 25.0 -> mapOf("X" to 27.0, "Y" to 18.0, "Z" to 9.0)
                else -> mapOf("X" to 24.0, "Y" to 16.0, "Z" to 8.0)
            }

        val siachenRate = 42500.0 * multiplier
        val hafaRate = 16900.0 * multiplier

        return EscalatedRates(
            daRate = daPercent,
            isEscalated = isEscalated,
            ceaMonthlyPerChild = ceaMonthly,
            ceaAnnualPerChild = ceaMonthly * 12.0,
            hostelSubsidyMonthly = hostelMonthly,
            hostelSubsidyAnnual = hostelMonthly * 12.0,
            dressAllowanceAnnual = dressAnnual,
            hraRates = hraRates,
            siachenMonthlyRate = siachenRate,
            hafaMonthlyRate = hafaRate,
        )
    }

    fun resolveTptaEntitlement(
        context: ActiveSituationalContext,
        basicPay: Double,
    ): ResolvedEntitlement {
        val da = resolveEffectiveDa(context)
        val isHigherCity = context.activeTileIds.contains(SituationalTileKeys.POST_PEACE_HIGHER)
        val baseRate = if (isHigherCity) 7200.0 else 3600.0
        val isDivyang = context.activeSpecializedFactors.contains(SpecializedMilitaryFactor.DIVYANG_OFFICER)
        val multiplier = if (isDivyang) 2.0 else 1.0

        val entitledMonthly = (baseRate * multiplier) * (1.0 + da / 100.0)
        val entitledAnnual = entitledMonthly * 12.0

        return ResolvedEntitlement(
            allowanceKey = "TPTA",
            allowanceName = if (isHigherCity) "Transport Allowance (Higher Rate UA City)" else "Transport Allowance (Other Places)",
            entitledMonthly = entitledMonthly,
            entitledAnnual = entitledAnnual,
            statutoryAuthority = "MoD Letter No. 1(25)/2017/D(Pay/Services) Appendix A",
            relevantRuleId = "ALLOWANCE_TPTA_001",
            explanation = "Entitled rate for Level 10-18 officers: ₹${baseRate.toInt()}/mo + $da% DA.",
            isEscalated = isDivyang,
        )
    }

    fun resolveHraEntitlement(
        context: ActiveSituationalContext,
        basicPay: Double,
    ): ResolvedEntitlement {
        val da = resolveEffectiveDa(context)
        val escalated = calculateEscalatedRates(da)
        val tier = context.sprCityTier.uppercase()
        val hraPercent = escalated.hraRates[tier] ?: 20.0
        val entitledMonthly = basicPay * (hraPercent / 100.0)
        val entitledAnnual = entitledMonthly * 12.0

        return ResolvedEntitlement(
            allowanceKey = "HRA_SPR",
            allowanceName = "Selected Place of Residence (SPR) HRA ($tier-City)",
            entitledMonthly = entitledMonthly,
            entitledAnnual = entitledAnnual,
            statutoryAuthority = "Handbook of Pay & Allowances 2023, Chapter 10, Para 5",
            relevantRuleId = "HRA_SPR_001",
            explanation = "Family at SPR qualifies for ${hraPercent.toInt()}% HRA on Basic Pay ₹${basicPay.toInt()}.",
            isEscalated = escalated.isEscalated,
        )
    }

    fun resolveEducationEntitlement(context: ActiveSituationalContext): ResolvedEntitlement {
        val da = resolveEffectiveDa(context)
        val escalated = calculateEscalatedRates(da)
        val childrenCount = context.numberOfChildrenCea.coerceIn(0, 2)
        val isDivyangChild = context.activeSpecializedFactors.contains(SpecializedMilitaryFactor.DIVYANG_CHILD)

        return if (context.hasHostelChild) {
            val annual = escalated.hostelSubsidyAnnual * childrenCount
            ResolvedEntitlement(
                allowanceKey = "HOSTEL_SUBSIDY",
                allowanceName = "Hostel Subsidy ($childrenCount Children)",
                entitledMonthly = annual / 12.0,
                entitledAnnual = annual,
                statutoryAuthority = "DoPT OM No. A-27012/02/2017-Estt.(AL) dated 16/17 July 2018",
                relevantRuleId = "ALLOWANCE_CEA_002",
                explanation = "Hostel subsidy rate of ₹${escalated.hostelSubsidyAnnual.toInt()}/child/yr.",
                isEscalated = escalated.isEscalated,
            )
        } else {
            val childRate = if (isDivyangChild) escalated.ceaAnnualPerChild * 2.0 else escalated.ceaAnnualPerChild
            val annual = childRate * childrenCount
            ResolvedEntitlement(
                allowanceKey = "CEA",
                allowanceName = "Children Education Allowance ($childrenCount Children)",
                entitledMonthly = annual / 12.0,
                entitledAnnual = annual,
                statutoryAuthority = "DoPT OM No. A-27012/02/2017-Estt.(AL)",
                relevantRuleId = "ALLOWANCE_CEA_001",
                explanation = "CEA annual reimbursement of ₹${childRate.toInt()}/child/yr.",
                isEscalated = escalated.isEscalated,
            )
        }
    }

    fun resolveLtcEncashment(
        context: ActiveSituationalContext,
        basicPay: Double,
        msp: Double,
    ): ResolvedEntitlement {
        val da = resolveEffectiveDa(context)
        val totalDailyPay = (basicPay + msp) * (1.0 + da / 100.0) / 30.0
        val encashmentAmount = totalDailyPay * 10.0

        return ResolvedEntitlement(
            allowanceKey = "LTC_ENCASHMENT",
            allowanceName = "10 Days Leave Encashment on LTC",
            entitledMonthly = encashmentAmount / 12.0,
            entitledAnnual = encashmentAmount,
            statutoryAuthority = "Rule 38(A) Travel Regulations & 7th CPC Orders",
            relevantRuleId = "LEAVE_ENCASH_LTC_001",
            explanation = "10 days (Basic Pay + MSP + DA) / 30 encashment for LTC concession.",
        )
    }

    fun resolveCtgEntitlement(
        context: ActiveSituationalContext,
        basicPay: Double,
    ): ResolvedEntitlement {
        val ctgAmount = basicPay * 0.80
        return ResolvedEntitlement(
            allowanceKey = "CTG",
            allowanceName = "Composite Transfer Grant (80% Basic Pay)",
            entitledMonthly = ctgAmount / 12.0,
            entitledAnnual = ctgAmount,
            statutoryAuthority = "MoD Order 19030/1/2017-E.IV dated 13.07.2017",
            relevantRuleId = "TRANSFER_CTG_001",
            explanation = "Composite Transfer Grant upon permanent transfer posting.",
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

    fun resolveAllEntitlements(
        context: ActiveSituationalContext,
        basicPay: Double,
        msp: Double,
    ): List<ResolvedEntitlement> {
        val list = mutableListOf<ResolvedEntitlement>()
        val tiles = context.activeTileIds

        if (tiles.contains(SituationalTileKeys.POST_PEACE_HIGHER) || tiles.contains(SituationalTileKeys.POST_PEACE_OTHER)) {
            list.add(resolveTptaEntitlement(context, basicPay))
        }
        if (tiles.contains(SituationalTileKeys.HOUSE_FAMILY_SPR) || tiles.contains(SituationalTileKeys.HOUSE_LIVING_OUT_NAC)) {
            list.add(resolveHraEntitlement(context, basicPay))
        }
        if (context.numberOfChildrenCea > 0 || tiles.contains(SituationalTileKeys.CEA_ONE_CHILD) || tiles.contains(SituationalTileKeys.CEA_TWO_CHILDREN) || tiles.contains(SituationalTileKeys.CEA_HOSTEL)) {
            list.add(resolveEducationEntitlement(context))
        }
        if (tiles.contains(SituationalTileKeys.AVAILED_LTC)) {
            list.add(resolveLtcEncashment(context, basicPay, msp))
        }
        if (tiles.contains(SituationalTileKeys.TRANSFER_CTG)) {
            list.add(resolveCtgEntitlement(context, basicPay))
        }
        if (tiles.contains(SituationalTileKeys.POST_SDA_NE)) {
            list.add(resolveSpecialDutyAllowance(context, basicPay))
        }

        return list
    }

    private fun resolveEffectiveDa(context: ActiveSituationalContext): Double {
        return context.customDaPercent ?: context.inferredFlags.inferredDaPercent
    }
}
