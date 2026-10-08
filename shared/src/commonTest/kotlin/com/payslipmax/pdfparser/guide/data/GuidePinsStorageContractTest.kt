package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.guide.domain.GuidePins
import com.payslipmax.pdfparser.testing.FakeGuidePinsStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The behaviour every [GuidePinsStorage] must have; the fake here is the one the app's tests use. */
class GuidePinsStorageContractTest {
    @Test
    fun aFreshInstallHasNoPins() {
        assertTrue(FakeGuidePinsStorage().load().newestFirst.isEmpty())
    }

    @Test
    fun savedPinsComeBackInTheSameOrder() {
        val storage = FakeGuidePinsStorage()
        val pins = GuidePins.Empty.toggle("RB-A").toggle("RB-B")

        storage.save(pins)

        assertEquals(pins, storage.load())
    }

    @Test
    fun aDamagedStoredValueLoadsOnlyItsPlainIds() {
        val storage = FakeGuidePinsStorage(stored = "RB-A\n<b>\nRB-B")

        assertEquals(listOf("RB-B", "RB-A"), storage.load().newestFirst)
    }
}
