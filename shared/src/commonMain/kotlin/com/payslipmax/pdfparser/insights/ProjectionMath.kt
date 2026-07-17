package com.payslipmax.pdfparser.insights

data class ProjectionResult(
    val years: Int,
    val totalContributions: Double,
    val totalInterest: Double,
    val projectedBalance: Double,
)

object ProjectionMath {
    fun calculateProjection(
        initialBalance: Double,
        monthlySubscription: Double,
        years: Int,
        annualRate: Double = 0.071,
    ): ProjectionResult {
        var balance = initialBalance
        val monthlyRate = annualRate / 12.0
        val totalMonths = years * 12
        var contributions = 0.0

        for (m in 1..totalMonths) {
            contributions += monthlySubscription
            balance = (balance + monthlySubscription) * (1.0 + monthlyRate)
        }

        val totalInterest = balance - initialBalance - contributions
        return ProjectionResult(years, contributions, totalInterest, balance)
    }
}
