"use strict";

const { buildManifest, isValidInterimKey } = require("../gemmaManifest");
const config = require("../gemmaModelConfig");

describe("buildManifest", () => {
  test("returns the SSOT version / url / sha256 / notice shape", () => {
    const manifest = buildManifest(config);
    expect(manifest).toEqual({
      version: config.MODEL_VERSION,
      url: config.DOWNLOAD_URL,
      sha256: config.EXPECTED_SHA256,
      noticeText: config.GEMMA_NOTICE_TEXT,
      noticeUrl: config.GEMMA_NOTICE_URL,
    });
  });

  test("url is under the Firebase Hosting domain and points at the versioned object", () => {
    const manifest = buildManifest(config);
    expect(manifest.url).toBe(
      "https://payslip-app-475e1.web.app/models/gemma3-1b-it-int4/v1/gemma3-1b-it-int4.litertlm",
    );
  });
});

describe("isValidInterimKey", () => {
  test("accepts an exact match", () => {
    expect(isValidInterimKey("s3cret-key", "s3cret-key")).toBe(true);
  });

  test("rejects a wrong key of the same length (constant-time compare still fails)", () => {
    expect(isValidInterimKey("s3cret-key", "s3cret-keZ")).toBe(false);
  });

  test("rejects a key of different length without throwing", () => {
    expect(isValidInterimKey("short", "much-longer-key")).toBe(false);
  });

  test("fails closed when the expected secret is unset (empty / undefined)", () => {
    expect(isValidInterimKey("anything", "")).toBe(false);
    expect(isValidInterimKey("anything", undefined)).toBe(false);
  });

  test("rejects a missing / non-string provided key without throwing", () => {
    expect(isValidInterimKey(undefined, "s3cret-key")).toBe(false);
    expect(isValidInterimKey(null, "s3cret-key")).toBe(false);
    expect(isValidInterimKey(123, "s3cret-key")).toBe(false);
    expect(isValidInterimKey("", "s3cret-key")).toBe(false);
  });
});
