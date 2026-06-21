"use strict";

/**
 * requestValidator.js
 *
 * Validates the incoming InsightProxyRequest body.
 * Throws a descriptive Error if validation fails so the caller can return 400.
 * Pure function — no external dependencies, fully unit-testable.
 */

const MAX_BODY_BYTES = 51_200; // 50 KB — prevents abuse

/**
 * @param {object|null} body - Parsed JSON body from the HTTP request
 * @param {number} [rawByteLength=0] - Raw Content-Length for size check
 * @throws {Error} Descriptive validation error
 */
function validateRequest(body, rawByteLength = 0) {
  if (rawByteLength > MAX_BODY_BYTES) {
    throw new Error(`Request body exceeds maximum size of ${MAX_BODY_BYTES} bytes.`);
  }

  if (!body || typeof body !== "object") {
    throw new Error("Request body must be a JSON object.");
  }

  if (!body.payslip || typeof body.payslip !== "object") {
    throw new Error("Missing required field: payslip (object).");
  }

  if (typeof body.monthlySavingRate !== "number") {
    throw new Error("Missing required field: monthlySavingRate (number).");
  }

  if (typeof body.taxRatio !== "number") {
    throw new Error("Missing required field: taxRatio (number).");
  }

  if (typeof body.healthScore !== "number") {
    throw new Error("Missing required field: healthScore (number).");
  }

  if (!Array.isArray(body.anomalies)) {
    throw new Error("Missing required field: anomalies (array).");
  }
}

module.exports = { validateRequest, MAX_BODY_BYTES };
