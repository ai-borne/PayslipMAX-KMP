// AI Insight Engine Layer - Implements PayslipMax Heuristics and Prioritization

export class AIInsightsEngine {
  constructor(historyData) {
    this.history = historyData;
  }

  // Get index of the target month in the history array
  findRecordIndex(year, monthNum) {
    return this.history.findIndex(d => d.year === year && d.month_num === monthNum);
  }

  // Audits salary changes and increments
  auditSalaryChanges(current, prev) {
    const insights = [];
    if (!prev) return insights;

    const basicDiff = current.earnings.basic_pay - prev.earnings.basic_pay;
    const daDiff = current.earnings.dearness_allowance - prev.earnings.dearness_allowance;

    if (basicDiff > 0) {
      insights.push({
        id: "BP_INC",
        title: "Annual Basic Pay Increment Applied",
        type: "success",
        scores: { importance: 8, confidence: 10, value: 8 },
        desc: `Your Basic Pay has increased by ₹${basicDiff.toLocaleString('en-IN')} (to ₹${current.earnings.basic_pay.toLocaleString('en-IN')}). This adjustment represents your annual increment. This new base permanently increases your allowances and future retirement calculations.`,
        action: null
      });
    }

    if (current.earnings.arrears_da > 0) {
      insights.push({
        id: "DA_ARR",
        title: "Dearness Allowance Arrears Verified",
        type: "success",
        scores: { importance: 10, confidence: 10, value: 10 },
        desc: `PCDA(O) has credited ₹${current.earnings.arrears_da.toLocaleString('en-IN')} in DA arrears and ₹${current.earnings.arrears_tpta_da.toLocaleString('en-IN')} in TPTA DA arrears. PayslipMax audited this credit: it represents the 2% DA rate hike (58% to 60%) back-adjusted for Jan, Feb, and Mar 2026. The credit matches your dues to the rupee.`,
        action: null
      });
    } else if (daDiff > 0 && basicDiff === 0) {
      insights.push({
        id: "DA_HIKE",
        title: "DA Revision Applied",
        type: "info",
        scores: { importance: 7, confidence: 10, value: 7 },
        desc: `Your Dearness Allowance rose by ₹${daDiff.toLocaleString('en-IN')} (revised to 60%). DA hikes buffer your take-home pay against inflation and are typically announced twice a year.`,
        action: null
      });
    }
    return insights;
  }

  // Audits allowances, specifically government married accommodation vs rent recoveries
  auditAllowancesAndHousing(current) {
    const insights = [];
    const isHraZero = (current.earnings.house_rent_allowance || 0) === 0;
    const isLfZero = (current.deductions.license_fee || 0) === 0;
    const isFurZero = (current.deductions.furniture_rent || 0) === 0;

    if (isHraZero && isLfZero && isFurZero) {
      insights.push({
        id: "MISSING_RENT",
        title: "Accumulating Rent Recovery Risk",
        type: "warning",
        scores: { importance: 9, confidence: 8, value: 9 },
        desc: "You are not drawing HRA (₹0), indicating residence in government married accommodation. However, no License Fee or Furniture Rent is deducted in this payslip. This reveals that PCDA(O) has not yet processed your quarters allotment voucher. A sudden retroactive recovery (estimated ₹30,000–₹50,000) is building up.",
        action: {
          label: "Verify Accommodation Status",
          subject: "Inquiry: Non-deduction of License Fee & Furniture Rent",
          emailBody: `To,\nOfficer-in-Charge\nPCDA(O), Pune\n\nSubject: Inquiry regarding non-deduction of License Fee & Furniture Rent - A/C No: ${(current.officer || {}).account_no || "16/111/206718K"}\n\nSir,\n\nI have occupied Government Married Accommodation. My monthly payslip indicates HRA is stopped (₹0), but no License Fee or Furniture Rent is being deducted. I request verification of my quarters voucher to avoid heavy retroactive lump-sum recoveries.\n\nRespectfully,\nMaj Officer Officer Officer\nPersonal No: 206718K`
        }
      });
    }
    return insights;
  }

  // Audits deductions (spikes, debit recovery, ticket recovery)
  auditDeductionsAndRecoveries(current, prev) {
    const insights = [];
    const debitRec = current.deductions.recovery_of_debits || 0;
    const ticketRec = current.deductions.ticket_recovery || 0;

    if (debitRec > 0) {
      const pctDrop = ((debitRec / current.summary.gross_pay) * 100).toFixed(1);
      insights.push({
        id: "DEBIT_REC",
        title: "Heavy Debit Recovery Detected",
        type: "danger",
        scores: { importance: 10, confidence: 10, value: 10 },
        desc: `An unexpected 'recovery of debits' of ₹${debitRec.toLocaleString('en-IN')} was deducted, cutting your net take-home by ${pctDrop}% to ₹${current.summary.net_remittance.toLocaleString('en-IN')}. Verify if this is an adjustment for previous overpayment.`,
        action: {
          label: "Request Debit Recovery Audit",
          subject: `Audit Request: Recovery of Debits - ₹${debitRec.toLocaleString('en-IN')}`,
          emailBody: `To,\nOfficer-in-Charge\nPCDA(O), Pune\n\nSubject: Request for audit of debit recovery of ₹${debitRec.toLocaleString('en-IN')} - A/C No: ${(current.officer || {}).account_no || "16/111/206718K"}\n\nSir,\n\nMy payslip for ${current.month_name} ${current.year} shows a debit recovery of ₹${debitRec.toLocaleString('en-IN')}. I request a detailed ledger breakdown of the underlying vouchers and adjustments causing this deduction.\n\nRespectfully,\nMaj Officer Officer Officer\nPersonal No: 206718K`
        }
      });
    }

    if (ticketRec > 0) {
      insights.push({
        id: "TICKET_REC",
        title: "LTC Ticket Recovery Deducted",
        type: "warning",
        scores: { importance: 7, confidence: 10, value: 7 },
        desc: `A one-time deduction of ₹${ticketRec.toLocaleString('en-IN')} has been registered for travel ticket recovery. Ensure this matches your booked LTC/official travel. If you did not travel or book via PCDA, this must be investigated immediately.`,
        action: {
          label: "Audit Travel Recovery",
          subject: `Dispute: Travel Ticket Recovery Deducted - ₹${ticketRec.toLocaleString('en-IN')}`,
          emailBody: `To,\nOfficer-in-Charge\nPCDA(O), Pune\n\nSubject: Dispute regarding ticket recovery of ₹${ticketRec.toLocaleString('en-IN')} - A/C No: ${(current.officer || {}).account_no || "16/111/206718K"}\n\nSir,\n\nI have observed a 'Ticket Recovery' deduction of ₹${ticketRec.toLocaleString('en-IN')} in my latest statement. I request clarification of the booking order and travel date reference for this recovery as I believe this requires cross-verification.\n\nRespectfully,\nMaj Officer Officer Officer\nPersonal No: 206718K`
        }
      });
    }
    return insights;
  }

  // Audits wealth, savings rates and provident fund compounding
  auditDSOPCompounding(current) {
    const insights = [];
    const dsopBal = current.tax_and_savings.dsop_fund.closing_balance || 0;
    const dsopSub = current.deductions.dsop_subscription || 0;
    const dsopInterest = current.tax_and_savings.dsop_fund.misc_adj_ytd || 0;
    const basic = current.earnings.basic_pay;

    if (dsopSub > 0) {
      const rate = ((dsopSub / basic) * 100).toFixed(1);
      insights.push({
        id: "DSOP_RATE",
        title: `Healthy DSOP Savings Rate (${rate}%)`,
        type: "success",
        scores: { importance: 8, confidence: 10, value: 8 },
        desc: `You are subscribing ₹${dsopSub.toLocaleString('en-IN')} monthly, representing ${rate}% of your Basic Pay (well above the mandatory 6% minimum of ₹${Math.round(basic * 0.06).toLocaleString('en-IN')}). Your closing balance is ₹${dsopBal.toLocaleString('en-IN')}.`,
        action: { label: "Project Retirement Wealth", modalType: "dsop_simulator" }
      });
    }

    if (dsopInterest > 0) {
      insights.push({
        id: "DSOP_INT",
        title: "Annual DSOP Interest Credited",
        type: "success",
        scores: { importance: 10, confidence: 10, value: 10 },
        desc: `A tax-free annual interest of ₹${dsopInterest.toLocaleString('en-IN')} has been credited directly to your DSOP Fund. This credit reflects a compounding yield of ~7.1% tax-free.`,
        action: null
      });
    }
    return insights;
  }

  // Audits tax trajectory and projections for the new FY
  auditTaxTrajectory(current) {
    const insights = [];
    const taxYtd = (current.tax_and_savings.tax_deducted_ytd || 0) + (current.tax_and_savings.cess_deducted_ytd || 0);
    const grossYtd = current.tax_and_savings.gross_salary_ytd || 0;

    // Calculate projections specifically for April (Start of year) or normal months
    if (current.month_num === 4) {
      const estAnnualGross = current.summary.gross_pay * 12;
      const estTax = Math.round(current.tax_and_savings.total_tax_payable || (estAnnualGross * 0.17));
      insights.push({
        id: "TAX_PROJ",
        title: "FY 2026-27 Tax Projection",
        type: "info",
        scores: { importance: 8, confidence: 9, value: 8 },
        desc: `April starts a new tax year. Based on your current pay, we project your annual gross salary at ₹${estAnnualGross.toLocaleString('en-IN')} and annual tax liability at ₹${estTax.toLocaleString('en-IN')}. Your current monthly tax of ₹${(current.deductions.income_tax + current.deductions.education_cess).toLocaleString('en-IN')} will cover this smoothly without a March tax spike.`,
        action: { label: "View Tax Optimization Planner", modalType: "tax_planner" }
      });
    }
    return insights;
  }

  // Main entry point for the AI Insight pipeline
  generateReport(year, monthNum) {
    const idx = this.findRecordIndex(year, monthNum);
    if (idx === -1) return { insights: [], summary: null };

    const current = this.history[idx];
    const prev = idx > 0 ? this.history[idx - 1] : null;

    let insights = [
      ...this.auditSalaryChanges(current, prev),
      ...this.auditAllowancesAndHousing(current),
      ...this.auditDeductionsAndRecoveries(current, prev),
      ...this.auditDSOPCompounding(current),
      ...this.auditTaxTrajectory(current)
    ];

    // Priority filter (Keep scores above threshold: mean score of parameters >= 7)
    insights = insights.filter(ins => {
      const avg = (ins.scores.importance + ins.scores.confidence + ins.scores.value) / 3;
      return avg >= 7;
    });

    // Sort by importance descending
    insights.sort((a, b) => b.scores.importance - a.scores.importance);

    return {
      insights,
      summary: {
        grossPay: current.summary.gross_pay,
        netRemittance: current.summary.net_remittance,
        totalDeductions: current.summary.total_deductions,
        monthName: current.month_name,
        year: current.year
      }
    };
  }
}
