package com.payslipmax.pdfparser.ui.screens.guide.part2.bank

import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.part2.domain.BankStatus
import com.payslipmax.pdfparser.part2.domain.VaultOrder
import com.payslipmax.pdfparser.part2.print.IndexDocument
import com.payslipmax.pdfparser.testing.SyntheticBank
import com.payslipmax.pdfparser.ui.theme.IndexStrings
import com.payslipmax.pdfparser.ui.theme.OrderStrings
import com.payslipmax.pdfparser.ui.theme.OrdersBankStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY: the paper is what the officer shows a clerk, so it must say what the Bank says (the same status words, counter and chain flag),
 * by year with the newest first, and carry only what the officer typed plus the manual's casualty names. The note and the Data columns
 * are the officer's private text and are never printed; nothing about who the officer is is known to this code at all.
 */
class IndexDocumentBuilderTest {
    private val today = CalendarDate(2026, 11, 1)
    private val names = mapOf("A1" to "First casualty", "B2" to "Second casualty")

    private fun build(
        entries: List<com.payslipmax.pdfparser.part2.domain.BankEntry>,
        orders: List<VaultOrder> = emptyList(),
    ): IndexDocument = IndexDocumentBuilder.build(entries, orders, names::get, today)

    private fun entry(
        id: String,
        code: String,
        from: CalendarDate,
        orderId: String? = null,
    ) = SyntheticBank.entry(id, code = code, from = from, orderId = orderId)

    @Test
    fun anEmptyBankHasNoYearsSoNothingIsPrinted() {
        val document = build(emptyList())

        assertEquals(emptyList(), document.years)
    }

    @Test
    fun theTopOfThePageIsTheFixedTitleTheLocalPrintedOnDateAndTheReferenceNotice() {
        val document = build(listOf(entry("e1", "A1", CalendarDate(2026, 3, 1))))

        assertEquals(IndexStrings.title, document.title)
        assertEquals("Printed on 01/11/2026", document.printedOn)
        assertEquals(OrderStrings.referenceCopy, document.notice)
        assertEquals("Page {page} of {pages}", document.pageLabel)
    }

    @Test
    fun casualtiesAreGroupedByYearOfFromNewestFirstWithCodeAndName() {
        val document =
            build(
                listOf(
                    entry("old", "B2", CalendarDate(2024, 6, 1)),
                    entry("new", "A1", CalendarDate(2026, 3, 1)),
                    entry("mid", "A1", CalendarDate(2024, 9, 1)),
                ),
            )

        assertEquals(listOf("2026", "2024"), document.years.map { it.heading })
        assertEquals(listOf("A1 · First casualty"), document.years[0].rows.map { it.headline })
        assertEquals(listOf("A1 · First casualty", "B2 · Second casualty"), document.years[1].rows.map { it.headline }, "September 2024 before June 2024")
    }

    @Test
    fun aCasualtyTheManualDoesNotNameIsPrintedByItsCodeAlone() {
        val document = build(listOf(entry("e1", "ZZ9", CalendarDate(2026, 3, 1))))

        assertEquals("ZZ9", document.years.single().rows.single().headline)
    }

    @Test
    fun aWaitingCasualtyPrintsOneLineOfDatesAndTheWaitingStatusAndNoOrder() {
        val row = build(listOf(entry("e1", "A1", CalendarDate(2026, 3, 1)))).years.single().rows.single()

        assertEquals(
            listOf(dateRange(CalendarDate(2026, 3, 1), CalendarDate(2026, 3, 9)) + IndexStrings.separator + OrdersBankStrings.status(BankStatus.NOT_ATTACHED)),
            row.details,
        )
    }

    @Test
    fun anAttachedCasualtyAlsoPrintsItsOrderNumberAndDateAndTheOrdersCounter() {
        val order = SyntheticBank.order("o1", orderNo = "12/2026", date = CalendarDate(2026, 3, 20)).copy(expectedCount = 5)
        val entries = listOf(entry("e1", "A1", CalendarDate(2026, 3, 1), "o1"), entry("e2", "B2", CalendarDate(2026, 3, 2), "o1"))

        val details = build(entries, listOf(order)).years.single().rows.first().details

        assertEquals(2, details.size)
        assertTrue(OrdersBankStrings.status(BankStatus.ATTACHED) in details[0])
        assertEquals("Order no. 12/2026, 20/03/2026${IndexStrings.separator}2 of 5 casualties", details[1])
    }

    @Test
    fun anOrderWithNoNumberOrDateSaysSoInTheShelfsWords() {
        val order = SyntheticBank.order("o1", orderNo = null, date = null)

        val details = build(listOf(entry("e1", "A1", CalendarDate(2026, 3, 1), "o1")), listOf(order)).years.single().rows.single().details

        assertTrue(details[1].startsWith("${OrderStrings.noOrderNumber}, ${OrderStrings.noOrderDate}"), details[1])
    }

    @Test
    fun theChainFlagIsPrintedOnlyWhenTheOrderQuotesAnOrderThatIsNotInTheBank() {
        val flagged = SyntheticBank.order("o1", orderNo = "12/2026").copy(lastOrderNo = "9/2026")
        val quiet = SyntheticBank.order("o1", orderNo = "12/2026")
        val entries = listOf(entry("e1", "A1", CalendarDate(2026, 3, 1), "o1"))

        assertTrue(build(entries, listOf(flagged)).years.single().rows.single().details[1].endsWith(IndexStrings.separator + OrderStrings.checkChain))
        assertFalse(OrderStrings.checkChain in build(entries, listOf(quiet)).years.single().rows.single().details[1])
    }

    @Test
    fun anEntryWhoseOrderIsGoneIsPrintedAsWaiting() {
        val row = build(listOf(entry("e1", "A1", CalendarDate(2026, 3, 1), "deleted-order"))).years.single().rows.single()

        assertEquals(1, row.details.size)
        assertTrue(OrdersBankStrings.status(BankStatus.NOT_ATTACHED) in row.details[0])
    }

    @Test
    fun theOfficersNoteAndDataColumnsAreNeverOnThePaperAndNoOtherTextIsEither() {
        val order = SyntheticBank.order("o1", orderNo = "12/2026").copy(lastOrderNo = "9/2026")
        val document = build(listOf(entry("e1", "A1", CalendarDate(2026, 3, 1), "o1"), entry("e2", "B2", CalendarDate(2025, 3, 1))), listOf(order))

        val everyLine = listOf(document.title, document.printedOn, document.notice, document.pageLabel) + document.years.flatMap { listOf(it.heading) + it.rows.flatMap { r -> listOf(r.headline) + r.details } }
        val text = everyLine.joinToString("\n")

        assertFalse(SyntheticBank.MARKER_NOTE in text, "the note is private")
        assertFalse(SyntheticBank.MARKER_DATA in text, "the Data columns are private")
        assertFalse(SyntheticBank.MARKER_ORDER_NO in text, "this order has another number; only the order's own is printed")
        assertEquals(everyLine.size, everyLine.count { it.isNotBlank() }, "no blank line")
    }

    @Test
    fun theDocumentHidesItsContentFromAnyLog() {
        val document = build(listOf(entry("e1", "A1", CalendarDate(2026, 3, 1))))

        assertFalse("A1" in document.toString() || "First casualty" in document.toString())
        assertFalse("A1" in document.years.single().toString() || "A1" in document.years.single().rows.single().toString())
    }
}
