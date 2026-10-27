package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.part2.domain.RealPart2.date
import com.payslipmax.pdfparser.part2.domain.RealPart2.ref
import com.payslipmax.pdfparser.part2.domain.RealPart2.text

/** Synthetic answers for the area re-grant tests (phase 9): invented stations, Corps, notifications and operations, never one from a real order. */
internal object AreaAnswers {
    val haa: Map<String, SlotValue> = mapOf("station" to text("ZZ01"), "haa_corps" to text("Alpha"), "haa_notif" to ref("N-17/2026", 3, 2), "haa_serial" to text("12"))
    val sccia: Map<String, SlotValue> = mapOf("operation" to text("OP TESTONE"), "sccia_corps" to text("Bravo"), "sccia_notif" to ref("N-21/2026", 4, 9), "sccia_serial" to text("7"))
    val field: Map<String, SlotValue> =
        mapOf("location" to text("Testpur"), "field_corps" to text("Charlie"), "field_notif" to ref("N-33/2026", 5, 11), "field_serial" to text("41"), "sanction_to" to date(12, 31))
    val tran: Map<String, SlotValue> = mapOf("tran_station" to text("Testville"), "tran_disability" to text("hearing impairment"), "tran_rank" to text("Maj Gen"))
    val everything: Map<String, SlotValue> = haa + sccia + field + tran

    fun haa(code: String) = AreaRules.ANSWER_HAA_PREFIX + code

    fun sccia(code: String) = AreaRules.ANSWER_SCCIA_PREFIX + code

    fun field(code: String) = AreaRules.ANSWER_FIELD_PREFIX + code

    fun tran(code: String) = AreaRules.ANSWER_TRAN_PREFIX + code

    /** A leave of [code] from [fromMonth]/[fromDay] to [toMonth]/[toDay] 2026 with the area details and every answer in [answers]. */
    fun leave(
        code: String,
        from: Pair<Int, Int>,
        to: Pair<Int, Int>,
        vararg answers: String,
        extra: Map<String, SlotValue> = emptyMap(),
    ) = OccurrenceRequest(code, mapOf("leave_from" to date(from.first, from.second), "leave_to" to date(to.first, to.second), "year" to RealPart2.number(2026)) + everything + extra, answers.toSet())

    fun codes(request: OccurrenceRequest): List<String> = RealPart2.rows(RealPart2.draft(request)).map { it.code }
}
