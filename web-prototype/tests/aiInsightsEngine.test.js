import { describe, it, expect } from 'vitest';
import { AIInsightsEngine } from '../../docs/ai_insights_engine.js';
import { payslipHistory } from '../../docs/ai_insights_data.js';

describe('AIInsightsEngine - Production Analytics Rules', () => {
  const engine = new AIInsightsEngine(payslipHistory);

  it('should identify annual DA revision and verify arrears in April 2026', () => {
    const report = engine.generateReport(2026, 4);
    
    // Verify arrears audit exists
    const daArrears = report.insights.find(i => i.id === 'DA_ARR');
    expect(daArrears).toBeDefined();
    expect(daArrears.scores.importance).toBe(10);
    expect(daArrears.scores.value).toBe(10);
    expect(daArrears.desc).toContain('₹9,870');
    expect(daArrears.desc).toContain('₹216');
  });

  it('should flag the massive debit recovery deduction in December 2025', () => {
    const report = engine.generateReport(2025, 12);
    
    const debitAnomaly = report.insights.find(i => i.id === 'DEBIT_REC');
    expect(debitAnomaly).toBeDefined();
    expect(debitAnomaly.scores.importance).toBe(10);
    expect(debitAnomaly.type).toBe('danger');
    expect(debitAnomaly.action.label).toBe('Request Debit Recovery Audit');
    expect(debitAnomaly.action.emailBody).toContain('₹15,742');
  });

  it('should detect annual DSOP interest credit and closing balance in March 2026', () => {
    const report = engine.generateReport(2026, 3);
    
    const interestCredit = report.insights.find(i => i.id === 'DSOP_INT');
    expect(interestCredit).toBeDefined();
    expect(interestCredit.scores.importance).toBe(10);
    expect(interestCredit.desc).toContain('₹1,46,341');
    expect(interestCredit.desc).toContain('7.1%');
  });

  it('should trigger housing recovery alerts when HRA, license fee, and furniture rent are all zero', () => {
    // True for all 6 months since they have married accommodation but no deduction voucher processed yet
    const report = engine.generateReport(2026, 4);
    
    const rentRisk = report.insights.find(i => i.id === 'MISSING_RENT');
    expect(rentRisk).toBeDefined();
    expect(rentRisk.scores.importance).toBe(9);
    expect(rentRisk.type).toBe('warning');
    expect(rentRisk.action.label).toBe('Verify Accommodation Status');
  });

  it('should output prioritized insights with avg score >= 7', () => {
    const report = engine.generateReport(2026, 4);
    
    // Ensure all returned insights satisfy high importance threshold
    report.insights.forEach(ins => {
      const avg = (ins.scores.importance + ins.scores.confidence + ins.scores.value) / 3;
      expect(avg).toBeGreaterThanOrEqual(7);
    });

    // Ensure it is sorted descending by importance
    for (let i = 0; i < report.insights.length - 1; i++) {
      expect(report.insights[i].scores.importance).toBeGreaterThanOrEqual(report.insights[i+1].scores.importance);
    }
  });
});
