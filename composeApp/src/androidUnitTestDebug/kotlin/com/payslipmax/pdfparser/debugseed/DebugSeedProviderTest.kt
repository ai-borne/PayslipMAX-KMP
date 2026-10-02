package com.payslipmax.pdfparser.debugseed

import com.payslipmax.pdfparser.ui.screens.DeveloperToolsRegistry
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** The debug build's only entry point: a provider that registers the section at process start. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DebugSeedProviderTest {
    @AfterTest
    fun tearDown() = DeveloperToolsRegistry.clear()

    @Test
    fun startingTheProviderRegistersExactlyOneSettingsSection() {
        DeveloperToolsRegistry.clear()
        DebugSeedProvider().onCreate()
        DebugSeedProvider().onCreate()
        assertEquals(1, DeveloperToolsRegistry.count(), "registering twice (e.g. a re-created provider) must not duplicate the section")
    }
}
