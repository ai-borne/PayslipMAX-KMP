"use strict";

/**
 * geminiClient.js
 *
 * Calls the Gemini 2.5 Flash REST API.
 * The API key is injected at runtime from Firebase Secret Manager via process.env.
 * Key is NEVER logged, NEVER echoed in any response, NEVER written to any file.
 */

const { buildPrompt } = require("./promptBuilder");

const GEMINI_MODEL = "gemini-2.5-flash";
const GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";

/**
 * Calls Gemini 2.5 Flash and returns the narrative string.
 *
 * @param {object} payload - Validated InsightProxyRequest body
 * @param {string} apiKey - Gemini API key (from Firebase Secret Manager)
 * @returns {Promise<string>} The generated narrative text
 * @throws {Error} On API failure or malformed response
 */
const RESPONSE_SCHEMA = {
  type: "object",
  properties: {
    salaryChanges: {
      type: "array",
      items: {
        type: "object",
        properties: {
          item: { type: "string" },
          change: { type: "string", enum: ["increased", "decreased", "unchanged"] },
          amount: { type: "number" }
        },
        required: ["item", "change", "amount"]
      }
    },
    missingAllowances: {
      type: "array",
      items: { type: "string" }
    },
    newDeductions: {
      type: "array",
      items: {
        type: "object",
        properties: {
          item: { type: "string" },
          change: { type: "string", enum: ["increased", "decreased"] },
          amount: { type: "number" }
        },
        required: ["item", "change", "amount"]
      }
    },
    riskAlerts: {
      type: "array",
      items: {
        type: "object",
        properties: {
          observation: { type: "string" },
          action: { type: "string" }
        },
        required: ["observation", "action"]
      }
    },
    opportunities: {
      type: "array",
      items: {
        type: "object",
        properties: {
          opportunity: { type: "string" },
          action: { type: "string" }
        },
        required: ["opportunity", "action"]
      }
    },
    actionRequired: {
      type: "array",
      items: { type: "string" }
    }
  },
  required: [
    "salaryChanges",
    "missingAllowances",
    "newDeductions",
    "riskAlerts",
    "opportunities",
    "actionRequired"
  ]
};

function buildRequestBody(prompt) {
  return {
    contents: [{ role: "user", parts: [{ text: prompt }] }],
    generationConfig: {
      temperature: 0.1,        // Highly deterministic for structured JSON output
      maxOutputTokens: 8192,
      topP: 0.8,
      responseMimeType: "application/json",
      responseSchema: RESPONSE_SCHEMA
    },
    safetySettings: [
      { category: "HARM_CATEGORY_HATE_SPEECH", threshold: "BLOCK_MEDIUM_AND_ABOVE" },
      { category: "HARM_CATEGORY_DANGEROUS_CONTENT", threshold: "BLOCK_MEDIUM_AND_ABOVE" },
    ],
  };
}

async function callGemini(payload, apiKey) {
  const prompt = buildPrompt(payload);
  const url = `${GEMINI_BASE_URL}/${GEMINI_MODEL}:generateContent`;
  const requestBody = buildRequestBody(prompt);

  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "x-goog-api-key": apiKey,
    },
    body: JSON.stringify(requestBody),
    signal: AbortSignal.timeout(15000),
  });

  if (!response.ok) {
    const errText = await response.text();
    // Never include apiKey in the error — log only the status
    throw new Error(`Gemini API error: HTTP ${response.status} — ${errText.slice(0, 200)}`);
  }

  const data = await response.json();

  const text = data?.candidates?.[0]?.content?.parts?.[0]?.text;
  if (!text) {
    throw new Error("Gemini returned an empty or malformed response.");
  }

  return text.trim();
}

module.exports = { callGemini };
