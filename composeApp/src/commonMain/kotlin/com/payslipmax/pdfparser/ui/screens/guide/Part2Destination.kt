package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.model.Part2Lookup
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.part2.domain.knowsCode
import com.payslipmax.pdfparser.part2.domain.knowsRibbon

/**
 * The levels of Part II Orders inside the Guide's stack. Ids only: a ribbon id or a manual code, never a
 * name, a query or anything the officer typed. It is a sealed sub-interface so the Guide's `when` needs one branch for all of them; it must stay in this
 * package, which Kotlin requires of every inheritor of [GuideDestination].
 */
sealed interface Part2Destination : GuideDestination {
    data object Home : Part2Destination

    data class Ribbon(val ribbonId: String) : Part2Destination

    /** Any one of a serial's codes; a variant serial opens the same page whichever of its codes is named. */
    data class Code(val code: String) : Part2Destination

    /** The Orders Bank list. */
    data object Bank : Part2Destination

    /** One Bank entry's form: an existing entry's id, or the id a new entry will be saved under. Never the casualty, a date or a word. */
    data class Entry(val entryId: String) : Part2Destination

    /** The attach flow: scan an order and tie it to casualties. [entryId] is the casualty it was opened from, ticked at the start. Never saved: its scan cannot outlive the process. */
    data class Attach(val entryId: String? = null) : Part2Destination

    /** One scanned order: its pages, casualties, counter and chain check. */
    data class Order(val orderId: String) : Part2Destination

    /** The orders shelf: the Bank seen by order. */
    data object Shelf : Part2Destination
}

private const val HOME = "home"
private const val RIBBON = "ribbon"
private const val CODE = "code"
private const val BANK = "bank"
private const val ENTRY = "entry"
private const val ORDER = "order"
private const val SHELF = "shelf"

/** The saved form of a level after the `part2` prefix: its kind, then its one id. Ids only, never a name or a query. */
internal fun encodePart2(destination: Part2Destination): List<String> =
    when (destination) {
        Part2Destination.Home -> listOf(HOME)
        is Part2Destination.Ribbon -> listOf(RIBBON, destination.ribbonId)
        is Part2Destination.Code -> listOf(CODE, destination.code)
        Part2Destination.Bank -> listOf(BANK)
        is Part2Destination.Entry -> listOf(ENTRY, destination.entryId)
        is Part2Destination.Order -> listOf(ORDER, destination.orderId)
        Part2Destination.Shelf -> listOf(SHELF)
        // Never reaches the saved stack (see GuideNavStateSaver): the scan it holds would not survive the process.
        is Part2Destination.Attach -> emptyList()
    }

/** Reads [encodePart2] back; anything else (a missing id, an extra part, an unknown kind) is null, which cuts the stack there. */
internal fun decodePart2(parts: List<String>): Part2Destination? {
    val id = parts.getOrNull(1)?.takeIf { it.isNotEmpty() }
    return when (parts.firstOrNull()) {
        HOME -> Part2Destination.Home.takeIf { parts.size == 1 }
        RIBBON -> id?.takeIf { parts.size == 2 }?.let(Part2Destination::Ribbon)
        CODE -> id?.takeIf { parts.size == 2 }?.let(Part2Destination::Code)
        BANK -> Part2Destination.Bank.takeIf { parts.size == 1 }
        ENTRY -> id?.takeIf { parts.size == 2 && Part2Limits.isPlainId(it) }?.let(Part2Destination::Entry)
        ORDER -> id?.takeIf { parts.size == 2 && Part2Limits.isPlainId(it) }?.let(Part2Destination::Order)
        SHELF -> Part2Destination.Shelf.takeIf { parts.size == 1 }
        else -> null
    }
}

/** Whether this lookup still offers the place: any ribbon, any code (search reaches every code page), the Bank, or an entry level with a plausible id (the store is asked separately). */
internal fun Part2Lookup.knows(destination: Part2Destination): Boolean =
    when (destination) {
        Part2Destination.Home -> true
        is Part2Destination.Ribbon -> knowsRibbon(destination.ribbonId)
        is Part2Destination.Code -> knowsCode(destination.code)
        Part2Destination.Bank -> true
        is Part2Destination.Entry -> Part2Limits.isPlainId(destination.entryId)
        is Part2Destination.Order -> Part2Limits.isPlainId(destination.orderId)
        Part2Destination.Shelf, is Part2Destination.Attach -> true
    }
