// Localized String Resources

export const strings = {
  en: {
    appTitle: 'PCDA Payslip Portal',
    appSubtitle: 'Army Personnel Financial Analytics Dashboard',
    
    // Badge
    badgeOfficer: 'Officer',
    badgeCda: 'CDA A/C No',
    badgePan: 'PAN',
    
    // Overview Cards
    cardNetTitle: 'Latest Net Remittance',
    cardNetUnit: 'INR (₹)',
    cardNetDescPrefix: 'As of',
    cardBpTitle: 'Basic Pay (BPAY)',
    cardBpUnit: 'INR (₹)',
    cardBpDesc: '+ Allowances & Arrears',
    cardDsopTitle: 'DSOP Closing Balance',
    cardDsopUnit: 'INR (₹)',
    cardDsopDesc: 'Provident Fund Balance',
    cardTaxTitle: 'Effective Tax Rate',
    cardTaxUnit: 'YTD',
    cardTaxDesc: 'Income Tax Deductions',
    
    // Upload Card
    uploadHeader: 'Drop Your Encrypted Payslip PDF Here',
    uploadDesc: 'Simulate parser engine with password 535d04 to upload new payslips',
    
    // Charts
    chartIncomeTitle: 'Income Trend (2022 - 2025)',
    chartShareTitle: 'Latest Month Share',
    chartDsopTitle: 'DSOP Fund Accumulation (₹)',
    chartTaxTitle: 'Income Tax Projections YTD',
    
    // Explorer
    explorerHeader: 'Payslip Digital Replica',
    explorerSubheader: 'Explore detailed transaction components with contextual financial learning',
    replicaEarningTitle: 'Aaya / Earnings',
    replicaDeductionTitle: 'kTaOtI / Deductions',
    replicaGrossLabel: 'Gross Pay (kuula Aaya):',
    replicaDeductionsLabel: 'Total Deductions (kuula kTaOtI):',
    replicaNetLabel: 'Net Remittance:',
    replicaFooter: 'Principal Controller of Defence Accounts (Officers), Pune\nMinistry of Defence, Government of India',
    
    // Insights
    insightsTitle: 'Educative Financial Insights',
    
    // Modal
    modalTitle: 'Mock Parser Engine',
    labelSelectPdf: 'Select Payslip PDF',
    labelPassword: 'PDF Password',
    btnDecrypt: 'Decrypt & Parse Payslip',
    loaderDecrypt: 'Decrypting PDF using password...',
    loaderOcr: 'Running OCR & Text layout alignment...',
    loaderTables: 'Extracting Earnings & Deductions Tables...',
    loaderCodes: 'Mapping custom army pay codes (BPAY, MSP, DSOP)...',
    loaderStandardize: 'Standardizing database records...',
    alertSelectPdf: 'Please select a payslip PDF file to parse.',
    alertEnterPassword: 'Please enter the PDF decryption password.',
    alertSuccess: 'Successfully decrypted and parsed payslip! Added new data records to dashboard.'
  }
};
