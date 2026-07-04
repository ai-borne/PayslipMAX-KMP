"use strict";

/**
 * index.js — PayslipMax Firebase Cloud Function: generateInsights
 *
 * Security model:
 *  - GEMINI_API_KEY injected at runtime from Firebase Secret Manager (never in code)
 *  - Firebase Auth ID token verified before ANY processing
 *  - API key is NEVER logged, NEVER echoed in any response
 *  - Body size capped at 50 KB to prevent denial-of-wallet attacks
 *
 * Architecture:
 *  index.js → requestValidator.js (validation)
 *           → geminiClient.js     → promptBuilder.js (prompt construction)
 *                                 → Gemini 2.5 Flash REST API
 */

const { onRequest } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const admin = require("firebase-admin");

const { validateRequest } = require("./requestValidator");
const { callGemini } = require("./geminiClient");
const gemmaConfig = require("./gemmaModelConfig");
const { buildManifest, isValidInterimKey } = require("./gemmaManifest");
const { resolveModelObjectPath } = require("./gemmaModelServer");
const { refreshModelCache } = require("./gemmaCacheRefresh");

// ── Secret Manager binding ───────────────────────────────────────────────────
// The key is injected by Firebase Secret Manager at runtime.
// It is never written to any file, never logged, never in source control.
const GEMINI_API_KEY = defineSecret("GEMINI_API_KEY");

// Offline-Gemma pipeline secrets (Phase 0). Both live in Secret Manager, never
// in source: HF_TOKEN authenticates the rare cache-refresh to Hugging Face;
// GEMMA_CACHE_KEY is the interim shared access key checked server-side.
const HF_TOKEN = defineSecret("HF_TOKEN");
const GEMMA_CACHE_KEY = defineSecret("GEMMA_CACHE_KEY");

// Initialise Firebase Admin SDK once (idempotent)
admin.initializeApp();

// ── Helper: verify Firebase Auth ID token ───────────────────────────────────
async function verifyAuthToken(req) {
  const authHeader = req.headers["authorization"] || "";
  if (!authHeader.startsWith("Bearer ")) {
    return { uid: null, error: "Unauthorized: missing or malformed Authorization header." };
  }
  const idToken = authHeader.slice("Bearer ".length);
  try {
    const decoded = await admin.auth().verifyIdToken(idToken);
    return { uid: decoded.uid, error: null };
  } catch {
    return { uid: null, error: "Forbidden: invalid or expired Firebase ID token." };
  }
}

// ── Cloud Function ───────────────────────────────────────────────────────────
exports.generateInsights = onRequest(
  {
    secrets: [GEMINI_API_KEY],
    region: "us-central1",
    timeoutSeconds: 60,
    memory: "256MiB",
    cors: false, // The Android/iOS app talks directly; no browser CORS needed
  },
  async (req, res) => {
    // 1. Only allow POST
    if (req.method !== "POST") {
      return res.status(405).json({ success: false, error: "Method Not Allowed." });
    }

    // 2. Verify Firebase Auth ID token
    const { uid, error: authError } = await verifyAuthToken(req);
    if (!uid) {
      const status = authError?.startsWith("Unauthorized") ? 401 : 403;
      return res.status(status).json({ success: false, error: authError });
    }

    // 3. Validate request body
    const bodyString = typeof req.body === "string" ? req.body : JSON.stringify(req.body || {});
    const rawBytes = Buffer.byteLength(bodyString, "utf8");
    try {
      validateRequest(req.body, rawBytes);
    } catch (validationError) {
      return res.status(400).json({ success: false, error: validationError.message });
    }

    // 4. Call Gemini — API key comes from Secret Manager, never from the request
    try {
      const narrative = await callGemini(req.body, GEMINI_API_KEY.value());
      return res.status(200).json({ success: true, narrative });
    } catch (geminiError) {
      // Log the error server-side but never expose the key in the response
      console.error("[generateInsights] Gemini call failed:", geminiError.message);
      return res.status(502).json({
        success: false,
        error: "AI service temporarily unavailable. Please try again later.",
      });
    }
  },
);

// ── Offline-Gemma model manifest (version SSOT the client checks) ────────────
// GET, guarded by the interim shared-key header (Decision 3). Returns the
// current version/url/sha256 plus the Gemma Terms-of-Use notice.
exports.gemmaModelManifest = onRequest(
  {
    secrets: [GEMMA_CACHE_KEY],
    region: "us-central1",
    timeoutSeconds: 30,
    memory: "256MiB",
    cors: false,
  },
  async (req, res) => {
    if (req.method !== "GET") {
      return res.status(405).json({ success: false, error: "Method Not Allowed." });
    }
    if (!isValidInterimKey(req.headers[gemmaConfig.INTERIM_KEY_HEADER], GEMMA_CACHE_KEY.value())) {
      return res.status(403).json({ success: false, error: "Forbidden: invalid access key." });
    }
    return res.status(200).json({ success: true, manifest: buildManifest(gemmaConfig) });
  },
);

// ── Cache-refresh (admin-invoked, rare — on a new model version only) ────────
// POST, guarded by the same interim key. Fetches the model from Hugging Face,
// verifies its checksum, and publishes it to the private GCS cache bucket.
exports.refreshGemmaModelCache = onRequest(
  {
    secrets: [HF_TOKEN, GEMMA_CACHE_KEY],
    region: "us-central1",
    timeoutSeconds: 540,
    memory: "1GiB",
    cors: false,
  },
  async (req, res) => {
    if (req.method !== "POST") {
      return res.status(405).json({ success: false, error: "Method Not Allowed." });
    }
    if (!isValidInterimKey(req.headers[gemmaConfig.INTERIM_KEY_HEADER], GEMMA_CACHE_KEY.value())) {
      return res.status(403).json({ success: false, error: "Forbidden: invalid access key." });
    }
    try {
      const { Storage } = require("@google-cloud/storage");
      const result = await refreshModelCache({
        storage: new Storage(),
        fetchImpl: fetch,
        hfToken: HF_TOKEN.value(),
        expectedSha256: gemmaConfig.EXPECTED_SHA256,
        config: gemmaConfig,
      });
      return res.status(200).json({ success: true, result });
    } catch (refreshError) {
      // Never echo the HF token or full error to the caller.
      console.error("[refreshGemmaModelCache] failed:", refreshError.message);
      return res.status(502).json({ success: false, error: "Cache refresh failed." });
    }
  },
);

// ── Model file server (Hosting rewrites /models/** here) ─────────────────────
// Streams the immutable versioned object from the private bucket with a
// long-lived Cache-Control so Hosting's CDN edge-caches it: repeat downloads
// across users are served from the edge, not GCS origin.
exports.serveGemmaModel = onRequest(
  {
    region: "us-central1",
    timeoutSeconds: 120,
    memory: "512MiB",
    cors: false,
  },
  async (req, res) => {
    if (req.method !== "GET") {
      return res.status(405).json({ success: false, error: "Method Not Allowed." });
    }
    const objectPath = resolveModelObjectPath(req.path, gemmaConfig);
    if (!objectPath) {
      return res.status(404).json({ success: false, error: "Not Found." });
    }
    const { Storage } = require("@google-cloud/storage");
    const file = new Storage().bucket(gemmaConfig.BUCKET_NAME).file(objectPath);
    const [exists] = await file.exists();
    if (!exists) {
      return res.status(404).json({ success: false, error: "Not Found." });
    }
    res.set("Cache-Control", gemmaConfig.IMMUTABLE_CACHE_CONTROL);
    res.set("Content-Type", "application/octet-stream");
    file
      .createReadStream()
      .on("error", (streamError) => {
        console.error("[serveGemmaModel] stream failed:", streamError.message);
        if (!res.headersSent) {
          res.status(502).json({ success: false, error: "Model stream failed." });
        } else {
          res.destroy(streamError);
        }
      })
      .pipe(res);
  },
);
