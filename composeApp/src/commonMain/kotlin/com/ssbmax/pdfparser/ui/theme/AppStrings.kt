package com.ssbmax.pdfparser.ui.theme

object AppStrings {
    // App Branding & Navigation
    const val appTitle = "PCDA Payslip Portal"
    const val appSubtitle = "Indian Army Personnel Financial Analytics"
    const val navigationHome = "Dashboard"
    const val navigationHistory = "History"
    const val navigationInsights = "Insights"
    const val navigationSettings = "Settings"
    const val badgeOfficer = "Officer"
    const val badgeCda = "CDA A/C No"
    const val badgePan = "PAN"

    // Statistics Cards
    const val cardNetTitle = "Latest Net Remittance"
    const val cardNetUnit = "INR (₹)"
    const val cardBpTitle = "Basic Pay (BPAY)"
    const val cardBpUnit = "INR (₹)"
    const val cardBpDesc = "+ Allowances & Arrears"
    const val cardDsopTitle = "DSOP Closing Balance"
    const val cardDsopUnit = "INR (₹)"
    const val cardDsopDesc = "Provident Fund Wealth"
    const val cardTaxTitle = "Effective Tax Rate"
    const val cardTaxUnit = "YTD"
    const val cardTaxDesc = "Average Annual TDS"

    // Dashboard Overview
    const val sectionOverview = "Financial Overview"
    const val sectionCharts = "Analytics Charts"
    const val chartIncomeTitle = "Monthly Payout Trend"
    const val chartShareTitle = "Earnings Allocation"
    const val chartDsopTitle = "DSOP Fund Growth"
    const val chartTaxTitle = "Tax Deductions vs Projections"

    // Explorer
    const val explorerHeader = "Payslip Digital Replica"
    const val explorerSubheader = "Tap any transaction code for detailed explanations"
    const val replicaEarningTitle = "Earnings"
    const val replicaDeductionTitle = "Deductions"
    const val replicaGrossLabel = "Gross Pay (कुल आय)"
    const val replicaDeductionsLabel = "Total Deductions (कुल कटौती)"
    const val replicaNetLabel = "Net Remittance (Take Home)"
    const val replicaFooter = "Principal Controller of Defence Accounts (Officers), Pune\nMinistry of Defence, Government of India"
    const val ledgerGrossPay = "Gross Pay (Credits)"
    const val ledgerTotalDeductions = "Total Deductions (Debits)"

    // Upload & Decrypt
    const val uploadHeader = "Import Encrypted Payslip"
    const val uploadDesc = "Secure, 100% offline-first parsing engine"
    const val labelSelectPdf = "Select PDF Payslip"
    const val labelPassword = "Decryption Password"
    const val btnDecrypt = "Decrypt & Parse"
    const val loaderDecrypt = "Decrypting PDF using Secure Enclave..."
    const val uploadDismiss = "Dismiss"
    const val uploadSuccess = "Payslip Imported Successfully!"

    // Dashboard - Officer Info Bar
    const val officerInfoLabel = "Officer"
    const val cdaInfoLabel = "CDA A/C"
    const val panInfoLabel = "PAN"

    // Dashboard - Year/Month Picker
    const val selectYearLabel = "Year"
    const val selectMonthLabel = "Month"
    const val analyzingStatement = "Analyze Payslip"

    // Security Lock Screen
    const val lockScreenTitle = "Secured Portal"
    const val lockScreenSubtitle = "Enter your 4-digit PIN to decrypt your dashboard"
    const val lockScreenError = "Incorrect PIN, please try again"

    // Settings Interface
    const val settingsThemeLabel = "Interface Theme"
    const val settingsThemeLight = "Light Mode"
    const val settingsThemeDark = "Dark Mode"
    const val settingsThemeSystem = "System Default"
    const val settingsAppLockLabel = "Security Passcode"
    const val settingsAppLockDesc = "Protect your financial data with a local PIN"
    const val settingsProfileHeader = "Custom Profile Overrides"
    const val settingsProfileDesc = "Override Name, CDA, or PAN if parsed incorrectly"
    const val settingsProfileName = "Officer Name"
    const val settingsProfileCda = "CDA Account Number"
    const val settingsProfilePan = "PAN Number"
    const val settingsProPlanTitle = "Pro Analytical Insights"
    const val proTeaserCrownIcon = "👑"
    const val proTeaserAiIcon = "🤖"
    const val proTeaserToolsIcon = "🛠"
    const val aiRegenerateIcon = "🔄"
    const val settingsProPlanDesc = "Unlock personalization & CA-grade tax advice"
    const val settingsProPlanPrice = "₹99 / Year"
    const val settingsProPlanActive = "Pro Plan Activated"
    const val settingsDangerZone = "Danger Zone"
    const val settingsDeleteAll = "Delete All App Data"
    const val settingsDeleteConfirm = "Are you sure you want to wipe all local database records? This action is permanent."
    const val settingsDangerZoneDesc = "Wipe all local database statements, cached PDFs, overrides, and passcode security configuration."
    const val settingsDeleteConfirmTitle = "Wipe App Data?"
    const val settingsDeleteConfirmBtn = "Delete Everything"
    const val btnCancel = "Cancel"

    // Passcode Setup Dialog
    const val settingsSetPasscodeTitle = "Set 4-Digit Passcode"
    const val settingsSetPasscodeLabel = "Enter 4-Digit PIN"
    const val settingsSetPasscodeConfirmBtn = "Enable Lock"
    const val settingsProfileSaveBtn = "Save Overrides"

    // Backup & Restore Card
    const val settingsBackupHeader = "Personal Cloud Sync & Backup"
    const val settingsBackupDesc = "Backup is locally AES-256 encrypted using your password."
    const val settingsBackupLocalBtn = "Local Backup"
    const val settingsRestoreLocalBtn = "Local Restore"
    const val settingsBackupCrossPlatform = "Cross-Platform Portability (iOS ⇄ Android)"
    const val settingsBackupExportBtn = "Export Archive"
    const val settingsBackupImportBtn = "Import Archive"
    const val settingsBackupCancelImportBtn = "Cancel Import"
    const val settingsBackupPasteLabel = "Paste Encrypted Backup String"
    const val settingsBackupDecryptBtn = "Decrypt & Restore"

    // Backup Status Messages
    const val statusSyncSuccess = "Sync Completed!"
    const val statusSyncFailed = "Sync Failed: "
    const val statusRestoreSuccess = "Restore Completed!"
    const val statusRestoreFailed = "Restore Failed: "
    const val statusCopiedSuccess = "Backup String Copied to Clipboard!"
    const val statusExportFailed = "Export Failed: "
    const val statusRestoreComplete = "Restore Complete!"
    const val statusInvalidFormat = "Invalid backup string format"

    // Main Settings Screen
    const val settingsSubtitle = "Manage your data, sync backups, and configure sandbox options"
    const val settingsOfflineFirst = "Offline-First"
    const val settingsOfflineSecureTitle = "100% Offline & Secure"
    const val settingsOfflineSecureDesc = "All payslip decryption and parsing happens locally on your device. Your data never leaves your control."
    const val settingsAiPoweredByProxy = "AI insights powered by Gemini"

    // Sandbox / Testing
    const val settingsStagingTitle = "Staging & Testing Sandbox"
    const val settingsStagingDesc = "Load simulated Army Officer records (2022-2025) to evaluate the analytical tools immediately."
    const val settingsStagingSeedBtn = "Seed Staging Data"
    const val settingsStagingClearBtn = "Clear All"

    // Help & Legal Docs
    const val settingsHelpDocsHeader = "Help & Legal Documentation"
    const val settingsHelpFaqTitle = "Frequently Asked Questions (FAQ)"
    const val settingsHelpPrivacyTitle = "Privacy & Data Security Policy"
    const val settingsHelpAiTitle = "Gemini AI: No PII Policy"
    const val settingsHelpDisclaimerTitle = "Legal Disclaimer"

    const val settingsHelpFaqContent = "Q: How is my payslip decrypted?\nA: Decryption happens entirely offline on your device using local AES-256 libraries. Your password is never sent online.\n\nQ: Where is my data stored?\nA: Your data is saved in a secure, local Room database on your device."
    const val settingsHelpPrivacyContent = "We enforce a 100% offline-first architecture. All parsing, decryption, and storage tasks run strictly in your local sandbox. No data leaves your control unless you explicitly run a sync backup."
    const val settingsHelpAiContent = "To preserve confidentiality, the app strips all Personal Identifying Information (PII) like Name, CDA account number, and PAN number from data sent to the Gemini API. Only numeric financial values are processed to generate insights."
    const val settingsHelpDisclaimerContent = "This analytical tool is for reference purposes only. It is not an official app of the PCDA, Ministry of Defence, or the Indian Army. It does not replace professional advice from chartered accountants or official audit statements."

    // Redesign & Pro Tier
    const val settingsProPlanBullet1 = "• CA-Grade Financial Audits & Advice"
    const val settingsProPlanBullet2 = "• Personalized Tax Saving Insights"
    const val settingsProPlanBullet3 = "• Secure Local & Cross-Platform Backups"
    const val settingsProPlanBullet4 = "• 100% Offline & Private Processing"
    const val settingsProUpgradeBtn = "Unlock Pro Tier"
    const val settingsDocumentationHeader = "Documentation"
    const val settingsAiInsightsLockedTitle = "CA Level AI Report"
    const val settingsRowThemeLabel = "Interface Theme"
    const val settingsRowPasscodeLabel = "App Passcode Lock"
    const val settingsRowBackupLabel = "Cloud Sync & Portability"
    const val settingsRowProfileLabel = "Custom Profile Overrides"
    const val settingsStatusNotConfigured = "Not Configured"
    const val settingsStatusConfigured = "Configured"
    const val settingsStatusProOnly = "Pro Feature"
    const val settingsStatusEnabled = "Enabled"
    const val settingsStatusDisabled = "Disabled"
    const val settingsStatusDefault = "Default"

    // History Redesign Strings
    const val historyActionDelete = "Delete Statement"
    const val historyConfirmDeleteTitle = "Delete Payslip?"
    const val historyConfirmDeleteMessage = "Are you sure you want to delete this payslip? This action is permanent and cannot be undone."
    const val historyHeaderDescription = "Browse and read your historical military payslip statements"
    const val historyEmptyState = "No payslips in history. Go to Dashboard to import one."
    const val historyActionViewReplica = "View Digital Replica"
    const val historyActionViewOriginal = "View Original PDF"
    const val historyActionShareSummary = "Share Summary"
    const val historyYearlySummary = "Yearly Summary"
    const val historyNetTakeHomeLabel = "Net Take-Home"
    const val historyNetTakeHomeShort = "Take Home"
    const val historyGrossPayLabel = "Gross"
    const val historyDsopLabel = "DSOP"
    const val historyTaxLabel = "Tax"
    const val historyTrendHike = "DA Hike / Increment"
    const val historyTabStatements = "Statements"
    const val historyTabAiReports = "AI Reports"
    const val historyEmptyAiReports = "No saved AI reports. Generate one from the Insights screen."
    const val historyPremiumIntelligenceNarrative = "Premium Intelligence Narrative"
    const val historyChevronContentDescription = "Toggle Year Visibility"

    // DSOP Simulator Strings
    const val dsopSimulatorTitle = "DSOP Compound Simulator"
    const val dsopSimulatorMonthlySub = "Monthly Subscription"
    const val dsopSimulatorTaxWarning = "Tax Warning: Annual DSOP contributions above ₹5 Lakhs (₹41,666/mo) attract income tax on interest earned. Stay below ₹41,666/mo to keep gains 100% tax-free."
    const val dsopSimulatorTaxSafe = "Tax optimized: Annual contribution is below ₹5 Lakhs. All DSOP interest remains 100% tax-free under Section 10(11) of the Income Tax Act."
    const val dsopSimulator5Years = "5 Years"
    const val dsopSimulator10Years = "10 Years"
    const val dsopSimulator15Years = "15 Years"

    // Insights Screen Strings
    const val insightsEmptyState = "Please import or select a payslip to unlock financial insights."

    const val insightsSavingsRateTitle = "Monthly Savings Rate"
    const val insightsSavingsRateTarget = "Target: 20%+. You save "
    const val insightsSavingsRateSuffix = "% of your gross pay in DSOP and AGIF."

    // Insights section labels
    const val executiveSummaryLabel = "Executive Summary"
    const val criticalAlertsLabel = "Critical Alerts"
    const val changesThisMonthLabel = "Changes This Month"

    // Chart Legends
    const val legendNetTakeHome = "Net Take-Home"
    const val legendDsop = "Provident Fund (DSOP)"
    const val legendTax = "Taxes & Cess"
    const val legendOtherDeductions = "Other Deductions"
    const val legendGrossPay = "Gross Pay"
    const val legendNetRemittance = "Net Remittance"
    const val chartNoDataAvailable = "No data available"
    const val pdfIconLabel = "PDF"
    const val pdfTapToOpen = "Tap to open original statement"

    // Dashboard Empty State
    const val dashboardEmptyStateTitle = "No Payslips Imported"
    const val dashboardEmptyStateDesc = "Import your monthly payslips to unlock digital replicas, historical tracking, financial insights, and tax audits."
    const val dashboardEmptyStateLabel = "Tap the + button below to get started"
    const val dashboardEmptyStateDescSandbox = "Import your PDF payslips or seed simulated data from the import screen to view financial analytics."

    // Gemini AI Section Extra
    const val geminiAiAnalyzeBtn = "Analyze Payslip with Gemini AI"
    const val geminiAiAnalyzeDesc = "Generate professional tax saving suggestions, investment recommendations, and error audits using Gemini."
    const val geminiAiViewReportBtn = "View Full Audit Report"

    // AI CA Audit card (Phase 5)
    const val aiAuditAnalyzingDesc = "Analyzing your payslip with Gemini AI…"
    const val aiAuditSummaryLabel = "Summary"
    const val aiAuditTeaserBullets = "Detect PCDA errors & recoverable amounts\nPersonalised tax-saving strategy (old regime)\nInvestment guidance from your AI CA"
    const val aiAuditUnlockBtn = "Unlock AI CA Audit"
    const val aiAuditPremiumBadge = "Premium"

    // Premium Features Screens Strings
    const val representationTitle = "Official Representations"
    const val representationSubtitle = "Formal correspondence for pay and allowance disputes"
    const val representationDraftsEmpty = "No salary discrepancies or missing allowances detected. Representation drafts will appear here if the local audit engine flags any issues."
    const val representationCopyBtn = "Copy to Clipboard"
    const val representationEditBtn = "Edit Draft"
    const val representationSaveBtn = "Save Draft"

    const val taxPlanningTitle = "Tax Optimization Planner"
    const val taxPlanningSubtitle = "Form 16 projections & tax-saving suggestions"
    const val taxPlanningGrossYtd = "YTD Gross Salary"
    const val taxPlanningTaxable = "Net Taxable Income"
    const val taxPlanningITax = "Estimated Tax Liability"

    const val retirementTitle = "Retirement Wealth Projection"
    const val retirementSubtitle = "DSOP contribution & compound wealth estimates"
    const val retirementCompounding = "Compounding Growth Projections"
    const val retirementBalance = "Projected DSOP Balance"
    const val retirementContributions = "Total Principal Contributions"
    const val retirementInterest = "Total Interest Earned"
    const val retirementProjectionDisclaimer = "Note: Compounding projections are estimated using a standard interest rate of 7.1% per annum. Actual rates may vary."
    const val premiumToolsTitle = "Premium Financial Tools"
    const val premiumToolsDraftClaims = "Draft Claims"
    const val premiumToolsTaxPlanner = "Tax Planner"
    const val premiumToolsDsopSimulator = "DSOP Simulator"

    const val representationMonthLabel = "Month:"
    const val representationRecipientPcda = "Recipient: PCDA Pune"
    const val representationEditDraftTitle = "Edit Draft:"

    const val retirementNoBalance = "Import a payslip with DSOP ledger balance to simulate growth."
    const val retirementCurrentBalance = "Current DSOP Balance"
    const val taxPlanningSavingsProjections = "Section 80C & 80D Tax Savings Projections"
    const val taxPlanningNoProjections = "No YTD Tax Projections available for this month."
    const val taxPlanningCurrentYtdContribution = "Current YTD Contribution: ₹"

    // Executive Summary row labels
    const val execSummaryNetSalary = "Net Salary"
    const val execSummaryGrossPay = "Gross Pay"
    const val execSummaryIncomeTax = "Income Tax"
    const val execSummaryDsop = "DSOP Subscription"
}
