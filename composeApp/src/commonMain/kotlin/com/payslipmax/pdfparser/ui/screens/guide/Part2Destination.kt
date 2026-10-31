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
}

private const val HOME = "home"
private const val RIBBON = "ribbon"
private const val CODE = "code"
private const val BANK = "bank"
private const val ENTRY = "entry"

/** The saved form of a level after the `part2` prefix: its kind, then its one id. Ids only, never a name or a query. */
internal fun encodePart2(destination: Part2Destination): List<String> =
    when (destination) {
        Part2Destination.Home -> listOf(HOME)
        is Part2Destination.Ribbon -> listOf(RIBBON, destination.ribbonId)
        is Part2Destination.Code -> listOf(CODE, destination.code)
        Part2Destination.Bank -> listOf(BANK)
        is Part2Destination.Entry -> listOf(ENTRY, destination.entryId)
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
    }
