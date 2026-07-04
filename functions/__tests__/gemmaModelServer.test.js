"use strict";

const { resolveModelObjectPath } = require("../gemmaModelServer");
const config = require("../gemmaModelConfig");

describe("resolveModelObjectPath", () => {
  test("resolves the exact configured versioned object path", () => {
    const path = "/models/gemma3-1b-it-int4/v1/gemma3-1b-it-int4.litertlm";
    expect(resolveModelObjectPath(path, config)).toBe(config.OBJECT_PATH);
  });

  test("tolerates a leading double slash from Hosting rewrites", () => {
    const path = "//models/gemma3-1b-it-int4/v1/gemma3-1b-it-int4.litertlm";
    expect(resolveModelObjectPath(path, config)).toBe(config.OBJECT_PATH);
  });

  test("returns null for a path not on the allowlist", () => {
    expect(resolveModelObjectPath("/models/other/v9/x.litertlm", config)).toBeNull();
  });

  test("rejects path-traversal attempts", () => {
    expect(
      resolveModelObjectPath("/models/gemma3-1b-it-int4/v1/../../../secret", config),
    ).toBeNull();
  });

  test("returns null for an empty or root path", () => {
    expect(resolveModelObjectPath("/", config)).toBeNull();
    expect(resolveModelObjectPath("", config)).toBeNull();
  });
});
