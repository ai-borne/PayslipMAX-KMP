import { describe, it, expect } from 'vitest';
import { PayslipModel } from '../model.js';
import { PayslipViewModel } from '../viewModel.js';

// Setup Mock Data for testing
const mockPayslips = [
  {
    year: 2022,
    month_num: 1,
    month_name: 'January',
    date_str: '01/2022',
    officer: { name: 'TEST OFFICER', account_no: '12345', pan: 'PAN123' },
    earnings: { basic_pay: 100000.0, military_service_pay: 15500.0 },
    deductions: { dsop_subscription: 10000.0, income_tax: 20000.0 },
    ledger_balances: {},
    summary: { gross_pay: 115500.0, total_deductions: 30000.0, net_remittance: 85500.0 },
    tax_and_savings: {
      total_taxable_income: 115500.0,
      total_tax_payable: 20000.0,
      dsop_fund: { closing_balance: 500000.0 }
    }
  }
];

class TestModel extends PayslipModel {
  constructor() {
    super();
    this.payslips = [...mockPayslips];
  }
}

describe('PayslipModel & PayslipViewModel Tests', () => {
  it('should format currency correctly in En-IN', () => {
    const model = new TestModel();
    const vm = new PayslipViewModel(model);
    
    expect(vm.formatCurrency(1000)).toBe('₹1,000');
    expect(vm.formatCurrency(100000)).toBe('₹1,00,000');
    expect(vm.formatCurrency(0)).toBe('₹0');
    expect(vm.formatCurrency(null)).toBe('₹0');
  });

  it('should initialize and select the latest record by default', () => {
    const model = new TestModel();
    const vm = new PayslipViewModel(model);
    
    expect(vm.selectedYear).toBe(2022);
    expect(vm.selectedMonth).toBe('January');
    expect(vm.activeRecord.officer.name).toBe('TEST OFFICER');
  });

  it('should calculate overview stats correctly', () => {
    const model = new TestModel();
    const vm = new PayslipViewModel(model);
    const stats = vm.getOverviewStats();
    
    expect(stats.netRemittance).toBe('₹85,500');
    expect(stats.basicPay).toBe('₹1,00,000');
    expect(stats.dsopBalance).toBe('₹5,00,000');
    expect(stats.taxRate).toBe('17.3%');
  });

  it('should generate financial insights correctly', () => {
    const model = new TestModel();
    const vm = new PayslipViewModel(model);
    const insights = vm.getInsights();
    
    // Check savings rate insight (10000 / 115500 = 8.66% -> 8.7%)
    const savingsInsight = insights.find(i => i.title.includes('Savings Rate'));
    expect(savingsInsight).toBeDefined();
    expect(savingsInsight.title).toBe('Excellent Savings Rate (8.7%)');
    expect(savingsInsight.type).toBe('success');

    // Check tax burden insight (20000 / 115500 = 17.31% -> 17.3%)
    const taxInsight = insights.find(i => i.title.includes('Tax Burden'));
    expect(taxInsight).toBeDefined();
    expect(taxInsight.title).toBe('Income Tax Burden (17.3%)');
    expect(taxInsight.type).toBe('accent');
  });
});
