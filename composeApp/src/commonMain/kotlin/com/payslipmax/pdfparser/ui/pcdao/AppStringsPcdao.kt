package com.payslipmax.pdfparser.ui.pcdao

/**
 * Centralized string resources for the PCDA(O) Military Financial Intelligence Engine & Cockpit.
 * Ensures zero hardcoded UI strings across all Phase 7 composables per Rule 4.
 */
object AppStringsPcdao {
    // Screen & Catalog Metadata
    const val screenIcon = "✨"
    const val screenTitle = "PayslipMax AI"
    const val screenSubtitle = "PCDA(O) Financial Intelligence & Moat Engine"
    const val screenDescription = "Automated PCDA(O) audit, 3-tier situational matrix, and 1-tap redressal kit."

    // Month Selector & Vault
    const val vaultMonthLabel = "Auditing Payslip: "
    const val vaultEmptyTitle = "No Payslips Found in Vault"
    const val vaultEmptyMessage = "Import your official monthly payslip PDF to initiate real-time PCDA(O) intelligence."
    const val selectMonthDropdownDesc = "Select payslip month for audit"

    // Top Impact KPI Counters Strip
    const val kpiUnclaimedTitle = "💰 Unclaimed Money"
    const val kpiUnclaimedSubtitle = "Annual statutory dues left on table"
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

    // Category Tabs
    const val tabPosting = "Posting"
    const val tabHousing = "Housing"
    const val tabChildrenCea = "Children (CEA)"
    const val tabCareer = "Career"
    const val tabFundsLtc = "Funds & LTC"

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
    const val chkMaskPiiLabel = "Mask Sensitive PII (Safe for Sharing)"
    const val btnCopyText = "📋 Copy Text"
    const val btnSaveDraft = "💾 Save to Claims"

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
}
