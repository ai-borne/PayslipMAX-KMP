package com.payslipmax.pcdao.reconciliation

class PersonalBenefitResolvers {
    fun resolveEducationEntitlement(
        context: ActiveSituationalContext,
        escalated: EscalatedRates,
    ): ResolvedEntitlement {
        val childrenCount =
            when {
                context.numberOfChildrenCea > 0 -> context.numberOfChildrenCea.coerceIn(0, 2)
                context.activeTileIds.contains(SituationalTileKeys.CEA_TWO_CHILDREN) -> 2
                context.activeTileIds.contains(SituationalTileKeys.CEA_ONE_CHILD) -> 1
                context.activeTileIds.contains(SituationalTileKeys.CEA_HOSTEL) -> 1
                else -> 0
            }
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
        effectivePay: Double,
        msp: Double,
        da: Double,
    ): ResolvedEntitlement {
        val totalDailyPay = (effectivePay + msp) * (1.0 + da / 100.0) / 30.0
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
        effectivePay: Double,
    ): ResolvedEntitlement {
        val ctgAmount = effectivePay * 0.80
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
}
