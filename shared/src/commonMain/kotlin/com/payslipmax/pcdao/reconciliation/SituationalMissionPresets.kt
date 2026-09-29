package com.payslipmax.pcdao.reconciliation

import kotlinx.serialization.Serializable

@Serializable
enum class MissionPresetId {
    RR_CI_OPS,
    SIACHEN_BRIGADE,
    AMC_HOSPITAL,
    ARMY_AVIATION,
    DSSC_COURSE,
    ANC_ISLAND,
    PEACE_REGIMENTAL,
    UN_MISSION,
}

@Serializable
data class SituationalMissionPreset(
    val id: MissionPresetId,
    val emoji: String,
    val title: String,
    val subtitle: String,
    val tileIds: Set<String>,
    val specializedFactors: Set<SpecializedMilitaryFactor> = emptySet(),
    val sprCityTier: String = "Y",
)

object SituationalMissionPresets {
    val RR_CI_OPS =
        SituationalMissionPreset(
            id = MissionPresetId.RR_CI_OPS,
            emoji = "🌲",
            title = "RR / CI Ops",
            subtitle = "Counter-Insurgency Field Deployment & 2-Location Concession",
            tileIds =
                setOf(
                    SituationalTileKeys.POST_FIELD_CFAA,
                    SituationalTileKeys.HOUSE_FAMILY_SPR,
                    SituationalTileKeys.HOUSE_TWO_LOCATION_CONCESSION,
                ),
            specializedFactors = emptySet(),
            sprCityTier = "Y",
        )

    val SIACHEN_BRIGADE =
        SituationalMissionPreset(
            id = MissionPresetId.SIACHEN_BRIGADE,
            emoji = "❄️",
            title = "Siachen Brigade",
            subtitle = "RH-MAX Sector & Family Selected Place of Residence",
            tileIds =
                setOf(
                    SituationalTileKeys.POST_SIACHEN,
                    SituationalTileKeys.HOUSE_FAMILY_SPR,
                    SituationalTileKeys.HOUSE_TWO_LOCATION_CONCESSION,
                ),
            specializedFactors = setOf(SpecializedMilitaryFactor.SIACHEN_GLACIER),
            sprCityTier = "Y",
        )

    val AMC_HOSPITAL =
        SituationalMissionPreset(
            id = MissionPresetId.AMC_HOSPITAL,
            emoji = "🏥",
            title = "AMC Military Hospital",
            subtitle = "Army Medical Corps: Non-Practicing Allowance & Peace Base",
            tileIds =
                setOf(
                    SituationalTileKeys.CADRE_AMC_NPA,
                    SituationalTileKeys.POST_PEACE_HIGHER,
                    SituationalTileKeys.HOUSE_GOVT_MQ,
                ),
            specializedFactors = setOf(SpecializedMilitaryFactor.NON_PRACTICING_ALLOWANCE_AMC),
            sprCityTier = "X",
        )

    val ARMY_AVIATION =
        SituationalMissionPreset(
            id = MissionPresetId.ARMY_AVIATION,
            emoji = "✈️",
            title = "Army Aviation Corps",
            subtitle = "Combat Aviation Squadron & Flying Pay Deployment",
            tileIds =
                setOf(
                    SituationalTileKeys.POST_PEACE_HIGHER,
                    SituationalTileKeys.HOUSE_GOVT_MQ,
                ),
            specializedFactors = setOf(SpecializedMilitaryFactor.FLYING_ALLOWANCE),
            sprCityTier = "X",
        )

    val DSSC_COURSE =
        SituationalMissionPreset(
            id = MissionPresetId.DSSC_COURSE,
            emoji = "🏛️",
            title = "DSSC / Long Course",
            subtitle = "Staff College Wellington / Training Establishment",
            tileIds =
                setOf(
                    SituationalTileKeys.DUTY_COURSE_LONG,
                    SituationalTileKeys.POST_PEACE_OTHER,
                    SituationalTileKeys.HOUSE_GOVT_MQ,
                ),
            specializedFactors = setOf(SpecializedMilitaryFactor.TRAINING_ALLOWANCE),
            sprCityTier = "Y",
        )

    val ANC_ISLAND =
        SituationalMissionPreset(
            id = MissionPresetId.ANC_ISLAND,
            emoji = "🌴",
            title = "ANC Island Command",
            subtitle = "Andaman & Nicobar Tri-Services Command (ISDA)",
            tileIds =
                setOf(
                    SituationalTileKeys.POST_ISDA_ISLAND,
                    SituationalTileKeys.HOUSE_GOVT_MQ,
                ),
            specializedFactors = setOf(SpecializedMilitaryFactor.ISLAND_SPECIAL_DUTY),
            sprCityTier = "Y",
        )

    val PEACE_REGIMENTAL =
        SituationalMissionPreset(
            id = MissionPresetId.PEACE_REGIMENTAL,
            emoji = "🎖️",
            title = "Peace Regimental Duty",
            subtitle = "Standard Cantonment Posting & Govt Married Accommodation",
            tileIds =
                setOf(
                    SituationalTileKeys.POST_PEACE_HIGHER,
                    SituationalTileKeys.HOUSE_GOVT_MQ,
                ),
            specializedFactors = emptySet(),
            sprCityTier = "Y",
        )

    val UN_MISSION =
        SituationalMissionPreset(
            id = MissionPresetId.UN_MISSION,
            emoji = "🌐",
            title = "UN Peacekeeping Mission",
            subtitle = "Overseas Deputation & Family Retention in Peace Station",
            tileIds =
                setOf(
                    SituationalTileKeys.HOUSE_PEACE_RETENTION,
                    SituationalTileKeys.HOUSE_TWO_LOCATION_CONCESSION,
                    SituationalTileKeys.POST_PEACE_HIGHER,
                ),
            specializedFactors = emptySet(),
            sprCityTier = "Y",
        )

    val ALL_PRESETS: List<SituationalMissionPreset> =
        listOf(
            RR_CI_OPS,
            SIACHEN_BRIGADE,
            AMC_HOSPITAL,
            ARMY_AVIATION,
            DSSC_COURSE,
            ANC_ISLAND,
            PEACE_REGIMENTAL,
            UN_MISSION,
        )

    val PRESET_MAP: Map<MissionPresetId, SituationalMissionPreset> =
        ALL_PRESETS.associateBy { it.id }

    fun getById(id: MissionPresetId): SituationalMissionPreset =
        PRESET_MAP[id] ?: RR_CI_OPS
}
