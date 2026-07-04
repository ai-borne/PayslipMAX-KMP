"use strict";

// Mirror authVerification.test.js: mock the firebase layer so index.js's
// onRequest handlers are returned as plain callables. defineSecret returns a
// fixed value ("mock-api-key") for every secret, including GEMMA_CACHE_KEY.
jest.mock("firebase-functions/v2/https", () => ({
  onRequest: (options, handler) => handler,
}));

jest.mock("firebase-functions/params", () => ({
  defineSecret: () => ({ value: () => "mock-api-key" }),
}));

jest.mock("firebase-admin", () => ({
  initializeApp: jest.fn(),
  auth: () => ({ verifyIdToken: jest.fn() }),
}));

const config = require("../gemmaModelConfig");
const { gemmaModelManifest, refreshGemmaModelCache, serveGemmaModel } = require("../index");

function makeRes() {
  return {
    status: jest.fn().mockReturnThis(),
    json: jest.fn().mockReturnThis(),
    set: jest.fn().mockReturnThis(),
  };
}

describe("gemmaModelManifest endpoint", () => {
  test("405 when method is not GET", async () => {
    const res = makeRes();
    await gemmaModelManifest({ method: "POST", headers: {} }, res);
    expect(res.status).toHaveBeenCalledWith(405);
  });

  test("403 when the interim key header is missing", async () => {
    const res = makeRes();
    await gemmaModelManifest({ method: "GET", headers: {} }, res);
    expect(res.status).toHaveBeenCalledWith(403);
    expect(res.json).toHaveBeenCalledWith(
      expect.objectContaining({ success: false, error: expect.stringContaining("access key") }),
    );
  });

  test("403 when the interim key is wrong", async () => {
    const res = makeRes();
    await gemmaModelManifest(
      { method: "GET", headers: { [config.INTERIM_KEY_HEADER]: "wrong-key" } },
      res,
    );
    expect(res.status).toHaveBeenCalledWith(403);
  });

  test("200 with the manifest when the interim key is valid", async () => {
    const res = makeRes();
    await gemmaModelManifest(
      { method: "GET", headers: { [config.INTERIM_KEY_HEADER]: "mock-api-key" } },
      res,
    );
    expect(res.status).toHaveBeenCalledWith(200);
    expect(res.json).toHaveBeenCalledWith({
      success: true,
      manifest: {
        version: config.MODEL_VERSION,
        url: config.DOWNLOAD_URL,
        sha256: config.EXPECTED_SHA256,
        noticeText: config.GEMMA_NOTICE_TEXT,
        noticeUrl: config.GEMMA_NOTICE_URL,
      },
    });
  });
});

describe("refreshGemmaModelCache endpoint", () => {
  test("405 when method is not POST", async () => {
    const res = makeRes();
    await refreshGemmaModelCache({ method: "GET", headers: {} }, res);
    expect(res.status).toHaveBeenCalledWith(405);
  });

  test("403 when the interim key is missing (before any HF/GCS work)", async () => {
    const res = makeRes();
    await refreshGemmaModelCache({ method: "POST", headers: {} }, res);
    expect(res.status).toHaveBeenCalledWith(403);
  });
});

describe("serveGemmaModel endpoint", () => {
  test("405 when method is not GET", async () => {
    const res = makeRes();
    await serveGemmaModel({ method: "POST", headers: {}, path: config.OBJECT_PATH }, res);
    expect(res.status).toHaveBeenCalledWith(405);
  });

  test("404 for a path not on the allowlist (before any GCS work)", async () => {
    const res = makeRes();
    await serveGemmaModel({ method: "GET", headers: {}, path: "/models/evil/../secret" }, res);
    expect(res.status).toHaveBeenCalledWith(404);
  });
});
