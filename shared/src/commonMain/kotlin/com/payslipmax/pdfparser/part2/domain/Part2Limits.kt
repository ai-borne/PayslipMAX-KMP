package com.payslipmax.pdfparser.part2.domain

/**
 * The bounds on what an officer can type into a Part II draft: the one home for them, so the engine and (later) the form agree. The
 * limits on bundle content ship in the bundle itself (`template_limits`). Storage caps (pages, payloads, backup rows) join here in the
 * storage phase.
 */
object Part2Limits {
    /** A typed remark, in characters. */
    const val TEXT_MAX = 200

    /** A published order number as typed, in characters. */
    const val ORDER_NO_MAX = 40

    /** A typed whole number (a year, a day count). */
    const val NUMBER_MAX = 9999

    /** Occurrence requests in one event; a leave event is a handful, never more than one per leave year. */
    const val REQUESTS_MAX = 10
}
