package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.model.Part2Lookup
import com.payslipmax.pdfparser.part2.domain.knowsCase
import com.payslipmax.pdfparser.part2.domain.knowsCode
import com.payslipmax.pdfparser.part2.domain.knowsRibbon

/**
 * The levels of Part II Orders inside the Guide's stack. Ids only: a ribbon id, a case id or a manual code, never a name, a query or
 * anything the officer typed. It is a sealed sub-interface so the Guide's `when` needs one branch for all of them; it must stay in this
 * package, which Kotlin requires of every inheritor of [GuideDestination].
 */
sealed interface Part2Destination : GuideDestination {
    data object Home : Part2Destination

    data class Ribbon(val ribbonId: String) : Part2Destination

    data class Case(val caseId: String) : Part2Destination

    /** Any one of a serial's codes; a variant serial opens the same page whichever of its codes is named. */
    data class Code(val code: String) : Part2Destination
}

private const val HOME = "home"
private const val RIBBON = "ribbon"
private const val CASE = "case"
private const val CODE = "code"

/** The saved form of a level after the `part2` prefix: its kind, then its one id. Ids only, never a name or a query. */
internal fun encodePart2(destination: Part2Destination): List<String> =
    when (destination) {
        Part2Destination.Home -> listOf(HOME)
        is Part2Destination.Ribbon -> listOf(RIBBON, destination.ribbonId)
        is Part2Destination.Case -> listOf(CASE, destination.caseId)
        is Part2Destination.Code -> listOf(CODE, destination.code)
    }

/** Reads [encodePart2] back; anything else (a missing id, an extra part, an unknown kind) is null, which cuts the stack there. */
internal fun decodePart2(parts: List<String>): Part2Destination? {
    val id = parts.getOrNull(1)?.takeIf { it.isNotEmpty() }
    return when (parts.firstOrNull()) {
        HOME -> Part2Destination.Home.takeIf { parts.size == 1 }
        RIBBON -> id?.takeIf { parts.size == 2 }?.let(Part2Destination::Ribbon)
        CASE -> id?.takeIf { parts.size == 2 }?.let(Part2Destination::Case)
        CODE -> id?.takeIf { parts.size == 2 }?.let(Part2Destination::Code)
        else -> null
    }
}

/** Whether this lookup still offers the place: a built ribbon or case, or any code (search reaches every code page). */
internal fun Part2Lookup.knows(destination: Part2Destination): Boolean =
    when (destination) {
        Part2Destination.Home -> true
        is Part2Destination.Ribbon -> knowsRibbon(destination.ribbonId)
        is Part2Destination.Case -> knowsCase(destination.caseId)
        is Part2Destination.Code -> knowsCode(destination.code)
    }
