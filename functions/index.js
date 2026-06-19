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

// ── Secret Manager binding ───────────────────────────────────────────────────
// The key is injected by Firebase Secret Manager at runtime.
// It is never written to any file, never logged, never in source control.
const GEMINI_API_KEY = defineSecret("GEMINI_API_KEY");

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
