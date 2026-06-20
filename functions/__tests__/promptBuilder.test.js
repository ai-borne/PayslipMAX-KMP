"use strict";

const { buildPrompt } = require("../promptBuilder");

// ── Fixtures ─────────────────────────────────────────────────────────────────

const VALID_PAYLOAD = {
  payslip: {
    earnings: {
      basicPay: 80000,
      grossPay: 120000,
      dearnessAllowance: 25000,
      militaryServicePay: 15000,
      transportAllowance: 3600,
      transportAllowanceDa: 1440,
      houseRentAllowance: 0,
    },
    deductions: {
      dsopSubscription: 8000,
      incomeTax: 12000,
    },
    summary: {
      grossPay: 120000,
      netRemittance: 88000,
    },
  },
  anomalies: [
    {
      type: "DEDUCTION_SPIKE",
      description: "Income tax deducted increased by 45% compared to previous month.",
      amount: 4000,
      month: "08/2024",
    },
  ],
  monthlySavingRate: 18.5,
  taxRatio: 10.0,
  healthScore: 72,
  history: [
    { year: 2024, monthNum: 7, netPay: 91000, grossPay: 118000, incomeTax: 8000 },
    { year: 2024, monthNum: 6, netPay: 90000, grossPay: 117500, incomeTax: 8200 },
  ],
};

// ── Tests ─────────────────────────────────────────────────────────────────────

describe("buildPrompt", () => {
  test("returns a non-empty string for valid payload", () => {
    const prompt = buildPrompt(VALID_PAYLOAD);
    expect(typeof prompt).toBe("string");
    expect(prompt.length).toBeGreaterThan(100);
  });

  test("prompt contains gross pay figure", () => {
    const prompt = buildPrompt(VALID_PAYLOAD);
    expect(prompt).toContain("120000");
  });

  test("prompt contains net remittance figure", () => {
    const prompt = buildPrompt(VALID_PAYLOAD);
    expect(prompt).toContain("88000");
  });

  test("prompt contains monthly savings rate", () => {
    const prompt = buildPrompt(VALID_PAYLOAD);
    expect(prompt).toContain("18.5");
  });

  test("prompt contains health score", () => {
    const prompt = buildPrompt(VALID_PAYLOAD);
    expect(prompt).toContain("72/100");
  });

  test("prompt contains anomaly type and description", () => {
    const prompt = buildPrompt(VALID_PAYLOAD);
    expect(prompt).toContain("DEDUCTION_SPIKE");
    expect(prompt).toContain("Income tax deducted increased by 45%");
  });

  test("prompt contains mandatory report sections", () => {
    const prompt = buildPrompt(VALID_PAYLOAD);
    expect(prompt).toContain("salaryChanges");
    expect(prompt).toContain("missingAllowances");
    expect(prompt).toContain("newDeductions");
    expect(prompt).toContain("riskAlerts");
    expect(prompt).toContain("opportunities");
    expect(prompt).toContain("actionRequired");
  });

  test("prompt contains historical context when history is provided", () => {
    const prompt = buildPrompt(VALID_PAYLOAD);
    expect(prompt).toContain("2024-06");
  });

  test("prompt contains YTD Gross and Tax details when taxAndSavings is provided", () => {
    const payload = {
      ...VALID_PAYLOAD,
      payslip: {
        ...VALID_PAYLOAD.payslip,
        taxAndSavings: {
          grossSalaryYtd: 3600000,
          taxDeductedYtd: 600000,
          dsopFund: { closingBalance: 2460000 },
        },
      },
    };
    const prompt = buildPrompt(payload);
    expect(prompt).toContain("YTD Gross: ₹3600000");
    expect(prompt).toContain("YTD Tax: ₹600000");
    expect(prompt).toContain("DSOP Balance: ₹2460000");
  });

  test("prompt contains multiple historical months in the history summary", () => {
    const prompt = buildPrompt(VALID_PAYLOAD);
    expect(prompt).toContain("2024-07");
    expect(prompt).toContain("2024-06");
  });

  test("empty anomalies list produces a valid prompt with no-anomaly message", () => {
    const payload = { ...VALID_PAYLOAD, anomalies: [] };
    const prompt = buildPrompt(payload);
    expect(prompt).toContain("None");
  });

  test("missing history defaults gracefully to no-history message", () => {
    const payload = { ...VALID_PAYLOAD, history: undefined };
    const prompt = buildPrompt(payload);
    expect(prompt).toContain("None");
  });

  test("prompt uses Gemini 2.5 Flash CA-grade framing (senior CA instruction)", () => {
    const prompt = buildPrompt(VALID_PAYLOAD);
    expect(prompt).toContain("Chartered Accountant");
  });

  test("prompt does not mention API key or any secret material", () => {
    const prompt = buildPrompt(VALID_PAYLOAD);
    expect(prompt).not.toMatch(/AIza/i);
    expect(prompt).not.toMatch(/api.?key/i);
    expect(prompt).not.toMatch(/secret/i);
  });

  test("previous month is selected as the last record in history", () => {
    const longHistory = Array.from({ length: 12 }, (_, i) => ({
      year: 2024,
      monthNum: i + 1,
      netPay: 90000 + i * 100,
      grossPay: 118000,
      incomeTax: 8000,
    }));
    const payload = { ...VALID_PAYLOAD, history: longHistory };
    const prompt = buildPrompt(payload);
    // Previous month context should select the last item (month 12)
    expect(prompt).toContain("2024-12");
    expect(prompt).not.toContain("2024-01");
  });
});
