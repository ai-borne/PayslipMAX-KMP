package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** The flag-off wiring: nothing is handed to the Guide tab and the Koin lookup behind it is never made. */
class PreClaimFactoryTest {
    private var lookups = 0

    private fun lookup(): PreClaimViewModel {
        lookups++
        error("no worksheet in this test")
    }

    @Test
    fun withTheFlagOffNoFactoryExistsAndTheLookupIsNeverMade() {
        assertNull(preClaimFactoryOrNull(enabled = false, create = ::lookup))
        assertEquals(0, lookups)
    }

    @Test
    fun withTheFlagOnTheLookupHappensOnlyWhenAScreenAsksForAWorksheet() {
        val factory = preClaimFactoryOrNull(enabled = true, create = ::lookup)
        assertNotNull(factory)
        assertEquals(0, lookups, "creating the factory must not build a worksheet")
        assertFailsWith<IllegalStateException> { factory() }
        assertEquals(1, lookups)
    }

    @Test
    fun theShippedFlagIsOffSoTheDefaultFactoryIsNull() {
        assertNull(preClaimFactoryOrNull(create = ::lookup))
        assertEquals(0, lookups)
    }
}
