package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.summary.TdFoodTiming
import com.payslipmax.pdfparser.guide.summary.TdLevelGroup
import com.payslipmax.pdfparser.guide.summary.TdTaxi
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimWording
import com.payslipmax.pdfparser.ui.theme.TdSummaryStrings as S

/**
 * Turns the TD summary's paise and minutes into the lines on the table (plan 24, P4). Amounts go through [PreClaimWording.rate], so a
 * half rupee always shows and nothing is rounded; a ceiling is worded "up to" and a day's food as "a day".
 */
internal object TdSummaryWording {
    private const val MINUTES_PER_HOUR = 60

    fun levels(levels: List<String>): String = if (levels.size == 1) S.level(levels.first()) else S.levels(levels.first(), levels.last())

    fun hotel(paise: Long): String = S.upToANight(PreClaimWording.rate(paise))

    fun food(paise: Long): String = S.foodADay(PreClaimWording.rate(paise))

    fun taxi(taxi: TdTaxi): String =
        when (taxi) {
            is TdTaxi.Amount -> S.nonAcTaxi(PreClaimWording.rate(taxi.paise))
            is TdTaxi.Limit -> taxi.text
        }

    fun band(
        from: Int,
        to: Int,
    ): String = S.band(from, to)

    fun cea(paise: Long): String = S.cea(PreClaimWording.rate(paise))

    fun hostel(paise: Long): String = S.hostel(PreClaimWording.rate(paise))

    /** One line per share of the day's food, named in hours. Empty unless there is an amount for every share. */
    fun timingLines(
        timings: List<TdFoodTiming>,
        amounts: List<Long>,
    ): List<String> {
        if (timings.isEmpty() || amounts.size != timings.size) return emptyList()
        if (timings.size == 1) return listOf(S.timeLine(S.anyTimeAway, PreClaimWording.rate(amounts.first())))
        return timings.indices.map { i ->
            val label =
                when (i) {
                    0 -> S.underHours(hours(timings[1].fromMinutes))
                    timings.lastIndex -> S.overHours(hours(timings[i].fromMinutes))
                    else -> S.betweenHours(hours(timings[i].fromMinutes), hours(timings[i + 1].fromMinutes))
                }
            S.timeLine(label, PreClaimWording.rate(amounts[i]))
        }
    }

    /** One row as a single sentence for TalkBack, so a reader hears the level and its three amounts together, own level first. */
    fun description(
        group: TdLevelGroup,
        timings: List<TdFoodTiming>,
        isOwn: Boolean,
    ): String =
        buildList {
            if (isOwn) add(S.yourLevelSentence)
            add("${levels(group.levels)}.")
            group.hotelPaise?.let { add(S.labelled(S.hotelLabel, hotel(it))) }
            group.taxi?.let { add(S.labelled(S.taxiLabel, taxi(it))) }
            group.foodPaise?.let { add(S.labelled(S.foodLabel, food(it))) }
            addAll(timingLines(timings, group.foodByTimingPaise).map { "$it." })
        }.joinToString(" ")

    /** A boundary in minutes as whole hours: 360 is 6, and 721 (the share that starts a minute after 12 hours) is 12. */
    private fun hours(minutes: Int): Int = if (minutes % MINUTES_PER_HOUR == 0) minutes / MINUTES_PER_HOUR else (minutes - 1) / MINUTES_PER_HOUR
}
