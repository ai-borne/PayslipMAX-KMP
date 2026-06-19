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
async function callGemini(payload, apiKey) {
  const prompt = buildPrompt(payload);
  const url = `${GEMINI_BASE_URL}/${GEMINI_MODEL}:generateContent`;

  const requestBody = {
    contents: [{ role: "user", parts: [{ text: prompt }] }],
    generationConfig: {
      temperature: 0.3,        // Low temperature — financial advice must be deterministic
      maxOutputTokens: 2048,
      topP: 0.8,
    },
    safetySettings: [
      { category: "HARM_CATEGORY_HATE_SPEECH", threshold: "BLOCK_MEDIUM_AND_ABOVE" },
      { category: "HARM_CATEGORY_DANGEROUS_CONTENT", threshold: "BLOCK_MEDIUM_AND_ABOVE" },
    ],
  };

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
