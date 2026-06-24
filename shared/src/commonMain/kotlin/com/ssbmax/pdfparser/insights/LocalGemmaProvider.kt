package com.ssbmax.pdfparser.insights

import com.ssbmax.pdfparser.domain.ParsedPayslip
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LocalGemmaProvider : AIInsightProvider {
    override suspend fun generateInsights(payload: PromptPayload): Result<String> {
        return try {
            val report = buildReport(payload)
            val jsonStr = Json.encodeToString(report)
            Result.success(jsonStr)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildReport(payload: PromptPayload): AiInsightReport {
        val anomalies = payload.engineResult.anomalies
        val payslip = payload.sanitizedPayslip

        val (salaryChanges, salaryActions) = buildSalaryChanges(anomalies)
        val (missingAllowances, allowanceActions) = buildMissingAllowances(anomalies)
        val newDeductions = buildNewDeductions(anomalies)
        val (riskAlerts, riskActions) = buildRiskAlerts(anomalies, payslip)
        val opportunities = buildOpportunities(payslip)
        val actionRequired = salaryActions + allowanceActions + riskActions

        return AiInsightReport(
            salaryChanges = salaryChanges,
            missingAllowances = missingAllowances,
            newDeductions = newDeductions,
            riskAlerts = riskAlerts,
            opportunities = opportunities,
            actionRequired = actionRequired,
        )
    }

    private fun buildSalaryChanges(anomalies: List<Anomaly>): Pair<List<SalaryChangeItem>, List<String>> {
        val actions = mutableListOf<String>()
        val items =
            anomalies.filter { it.type == "SALARY_LOSS" }.map {
                actions.add("Verify basic pay and submit representation for salary reduction.")
                SalaryChangeItem(
                    item = it.field.ifEmpty { "Basic Pay" },
                    change = "decreased",
                    amount = it.amount,
                )
            }
        val finalItems =
            items.ifEmpty {
                listOf(SalaryChangeItem(item = "Basic Pay", change = "unchanged", amount = 0.0))
            }
        return Pair(finalItems, actions)
    }

    private fun buildMissingAllowances(anomalies: List<Anomaly>): Pair<List<String>, List<String>> {
        val names = mutableListOf<String>()
        val actions = mutableListOf<String>()
        anomalies.forEach { anomaly ->
            if (anomaly.type == "MISSING_ALLOWANCE" || anomaly.type == "TPTA_ENTITLEMENT") {
                names.add(anomaly.field)
                val typeName = if (anomaly.type == "MISSING_ALLOWANCE") "missing" else "discrepant"
                actions.add("Submit representation letter for $typeName ${anomaly.field} allowance.")
            }
        }
        return Pair(names, actions)
    }

    private fun buildNewDeductions(anomalies: List<Anomaly>): List<NewDeductionItem> {
        return anomalies.filter { it.type == "DEDUCTION_SPIKE" }.map {
            NewDeductionItem(
                item = it.field,
                change = "increased",
                amount = it.amount,
            )
        }
    }

    private fun buildRiskAlerts(
        anomalies: List<Anomaly>,
        payslip: ParsedPayslip,
    ): Pair<List<RiskAlertItem>, List<String>> {
        val alerts = mutableListOf<RiskAlertItem>()
        val actions = mutableListOf<String>()
        anomalies.forEach { anomaly ->
            when (anomaly.type) {
                "DSOP_COMPLIANCE" -> {
                    alerts.add(
                        RiskAlertItem(
                            observation = "DSOP subscription non-compliance: ${anomaly.description}",
                            action = "Adjust monthly subscription to be within permissible limits (6% to 100% of basic pay).",
                        ),
                    )
                    actions.add("Adjust DSOP subscription rate in next monthly cycle.")
                }
                "RENT_RECOVERY_RISK" -> {
                    alerts.add(
                        RiskAlertItem(
                            observation = anomaly.description,
                            action = "Submit occupancy returns and clarify quarters rent recovery status with PCDA.",
                        ),
                    )
                }
                "DEBIT_RECOVERY" -> {
                    alerts.add(
                        RiskAlertItem(
                            observation = "Unexpected debit recovery: ${anomaly.description}",
                            action = "Check DOB Part II order or recovery schedule for details.",
                        ),
                    )
                }
            }
        }
        alerts.add(
            RiskAlertItem(
                observation = "Offline local audit completed.",
                action = "Ensure settings match PCDA records.",
            ),
        )
        return Pair(alerts, actions)
    }

    private fun buildOpportunities(payslip: ParsedPayslip): List<OpportunityItem> {
        val optResult = WealthOptimizationEngine.analyze(payslip)
        return optResult.opportunities.map {
            OpportunityItem(
                opportunity = it.title,
                action = it.action,
            )
        }
    }
}
