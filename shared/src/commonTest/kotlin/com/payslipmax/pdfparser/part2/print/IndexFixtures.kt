package com.payslipmax.pdfparser.part2.print

/** Invented index text and a deterministic measurer (half the point size per character), for the layout and printer tests. */
internal object IndexFixtures {
    val measurer = IndexTextMeasurer { text, font -> text.length * font.size * 0.5 }

    fun row(n: Int) = IndexRow("CODE$n Casualty $n", listOf("1 Jan 2024 – 3 Jan 2024 · Not attached", "Order no. $n, 5 Jan 2024 · 1 of 2 casualties"))

    fun year(
        heading: String,
        rows: Int,
        first: Int = 0,
    ) = IndexYear(heading, List(rows) { row(first + it) })

    fun document(years: List<IndexYear>) = IndexDocument("Index title", "Printed on 1 Nov 2026", "Reference copy", years, "Page {page} of {pages}")

    /** Several pages: three years of 20 rows. */
    val document = document(listOf(year("2026", 20), year("2025", 20, 20), year("2024", 20, 40)))

    val pages: List<IndexPage> get() = IndexLayout.paginate(document, measurer)
}
