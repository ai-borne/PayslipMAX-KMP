package com.payslipmax.pdfparser.ui.pcdao

import com.payslipmax.pcdao.reconciliation.ActiveSituationalContext
import com.payslipmax.pcdao.reconciliation.SituationalCategory
import com.payslipmax.pcdao.reconciliation.SituationalTile
import com.payslipmax.pcdao.reconciliation.SituationalTileKeys

/**
 * Pure domain catalog builder for the 6 Situational Matrix tabs.
 * Maps operational, housing, duty, cadre, and retirement tiles with full metadata.
 */
object CategoryTileCatalog {
    fun getTilesForCategory(
        category: SituationalCategory,
        context: ActiveSituationalContext,
        autoInferred: Set<String>,
    ): List<SituationalTile> {
        val active = context.activeTileIds
        val isEscalated = context.customDaPercent?.let { it >= 50.0 } ?: context.inferredFlags.inferredDaCrossed50
        return when (category) {
            SituationalCategory.POSTING_OPS -> postingOpsTiles(active, autoInferred, category, isEscalated)
            SituationalCategory.HOUSING_TLC -> housingTlcTiles(active, autoInferred, category)
            SituationalCategory.CHILDREN_CEA -> childrenCeaTiles(active, autoInferred, category, isEscalated)
            SituationalCategory.DUTY_COURSES_LEAVE -> dutyCoursesLeaveTiles(active, autoInferred, category)
            SituationalCategory.CAREER_CADRES -> careerCadresTiles(active, autoInferred, category)
            SituationalCategory.FUNDS_RETIREMENT -> fundsRetirementTiles(active, autoInferred, category)
        }
    }

    private fun postingOpsTiles(
        active: Set<String>,
        inferred: Set<String>,
        cat: SituationalCategory,
        isEscalated: Boolean,
    ): List<SituationalTile> {
        val hafaaRate = if (isEscalated) "₹21,125/mo" else "₹16,900/mo"
        val cfaaRate = if (isEscalated) "₹13,125/mo" else "₹10,500/mo"
        val cmfaaRate = if (isEscalated) "₹7,875/mo" else "₹6,300/mo"
        val siachenRate = if (isEscalated) "₹53,125/mo" else "₹42,500/mo"
        return listOf(
            tile(SituationalTileKeys.POST_FIELD_HAFAA, cat, "🏔️ HAFAA Field Area", "Serving in Highly Active Field Area", hafaaRate, active, inferred),
            tile(SituationalTileKeys.POST_FIELD_CFAA, cat, "🌲 CFAA (CI Ops / RR)", "Counter-Insurgency Field Deployment", cfaaRate, active, inferred),
            tile(SituationalTileKeys.POST_FIELD_CMFAA, cat, "🌾 CMFAA (Modified Field)", "Modified Field Area Deployment", cmfaaRate, active, inferred),
            tile(SituationalTileKeys.POST_PEACE_HIGHER, cat, "🏙️ Peace (Pune / Higher UA)", "Higher Rate City (20 UA Cities)", "₹7,200 + DA", active, inferred, isRadio = true),
            tile(SituationalTileKeys.POST_PEACE_OTHER, cat, "🌾 Peace (Other Locations)", "Standard Peace TPTA", "₹3,600 + DA", active, inferred, isRadio = true),
            tile(SituationalTileKeys.POST_SIACHEN, cat, "❄️ Siachen Glacier", "RH-MAX deployment zone", siachenRate, active, inferred),
            tile(SituationalTileKeys.POST_SDA_NE, cat, "🌿 North-East SDA", "Special Duty Allowance", "10% of Basic Pay", active, inferred),
            tile(SituationalTileKeys.POST_ISDA_ISLAND, cat, "🌴 Island Command (ISDA)", "A&N Islands Special Duty Allowance", "10%/16%/20%", active, inferred),
        )
    }

    private fun housingTlcTiles(
        active: Set<String>,
        inferred: Set<String>,
        cat: SituationalCategory,
    ): List<SituationalTile> =
        listOf(
            tile(SituationalTileKeys.HOUSE_GOVT_MQ, cat, "🏢 Govt Married Accomm", "License Fee deducted, no HRA", "License Fee", active, inferred, isRadio = true),
            tile(SituationalTileKeys.HOUSE_FAMILY_SPR, cat, "📍 Family at SPR", "Selected Place of Residence", "20%/30% HRA", active, inferred, isRadio = true),
            tile(SituationalTileKeys.HOUSE_PEACE_RETENTION, cat, "🏠 Peace Accomm Retention", "Govt Married Accomm at Peace Stn", "No HRA + Lic Fee", active, inferred, isRadio = true),
            tile(SituationalTileKeys.HOUSE_SF_ACCOMMODATION, cat, "🏘️ SF Accommodation", "Separated Family Accomm Pool", "No HRA + SF Lic Fee", active, inferred, isRadio = true),
            tile(SituationalTileKeys.HOUSE_LIVING_OUT_NAC, cat, "📜 Living Out on NAC", "Non-Availability Certificate issued", "Full Station HRA", active, inferred, isRadio = true),
            tile(SituationalTileKeys.HOUSE_TWO_LOCATION_CONCESSION, cat, "🗺️ Two-Location Concession (TLC)", "Concurrent Field Single Accomm + Family HRA/Accomm", "Dual Admissible", active, inferred),
            tile(SituationalTileKeys.HOUSE_GOVT_CONVEYANCE, cat, "🚗 Govt Conveyance Provided", "Unit transport allocated", "Bars TPTA", active, inferred),
        )

    private fun childrenCeaTiles(
        active: Set<String>,
        inferred: Set<String>,
        cat: SituationalCategory,
        isEscalated: Boolean,
    ): List<SituationalTile> {
        val oneChildRate = if (isEscalated) "₹33,750/yr" else "₹27,000/yr"
        val twoChildrenRate = if (isEscalated) "₹67,500/yr" else "₹54,000/yr"
        val hostelRate = if (isEscalated) "₹1,01,250/yr" else "₹81,000/yr"
        return listOf(
            tile(SituationalTileKeys.CEA_NONE, cat, "0 School Children", "No education claims active", "₹0", active, inferred, isRadio = true),
            tile(SituationalTileKeys.CEA_ONE_CHILD, cat, "🎒 1 Child in Day School", "Class Nursery to XII", oneChildRate, active, inferred, isRadio = true),
            tile(SituationalTileKeys.CEA_TWO_CHILDREN, cat, "🎒 2 Children in Day School", "Standard 2-child entitlement", twoChildrenRate, active, inferred, isRadio = true),
            tile(SituationalTileKeys.CEA_HOSTEL, cat, "🏫 Child in Hostel", "Hostel Subsidy (Escalated 25%)", hostelRate, active, inferred),
        )
    }

    private fun dutyCoursesLeaveTiles(
        active: Set<String>,
        inferred: Set<String>,
        cat: SituationalCategory,
    ): List<SituationalTile> =
        listOf(
            tile(SituationalTileKeys.DUTY_COURSE_LONG, cat, "🏛️ Long Course / DSSC / JC", "Staff College / Institutional Training", "Training Allowance", active, inferred),
            tile(SituationalTileKeys.DUTY_FIELD_FIRING, cat, "🎯 Field Firing / Detachment", "Temporary deployment to range", "DA on TD", active, inferred),
            tile(SituationalTileKeys.DUTY_TEMPORARY_DUTY, cat, "📋 Temporary Duty (> 30 Days)", "External attachment / TD orders", "TD Entitlements", active, inferred),
            tile(SituationalTileKeys.LEAVE_FULL_MONTH, cat, "🏖️ Full Calendar Month Leave", "Absent 1st to end of month (TR-230B)", "Bars TPTA", active, inferred, isRadio = true),
        )

    private fun careerCadresTiles(
        active: Set<String>,
        inferred: Set<String>,
        cat: SituationalCategory,
    ): List<SituationalTile> =
        listOf(
            tile(SituationalTileKeys.CADRE_AMC_NPA, cat, "🏥 AMC / ADC Officer (20% NPA)", "Non-Practicing Allowance (Compounding)", "20% Basic (Apex Cap)", active, inferred),
            tile(SituationalTileKeys.CADRE_TECHNICAL_OFFICER, cat, "⚙️ Technical Officer", "Signals / EME / Technical Pay Tier 1/2", "₹3,000 / ₹4,500", active, inferred),
            tile(SituationalTileKeys.PROMOTION_ACTIVE, cat, "⭐ Substantive Promotion Due", "Triggers Rule 10/11 Fixation", "Option 1 vs 2", active, inferred),
            tile(SituationalTileKeys.DNI_SCHEDULED, cat, "📅 Annual Increment: 1 July", "Scheduled DNI cycle", "Next Increment", active, inferred),
        )

    private fun fundsRetirementTiles(
        active: Set<String>,
        inferred: Set<String>,
        cat: SituationalCategory,
    ): List<SituationalTile> =
        listOf(
            tile(SituationalTileKeys.RETIRE_NEAR, cat, "⏳ Retiring in < 3 Months", "Superannuation / Release", "DSOP Stop Alarm", active, inferred),
            tile(SituationalTileKeys.DSOP_HIGH_PACING, cat, "📈 DSOP > ₹5 Lakh Annual", "Sec 10(11) tax-exempt limit", "Tax Drag Alert", active, inferred),
            tile(SituationalTileKeys.TRANSFER_CTG, cat, "🧳 Permanent Transfer (CTG)", "Composite Transfer Grant", "80% Basic Pay", active, inferred),
            tile(SituationalTileKeys.AVAILED_LTC, cat, "🎫 Availed LTC Concession", "10-day leave encashment eligible", "10 Days Pay+DA", active, inferred),
            tile(SituationalTileKeys.TAX_ARREARS_SEC89, cat, "🛡️ Arrears Sec 89(1) Relief", "Tax relief on retrospective salary", "Form 10E Shield", active, inferred),
        )

    private fun tile(
        key: String,
        category: SituationalCategory,
        title: String,
        description: String,
        preview: String,
        active: Set<String>,
        inferred: Set<String>,
        isRadio: Boolean = false,
    ): SituationalTile =
        SituationalTile(
            id = key,
            category = category,
            title = title,
            description = description,
            valuePreview = preview,
            isAutoInferred = inferred.contains(key),
            isSelected = active.contains(key),
            isRadioStyle = isRadio,
        )
}
