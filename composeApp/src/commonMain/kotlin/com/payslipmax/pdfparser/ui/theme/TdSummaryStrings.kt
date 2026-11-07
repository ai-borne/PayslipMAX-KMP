package com.payslipmax.pdfparser.ui.theme

/**
 * Copy for the TD summary table on the Guide (plan 24). Only the words around the numbers live here: every amount is worked out by
 * `TdSummaryBuilder` from the approved figures and is passed in already formatted, so no rupee figure is written in this file.
 */
object TdSummaryStrings {
    const val yourLevel = "Your level"
    const val yourLevelSentence = "Your level."
    const val hotelLabel = "Hotel or guest house"
    const val taxiLabel = "Taxi in the city"
    const val foodLabel = "Food"
    const val anyTimeAway = "Any time away"

    fun band(
        from: Int,
        to: Int,
    ) = "Rates at DA $from to $to%"

    fun levels(
        first: String,
        last: String,
    ) = "Levels $first to $last"

    fun level(label: String) = "Level $label"

    fun upToANight(amount: String) = "Up to $amount a night"

    fun nonAcTaxi(amount: String) = "Non-AC taxi up to $amount a day"

    fun foodADay(amount: String) = "$amount a day"

    fun underHours(hours: Int) = "Under $hours hours"

    fun betweenHours(
        from: Int,
        to: Int,
    ) = "$from to $to hours"

    fun overHours(hours: Int) = "Over $hours hours"

    fun timeLine(
        label: String,
        amount: String,
    ) = "$label: $amount"

    fun labelled(
        label: String,
        value: String,
    ) = "$label: $value."
}
