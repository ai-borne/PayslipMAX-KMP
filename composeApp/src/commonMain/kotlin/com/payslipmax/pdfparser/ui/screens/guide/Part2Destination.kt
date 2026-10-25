package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.model.Part2Lookup
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.part2.domain.knowsCase
import com.payslipmax.pdfparser.part2.domain.knowsCode
import com.payslipmax.pdfparser.part2.domain.knowsRibbon

/**
 * The levels of Part II Orders inside the Guide's stack. Ids only: a ribbon id, a case id, a manual code or a draft's random id, never a
 * name, a query or anything the officer typed. It is a sealed sub-interface so the Guide's `when` needs one branch for all of them; it must stay in this
 * package, which Kotlin requires of every inheritor of [GuideDestination].
 */
sealed interface Part2Destination : GuideDestination {
    data object Home : Part2Destination

    data class Ribbon(val ribbonId: String) : Part2Destination

    data class Case(val caseId: String) : Part2Destination

    /** Any one of a serial's codes; a variant serial opens the same page whichever of its codes is named. */
    data class Code(val code: String) : Part2Destination

    /** What a user without Premium sees when they ask to draft: the upgrade panel. No id, nothing to restore. */
    data object Locked : Part2Destination

    /** The leave form for one draft. Only its random id is kept; the answers live in the encrypted repository. */
    data class Draft(val draftId: String) : Part2Destination

    /** The slip preview of one draft. Only its random id is kept. */
    data class Slip(val draftId: String) : Part2Destination

    /** The "Check the HRMS draft" screen of one draft. Only its random id is kept. */
    data class Hrms(val draftId: String) : Part2Destination

    /** One published order of the vault. Only its random id is kept; its number, date and pages live in the encrypted repository. */
    data class Order(val orderId: String) : Part2Destination
}

/** The vault order this level shows, for the order level; null for any other level. */
internal val Part2Destination.orderId: String?
    get() = (this as? Part2Destination.Order)?.orderId

/** The draft this level belongs to, for the three draft levels; null for any other level. */
internal val Part2Destination.draftId: String?
    get() =
        when (this) {
            is Part2Destination.Draft -> draftId
            is Part2Destination.Slip -> draftId
            is Part2Destination.Hrms -> draftId
            else -> null
        }

private const val HOME = "home"
private const val RIBBON = "ribbon"
private const val CASE = "case"
private const val CODE = "code"
private const val LOCKED = "locked"
private const val DRAFT = "draft"
private const val SLIP = "slip"
private const val HRMS = "hrms"
private const val ORDER = "order"

/** The saved form of a level after the `part2` prefix: its kind, then its one id. Ids only, never a name or a query. */
internal fun encodePart2(destination: Part2Destination): List<String> =
    when (destination) {
        Part2Destination.Home -> listOf(HOME)
        is Part2Destination.Ribbon -> listOf(RIBBON, destination.ribbonId)
        is Part2Destination.Case -> listOf(CASE, destination.caseId)
        is Part2Destination.Code -> listOf(CODE, destination.code)
        Part2Destination.Locked -> listOf(LOCKED)
        is Part2Destination.Draft -> listOf(DRAFT, destination.draftId)
        is Part2Destination.Slip -> listOf(SLIP, destination.draftId)
        is Part2Destination.Hrms -> listOf(HRMS, destination.draftId)
        is Part2Destination.Order -> listOf(ORDER, destination.orderId)
    }

/** Reads [encodePart2] back; anything else (a missing id, an extra part, an unknown kind) is null, which cuts the stack there. */
internal fun decodePart2(parts: List<String>): Part2Destination? {
    val id = parts.getOrNull(1)?.takeIf { it.isNotEmpty() }
    return when (parts.firstOrNull()) {
        HOME -> Part2Destination.Home.takeIf { parts.size == 1 }
        RIBBON -> id?.takeIf { parts.size == 2 }?.let(Part2Destination::Ribbon)
        CASE -> id?.takeIf { parts.size == 2 }?.let(Part2Destination::Case)
        CODE -> id?.takeIf { parts.size == 2 }?.let(Part2Destination::Code)
        LOCKED -> Part2Destination.Locked.takeIf { parts.size == 1 }
        DRAFT -> id?.takeIf { parts.size == 2 }?.let(Part2Destination::Draft)
        SLIP -> id?.takeIf { parts.size == 2 }?.let(Part2Destination::Slip)
        HRMS -> id?.takeIf { parts.size == 2 }?.let(Part2Destination::Hrms)
        ORDER -> id?.takeIf { parts.size == 2 }?.let(Part2Destination::Order)
        else -> null
    }
}

/** Whether this lookup still offers the place: a built ribbon or case, any code (search reaches every code page), or a draft level with a plausible id. */
internal fun Part2Lookup.knows(destination: Part2Destination): Boolean =
    when (destination) {
        Part2Destination.Home -> true
        is Part2Destination.Ribbon -> knowsRibbon(destination.ribbonId)
        is Part2Destination.Case -> knowsCase(destination.caseId)
        is Part2Destination.Code -> knowsCode(destination.code)
        Part2Destination.Locked -> true
        // A draft id is one the app made (a UUID): anything else is never looked up. Whether the repository still holds it is asked later.
        is Part2Destination.Draft -> Part2Limits.isPlainId(destination.draftId)
        is Part2Destination.Slip -> Part2Limits.isPlainId(destination.draftId)
        is Part2Destination.Hrms -> Part2Limits.isPlainId(destination.draftId)
        is Part2Destination.Order -> Part2Limits.isPlainId(destination.orderId)
    }
