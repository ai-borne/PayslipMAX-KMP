/**
 * AIInsightsEngine - Production Analytics Engine for Web Prototype.
 * Single Source of Truth for web analytics summaries and tax recommendations.
 */
export class AIInsightsEngine {
  constructor(payslips = []) {
    this.payslips = Array.isArray(payslips) ? payslips : [];
  }

  /**
   * Aggregates total taxable income and total tax paid across the payslip ledger history.
   * @returns {{ totalTaxableIncome: number, totalTaxPaid: number, payslipCount: number }}
   */
  getAnalyticsSummary() {
    let totalTaxableIncome = 0.0;
    let totalTaxPaid = 0.0;

    for (const payslip of this.payslips) {
      if (payslip?.tax_and_savings?.total_taxable_income) {
        totalTaxableIncome += Number(payslip.tax_and_savings.total_taxable_income);
      }
      if (payslip?.tax_and_savings?.total_tax_payable) {
        totalTaxPaid += Number(payslip.tax_and_savings.total_tax_payable);
      }
    }

    return {
      totalTaxableIncome,
      totalTaxPaid,
      payslipCount: this.payslips.length,
    };
  }

  /**
   * Evaluates tax optimization rules to yield actionable wealth recommendations.
   * @returns {Array<{ id: string, title: string, estSaving: number, description: string }>}
   */
  getRecommendations() {
    if (this.payslips.length === 0) {
      return [];
    }

    const recommendations = [];
    const latestPayslip = this.payslips[this.payslips.length - 1];
    const annualDsop = (latestPayslip?.deductions?.dsop_subscription || 0) * 12;

    if (annualDsop < 150000) {
      const gap = 150000 - annualDsop;
      recommendations.push({
        id: 'dsop_80c_opt',
        title: 'Maximize Section 80C via DSOP',
        estSaving: Math.round(gap * 0.3),
        description: `Increase monthly DSOP by ${this.formatCurrency(Math.round(gap / 12))} to fully utilize Section 80C.`,
      });
    }

    recommendations.push({
      id: 'nps_80ccd_opt',
      title: 'NPS Section 80CCD(1B) Deduction',
      estSaving: 15000,
      description: 'Invest ₹50,000 in NPS Tier-1 for an additional tax deduction over Section 80C.',
    });

    return recommendations;
  }

  /**
   * Formats a numeric value into INR currency format (e.g. ₹1,00,000).
   * @param {number|null|undefined} val
   * @returns {string}
   */
  formatCurrency(val) {
    if (val === null || val === undefined || isNaN(val)) {
      return '₹0';
    }
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(val);
  }
}
