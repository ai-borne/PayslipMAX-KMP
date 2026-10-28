package com.payslipmax.pdfparser.testing

/**
 * The committed expectation for `LaunchFlags.PART2_ENABLED` (Phase 11 item 55): the one place, besides the flag itself, that the flip commit
 * edits. The pin test compares the two, so a flag changed by accident fails, while `scripts/run_flag_on_tests.py` switches both together in
 * a throwaway copy and runs the suites flag-on with no expected failure.
 */
object Part2FlagExpectation {
    const val PART2_ENABLED: Boolean = false
}
