import { describe, it, expect } from 'vitest';
import { AIInsightsEngine } from '../docs/ai_insights_engine.js';
import { payslipHistory } from '../docs/ai_insights_data.js';

describe('AIInsightsEngine - Production Analytics Rules', () => {
  it('should initialize and aggregate total taxable income and tax paid correctly', () => {
    const engine = new AIInsightsEngine(payslipHistory);
    const summary = engine.getAnalyticsSummary();

    expect(summary.totalTaxableIncome).toBe(391000.0);
    expect(summary.totalTaxPaid).toBe(50000.0);
    expect(summary.payslipCount).toBe(2);
  });

  it('should format currency correctly in INR format', () => {
    const engine = new AIInsightsEngine(payslipHistory);

    expect(engine.formatCurrency(100000)).toBe('₹1,00,000');
    expect(engine.formatCurrency(0)).toBe('₹0');
    expect(engine.formatCurrency(null)).toBe('₹0');
  });

  it('should calculate tax optimization recommendations correctly', () => {
    const engine = new AIInsightsEngine(payslipHistory);
    const recommendations = engine.getRecommendations();

    expect(Array.isArray(recommendations)).toBe(true);
    expect(recommendations.length).toBeGreaterThan(0);

    const dsopRec = recommendations.find((r) => r.id === 'dsop_80c_opt');
    expect(dsopRec).toBeDefined();
    expect(dsopRec.title).toContain('80C');
  });

  it('should handle empty or null history array without crashing', () => {
    const emptyEngine = new AIInsightsEngine([]);
    const summary = emptyEngine.getAnalyticsSummary();

    expect(summary.totalTaxableIncome).toBe(0.0);
    expect(summary.totalTaxPaid).toBe(0.0);
    expect(summary.payslipCount).toBe(0);
    expect(emptyEngine.getRecommendations()).toEqual([]);

    const nullEngine = new AIInsightsEngine(null);
    expect(nullEngine.getAnalyticsSummary().payslipCount).toBe(0);
  });
});
