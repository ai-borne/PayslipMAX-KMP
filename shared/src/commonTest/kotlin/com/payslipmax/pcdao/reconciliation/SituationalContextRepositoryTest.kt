package com.payslipmax.pcdao.reconciliation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SituationalContextRepositoryTest {
    private val repository = InMemorySituationalContextRepository()

    @Test
    fun testContextIsolationBetweenMonths() {
        val mayContext =
            ActiveSituationalContext(
                activeTileIds = setOf(SituationalTileKeys.POST_FIELD_CFAA),
                sprCityTier = "X",
            )
        val juneContext =
            ActiveSituationalContext(
                activeTileIds = setOf(SituationalTileKeys.POST_PEACE_HIGHER),
                sprCityTier = "Y",
            )

        repository.setContextForMonth("2024-05", mayContext)
        repository.setContextForMonth("2024-06", juneContext)

        assertEquals(mayContext, repository.getContextForMonth("2024-05"))
        assertEquals(juneContext, repository.getContextForMonth("2024-06"))
        assertNull(repository.getContextForMonth("2024-07"))
    }

    @Test
    fun testManualOverrideTracking() {
        assertFalse(repository.hasManualOverride("2024-05"))

        repository.setContextForMonth(
            "2024-05",
            ActiveSituationalContext(activeTileIds = setOf(SituationalTileKeys.CADRE_AMC_NPA)),
        )

        assertTrue(repository.hasManualOverride("2024-05"))
        assertFalse(repository.hasManualOverride("2024-06"))

        repository.removeContextForMonth("2024-05")
        assertFalse(repository.hasManualOverride("2024-05"))
        assertNull(repository.getContextForMonth("2024-05"))
    }

    @Test
    fun testClearAllContexts() {
        repository.setContextForMonth("2024-01", ActiveSituationalContext())
        repository.setContextForMonth("2024-02", ActiveSituationalContext())
        assertEquals(2, repository.getAllContexts().size)

        repository.clearAll()
        assertTrue(repository.getAllContexts().isEmpty())
        assertFalse(repository.hasManualOverride("2024-01"))
    }
}
