"use strict";

/**
 * gemmaManifest.js
 *
 * Builds the version-manifest the client checks before deciding whether to
 * (re)download the offline Gemma model, and validates the interim access key.
 * Pure functions — no I/O, fully unit-testable.
 */

const { timingSafeEqual } = require("crypto");

/**
 * The SSOT manifest the client fetches. Versioned filename + sha256 let the
 * client compare against its locally-recorded active version and verify the
 * download before promoting it into the active slot (Phase 1 dual-slot).
 *
 * @param {object} config - gemmaModelConfig
 */
function buildManifest(config) {
  return {
    version: config.MODEL_VERSION,
    url: config.DOWNLOAD_URL,
    sha256: config.EXPECTED_SHA256,
    noticeText: config.GEMMA_NOTICE_TEXT,
    noticeUrl: config.GEMMA_NOTICE_URL,
  };
}

/**
 * Constant-time comparison of the caller-supplied interim key against the
 * expected secret. Fails closed if the secret is unset, and never throws on
 * malformed input.
 *
 * @param {*} provided - Raw header value from the request
 * @param {string|undefined} expected - GEMMA_CACHE_KEY secret value
 * @returns {boolean}
 */
function isValidInterimKey(provided, expected) {
  if (typeof expected !== "string" || expected.length === 0) {
    return false; // fail closed: never authorize when the secret is unconfigured
  }
  if (typeof provided !== "string" || provided.length === 0) {
    return false;
  }
  const a = Buffer.from(provided, "utf8");
  const b = Buffer.from(expected, "utf8");
  if (a.length !== b.length) {
    return false; // timingSafeEqual requires equal length; length itself is not secret
  }
  return timingSafeEqual(a, b);
}

module.exports = { buildManifest, isValidInterimKey };
