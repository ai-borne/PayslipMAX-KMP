package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import org.junit.Test
import java.lang.reflect.ParameterizedType
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The slip is handed to a clerk, so it must be unable to carry who the officer is: no name, rank, number, PAN or account field can
 * exist on any slip type, and everything printed on a slip is either a template's own words or something the officer typed for that
 * slip. Both halves are tested, because a type check alone would miss text that leaked in through a narrative.
 */
class Part2SlipPrivacyTest {
    private val slipTypes =
        listOf(
            Part2Slip::class.java,
            Part2Order::class.java,
            Part2Occurrence::class.java,
            Part2Origin.Linked::class.java,
            RuleFinding::class.java,
            UndraftedLink::class.java,
            OrderRef::class.java,
        )

    private val identityWords =
        setOf("name", "officer", "pan", "account", "cda", "persno", "pers", "rank", "email", "phone", "mobile", "address", "dob", "aadhaar", "arm", "unit")

    private fun words(identifier: String): List<String> = identifier.split(Regex("(?<=[a-z0-9])(?=[A-Z])|_")).map { it.lowercase() }

    @Test
    fun noSlipTypeHasAFieldThatCouldHoldWhoTheOfficerIs() {
        for (type in slipTypes) {
            for (field in type.declaredFields.filterNot { it.isSynthetic }) {
                val clash = words(field.name).filter { it in identityWords }
                assertTrue(clash.isEmpty(), "${type.simpleName}.${field.name} could hold identity data: $clash")
            }
        }
    }

    @Test
    fun everySlipFieldIsAPlainValueNeverAnOpenMapOrAnyObject() {
        val allowed = setOf("String", "int", "Integer", "CalendarDate", "List", "OrderFamily", "RuleSeverity", "LinkBasis", "Part2Origin", "Linked", "OrderRef", "Part2Occurrence", "Part2Order", "RuleFinding", "UndraftedLink")
        for (type in slipTypes) {
            for (field in type.declaredFields.filterNot { it.isSynthetic }) {
                val outer = field.type.simpleName
                assertTrue(outer in allowed, "${type.simpleName}.${field.name} is a $outer")
                (field.genericType as? ParameterizedType)?.actualTypeArguments?.forEach {
                    assertTrue((it as? Class<*>)?.simpleName in allowed || it.typeName.substringAfterLast('.').removePrefix("? extends ").substringAfterLast('.') in allowed, "${type.simpleName}.${field.name}: ${it.typeName}")
                }
            }
        }
    }

    @Test
    fun everyWordOnARenderedSlipIsATemplateWordOrSomethingTheOfficerTyped() {
        val typed = "Zephyr7"
        val values =
            mapOf(
                "noted_on" to SlotValue.DateValue(CalendarDate(2026, 7, 5)),
                "remark" to SlotValue.TextValue(typed),
            )
        val engine = Part2DraftEngine(SyntheticPart2Templates.lookup, SyntheticPart2Templates.registry())
        val last = OrderRef("LAST/9", CalendarDate(2026, 6, 1))
        val slip = assertIs<DraftResult.Drafted>(engine.draft(LeaveEventInput(listOf(OccurrenceRequest(SyntheticPart2Templates.NOTE, values)), last))).slip
        val printed = slipText(slip)
        val fixed = SyntheticPart2Templates.templates.flatMap { listOfNotNull(it.narrative, it.authority, it.certificate) }.flatMap { literalWords(it) }.toSet()
        val officerTyped = setOf(typed, "05/07/2026", "LAST/9", "01/06/2026")
        val strangers = printed.filter { it !in fixed && it !in officerTyped && it !in codes() }
        assertEquals(emptyList(), strangers, "text on the slip that neither a template nor the officer supplied")
        assertFalse(printed.any { "AR*****" in it || '{' in it || '}' in it }, "no PAN placeholder and no unfilled blank")
    }

    private fun codes(): Set<String> = SyntheticPart2Templates.templates.flatMap { listOfNotNull(it.code, it.print) }.toSet()

    private fun literalWords(template: String): List<String> =
        template.split('{').flatMap { piece -> if ('}' in piece) piece.substringAfter('}').split(' ') else piece.split(' ') }.filter { it.isNotBlank() }

    private fun slipText(slip: Part2Slip): List<String> =
        slip.orders.flatMap { it.occurrences }.flatMap { occurrence ->
            listOfNotNull(occurrence.code, occurrence.narrative, occurrence.authority, occurrence.certificate) + occurrence.data
        }.flatMap { it.split(' ') }.filter { it.isNotBlank() } +
            listOfNotNull(slip.lastOrder?.orderNo, slip.lastOrder?.date?.let { com.payslipmax.pdfparser.calendar.CivilCalendar.format(it) })
}
