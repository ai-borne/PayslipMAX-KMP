package com.payslipmax.pdfparser.guide.data

import platform.Foundation.NSUserDefaults
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GuideWhatsNewStorageIosTest {
    private val suite = "guide_whats_new_ios_test"
    private val defaults = NSUserDefaults(suiteName = suite)

    @AfterTest
    fun clean() {
        defaults.removePersistentDomainForName(suite)
    }

    @Test
    fun theSeenDateSurvivesANewStorageObject() {
        IosGuideWhatsNewStorage(defaults).save("2026-10-30")

        assertEquals("2026-10-30", IosGuideWhatsNewStorage(defaults).load())
    }

    @Test
    fun aFreshInstallHasSeenNothing() {
        assertNull(IosGuideWhatsNewStorage(defaults).load())
    }
}
