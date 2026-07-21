package com.payslipmax.pdfparser.insights

object TaxRecommendationEngine {
    private const val LIMIT_80C = 150_000.0
    private const val LIMIT_80CCD1B = 50_000.0

    fun generateOpportunities(
        annualDsop: Double,
        annualAgif: Double,
        marginalRate: Double,
        currentMonthNum: Int = 11,
    ): List<Opportunity> {
        val totalAuto80C = annualDsop + annualAgif
        val is80CCapped = totalAuto80C >= LIMIT_80C

        return buildList {
            if (is80CCapped) {
                add(
                    Opportunity(
                        id = "80c_dsop_capped",
                        title = "Sec 80C Limit Fully Capped",
                        unusedAmount = 0.0,
                        estTaxSaved = 0.0,
                        action = "Your DSOP + AGIF contributions (₹${TaxLedgerAggregator.formatIndianCurrency(totalAuto80C)}) fully cap Section 80C (₹1.5L). No extra ELSS investment is required.",
                    ),
                )
            } else {
                val headroom = maxOf(0.0, LIMIT_80C - totalAuto80C)
                val monthlyIncrease = (headroom / 12.0).toInt()
                add(
                    Opportunity(
                        id = "80c_dsop",
                        title = "80C: Increase DSOP Subscription",
                        unusedAmount = headroom,
                        estTaxSaved = headroom * marginalRate,
                        action = "Increase DSOP by ₹$monthlyIncrease/month to utilize your remaining ₹${TaxLedgerAggregator.formatIndianCurrency(headroom)} 80C headroom.",
                    ),
                )
            }

            add(
                Opportunity(
                    id = "80ccd_nps",
                    title = "80CCD(1B): NPS Additional Deduction",
                    unusedAmount = LIMIT_80CCD1B,
                    estTaxSaved = LIMIT_80CCD1B * marginalRate,
                    action = "Invest ₹50,000/year in Tier-1 NPS to claim an additional tax deduction under Sec 80CCD(1B).",
                ),
            )

            if (currentMonthNum in 10..12) {
                add(
                    Opportunity(
                        id = "pcda_declaration_cutoff",
                        title = "PCDA Tax Declaration Deadline",
                        unusedAmount = 0.0,
                        estTaxSaved = 0.0,
                        action = "Submit your tax regime selection to PCDA before December to avoid high TDS deductions in Jan–Mar.",
                    ),
                )
            }
        }
    }
}
