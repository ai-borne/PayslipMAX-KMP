// Glossary terms dictionary for military payslip terms

export const glossary = {
  basic_pay: {
    title: 'Basic Pay (BPAY)',
    desc: 'The core salary component determined by your rank and pay level under the 7th Pay Commission. It forms the base for calculating allowances like DA.'
  },
  dearness_allowance: {
    title: 'Dearness Allowance (DA)',
    desc: 'A cost-of-living adjustment allowance paid to military personnel to mitigate inflation. Revised twice a year (January and July) based on the Consumer Price Index.'
  },
  military_service_pay: {
    title: 'Military Service Pay (MSP)',
    desc: 'A unique monthly allowance of ₹15,500 paid to Indian military officers to compensate for the hazards, hardships, and unique constraints of military life.'
  },
  transport_allowance: {
    title: 'Transport Allowance (TPTA)',
    desc: 'A fixed allowance paid to cover commuting costs between residence and headquarters. Varies based on pay level and cities classified by location.'
  },
  transport_allowance_da: {
    title: 'DA on Transport Allowance (TPTADA)',
    desc: 'The inflation adjustment (Dearness Allowance percentage) applied specifically on top of your Transport Allowance rate.'
  },
  dress_allowance: {
    title: 'Dress Allowance (DRESALW)',
    desc: 'An annual uniform maintenance allowance (typically ₹20,000, paid in July) to assist officers with uniform procurement and tailoring.'
  },
  ration_money: {
    title: 'Ration Money Allowance (RSHNA)',
    desc: 'A cash allowance paid to officers to cover dietary expenses when free messing or dry rations are not provided at their station.'
  },
  special_forces_pay: {
    title: 'Special Forces / Command Pay (SPCDO)',
    desc: 'A hazard and proficiency allowance paid to officers serving in Special Forces, airborne units, or specialized operational commands.'
  },
  field_allowance: {
    title: 'Field Area Allowance (FD)',
    desc: 'A monthly allowance compensating for postings in active field areas, modified field areas, or high-altitude sectors.'
  },
  children_education_allowance: {
    title: 'Children Education Allowance (CEA)',
    desc: 'A reimbursement allowance to assist with the schooling and hostel expenses of up to two children.'
  },
  dsop_subscription: {
    title: 'Defence Services Officers Provident Fund (DSOP)',
    desc: 'A mandatory retirement savings scheme. Contributed monthly (minimum 6% of basic pay). It earns high, compound tax-free interest.'
  },
  agif: {
    title: 'Army Group Insurance Fund (AGIF)',
    desc: 'A mandatory group insurance scheme providing high life cover and terminal benefits. Premium is deducted directly from monthly salary.'
  },
  income_tax: {
    title: 'Income Tax Deducted (ITAX)',
    desc: 'Tax Deducted at Source (TDS) calculated based on your estimated taxable annual income under the selected tax regime.'
  },
  education_cess: {
    title: 'Health & Education Cess (EHCESS)',
    desc: 'An additional 4% surcharge levied on your Income Tax amount to fund national education and health programs.'
  },
  license_fee: {
    title: 'License Fee (LF)',
    desc: 'A highly subsidized monthly rent deducted for officers occupying government-provided married or single accommodation.'
  },
  furniture_rent: {
    title: 'Furniture Rent (FUR)',
    desc: 'A nominal charge deducted for using government-provided furniture, fans, geysers, or electrical appliances in military quarters.'
  },
  water_charges: {
    title: 'Water Charges (WATER)',
    desc: 'A nominal recovery fee for water supply provided to your government accommodation.'
  },
  electricity_charges: {
    title: 'Electricity Charges (Elec)',
    desc: 'Deduction for electric power consumed in your quarter, based on reading units or flat-rate quarters classification.'
  },
  barrack_damage: {
    title: 'Barrack Damage Recovery',
    desc: 'Deductions made to recover the cost of repairs for any damage done to government quarters or property during occupancy.'
  },
  ticket_recovery: {
    title: 'Air Ticket Recovery (ETKT)',
    desc: 'Recovery of advances generated when booking official air travel through the Defense Travel System (DTS) portal.'
  },
  opening_credit_balance: {
    title: 'Opening Credit Balance (Op Cr Bal)',
    desc: "A positive balance brought forward from your previous month's ledger. Indicates PCDA owed you money."
  },
  opening_debit_balance: {
    title: 'Opening Debit Balance (Op Dr Bal)',
    desc: 'A negative balance brought forward from the previous month. Indicates you owed money to the PCDA ledger.'
  },
  closing_credit_balance: {
    title: 'Closing Credit Balance (Cl. Cr. Bal.)',
    desc: "The net positive balance at the month's end. Carries forward to next month's ledger instead of being paid out as cash."
  },
  closing_debit_balance: {
    title: 'Closing Debit Balance (Cl. Dr. Bal.)',
    desc: "The net negative balance at the month's end. Represents your outstanding dues to PCDA for the next month."
  },
  recovery_of_debits: {
    title: 'Recovery of Debits',
    desc: 'Ledger adjustments where past outstanding dues are reconciled and recovered from your current credits.'
  },
  house_rent_allowance: {
    title: 'House Rent Allowance (HRA)',
    desc: 'Paid to officers who do not reside in government-provided married quarters, compensating for rental accommodation costs.'
  },
  risk_hardship_allowance: {
    title: 'Risk & Hardship Allowance (RHA)',
    desc: 'Compensates military personnel deployed in hazardous operational zones, high-altitude sectors, or counter-insurgency areas.'
  },
  non_practicing_allowance: {
    title: 'Non-Practicing Allowance (NPA)',
    desc: 'A compensatory allowance paid to medical officers of the armed forces in lieu of private practice.'
  },
  arrears_risk_hardship: {
    title: 'Arrears of RHA',
    desc: 'Retroactive payments credited for past deployments in risk-designated areas.'
  },
  misc_earnings: {
    title: 'Miscellaneous Credits (MISC)',
    desc: 'Unmapped credits or ledger adjustment reconciliation differences computed to balance parsed totals with printed Gross Pay.'
  },
  aobf: {
    title: 'Army Officers Benevolent Fund (AOBF)',
    desc: 'A monthly contribution deducted for officers welfare and benevolent fund schemes.'
  },
  agif_loan_recovery: {
    title: 'AGIF Loan Recovery',
    desc: 'Recovery of advances or loans (such as Car or Motorcycle loans) obtained from the Army Group Insurance Fund.'
  },
  misc_deductions: {
    title: 'Miscellaneous Deductions (MISC)',
    desc: 'Unmapped deductions or ledger debit reconciliation differences computed to balance parsed totals with printed Deductions.'
  }
};
