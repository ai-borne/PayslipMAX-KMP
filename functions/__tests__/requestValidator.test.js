"use strict";

const { validateRequest, MAX_BODY_BYTES } = require("../requestValidator");

// ── Fixtures ─────────────────────────────────────────────────────────────────

const VALID_BODY = {
  payslip: { earnings: { grossPay: 120000 }, deductions: { incomeTax: 8000 }, summary: { netRemittance: 88000 } },
  anomalies: [],
  monthlySavingRate: 18.5,
  taxRatio: 10.0,
  healthScore: 72,
};

// ── Tests ─────────────────────────────────────────────────────────────────────

describe("validateRequest", () => {
  test("does not throw for a fully valid body", () => {
    expect(() => validateRequest(VALID_BODY, 100)).not.toThrow();
  });

  test("throws when body exceeds MAX_BODY_BYTES", () => {
    expect(() => validateRequest(VALID_BODY, MAX_BODY_BYTES + 1)).toThrow(
      /exceeds maximum size/,
    );
  });

  test("throws when body is null", () => {
    expect(() => validateRequest(null, 100)).toThrow(/JSON object/);
  });

  test("throws when body is a string instead of object", () => {
    expect(() => validateRequest("hello", 100)).toThrow(/JSON object/);
  });

  test("throws when payslip field is missing", () => {
    const { payslip: _, ...bodyWithoutPayslip } = VALID_BODY;
    expect(() => validateRequest(bodyWithoutPayslip, 100)).toThrow(/payslip/);
  });

  test("throws when payslip is not an object", () => {
    expect(() => validateRequest({ ...VALID_BODY, payslip: "invalid" }, 100)).toThrow(/payslip/);
  });

  test("throws when monthlySavingRate is missing", () => {
    const { monthlySavingRate: _, ...body } = VALID_BODY;
    expect(() => validateRequest(body, 100)).toThrow(/monthlySavingRate/);
  });

  test("throws when taxRatio is missing", () => {
    const { taxRatio: _, ...body } = VALID_BODY;
    expect(() => validateRequest(body, 100)).toThrow(/taxRatio/);
  });

  test("throws when healthScore is missing", () => {
    const { healthScore: _, ...body } = VALID_BODY;
    expect(() => validateRequest(body, 100)).toThrow(/healthScore/);
  });

  test("throws when anomalies is not an array", () => {
    expect(() => validateRequest({ ...VALID_BODY, anomalies: "invalid" }, 100)).toThrow(/anomalies/);
  });

  test("accepts zero as a valid healthScore", () => {
    expect(() => validateRequest({ ...VALID_BODY, healthScore: 0 }, 100)).not.toThrow();
  });

  test("accepts empty anomalies array", () => {
    expect(() => validateRequest({ ...VALID_BODY, anomalies: [] }, 100)).not.toThrow();
  });

  test("accepts body right at the size limit", () => {
    expect(() => validateRequest(VALID_BODY, MAX_BODY_BYTES)).not.toThrow();
  });

  test("MAX_BODY_BYTES is exported and equals 51200", () => {
    expect(MAX_BODY_BYTES).toBe(51_200);
  });
});
