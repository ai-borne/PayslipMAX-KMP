"use strict";

/**
 * gemmaModelConfig.js
 *
 * SSOT for the offline Gemma model distribution pipeline (Phase 0).
 * Every other module (cache-refresh, manifest, model-server) reads its
 * bucket / version / path / notice values from here — never re-declares them.
 *
 * These are non-secret configuration values. Secrets (the Hugging Face token
 * and the interim access key) live in Firebase Secret Manager, never here.
 */

// ── Model identity ───────────────────────────────────────────────────────────
// One model variant across BOTH platforms (Decision 1): the generic CPU/GPU
// int4 build. Same file, same code path — no per-platform fork.
const MODEL_VERSION = "v1";
const MODEL_BASENAME = "gemma3-1b-it-int4.litertlm";

// Versioned, immutable object path inside the private cache bucket. Versioned
// filenames let two versions coexist simultaneously (needed for the Phase 1
// dual-slot A/B rollback) and make each object an ideal immutable CDN entry.
const OBJECT_PATH = `models/gemma3-1b-it-int4/${MODEL_VERSION}/${MODEL_BASENAME}`;

// ── Cache origin (private GCS bucket) ────────────────────────────────────────
// Dedicated private bucket, distinct from the default Firebase Storage bucket.
// Provisioned as an ops step (see Phase 0 handoff); the app never reads GCS
// directly — it goes through the Firebase Hosting URL below.
const BUCKET_NAME = "payslip-app-475e1-gemma-models";

// ── Hugging Face source (fetched ONCE by the cache-refresh function) ──────────
// litert-community mirror of Gemma 3 1B IT in LiteRT-LM format. Requires an
// auth token (stored in Secret Manager) — anonymous access is 401.
const HF_SOURCE_URL =
  `https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/${MODEL_BASENAME}`;

// Known-good SHA-256 of the model object. The cache-refresh function refuses to
// publish any download whose hash does not match this, and the manifest serves
// it to clients so they can verify their own download before promoting a slot.
//
// PLACEHOLDER — must be set to the real published hash before the cache-refresh
// function is run against production. Flagged in the Phase 0 handoff.
const EXPECTED_SHA256 = "REPLACE_WITH_REAL_SHA256_BEFORE_PRODUCTION_REFRESH";

// ── Client-facing distribution URL (Firebase Hosting, edge-cached) ───────────
const HOSTING_BASE_URL = "https://payslip-app-475e1.web.app";
const DOWNLOAD_URL = `${HOSTING_BASE_URL}/${OBJECT_PATH}`;

// Long-lived immutable caching: safe because a version's object never changes
// (a new build gets a new version path). Set on both the GCS object metadata
// and the Hosting-served response so repeat downloads hit the edge, not origin.
const IMMUTABLE_CACHE_CONTROL = "public, max-age=31536000, immutable";

// ── Interim access control (Decision 3) ──────────────────────────────────────
// Header the manifest endpoint validates against the GEMMA_CACHE_KEY secret.
// Explicitly weaker than real Firebase App Check — a documented, temporary
// tradeoff until both apps are enrolled in Play Console / App Store Connect.
const INTERIM_KEY_HEADER = "x-gemma-cache-key";

// ── Gemma Terms of Use notice (license redistribution requirement) ───────────
const GEMMA_NOTICE_TEXT =
  "Gemma is provided under and subject to the Gemma Terms of Use found at " +
  "ai.google.dev/gemma/terms";
const GEMMA_NOTICE_URL = "https://ai.google.dev/gemma/terms";

module.exports = {
  MODEL_VERSION,
  MODEL_BASENAME,
  OBJECT_PATH,
  BUCKET_NAME,
  HF_SOURCE_URL,
  EXPECTED_SHA256,
  HOSTING_BASE_URL,
  DOWNLOAD_URL,
  IMMUTABLE_CACHE_CONTROL,
  INTERIM_KEY_HEADER,
  GEMMA_NOTICE_TEXT,
  GEMMA_NOTICE_URL,
  // Allowlist of object paths the model-server may stream — exact-match only,
  // which structurally prevents path-traversal / arbitrary-object reads.
  // Append future version paths here when they go live.
  ALLOWED_OBJECT_PATHS: [OBJECT_PATH],
};
