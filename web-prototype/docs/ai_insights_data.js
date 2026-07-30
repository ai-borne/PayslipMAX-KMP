/**
 * Standardized mock payslip history dataset for AI Insights Engine testing.
 */
export const payslipHistory = [
  {
    year: 2026,
    month_num: 1,
    month_name: 'January',
    date_str: '01/2026',
    officer: { name: 'CAPTAIN VIKRAM', account_no: '987654', pan: 'ABCDE1234F' },
    earnings: { basic_pay: 120000.0, military_service_pay: 15500.0, da: 60000.0 },
    deductions: { dsop_subscription: 12000.0, income_tax: 25000.0, agif: 5000.0 },
    ledger_balances: {},
    summary: { gross_pay: 195500.0, total_deductions: 42000.0, net_remittance: 153500.0 },
    tax_and_savings: {
      total_taxable_income: 195500.0,
      total_tax_payable: 25000.0,
      dsop_fund: { closing_balance: 650000.0 }
    }
  },
  {
    year: 2026,
    month_num: 2,
    month_name: 'February',
    date_str: '02/2026',
    officer: { name: 'CAPTAIN VIKRAM', account_no: '987654', pan: 'ABCDE1234F' },
    earnings: { basic_pay: 120000.0, military_service_pay: 15500.0, da: 60000.0 },
    deductions: { dsop_subscription: 12000.0, income_tax: 25000.0, agif: 5000.0 },
    ledger_balances: {},
    summary: { gross_pay: 195500.0, total_deductions: 42000.0, net_remittance: 153500.0 },
    tax_and_savings: {
      total_taxable_income: 195500.0,
      total_tax_payable: 25000.0,
      dsop_fund: { closing_balance: 662000.0 }
    }
  }
];
