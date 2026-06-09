// ViewModel Layer - Translates model data for UI consumption

import { glossary } from './glossary.js';
import { strings } from './strings.js';

export class PayslipViewModel {
  constructor(model) {
    this.model = model;
    this.selectedYear = null;
    this.selectedMonth = null;
    this.activeRecord = null;
    
    this.listeners = [];
    
    // Subscribe to model changes
    this.model.subscribe(() => {
      this.refreshState();
      this.notify();
    });

    this.init();
  }

  init() {
    const years = this.model.getYears();
    if (years.length > 0) {
      this.selectedYear = years[0];
      const months = this.model.getMonthsForYear(this.selectedYear);
      if (months.length > 0) {
        this.selectedMonth = months[0];
      }
    }
    this.refreshActiveRecord();
  }

  // Observer Pattern
  subscribe(listener) {
    this.listeners.push(listener);
    return () => {
      this.listeners = this.listeners.filter(l => l !== listener);
    };
  }

  notify() {
    this.listeners.forEach(listener => listener());
  }

  refreshState() {
    this.refreshActiveRecord();
  }

  refreshActiveRecord() {
    if (this.selectedYear && this.selectedMonth) {
      this.activeRecord = this.model.getPayslip(this.selectedYear, this.selectedMonth);
    } else {
      this.activeRecord = this.model.getLatestRecord();
    }
  }

  // Setters
  setYear(year) {
    this.selectedYear = parseInt(year);
    const months = this.model.getMonthsForYear(this.selectedYear);
    if (months.length > 0 && !months.includes(this.selectedMonth)) {
      this.selectedMonth = months[0];
    }
    this.refreshActiveRecord();
    this.notify();
  }

  setMonth(monthName) {
    this.selectedMonth = monthName;
    this.refreshActiveRecord();
    this.notify();
  }

  // Formatter utilities
  formatCurrency(value) {
    if (value === undefined || value === null) return '₹0';
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(value);
  }

  formatLabel(key) {
    return key
      .split("_")
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(" ");
  }

  // Derived Properties for Summary Stats
  getOverviewStats() {
    const latest = this.model.getLatestRecord();
    const latestWithBasic = this.model.getLatestWithBasicPay();
    const latestWithTax = this.model.getLatestWithTax();

    const netRemittance = latest ? this.formatCurrency(latest.summary.net_remittance) : '₹0';
    const netDate = latest ? `${strings.en.cardNetDescPrefix} ${latest.month_name} ${latest.year}` : '';
    
    const basicPay = latestWithBasic ? this.formatCurrency(latestWithBasic.earnings.basic_pay) : '₹0';
    
    const dsopBalance = latestWithTax ? this.formatCurrency(latestWithTax.tax_and_savings.dsop_fund.closing_balance) : '₹0';
    
    let taxRate = '0.0%';
    if (latestWithTax && latestWithTax.tax_and_savings.total_taxable_income > 0) {
      const rate = (latestWithTax.tax_and_savings.total_tax_payable / latestWithTax.tax_and_savings.total_taxable_income) * 100;
      taxRate = `${rate.toFixed(1)}%`;
    }

    return { netRemittance, netDate, basicPay, dsopBalance, taxRate };
  }

  // Insight Generation (Pure calculation)
  getInsights() {
    const record = this.activeRecord;
    if (!record) return [];

    const insights = [];
    const basicPay = record.earnings.basic_pay || 0;
    const dsop = record.deductions.dsop_subscription || 0;
    const gross = record.summary.gross_pay || 1;

    // 1. Savings Rate
    if (dsop > 0) {
      const dsopRate = (dsop / gross) * 100;
      insights.push({
        title: `Excellent Savings Rate (${dsopRate.toFixed(1)}%)`,
        desc: `You saved ${this.formatCurrency(dsop)} in your DSOP Fund this month, which is ${dsopRate.toFixed(1)}% of your gross earnings. DSOP is compounding tax-free interest, making it a powerful wealth builder.`,
        icon: "📈",
        type: "success"
      });
    } else if (basicPay > 0) {
      insights.push({
        title: "Action Needed: Zero DSOP Contribution",
        desc: "No DSOP subscription was deducted this month. Army rules mandate a minimum contribution of 6% of your Basic Pay to your provident fund for retirement security.",
        icon: "⚠️",
        type: "warning"
      });
    }

    // 2. Tax Burden
    const tax = (record.deductions.income_tax || 0) + (record.deductions.education_cess || 0);
    if (tax > 0) {
      const taxRate = (tax / gross) * 100;
      insights.push({
        title: `Income Tax Burden (${taxRate.toFixed(1)}%)`,
        desc: `A total of ${this.formatCurrency(tax)} was deducted for Income Tax (including Cess) this month, consuming ${taxRate.toFixed(1)}% of your gross pay.`,
        icon: "🏛️",
        type: "accent"
      });
    }

    // 3. Housing
    const lf = record.deductions.license_fee || 0;
    const fur = record.deductions.furniture_rent || 0;
    if (lf > 0) {
      const totalAcc = lf + fur;
      insights.push({
        title: "Government Housing Value",
        desc: `You were charged a License Fee of ${this.formatCurrency(lf)} and Furniture Rent of ${this.formatCurrency(fur)} (Total: ${this.formatCurrency(totalAcc)}). Compared to open-market rentals, this represents an immense cost subsidy of approximately ₹20,000+ per month.`,
        icon: "🏠",
        type: "success"
      });
    }

    // 4. Special Allowances
    const spcf = record.earnings.special_forces_pay || 0;
    if (spcf > 0) {
      insights.push({
        title: "Special Forces Operational Pay Active",
        desc: `Your payslip shows ${this.formatCurrency(spcf)} credited as Special Forces/Command Pay. This is an elite hazard allowance paid for specialized service conditions.`,
        icon: "🦅",
        type: "success"
      });
    }

    // 5. Arrears
    const arrearsList = Object.keys(record.earnings).filter(k => k.startsWith("arrears_") || k.startsWith("adj_"));
    if (arrearsList.length > 0) {
      const totalArr = arrearsList.reduce((sum, k) => sum + record.earnings[k], 0);
      insights.push({
        title: `Arrears & Adjustments Credited: ${this.formatCurrency(totalArr)}`,
        desc: `Your account received arrears credits this month (e.g. ${arrearsList.map(a => this.formatLabel(a)).join(", ")}). Consider routing these lump-sums directly into savings rather than discretionary spending.`,
        icon: "💰",
        type: "success"
      });
    }

    // 6. General
    const msp = record.earnings.military_service_pay || 0;
    if (msp > 0) {
      insights.push({
        title: `Military Service Pay (${this.formatCurrency(msp)})`,
        desc: `MSP is a compensation model paid only to military officers. Unlike civil services, it compensates for the constant hazard, relocation, and physical challenges of your military career.`,
        icon: "🎖️",
        type: "accent"
      });
    }

    return insights;
  }

  // Parse Action Trigger
  parseUploadedPayslip(password, onProgress, onComplete, onError) {
    if (!password) {
      onError(strings.en.alertEnterPassword);
      return;
    }

    const steps = [
      { text: strings.en.loaderDecrypt, delay: 500 },
      { text: strings.en.loaderOcr, delay: 800 },
      { text: strings.en.loaderTables, delay: 800 },
      { text: strings.en.loaderCodes, delay: 800 },
      { text: strings.en.loaderStandardize, delay: 800 }
    ];

    let currentStep = 0;

    const runStep = () => {
      if (currentStep < steps.length) {
        onProgress(steps[currentStep].text);
        setTimeout(() => {
          currentStep++;
          runStep();
        }, steps[currentStep].delay);
      } else {
        // Success: Inject mock record
        // We simulate adding August 2025 record if not already present
        // Let's find latest record to copy its metadata
        const base = this.model.getPayslips().find(d => d.year == 2025 && d.month_num == 8);
        if (base) {
          const addedRecord = this.model.addMockPayslip(2025, 8, "August", base);
          this.selectedYear = 2025;
          this.selectedMonth = "August";
          this.refreshActiveRecord();
          onComplete(addedRecord);
        } else {
          onError("Parser base record not found.");
        }
      }
    };

    runStep();
  }
}
