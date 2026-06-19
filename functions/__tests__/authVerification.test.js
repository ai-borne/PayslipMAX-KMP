"use strict";

// Mock firebase-functions first, so it doesn't try to initialize or do anything complex.
jest.mock("firebase-functions/v2/https", () => ({
  onRequest: (options, handler) => handler,
}));

jest.mock("firebase-functions/params", () => ({
  defineSecret: (name) => ({
    value: () => "mock-api-key",
  }),
}));

const mockVerifyIdToken = jest.fn();

jest.mock("firebase-admin", () => ({
  initializeApp: jest.fn(),
  auth: () => ({
    verifyIdToken: mockVerifyIdToken,
  }),
}));

const mockCallGemini = jest.fn();
jest.mock("../geminiClient", () => ({
  callGemini: mockCallGemini,
}));

// Now import index.js which contains generateInsights.
const { generateInsights } = require("../index");

describe("generateInsights auth verification and routing", () => {
  let req;
  let res;

  beforeEach(() => {
    jest.clearAllMocks();
    req = {
      method: "POST",
      headers: {
        authorization: "Bearer valid-token",
        "content-length": "100",
      },
      body: {
        payslip: { earnings: { grossPay: 120000 }, deductions: { incomeTax: 8000 }, summary: { netRemittance: 88000 } },
        anomalies: [],
        monthlySavingRate: 18.5,
        taxRatio: 10.0,
        healthScore: 72,
      },
    };
    res = {
      status: jest.fn().mockReturnThis(),
      json: jest.fn().mockReturnThis(),
    };
  });

  test("returns 405 when method is not POST", async () => {
    req.method = "GET";
    await generateInsights(req, res);
    expect(res.status).toHaveBeenCalledWith(405);
    expect(res.json).toHaveBeenCalledWith(
      expect.objectContaining({ success: false, error: "Method Not Allowed." })
    );
  });

  test("returns 401 when Authorization header is missing", async () => {
    delete req.headers.authorization;
    await generateInsights(req, res);
    expect(res.status).toHaveBeenCalledWith(401);
    expect(res.json).toHaveBeenCalledWith(
      expect.objectContaining({
        success: false,
        error: "Unauthorized: missing or malformed Authorization header.",
      })
    );
  });

  test("returns 401 when Authorization header does not start with Bearer ", async () => {
    req.headers.authorization = "Basic user:pass";
    await generateInsights(req, res);
    expect(res.status).toHaveBeenCalledWith(401);
    expect(res.json).toHaveBeenCalledWith(
      expect.objectContaining({
        success: false,
        error: "Unauthorized: missing or malformed Authorization header.",
      })
    );
  });

  test("returns 403 when token verification fails", async () => {
    mockVerifyIdToken.mockRejectedValue(new Error("Invalid token"));
    await generateInsights(req, res);
    expect(mockVerifyIdToken).toHaveBeenCalledWith("valid-token");
    expect(res.status).toHaveBeenCalledWith(403);
    expect(res.json).toHaveBeenCalledWith(
      expect.objectContaining({
        success: false,
        error: "Forbidden: invalid or expired Firebase ID token.",
      })
    );
  });

  test("returns 400 when request body fails validation", async () => {
    mockVerifyIdToken.mockResolvedValue({ uid: "user-123" });
    delete req.body.payslip; // invalid payload
    await generateInsights(req, res);
    expect(res.status).toHaveBeenCalledWith(400);
    expect(res.json).toHaveBeenCalledWith(
      expect.objectContaining({
        success: false,
        error: expect.stringContaining("payslip"),
      })
    );
  });

  test("calls geminiClient and returns 200 with narrative on success", async () => {
    mockVerifyIdToken.mockResolvedValue({ uid: "user-123" });
    mockCallGemini.mockResolvedValue("Mock AI narrative response.");
    await generateInsights(req, res);
    expect(mockVerifyIdToken).toHaveBeenCalledWith("valid-token");
    expect(mockCallGemini).toHaveBeenCalledWith(req.body, "mock-api-key");
    expect(res.status).toHaveBeenCalledWith(200);
    expect(res.json).toHaveBeenCalledWith({
      success: true,
      narrative: "Mock AI narrative response.",
    });
  });

  test("returns 502 when Gemini call throws an error", async () => {
    mockVerifyIdToken.mockResolvedValue({ uid: "user-123" });
    mockCallGemini.mockRejectedValue(new Error("API Limit reached"));
    await generateInsights(req, res);
    expect(res.status).toHaveBeenCalledWith(502);
    expect(res.json).toHaveBeenCalledWith({
      success: false,
      error: "AI service temporarily unavailable. Please try again later.",
    });
  });
});
