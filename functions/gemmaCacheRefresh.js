"use strict";

/**
 * gemmaCacheRefresh.js
 *
 * The rare, admin-invoked cache-refresh operation: fetch the Gemma model from
 * Hugging Face (authenticated via a Secret Manager token), verify its SHA-256
 * against the known-good value, and publish it to the private GCS cache bucket
 * under its versioned, immutable path.
 *
 * Single responsibility: get the right bytes into the cache, safely. It knows
 * nothing about HTTP handlers, ViewModels, or the manifest. Storage client and
 * fetch are dependency-injected so this is fully unit-testable without network.
 */

const { createHash } = require("crypto");

/**
 * @param {object}   deps
 * @param {object}   deps.storage         - @google-cloud/storage Storage instance
 * @param {Function} deps.fetchImpl       - fetch implementation
 * @param {string}   deps.hfToken         - Hugging Face token (from Secret Manager)
 * @param {string}   deps.expectedSha256  - known-good hash (config.EXPECTED_SHA256)
 * @param {object}   deps.config          - gemmaModelConfig
 * @returns {Promise<{status: string, objectPath: string, version: string, sha256?: string}>}
 * @throws {Error} on HF fetch failure or checksum mismatch — never uploads in those cases
 */
async function refreshModelCache({ storage, fetchImpl, hfToken, expectedSha256, config }) {
  const file = storage.bucket(config.BUCKET_NAME).file(config.OBJECT_PATH);

  // Cache-hit: the immutable versioned object already exists — nothing to do.
  const [exists] = await file.exists();
  if (exists) {
    return {
      status: "cache-hit",
      objectPath: config.OBJECT_PATH,
      version: config.MODEL_VERSION,
    };
  }

  // Cache-miss: fetch the model from Hugging Face with the Bearer token.
  // On failure we surface only the HTTP status — never the token.
  const response = await fetchImpl(config.HF_SOURCE_URL, {
    headers: { Authorization: `Bearer ${hfToken}` },
  });
  if (!response.ok) {
    throw new Error(`Hugging Face fetch failed: HTTP ${response.status}`);
  }

  const buffer = Buffer.from(await response.arrayBuffer());

  // Verify integrity before publishing. A mismatch means corruption or
  // tampering — reject rather than serve bad bytes to thousands of clients.
  const actualSha256 = createHash("sha256").update(buffer).digest("hex");
  if (actualSha256 !== expectedSha256) {
    throw new Error(
      `Checksum mismatch: expected ${expectedSha256}, got ${actualSha256}`,
    );
  }

  await file.save(buffer, {
    resumable: false,
    metadata: {
      contentType: "application/octet-stream",
      cacheControl: config.IMMUTABLE_CACHE_CONTROL,
    },
  });

  return {
    status: "cache-miss-written",
    objectPath: config.OBJECT_PATH,
    version: config.MODEL_VERSION,
    sha256: actualSha256,
  };
}

module.exports = { refreshModelCache };
