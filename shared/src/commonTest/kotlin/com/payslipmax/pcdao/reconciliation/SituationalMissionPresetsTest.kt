package com.payslipmax.pcdao.reconciliation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SituationalMissionPresetsTest {
    @Test
    fun testAllEightPresetsExist() {
        assertEquals(8, MissionPresetId.values().size)
        assertEquals(8, SituationalMissionPresets.ALL_PRESETS.size)
        assertEquals(8, SituationalMissionPresets.PRESET_MAP.size)

        for (preset in SituationalMissionPresets.ALL_PRESETS) {
            assertTrue(preset.emoji.isNotBlank())
            assertTrue(preset.title.isNotBlank())
            assertTrue(preset.subtitle.isNotBlank())
            assertTrue(preset.tileIds.isNotEmpty())
        }
    }

    @Test
    fun testRrCiOpsPreset() {
        val preset = SituationalMissionPresets.getById(MissionPresetId.RR_CI_OPS)
        assertEquals("🌲", preset.emoji)
        assertTrue(preset.tileIds.contains(SituationalTileKeys.POST_FIELD_CFAA))
        assertTrue(preset.tileIds.contains(SituationalTileKeys.HOUSE_FAMILY_SPR))
        assertTrue(preset.tileIds.contains(SituationalTileKeys.HOUSE_TWO_LOCATION_CONCESSION))
        assertEquals("Y", preset.sprCityTier)
    }

    @Test
    fun testSiachenBrigadePreset() {
        val preset = SituationalMissionPresets.getById(MissionPresetId.SIACHEN_BRIGADE)
        assertEquals("❄️", preset.emoji)
        assertTrue(preset.tileIds.contains(SituationalTileKeys.POST_SIACHEN))
        assertTrue(preset.tileIds.contains(SituationalTileKeys.HOUSE_FAMILY_SPR))
        assertTrue(preset.tileIds.contains(SituationalTileKeys.HOUSE_TWO_LOCATION_CONCESSION))
        assertTrue(preset.specializedFactors.contains(SpecializedMilitaryFactor.SIACHEN_GLACIER))
    }

    @Test
    fun testAmcHospitalPreset() {
        val preset = SituationalMissionPresets.getById(MissionPresetId.AMC_HOSPITAL)
        assertEquals("🏥", preset.emoji)
        assertTrue(preset.tileIds.contains(SituationalTileKeys.CADRE_AMC_NPA))
        assertTrue(preset.tileIds.contains(SituationalTileKeys.POST_PEACE_HIGHER))
        assertTrue(preset.tileIds.contains(SituationalTileKeys.HOUSE_GOVT_MQ))
        assertTrue(preset.specializedFactors.contains(SpecializedMilitaryFactor.NON_PRACTICING_ALLOWANCE_AMC))
    }

    @Test
    fun testArmyAviationPreset() {
        val preset = SituationalMissionPresets.getById(MissionPresetId.ARMY_AVIATION)
        assertEquals("✈️", preset.emoji)
        assertTrue(preset.tileIds.contains(SituationalTileKeys.POST_PEACE_HIGHER))
        assertTrue(preset.specializedFactors.contains(SpecializedMilitaryFactor.FLYING_ALLOWANCE))
    }

    @Test
    fun testDsscCoursePreset() {
        val preset = SituationalMissionPresets.getById(MissionPresetId.DSSC_COURSE)
        assertEquals("🏛️", preset.emoji)
        assertTrue(preset.tileIds.contains(SituationalTileKeys.DUTY_COURSE_LONG))
        assertTrue(preset.specializedFactors.contains(SpecializedMilitaryFactor.TRAINING_ALLOWANCE))
    }

    @Test
    fun testAncIslandPreset() {
        val preset = SituationalMissionPresets.getById(MissionPresetId.ANC_ISLAND)
        assertEquals("🌴", preset.emoji)
        assertTrue(preset.tileIds.contains(SituationalTileKeys.POST_ISDA_ISLAND))
        assertTrue(preset.specializedFactors.contains(SpecializedMilitaryFactor.ISLAND_SPECIAL_DUTY))
    }

    @Test
    fun testPeaceRegimentalPreset() {
        val preset = SituationalMissionPresets.getById(MissionPresetId.PEACE_REGIMENTAL)
        assertEquals("🎖️", preset.emoji)
        assertTrue(preset.tileIds.contains(SituationalTileKeys.POST_PEACE_HIGHER))
        assertTrue(preset.tileIds.contains(SituationalTileKeys.HOUSE_GOVT_MQ))
        assertTrue(preset.specializedFactors.isEmpty())
    }

    @Test
    fun testUnMissionPreset() {
        val preset = SituationalMissionPresets.getById(MissionPresetId.UN_MISSION)
        assertEquals("🌐", preset.emoji)
        assertTrue(preset.tileIds.contains(SituationalTileKeys.HOUSE_PEACE_RETENTION))
        assertTrue(preset.tileIds.contains(SituationalTileKeys.HOUSE_TWO_LOCATION_CONCESSION))
        assertTrue(preset.tileIds.contains(SituationalTileKeys.POST_PEACE_HIGHER))
    }

    @Test
    fun testTileKeysRadioGroups() {
        assertTrue(SituationalTileKeys.STATION_POSTING_KEYS.contains(SituationalTileKeys.POST_FIELD_CFAA))
        assertTrue(SituationalTileKeys.STATION_POSTING_KEYS.contains(SituationalTileKeys.POST_FIELD_CMFAA))
        assertTrue(SituationalTileKeys.HOUSING_KEYS.contains(SituationalTileKeys.HOUSE_PEACE_RETENTION))
        assertTrue(SituationalTileKeys.HOUSING_KEYS.contains(SituationalTileKeys.HOUSE_SF_ACCOMMODATION))
        assertTrue(SituationalTileKeys.LEAVE_KEYS.contains(SituationalTileKeys.LEAVE_FULL_MONTH))
    }
}
