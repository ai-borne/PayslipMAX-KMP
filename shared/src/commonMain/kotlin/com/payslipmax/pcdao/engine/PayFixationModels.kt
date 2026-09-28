package com.payslipmax.pcdao.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class FixationOption(val title: String) {
    @SerialName("OPTION_1")
    OPTION_1("Option 1: Fixation from Date of Promotion"),

    @SerialName("OPTION_2")
    OPTION_2("Option 2: Fixation from Date of Next Increment (DNI) in Lower Post"),
}

@Serializable
data class MonthlyPayPoint(
    val monthIndex: Int,
    val year: Int,
    val month: Int,
    val monthLabel: String,
    val option1BasicPay: Int,
    val option2BasicPay: Int,
    val delta: Int,
)

@Serializable
data class PayFixationRequest(
    val fromLevel: String,
    val fromStage: Int,
    val toLevel: String,
    val promotionDate: String,
    val dniMonth: Int,
    val mspMonthly: Int = 15500,
)

@Serializable
data class PayFixationResult(
    val fromLevel: String,
    val fromStage: Int,
    val fromBasicPay: Int,
    val toLevel: String,
    val promotionDate: String,
    val dniMonth: Int,
    val opt1FixedPay: Int,
    val opt1InitialStage: Int,
    val opt2PreDniPay: Int,
    val opt2PostDniFixedPay: Int,
    val opt2DniStage: Int,
    val opt1Total36Months: Long,
    val opt2Total36Months: Long,
    val cumulativeDelta: Long,
    val recommendedOption: FixationOption,
    val recommendationSummary: String,
    val statutoryElectionDeadline: String,
    val statutoryWarning: String,
    val monthlyTrajectory: List<MonthlyPayPoint>,
)
