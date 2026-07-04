"use strict";

const { createHash } = require("crypto");
const { refreshModelCache } = require("../gemmaCacheRefresh");
const config = require("../gemmaModelConfig");

// ── Helpers ──────────────────────────────────────────────────────────────────

const MODEL_BYTES = Buffer.from("fake-litertlm-model-bytes");
const MODEL_SHA256 = createHash("sha256").update(MODEL_BYTES).digest("hex");

/**
 * Builds a fake @google-cloud/storage client. `exists` controls the cache-hit
 * branch; `save` records what would be uploaded.
 */
function makeStorage({ exists = false } = {}) {
  const save = jest.fn().mockResolvedValue(undefined);
  const file = {
    exists: jest.fn().mockResolvedValue([exists]),
    save,
  };
  const bucket = jest.fn().mockReturnValue({ file: jest.fn().mockReturnValue(file) });
  return { storage: { bucket }, file, save, bucket };
}

function okFetch(bytes = MODEL_BYTES) {
  return jest.fn().mockResolvedValue({
    ok: true,
    status: 200,
    arrayBuffer: () => Promise.resolve(bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength)),
  });
}

const BASE_DEPS = {
  hfToken: "hf_test_token",
  expectedSha256: MODEL_SHA256,
  config,
};

// ── Tests ─────────────────────────────────────────────────────────────────────

describe("refreshModelCache", () => {
  test("cache-hit: object already exists → does NOT fetch or upload", async () => {
    const { storage, save } = makeStorage({ exists: true });
    const fetchImpl = okFetch();

    const result = await refreshModelCache({ ...BASE_DEPS, storage, fetchImpl });

    expect(result.status).toBe("cache-hit");
    expect(fetchImpl).not.toHaveBeenCalled();
    expect(save).not.toHaveBeenCalled();
  });

  test("cache-miss: fetches from HF with the auth token, verifies checksum, uploads", async () => {
    const { storage, save } = makeStorage({ exists: false });
    const fetchImpl = okFetch();

    const result = await refreshModelCache({ ...BASE_DEPS, storage, fetchImpl });

    expect(result.status).toBe("cache-miss-written");
    expect(result.version).toBe(config.MODEL_VERSION);
    expect(result.sha256).toBe(MODEL_SHA256);

    // Fetched the configured HF source URL with a Bearer token
    const [calledUrl, calledOpts] = fetchImpl.mock.calls[0];
    expect(calledUrl).toBe(config.HF_SOURCE_URL);
    expect(calledOpts.headers.Authorization).toBe("Bearer hf_test_token");

    // Uploaded with immutable cache-control metadata
    expect(save).toHaveBeenCalledTimes(1);
    const [, saveOpts] = save.mock.calls[0];
    expect(saveOpts.metadata.cacheControl).toBe(config.IMMUTABLE_CACHE_CONTROL);
  });

  test("HF auth failure (401) → throws, never uploads", async () => {
    const { storage, save } = makeStorage({ exists: false });
    const fetchImpl = jest.fn().mockResolvedValue({ ok: false, status: 401 });

    await expect(
      refreshModelCache({ ...BASE_DEPS, storage, fetchImpl }),
    ).rejects.toThrow(/Hugging Face fetch failed: HTTP 401/);
    expect(save).not.toHaveBeenCalled();
  });

  test("checksum mismatch → throws, never uploads (corrupt/tampered model rejected)", async () => {
    const { storage, save } = makeStorage({ exists: false });
    const fetchImpl = okFetch(Buffer.from("different-bytes"));

    await expect(
      refreshModelCache({ ...BASE_DEPS, storage, fetchImpl }),
    ).rejects.toThrow(/Checksum mismatch/);
    expect(save).not.toHaveBeenCalled();
  });

  test("does not leak the HF token in a fetch-failure error", async () => {
    const { storage } = makeStorage({ exists: false });
    const fetchImpl = jest.fn().mockResolvedValue({ ok: false, status: 403 });

    try {
      await refreshModelCache({ ...BASE_DEPS, storage, fetchImpl });
      throw new Error("expected rejection");
    } catch (err) {
      expect(err.message).not.toContain("hf_test_token");
    }
  });
});
