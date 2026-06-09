// Model Layer - Manages payslip data and notifications

import rawData from './payslips_data_standardized.json';

export class PayslipModel {
  constructor() {
    this.payslips = [...rawData];
    this.listeners = [];
  }

  // Observer Pattern
  subscribe(listener) {
    this.listeners.push(listener);
    return () => {
      this.listeners = this.listeners.filter(l => l !== listener);
    };
  }

  notify() {
    this.listeners.forEach(listener => listener(this.payslips));
  }

  // Data Queries
  getPayslips() {
    return this.payslips;
  }

  getPayslip(year, monthName) {
    return this.payslips.find(
      d => d.year == year && d.month_name.toLowerCase() === monthName.toLowerCase()
    );
  }

  getYears() {
    return [...new Set(this.payslips.map(d => d.year))].sort((a, b) => b - a);
  }

  getMonthsForYear(year) {
    return this.payslips
      .filter(d => d.year == year)
      .sort((a, b) => b.month_num - a.month_num)
      .map(d => d.month_name);
  }

  getLatestRecord() {
    if (this.payslips.length === 0) return null;
    return this.payslips[this.payslips.length - 1];
  }

  getLatestWithBasicPay() {
    return [...this.payslips]
      .reverse()
      .find(d => d.earnings && d.earnings.basic_pay > 0);
  }

  getLatestWithTax() {
    return [...this.payslips]
      .reverse()
      .find(
        d => d.tax_and_savings && 
             d.tax_and_savings.dsop_fund && 
             d.tax_and_savings.dsop_fund.closing_balance > 0
      );
  }

  // Data Actions
  addMockPayslip(year, monthNum, monthName, baseRecord) {
    // Check if it already exists to prevent duplicates
    const existingIndex = this.payslips.findIndex(
      d => d.year == year && d.month_num == monthNum
    );
    
    const newRecord = {
      ...baseRecord,
      year: parseInt(year),
      month_num: parseInt(monthNum),
      month_name: monthName,
      date_str: `${String(monthNum).padStart(2, '0')}/${year}`,
      file: `${String(monthNum).padStart(2, '0')} ${monthName} ${year}.pdf`
    };

    if (existingIndex !== -1) {
      this.payslips[existingIndex] = newRecord;
    } else {
      this.payslips.push(newRecord);
      // Sort chronologically by year and then month_num
      this.payslips.sort((a, b) => {
        if (a.year !== b.year) return a.year - b.year;
        return a.month_num - b.month_num;
      });
    }

    this.notify();
    return newRecord;
  }
}
