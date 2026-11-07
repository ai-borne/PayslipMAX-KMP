package com.payslipmax.pdfparser.guide.summary

/**
 * Everything the TD summary card shows, already worked out from the bundle's approved figures and one DA percent. Amounts are paise
 * so a half rupee (Rs 2,812.50 is 281250) is never rounded. Built by [TdSummaryBuilder]; the UI only lays it out.
 *
 * [daBandFrom] to [daBandTo] is the DA range the table is drawn for ("DA 50 to 99%"), and [stepPercent] the rise over the base it
 * applies. The card names the band, never a DA number, because the step is the same at 60% and 63%.
 */
data class TdSummary(
    val stepPercent: Int,
    val daBandFrom: Int,
    val daBandTo: Int,
    val levelGroups: List<TdLevelGroup>,
    val foodTimings: List<TdFoodTiming>,
) {
    /** The group whose pay levels include [level] (as printed on the payslip, for example "12A"), or null: a row is never guessed. */
    fun groupFor(level: String): TdLevelGroup? = levelGroups.firstOrNull { level in it.levels }
}

/**
 * One row: the pay [levels] that share these rates. A null amount means the bundle holds no figure for it, so the cell is left out.
 * [foodByTimingPaise] lines up with [TdSummary.foodTimings] and is empty when there is no food figure.
 */
data class TdLevelGroup(
    val levels: List<String>,
    val hotelPaise: Long?,
    val taxi: TdTaxi?,
    val foodPaise: Long?,
    val foodByTimingPaise: List<Long>,
)

/** Taxi on TD is a rupee cap for the lowest levels and only a worded limit above them: never an invented amount. */
sealed interface TdTaxi {
    data class Amount(val paise: Long) : TdTaxi

    data class Limit(val text: String) : TdTaxi
}

/** From [fromMinutes] away the day's food is [percent] of the full amount. */
data class TdFoodTiming(
    val fromMinutes: Int,
    val percent: Int,
)
