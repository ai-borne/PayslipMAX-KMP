package com.ssbmax.pdfparser.ui.theme

object AppStrings {
    // App Branding & Navigation
    const val appTitle = "PCDA Payslip Portal"
    const val appSubtitle = "Indian Army Personnel Financial Analytics"
    const val navigationHome = "Dashboard"
    const val navigationExplorer = "Explorer"
    const val navigationInsights = "Insights"
    const val navigationImport = "Import"

    // Metadata Badges
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
    const val replicaEarningTitle = "Aaya / Credits"
    const val replicaDeductionTitle = "kTaOtI / Debits"
    const val replicaGrossLabel = "Gross Pay (kuula Aaya)"
    const val replicaDeductionsLabel = "Total Deductions (kuula kTaOtI)"
    const val replicaNetLabel = "Net Remittance"
    const val replicaFooter = "Principal Controller of Defence Accounts (Officers), Pune\nMinistry of Defence, Government of India"

    // Upload & Decrypt
    const val uploadHeader = "Import Encrypted Payslip"
    const val uploadDesc = "Secure, 100% offline-first parsing engine"
    const val labelSelectPdf = "Select PDF Payslip"
    const val labelPassword = "Decryption Password"
    const val btnDecrypt = "Decrypt & Parse"
    const val loaderDecrypt = "Decrypting PDF using Secure Enclave..."
    
    // Glossary Tooltip Titles
    const val glossaryTitleBasicPay = "Basic Pay (BPAY)"
    const val glossaryTitleDa = "Dearness Allowance (DA)"
    const val glossaryTitleMsp = "Military Service Pay (MSP)"
    const val glossaryTitleTpta = "Transport Allowance (TPTA)"
    const val glossaryTitleTptaDa = "DA on Transport Allowance (TPTADA)"
    const val glossaryTitleDress = "Dress Allowance (DRESALW)"
    const val glossaryTitleRation = "Ration Money Allowance (RSHNA)"
    const val glossaryTitleSf = "Special Forces Pay (SPCDO)"
    const val glossaryTitleField = "Field Area Allowance (FD)"
    const val glossaryTitleCea = "Children Education Allowance (CEA)"
    const val glossaryTitleDsop = "Defence Services Officers Provident Fund (DSOP)"
    const val glossaryTitleAgif = "Army Group Insurance Fund (AGIF)"
    const val glossaryTitleTax = "Income Tax Deducted (ITAX)"
    const val glossaryTitleCess = "Health & Education Cess (EHCESS)"
    const val glossaryTitleLf = "License Fee (LF)"
    const val glossaryTitleFur = "Furniture Rent (FUR)"
    const val glossaryTitleWater = "Water Charges"
    const val glossaryTitleElec = "Electricity Charges"
    const val glossaryTitleBarrack = "Barrack Damage Recovery"
    const val glossaryTitleTicket = "Air Ticket Recovery (ETKT)"

    // Dashboard - Officer Info Bar
    const val officerInfoLabel = "Officer"
    const val cdaInfoLabel = "CDA A/C"
    const val panInfoLabel = "PAN"

    // Dashboard - Year/Month Picker
    const val selectYearLabel = "Year"
    const val selectMonthLabel = "Month"
    const val analyzingStatement = "Analyzing Payslip Statement"
}
