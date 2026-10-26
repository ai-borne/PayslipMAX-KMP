package com.payslipmax.pdfparser.part2.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: the form offers the vault only for a box that quotes a published Part II order. The ids that say so are written in Kotlin, the
 * boxes are written in the shipped templates, so this proves the two agree on the real bundle: every vault box is a reference box that
 * exists, and the one reference box left out (the Corps notification, which is not a Part II order) is left out on purpose.
 */
class VaultOrderSlotsTest {
    private val referenceSlots = RealPart2.section.templates.flatMap { it.slots }.filter { it.kind == "ref" }.map { it.id }.toSet()

    @Test
    fun everyVaultBoxIsAReferenceBoxInTheShippedTemplates() {
        assertTrue(Part2Rules.VAULT_ORDER_SLOTS.all { it in referenceSlots }, "a vault box that no template has: ${Part2Rules.VAULT_ORDER_SLOTS - referenceSlots}")
    }

    @Test
    fun thePartIIOrdersAreVaultOrdersAndTheCorpsNotificationTheApprovingLetterAndTheMroAreNot() {
        assertEquals(
            setOf(LeaveRules.GRANT, LeaveRules.CARRY_ORDER, CancelRules.CANCELLED, LongLeaveRules.DECLARED, LongLeaveRules.ADOPTION, LtcRules.ADVANCE, LtcRules.LAST_LTC),
            Part2Rules.VAULT_ORDER_SLOTS,
        )
        assertEquals(setOf("notif", "letter", "mro"), referenceSlots - Part2Rules.VAULT_ORDER_SLOTS, "any other reference box needs a decision: is it a Part II order of the vault?")
    }
}
