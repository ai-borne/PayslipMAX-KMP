package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.guide.domain.GuidePins
import platform.Foundation.NSUserDefaults
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GuidePinsStorageIosTest {
    private val suite = "guide_pins_ios_test"
    private val defaults = NSUserDefaults(suiteName = suite)

    @AfterTest
    fun clean() {
        defaults.removePersistentDomainForName(suite)
    }

    @Test
    fun pinsSurviveANewStorageObject() {
        IosGuidePinsStorage(defaults).save(GuidePins.Empty.toggle("RB-A").toggle("RB-B"))

        assertEquals(listOf("RB-B", "RB-A"), IosGuidePinsStorage(defaults).load().newestFirst)
    }

    @Test
    fun aFreshInstallHasNoPins() {
        assertEquals(emptyList(), IosGuidePinsStorage(defaults).load().newestFirst)
    }
}
