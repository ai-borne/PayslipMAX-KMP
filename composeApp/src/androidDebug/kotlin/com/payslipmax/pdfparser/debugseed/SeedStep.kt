package com.payslipmax.pdfparser.debugseed

/** One button's worth of synthetic payslips. A follow-up [requires] its base step to be seeded first. */
enum class SeedStep(val requires: SeedStep? = null) {
    PROVEN_TPTA,
    HELD_TPTA,
    HELD_FOR_RESOLVE,
    RESOLVE_BY_RELOCATION(HELD_FOR_RESOLVE),
    HELD_FOR_SURFACE,
    SURFACE_BY_SAME_CITY(HELD_FOR_SURFACE),
    INCREMENT_MISS,
}
