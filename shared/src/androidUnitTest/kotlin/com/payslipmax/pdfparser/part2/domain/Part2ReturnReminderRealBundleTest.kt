package com.payslipmax.pdfparser.part2.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Phase 11 item 45: every leave that sets a return reminder must carry, in its REAL template, the two date slots the reminder rules read
 * (`leave_from`, `leave_to`) and the suffix slot where the manual has one. A template that renamed one would silently stop reminding.
 */
class Part2ReturnReminderRealBundleTest {
    @Test
    fun everyLeaveThatSetsAReturnReminderDeclaresTheDateSlotsTheRulesRead() {
        val byCode = RealPart2.section.templates.associateBy { it.code }
        for (code in ReminderRules.RETURN_GRANTS) {
            val template = checkNotNull(byCode[code]) { "$code has no template" }
            val kinds = template.slots.associate { it.id to it.kind }
            assertEquals("date", kinds[LeaveRules.LEAVE_FROM], "$code: first day of leave")
            assertEquals("date", kinds[LeaveRules.LEAVE_TO], "$code: last day of leave")
        }
        assertEquals(17, ReminderRules.RETURN_GRANTS.size, "3 annual + 2 casual + 12 other leaves, not terminal leave")
        assertTrue("TERLEV" !in ReminderRules.RETURN_GRANTS)
    }
}
