package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CivilCalendar

/** One row of the slip as it was printed: the rendered text of its columns, dates as HRMS prints them. Wording-free, so a later app's edit to a label is not a change. */
data class PrintedRow(
    val code: String,
    val narrative: String,
    val from: String,
    val to: String,
    val data: List<String>,
    val authority: String?,
    val certificate: String?,
) {
    override fun toString(): String = "PrintedRow(data=${data.size})"

    companion object {
        fun of(occurrence: Part2Occurrence) =
            PrintedRow(
                code = occurrence.code,
                narrative = occurrence.narrative,
                from = occurrence.from?.let(CivilCalendar::format).orEmpty(),
                to = occurrence.to?.let(CivilCalendar::format).orEmpty(),
                data = occurrence.data,
                authority = occurrence.authority,
                certificate = occurrence.certificate,
            )
    }
}

/** How a printed draft stands now: still what the clerk was handed, edited since, or drafted from a newer template than the printed one. */
enum class PrintedStatus { CURRENT, CHANGED, TEMPLATE_UPDATED }

/** What the clerk was handed: when it was printed ([at], epoch millis), the template revision of every code on it and its rows in slip order. */
data class PrintedSnapshot(
    val at: Long,
    val revs: Map<String, String>,
    val rows: List<PrintedRow>,
) {
    override fun toString(): String = "PrintedSnapshot(rows=${rows.size})"

    /** Where this print stands against the slip the draft gives now ([slip] is null while the answers are incomplete) and the revisions it drafts from. */
    fun statusFor(
        slip: Part2Slip?,
        currentRevs: Map<String, String>,
    ): PrintedStatus =
        when {
            // Only a code in both: a code added or dropped since is the officer's edit, which the rows show.
            revs.any { (code, rev) -> currentRevs[code]?.let { it != rev } == true } -> PrintedStatus.TEMPLATE_UPDATED
            slip == null || rows != rowsOf(slip) -> PrintedStatus.CHANGED
            else -> PrintedStatus.CURRENT
        }

    companion object {
        fun of(
            slip: Part2Slip,
            revs: Map<String, String>,
            at: Long,
        ): PrintedSnapshot = PrintedSnapshot(at, revs, rowsOf(slip))

        private fun rowsOf(slip: Part2Slip) = slip.orders.flatMap { it.occurrences }.map(PrintedRow::of)
    }
}
