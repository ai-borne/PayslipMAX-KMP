package com.payslipmax.pdfparser.ui.pcdao

import com.payslipmax.pcdao.reconciliation.ActiveSituationalContext
import com.payslipmax.pcdao.reconciliation.SituationalCategory
import com.payslipmax.pcdao.reconciliation.SituationalTileKeys
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CategoryTileCatalogTest {
    @Test
    fun testAllSixCategoriesReturnNonEmptyTiles() {
        val context = ActiveSituationalContext()
        val autoInferred = emptySet<String>()

        for (category in SituationalCategory.values()) {
            val tiles = CategoryTileCatalog.getTilesForCategory(category, context, autoInferred)
            assertTrue(tiles.isNotEmpty(), "Category $category must return at least one tile")
            tiles.forEach { tile ->
                assertTrue(tile.id.isNotBlank(), "Tile id must not be blank")
                assertTrue(tile.title.isNotBlank(), "Tile title must not be blank")
                assertTrue(tile.description.isNotBlank(), "Tile description must not be blank")
                assertTrue(tile.valuePreview.isNotBlank(), "Tile valuePreview must not be blank")
                assertEquals(category, tile.category)
            }
        }
    }

    @Test
    fun testPostingOpsTilesContainCanonicalOperationalSpectrum() {
        val context = ActiveSituationalContext(activeTileIds = setOf(SituationalTileKeys.POST_FIELD_CFAA))
        val autoInferred = setOf(SituationalTileKeys.POST_FIELD_CFAA)
        val tiles = CategoryTileCatalog.getTilesForCategory(SituationalCategory.POSTING_OPS, context, autoInferred)

        val keys = tiles.map { it.id }.toSet()
        assertTrue(keys.contains(SituationalTileKeys.POST_FIELD_HAFAA))
        assertTrue(keys.contains(SituationalTileKeys.POST_FIELD_CFAA))
        assertTrue(keys.contains(SituationalTileKeys.POST_FIELD_CMFAA))
        assertTrue(keys.contains(SituationalTileKeys.POST_PEACE_HIGHER))
        assertTrue(keys.contains(SituationalTileKeys.POST_PEACE_OTHER))
        assertTrue(keys.contains(SituationalTileKeys.POST_SIACHEN))
        assertTrue(keys.contains(SituationalTileKeys.POST_SDA_NE))
        assertTrue(keys.contains(SituationalTileKeys.POST_ISDA_ISLAND))

        val cfaaTile = tiles.first { it.id == SituationalTileKeys.POST_FIELD_CFAA }
        assertTrue(cfaaTile.isSelected)
        assertTrue(cfaaTile.isAutoInferred)
    }

    @Test
    fun testPostingOpsTilesContainEscalatedRatesWhenDaCrosses50() {
        val escalatedContext = ActiveSituationalContext(customDaPercent = 60.0)
        val tiles = CategoryTileCatalog.getTilesForCategory(SituationalCategory.POSTING_OPS, escalatedContext, emptySet())
        val hafaaTile = tiles.first { it.id == SituationalTileKeys.POST_FIELD_HAFAA }
        assertEquals("₹21,125/mo", hafaaTile.valuePreview)

        val unescalatedContext = ActiveSituationalContext(customDaPercent = 45.0)
        val unescalatedTiles = CategoryTileCatalog.getTilesForCategory(SituationalCategory.POSTING_OPS, unescalatedContext, emptySet())
        val unescalatedHafaa = unescalatedTiles.first { it.id == SituationalTileKeys.POST_FIELD_HAFAA }
        assertEquals("₹16,900/mo", unescalatedHafaa.valuePreview)
    }

    @Test
    fun testHousingTlcTilesContainCanonicalHousingOptionsAndTlc() {
        val context =
            ActiveSituationalContext(
                activeTileIds =
                    setOf(
                        SituationalTileKeys.HOUSE_FAMILY_SPR,
                        SituationalTileKeys.HOUSE_TWO_LOCATION_CONCESSION,
                    ),
            )
        val tiles = CategoryTileCatalog.getTilesForCategory(SituationalCategory.HOUSING_TLC, context, emptySet())

        val keys = tiles.map { it.id }.toSet()
        assertTrue(keys.contains(SituationalTileKeys.HOUSE_GOVT_MQ))
        assertTrue(keys.contains(SituationalTileKeys.HOUSE_FAMILY_SPR))
        assertTrue(keys.contains(SituationalTileKeys.HOUSE_PEACE_RETENTION))
        assertTrue(keys.contains(SituationalTileKeys.HOUSE_SF_ACCOMMODATION))
        assertTrue(keys.contains(SituationalTileKeys.HOUSE_LIVING_OUT_NAC))
        assertTrue(keys.contains(SituationalTileKeys.HOUSE_TWO_LOCATION_CONCESSION))
        assertTrue(keys.contains(SituationalTileKeys.HOUSE_GOVT_CONVEYANCE))

        val sprTile = tiles.first { it.id == SituationalTileKeys.HOUSE_FAMILY_SPR }
        val tlcTile = tiles.first { it.id == SituationalTileKeys.HOUSE_TWO_LOCATION_CONCESSION }
        assertTrue(sprTile.isSelected)
        assertTrue(sprTile.isRadioStyle)
        assertTrue(tlcTile.isSelected)
        assertFalse(tlcTile.isRadioStyle)
    }

    @Test
    fun testDutyCoursesLeaveTilesContainCanonicalDutySpectrum() {
        val context = ActiveSituationalContext(activeTileIds = setOf(SituationalTileKeys.LEAVE_FULL_MONTH))
        val tiles = CategoryTileCatalog.getTilesForCategory(SituationalCategory.DUTY_COURSES_LEAVE, context, emptySet())

        val keys = tiles.map { it.id }.toSet()
        assertTrue(keys.contains(SituationalTileKeys.DUTY_COURSE_LONG))
        assertTrue(keys.contains(SituationalTileKeys.DUTY_FIELD_FIRING))
        assertTrue(keys.contains(SituationalTileKeys.DUTY_TEMPORARY_DUTY))
        assertTrue(keys.contains(SituationalTileKeys.LEAVE_FULL_MONTH))

        val leaveTile = tiles.first { it.id == SituationalTileKeys.LEAVE_FULL_MONTH }
        assertTrue(leaveTile.isSelected)
        assertTrue(leaveTile.isRadioStyle)
    }

    @Test
    fun testCareerCadresAndFundsRetirementTiles() {
        val context = ActiveSituationalContext()
        val cadreTiles = CategoryTileCatalog.getTilesForCategory(SituationalCategory.CAREER_CADRES, context, emptySet())
        val cadreKeys = cadreTiles.map { it.id }.toSet()
        assertTrue(cadreKeys.contains(SituationalTileKeys.CADRE_AMC_NPA))
        assertTrue(cadreKeys.contains(SituationalTileKeys.CADRE_TECHNICAL_OFFICER))
        assertTrue(cadreKeys.contains(SituationalTileKeys.PROMOTION_ACTIVE))
        assertTrue(cadreKeys.contains(SituationalTileKeys.DNI_SCHEDULED))

        val fundsTiles = CategoryTileCatalog.getTilesForCategory(SituationalCategory.FUNDS_RETIREMENT, context, emptySet())
        val fundsKeys = fundsTiles.map { it.id }.toSet()
        assertTrue(fundsKeys.contains(SituationalTileKeys.RETIRE_NEAR))
        assertTrue(fundsKeys.contains(SituationalTileKeys.DSOP_HIGH_PACING))
        assertTrue(fundsKeys.contains(SituationalTileKeys.TRANSFER_CTG))
        assertTrue(fundsKeys.contains(SituationalTileKeys.AVAILED_LTC))
        assertTrue(fundsKeys.contains(SituationalTileKeys.TAX_ARREARS_SEC89))
    }
}
