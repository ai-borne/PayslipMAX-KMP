package com.payslipmax.pcdao.reconciliation

class SituationalRuleResolver(
    private val operationalResolvers: OperationalAllowanceResolvers = OperationalAllowanceResolvers(),
    private val housingResolver: HousingAllowanceResolver = HousingAllowanceResolver(),
    private val personalResolver: PersonalBenefitResolvers = PersonalBenefitResolvers(),
) {
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

        return EscalatedRates(
            daRate = daPercent,
            isEscalated = isEscalated,
            ceaMonthlyPerChild = ceaMonthly,
            ceaAnnualPerChild = ceaMonthly * 12.0,
            hostelSubsidyMonthly = hostelMonthly,
            hostelSubsidyAnnual = hostelMonthly * 12.0,
            dressAllowanceAnnual = dressAnnual,
            hraRates = hraRates,
            siachenMonthlyRate = 42500.0 * multiplier,
            hafaMonthlyRate = 16900.0 * multiplier,
            cfaaMonthlyRate = 10500.0 * multiplier,
            cmfaaMonthlyRate = 6300.0 * multiplier,
        )
    }

    fun calculateNpaAmount(basicPay: Double): Double {
        val rawNpa = basicPay * 0.20
        val maxAllowableNpa = (237500.0 - basicPay).coerceAtLeast(0.0)
        return minOf(rawNpa, maxAllowableNpa)
    }

    fun calculateEffectivePay(
        context: ActiveSituationalContext,
        basicPay: Double,
    ): Double {
        val hasNpa =
            context.activeTileIds.contains(SituationalTileKeys.CADRE_AMC_NPA) ||
                context.activeSpecializedFactors.contains(SpecializedMilitaryFactor.NON_PRACTICING_ALLOWANCE_AMC)
        return if (hasNpa) basicPay + calculateNpaAmount(basicPay) else basicPay
    }

    fun calculateDearnessAllowance(
        context: ActiveSituationalContext,
        basicPay: Double,
    ): Double {
        val effectivePay = calculateEffectivePay(context, basicPay)
        val daPercent = resolveEffectiveDa(context)
        return effectivePay * (daPercent / 100.0)
    }

    fun resolveNpaEntitlement(
        context: ActiveSituationalContext,
        basicPay: Double,
    ): ResolvedEntitlement {
        val npaAmount = calculateNpaAmount(basicPay)
        val isCapped = (basicPay + basicPay * 0.20) > 237500.0
        val explanation =
            if (isCapped) {
                "NPA of 20% capped at ₹${npaAmount.toInt()}/mo to maintain ₹2,37,500 Apex ceiling."
            } else {
                "20% NPA on Basic Pay ₹${basicPay.toInt()} = ₹${npaAmount.toInt()}/mo."
            }
        return ResolvedEntitlement(
            allowanceKey = "NPA",
            allowanceName = "Non-Practicing Allowance (AMC/ADC/RVC)",
            entitledMonthly = npaAmount,
            entitledAnnual = npaAmount * 12.0,
            statutoryAuthority = "MoD Letter No. 1(7)/2017/D(Pay/Services) dated 28.09.2017",
            relevantRuleId = "NPA_002",
            explanation = explanation,
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
        val effectivePay = calculateEffectivePay(context, basicPay)
        return housingResolver.resolveHraEntitlement(context, effectivePay, da, escalated)
    }

    fun resolveTwoLocationConcessionEntitlement(
        context: ActiveSituationalContext,
        basicPay: Double,
    ): ResolvedEntitlement {
        val da = resolveEffectiveDa(context)
        val escalated = calculateEscalatedRates(da)
        val effectivePay = calculateEffectivePay(context, basicPay)
        return housingResolver.resolveTwoLocationConcessionEntitlement(context, effectivePay, da, escalated)
    }

    fun resolveEducationEntitlement(context: ActiveSituationalContext): ResolvedEntitlement {
        val da = resolveEffectiveDa(context)
        val escalated = calculateEscalatedRates(da)
        return personalResolver.resolveEducationEntitlement(context, escalated)
    }

    fun resolveLtcEncashment(
        context: ActiveSituationalContext,
        basicPay: Double,
        msp: Double,
    ): ResolvedEntitlement {
        val da = resolveEffectiveDa(context)
        val effectivePay = calculateEffectivePay(context, basicPay)
        return personalResolver.resolveLtcEncashment(context, effectivePay, msp, da)
    }

    fun resolveCtgEntitlement(
        context: ActiveSituationalContext,
        basicPay: Double,
    ): ResolvedEntitlement {
        val effectivePay = calculateEffectivePay(context, basicPay)
        return personalResolver.resolveCtgEntitlement(context, effectivePay)
    }

    fun resolveSiachenEntitlement(context: ActiveSituationalContext): ResolvedEntitlement =
        operationalResolvers.resolveSiachenEntitlement(context, resolveEffectiveDa(context) >= 50.0)

    fun resolveHafaaEntitlement(context: ActiveSituationalContext): ResolvedEntitlement =
        operationalResolvers.resolveHafaaEntitlement(context, resolveEffectiveDa(context) >= 50.0)

    fun resolveCfaaEntitlement(context: ActiveSituationalContext): ResolvedEntitlement =
        operationalResolvers.resolveCfaaEntitlement(context, resolveEffectiveDa(context) >= 50.0)

    fun resolveCmfaaEntitlement(context: ActiveSituationalContext): ResolvedEntitlement =
        operationalResolvers.resolveCmfaaEntitlement(context, resolveEffectiveDa(context) >= 50.0)

    fun resolveSpecialDutyAllowance(
        context: ActiveSituationalContext,
        basicPay: Double,
    ): ResolvedEntitlement = operationalResolvers.resolveSpecialDutyAllowance(context, basicPay)

    fun resolveIslandSpecialDutyAllowance(
        context: ActiveSituationalContext,
        basicPay: Double,
        tierPercent: Double = 16.0,
    ): ResolvedEntitlement =
        operationalResolvers.resolveIslandSpecialDutyAllowance(context, basicPay, tierPercent)

    fun resolveTrainingAllowance(
        context: ActiveSituationalContext,
        basicPay: Double,
        isNationalAcademy: Boolean = false,
    ): ResolvedEntitlement =
        operationalResolvers.resolveTrainingAllowance(context, basicPay, isNationalAcademy)

    fun resolveTechnicalPayEntitlement(
        context: ActiveSituationalContext,
        tier: Int = 1,
    ): ResolvedEntitlement =
        operationalResolvers.resolveTechnicalPayEntitlement(context, tier)

    fun resolveParachuteAllowance(context: ActiveSituationalContext): ResolvedEntitlement =
        operationalResolvers.resolveParachuteAllowance(context, resolveEffectiveDa(context) >= 50.0)

    fun resolveAllEntitlements(
        context: ActiveSituationalContext,
        basicPay: Double,
        msp: Double,
    ): List<ResolvedEntitlement> {
        val list = mutableListOf<ResolvedEntitlement>()
        val tiles = context.activeTileIds
        val factors = context.activeSpecializedFactors
        val effectivePay = calculateEffectivePay(context, basicPay)

        if (tiles.contains(SituationalTileKeys.CADRE_AMC_NPA) ||
            factors.contains(SpecializedMilitaryFactor.NON_PRACTICING_ALLOWANCE_AMC)
        ) {
            list.add(resolveNpaEntitlement(context, basicPay))
        }
        if (tiles.contains(SituationalTileKeys.POST_PEACE_HIGHER) || tiles.contains(SituationalTileKeys.POST_PEACE_OTHER)) {
            list.add(resolveTptaEntitlement(context, effectivePay))
        }
        if (tiles.contains(SituationalTileKeys.HOUSE_TWO_LOCATION_CONCESSION)) {
            list.add(resolveTwoLocationConcessionEntitlement(context, effectivePay))
        } else if (tiles.contains(SituationalTileKeys.HOUSE_FAMILY_SPR) || tiles.contains(SituationalTileKeys.HOUSE_LIVING_OUT_NAC)) {
            list.add(resolveHraEntitlement(context, effectivePay))
        }
        if (context.numberOfChildrenCea > 0 || tiles.contains(SituationalTileKeys.CEA_ONE_CHILD) || tiles.contains(SituationalTileKeys.CEA_TWO_CHILDREN) || tiles.contains(SituationalTileKeys.CEA_HOSTEL)) {
            list.add(resolveEducationEntitlement(context))
        }
        if (tiles.contains(SituationalTileKeys.POST_SIACHEN) || factors.contains(SpecializedMilitaryFactor.SIACHEN_GLACIER)) {
            list.add(resolveSiachenEntitlement(context))
        }
        if (tiles.contains(SituationalTileKeys.POST_FIELD_HAFAA)) {
            list.add(resolveHafaaEntitlement(context))
        }
        if (tiles.contains(SituationalTileKeys.POST_FIELD_CFAA)) {
            list.add(resolveCfaaEntitlement(context))
        }
        if (tiles.contains(SituationalTileKeys.POST_FIELD_CMFAA)) {
            list.add(resolveCmfaaEntitlement(context))
        }
        if (tiles.contains(SituationalTileKeys.POST_SDA_NE)) {
            list.add(resolveSpecialDutyAllowance(context, basicPay))
        }
        if (tiles.contains(SituationalTileKeys.POST_ISDA_ISLAND) || factors.contains(SpecializedMilitaryFactor.ISLAND_SPECIAL_DUTY)) {
            list.add(resolveIslandSpecialDutyAllowance(context, basicPay))
        }
        if (factors.contains(SpecializedMilitaryFactor.TRAINING_ALLOWANCE) || tiles.contains(SituationalTileKeys.DUTY_COURSE_LONG)) {
            list.add(resolveTrainingAllowance(context, basicPay))
        }
        if (tiles.contains(SituationalTileKeys.CADRE_TECHNICAL_OFFICER) || factors.contains(SpecializedMilitaryFactor.TECHNICAL_ALLOWANCE)) {
            list.add(resolveTechnicalPayEntitlement(context))
        }
        if (factors.contains(SpecializedMilitaryFactor.PARACHUTE_ALLOWANCE)) {
            list.add(resolveParachuteAllowance(context))
        }
        if (tiles.contains(SituationalTileKeys.AVAILED_LTC)) {
            list.add(resolveLtcEncashment(context, basicPay, msp))
        }
        if (tiles.contains(SituationalTileKeys.TRANSFER_CTG)) {
            list.add(resolveCtgEntitlement(context, basicPay))
        }

        return list
    }

    private fun resolveEffectiveDa(context: ActiveSituationalContext): Double {
        return context.customDaPercent ?: context.inferredFlags.inferredDaPercent
    }
}
