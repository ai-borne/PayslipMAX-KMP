package com.payslipmax.pdfparser.ui

/**
 * How long a `stateIn(WhileSubscribed(...))` upstream stays alive after its last collector leaves. Covers a configuration
 * change or a quick tab switch without re-reading the source, and is short enough that a screen left behind releases it.
 */
const val UI_STOP_TIMEOUT_MS = 5_000L
