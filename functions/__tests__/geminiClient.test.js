"use strict";

const { callGemini } = require("../geminiClient");

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
  anomalies: [],
  monthlySavingRate: 18.5,
  taxRatio: 10.0,
  healthScore: 72,
  history: [],
};

describe("callGemini client", () => {
  let fetchSpy;

  beforeAll(() => {
    if (!global.fetch) {
      global.fetch = jest.fn();
    }
    fetchSpy = jest.spyOn(global, "fetch");
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  test("successfully calls Gemini REST API and returns narrative text", async () => {
    const mockResponseText = "This is the generated narrative report.";
    const mockJsonPromise = Promise.resolve({
      candidates: [
        {
          content: {
            parts: [{ text: mockResponseText }],
          },
        },
      ],
    });
    
    fetchSpy.mockResolvedValue({
      ok: true,
      status: 200,
      json: () => mockJsonPromise,
    });

    const apiKey = "AIzaSyTestKey123";
    const result = await callGemini(VALID_PAYLOAD, apiKey);

    expect(result).toBe(mockResponseText);
    expect(fetchSpy).toHaveBeenCalledTimes(1);
    
    const [calledUrl, calledOptions] = fetchSpy.mock.calls[0];
    expect(calledUrl).toBe("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent");
    expect(calledOptions.method).toBe("POST");
    expect(calledOptions.headers).toEqual({
      "Content-Type": "application/json",
      "x-goog-api-key": apiKey,
    });
    expect(calledOptions.signal).toBeDefined();
    
    const parsedBody = JSON.parse(calledOptions.body);
    expect(parsedBody.contents[0].parts[0].text).toContain("Chartered Accountant");
  });

  test("throws a clean error on non-OK response from Gemini and hides the API key", async () => {
    const errorBody = "Quota exceeded or invalid API key configuration.";
    fetchSpy.mockResolvedValue({
      ok: false,
      status: 403,
      text: () => Promise.resolve(errorBody),
    });

    const apiKey = "AIzaSySecretLeakPreventionTest";
    
    await expect(callGemini(VALID_PAYLOAD, apiKey)).rejects.toThrow(
      "Gemini API error: HTTP 403 — Quota exceeded or invalid API key configuration."
    );

    // Verify key did not leak in error message
    try {
      await callGemini(VALID_PAYLOAD, apiKey);
    } catch (err) {
      expect(err.message).not.toContain(apiKey);
    }
  });

  test("throws a malformed error if Gemini returns empty candidates list", async () => {
    fetchSpy.mockResolvedValue({
      ok: true,
      status: 200,
      json: () => Promise.resolve({ candidates: [] }),
    });

    await expect(callGemini(VALID_PAYLOAD, "mock-key")).rejects.toThrow(
      "Gemini returned an empty or malformed response."
    );
  });

  test("throws a malformed error if text property is missing inside parts list", async () => {
    fetchSpy.mockResolvedValue({
      ok: true,
      status: 200,
      json: () => Promise.resolve({
        candidates: [{ content: { parts: [{}] } }],
      }),
    });

    await expect(callGemini(VALID_PAYLOAD, "mock-key")).rejects.toThrow(
      "Gemini returned an empty or malformed response."
    );
  });

  test("propagates network/timeout abort error if fetch fails", async () => {
    const abortError = new Error("The operation was aborted.");
    abortError.name = "AbortError";
    fetchSpy.mockRejectedValue(abortError);

    await expect(callGemini(VALID_PAYLOAD, "mock-key")).rejects.toThrow(
      "The operation was aborted."
    );
  });
});
