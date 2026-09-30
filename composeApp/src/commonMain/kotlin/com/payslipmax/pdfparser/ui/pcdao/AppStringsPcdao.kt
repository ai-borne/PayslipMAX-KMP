package com.payslipmax.pdfparser.ui.pcdao

/**
 * Centralized string resources for the PCDA(O) Military Financial Intelligence Engine & Cockpit.
 * Ensures zero hardcoded UI strings across all Phase 7 composables per Rule 4.
 */
object AppStringsPcdao {
    // Screen & Catalog Metadata
    const val screenIcon = "✨"
    const val screenTitle = "PayslipMax AI"
    const val screenSubtitle = "PCDA(O) Military Financial Intelligence & Audit Engine"
    const val screenDescription = "Automated PCDA(O) audit, 3-tier situational matrix, and 1-tap redressal kit."

    // Month Selector & Vault
    const val vaultMonthLabel = "Auditing Payslip: "
    const val vaultEmptyTitle = "No Payslips Found in Vault"
    const val vaultEmptyMessage = "Import your official monthly payslip PDF to initiate real-time PCDA(O) intelligence."
    const val selectMonthDropdownDesc = "Select payslip month for audit"

    // Top Impact KPI Counters Strip
    const val kpiUnclaimedTitle = "💰 Unclaimed Money"
    const val kpiUnclaimedSubtitle = "Annual statutory dues underdrawn"
    const val kpiHazardsTitle = "⚠️ Recovery Hazards"
    const val kpiHazardsSubtitle = "Risk of 18% penal debit recovery"
    const val kpiAlarmsTitle = "🚨 Critical Alarms"
    const val kpiAlarmsSubtitle = "DO2 rejection & time-bar cutoffs"
    const val alarmsCountSuffix = " Alarms"
    const val alarmSingularSuffix = " Alarm"

    // Situational Tile Matrix
    const val matrixHeaderTitle = "🎛️ Situational Matrix"
    const val matrixActiveBadge = "ACTIVE CONTEXT"
    const val tileAutoDetectedBadge = "AUTO-DETECTED"
    const val addFactorButtonLabel = "+ Add Specialized Factor"
    const val specializedFactorsActivePrefix = "Active Factors: "

    // Category Tabs (6 Tabs)
    const val tabPostingOps = "Posting & Ops"
    const val tabHousingTlc = "Housing & TLC"
    const val tabChildrenCea = "Children (CEA)"
    const val tabDutyLeave = "Duty & Leave"
    const val tabCareerCadres = "Career & Cadres"
    const val tabFundsRetirement = "Funds & Release"

    // Backward-compatibility aliases
    const val tabPosting = tabPostingOps
    const val tabHousing = tabHousingTlc
    const val tabCareer = tabCareerCadres
    const val tabFundsLtc = tabFundsRetirement

    // 8 Mission Presets Copy
    const val presetsHeaderTitle = "⚡ 1-Tap Mission Presets"
    const val presetRrCiOps = "🌲 RR CI Ops"
    const val presetSiachen = "❄️ Siachen"
    const val presetAmc = "🏥 AMC Hospital"
    const val presetAviation = "✈️ Aviation"
    const val presetDssc = "🏛️ DSSC Course"
    const val presetAnc = "🌴 ANC Island"
    const val presetPeace = "🎖️ Peace Station"
    const val presetUn = "🌐 UN Mission"

    fun getPresetLabel(id: com.payslipmax.pcdao.reconciliation.MissionPresetId): String =
        when (id) {
            com.payslipmax.pcdao.reconciliation.MissionPresetId.RR_CI_OPS -> presetRrCiOps
            com.payslipmax.pcdao.reconciliation.MissionPresetId.SIACHEN_BRIGADE -> presetSiachen
            com.payslipmax.pcdao.reconciliation.MissionPresetId.AMC_HOSPITAL -> presetAmc
            com.payslipmax.pcdao.reconciliation.MissionPresetId.ARMY_AVIATION -> presetAviation
            com.payslipmax.pcdao.reconciliation.MissionPresetId.DSSC_COURSE -> presetDssc
            com.payslipmax.pcdao.reconciliation.MissionPresetId.ANC_ISLAND -> presetAnc
            com.payslipmax.pcdao.reconciliation.MissionPresetId.PEACE_REGIMENTAL -> presetPeace
            com.payslipmax.pcdao.reconciliation.MissionPresetId.UN_MISSION -> presetUn
        }

    // Pay Fixation Optimizer Card (Rule 10 & 11)
    const val fixationCardTitle = "⭐ Rule 10 & 11 Army Pay Rules 2017: Pay Fixation Optimizer"
    const val fixationElectionBadge = "30-DAY STATUTORY ELECTION"
    const val fixationPromotionPrompt = "Rule 11 requires you to elect your fixation option in writing within 1 month:"
    const val fixationOpt1Title = "Option 1: Fix on Promotion Date"
    const val fixationOpt1Desc = "Immediate placement at higher stage with 1 promotional increment."
    const val fixationOpt2Title = "Option 2: Fix from DNI"
    const val fixationOpt2Desc = "Earn annual increment first, then 2 increments into higher level."
    const val fixationRecommendedBadge = "★ RECOMMENDED"
    const val fixation36MonthPrefix = "36-Mo: ₹"
    const val fixationUrgentCallout =
        "⚠️ Urgent: Option must be submitted to PCDA(O) within 30 days of promotion order, or Option 1 is applied by default!"
    const val fixationSandboxTitle = "Sandbox Mode: Project Future Promotion"
    const val fixationFromLevelLabel = "From:"
    const val fixationToLevelLabel = "To:"
    const val fixationPromotionAdvisory =
        "Substantive Promotion Approaching: Compare Option 1 vs Option 2 to maximize 36-month pay."
    const val fixationLevelPrefix = "Level "

    // Intelligence Feed & Filter Tabs
    const val feedHeaderTitle = "⚡ Intelligence Feed"
    const val btnRedressalKit = "📄 1-Tap PCDA(O) Redressal Kit"
    const val filterAll = "All"
    const val filterEntitlements = "💰 Entitlements"
    const val filterHazards = "⚠️ Hazards"
    const val filterAlarms = "🚨 Alarms"
    const val filterShield = "🛡️ Tax Shield"
    const val feedEmptyTitle = "Clean Ledger Audit"
    const val feedEmptyMessage = "No discrepancies or recovery hazards detected for this payslip."

    // Audit Discrepancy Card
    const val colEntitled = "Entitled"
    const val colCredited = "Credited"
    const val colNetDue = "Net Due"
    const val authorityPrefix = "Authority: "
    const val annualExposurePrefix = "Annual Exposure: ₹"

    // Specialized Factor Bottom Sheet
    const val sheetTitle = "Specialized Military Factors"
    const val sheetSubtitle = "Activate specialized operational, medical, or gallantry factors applicable to your profile"
    const val btnApplyFactors = "Apply Factors"
    const val factorActiveLabel = "ACTIVE"
    const val factorInactiveLabel = "INACTIVE"

    // Redressal Letter Export
    const val letterModalTitle = "📜 Official PCDA(O) Pune Representation"
    const val redressalDraftTitle = "PCDA(O) Official Representation"
    const val redressalTableColItem = "Discrepancy Line Item"
    const val redressalTableColEntitled = "Entitled"
    const val redressalTableColCredited = "Credited"
    const val redressalTableColDue = "Net Due"
    const val chkMaskPiiLabel = "Mask Sensitive PII (Safe for Sharing)"
    const val btnCopyText = "📋 Copy Text"
    const val btnSaveDraft = "💾 Save to Claims"
    const val btnExportPdf = "📄 Export PDF"
    const val btnShareText = "📤 Share"
    const val btnEditText = "✏️ Edit Text"

    // Phase 8 Monetization & Teaser Strings
    const val switchMonthButton = "▼ Switch Month"
    const val proBadge = "PRO"
    const val maskedAmount = "₹••••••"
    const val proUnlockMathBadge = "PRO 🔒"
    const val proAuthorityLocked = "🔒 Official PCDA(O) Rule Citation (PRO • Tap to unlock)"
    const val proRedressalLocked = "🔒 1-Tap PCDA(O) Redressal Kit (PRO)"
    const val teaserHeadlineClaimsPrefix = "Potential Claims Detected: "
    const val teaserHeadlineClaimsSuffix = "Unclaimed"
    const val teaserHeadlineDesc = "Upgrade to PRO to unlock full math diffs, statutory citations & 1-tap redressal."
    const val teaserUpgradeButton = "Unlock PRO"
    const val fixationOptionLocked = "🔒 36-Mo Trajectory (PRO • Tap to unlock)"

    // Phase 9 Multi-Month Cumulative Intelligence & Timeline Strings
    const val cumulativeBannerTitle = "Cumulative Multi-Month Back-Dues"
    const val cumulativeMonthPrefix = "Calculated across "
    const val cumulativeMonthSuffix = " uploaded statements"
    const val cumulativeViewToggleSelected = "Selected Month"
    const val cumulativeViewToggleAll = "All Months (Cumulative)"
    const val cumulativeArrearsTableTitle = "Chronological Dues Schedule"
    const val careerMilestoneTitle = "🎖️ Career Milestones & Increments"
    const val careerMilestoneSubtitle = "7th CPC statutory progression & Central DA revision tracker"
    const val milestoneVerifiedBadge = "VERIFIED"
    const val milestoneAlertBadge = "ATTENTION"
    const val milestoneShowAll = "Show All Milestones (%d) ▾"
    const val milestoneShowLess = "Show Recent Only ▴"
    const val monthSelectorTitle = "Select Audit Month"
    const val btnGenerateCumulativeLetter = "📄 Multi-Month Cumulative Claim Kit"

    fun formatMilestoneShowAll(count: Int): String = milestoneShowAll.replace("%d", count.toString())

    // Dashboard AI Audit Discovery Banner (Phase 2)
    const val dashboardBannerTitle = "PCDA(O) Military Financial Intelligence"
    const val dashboardBannerSubtitleZero = "Import your first payslip to unlock 7th CPC audit & IRLA reconciliation."
    const val dashboardBannerSubtitleSingle = "1 statement analyzed across your IRLA (Statement of Account)"
    const val dashboardBannerSubtitleMultiple = "%d statements analyzed across your IRLA"
    const val dashboardBannerCta = "Run 1-Tap Audit →"
    const val replicaAuditCta = "Audit with PayslipMax AI"

    fun formatDashboardStatementsAnalyzed(count: Int): String =
        when (count) {
            0 -> dashboardBannerSubtitleZero
            1 -> dashboardBannerSubtitleSingle
            else -> dashboardBannerSubtitleMultiple.replace("%d", count.toString())
        }

    // Military Orientation Primer Sheet (Phase 3)
    const val onboardingTopBarGuideDesc = "Orientation Guide"
    const val onboardingTopBarGuideLabel = "Guide"
    const val onboardingSlide1Title = "Automated IRLA Statutory Audit"
    const val onboardingSlide1Body =
        "Cross-examines your monthly Individual Running Ledger Account against 7th CPC regulations, MoD circulars, and PCDA(O) rules."
    const val onboardingSlide2Title = "Interactive Situational Matrix"
    const val onboardingSlide2Body =
        "Select your operational deployment (Field, High Altitude, Peace, AMC, RR) using 1-Tap Mission Presets to calculate true entitlements."
    const val onboardingSlide3Title = "1-Tap Official Redressal Kit"
    const val onboardingSlide3Body =
        "Generates legally precise representation letters formatted directly to PCDA(O) Pune sections with MoD citations."
    const val onboardingBtnNext = "Next"
    const val onboardingBtnBack = "Back"
    const val onboardingBtnEnter = "Enter Cockpit"
    const val onboardingBtnSkip = "Skip"

    // Matrix Discovery & Hazard Demystification (Phase 4)
    const val matrixTabScrollCue = "More tabs →"
    const val presetsSimulationHint =
        "⚡ Quick Simulation Baselines — Tap to preview entitlements (safe, non-destructive)"
    const val hazardDialogTitle = "Understanding Recovery Hazards"
    const val hazardDialogBody =
        "This is a statutory audit risk warning, not an active deduction. PCDA(O) Pune routinely initiates retroactive debit recoveries with 18% penal interest when allowances clash (e.g. TPTA drawn during field tenure per TR-230(B)). PayslipMax AI detects these clashes early so you can publish casualty Part II Orders and prevent penal loss."
    const val hazardDialogDismiss = "Understood"
}
