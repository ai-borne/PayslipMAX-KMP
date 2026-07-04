"use strict";

/**
 * gemmaModelServer.js
 *
 * Resolves an incoming Hosting-rewritten request path to a concrete, allowed
 * GCS object path. Exact-allowlist match only, which structurally prevents
 * path-traversal and arbitrary-object reads. Pure function — unit-testable.
 */

/**
 * @param {string} requestPath - req.path, e.g. "/models/.../v1/....litertlm"
 * @param {object} config - gemmaModelConfig (provides ALLOWED_OBJECT_PATHS)
 * @returns {string|null} the object path if allowed, else null
 */
function resolveModelObjectPath(requestPath, config) {
  if (typeof requestPath !== "string" || requestPath.length === 0) {
    return null;
  }
  const clean = requestPath.replace(/^\/+/, ""); // strip leading slash(es)
  return config.ALLOWED_OBJECT_PATHS.includes(clean) ? clean : null;
}

module.exports = { resolveModelObjectPath };
