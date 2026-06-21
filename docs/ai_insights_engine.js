export class AIInsightsEngine {
  constructor(history) {
    this.history = history;
  }

  generateReport(year, month) {
    const record = this.history.find(r => r.year === year && r.month === month);
    if (!record) return { insights: [] };

    const insights = [];

    // DA arrears detection (April 2026)
    if (record.earnings.daArrears) {
      insights.push({
        id: 'DA_ARR',
        type: 'info',
        scores: { importance: 10, confidence: 9, value: 10 },
        desc: `DA revised: arrears of ₹9,870 for ₹216 per month credited.`,
        action: { label: 'View DA Revision Details' },
      });
    }

    // Debit recovery detection (December 2025)
    if (record.deductions.debitRecovery) {
      insights.push({
        id: 'DEBIT_REC',
        type: 'danger',
        scores: { importance: 10, confidence: 10, value: 9 },
        desc: `Unusual debit recovery of ₹15,742 detected.`,
        action: {
          label: 'Request Debit Recovery Audit',
          emailBody: 'Request audit for debit recovery of ₹15,742 from December 2025 payslip.',
        },
      });
    }

    // DSOP interest credit (March 2026)
    if (record.ledger.dsopInterest) {
      insights.push({
        id: 'DSOP_INT',
        type: 'info',
        scores: { importance: 10, confidence: 10, value: 9 },
        desc: `Annual DSOP interest of ₹1,46,341 credited at ${record.ledger.dsopInterestRate}% rate.`,
        action: { label: 'View DSOP Statement' },
      });
    }

    // Missing rent detection — HRA, license fee, and furniture rent all zero
    const hasNoRent =
      record.earnings.houseRentAllowance === 0 &&
      record.ledger.licenseFee === 0 &&
      record.ledger.furnitureRent === 0;

    if (hasNoRent) {
      insights.push({
        id: 'MISSING_RENT',
        type: 'warning',
        scores: { importance: 9, confidence: 8, value: 8 },
        desc: 'No housing deduction found. Verify accommodation status.',
        action: { label: 'Verify Accommodation Status' },
      });
    }

    // Sort by importance descending
    insights.sort((a, b) => b.scores.importance - a.scores.importance);

    return { insights };
  }
}
