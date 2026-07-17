// swift-tools-version: 5.9
//
// Vendored copy of google-ai-edge/LiteRT-LM's Swift wrapper (tag v0.14.0's `swift/` sources), with the
// upstream `unsafeFlags(["-Xlinker", "-all_load"])` linker setting removed. SwiftPM refuses to link any
// package that declares unsafe flags unless it is the root of the build graph — a hard rule with no
// Xcode override — so upstream's remote package reference cannot be consumed directly by this app target.
// The equivalent `-Xlinker -all_load` flag is instead set on the `iosApp` target's own Other Linker Flags
// build setting, which is legal because that target IS the root package here.
//
// Test targets are omitted (not needed for app consumption). Original license preserved in LICENSE.

import PackageDescription

let package = Package(
  name: "LiteRTLM",
  platforms: [
    .iOS(.v15),
    .macOS(.v12),
  ],
  products: [
    .library(
      name: "LiteRTLM",
      targets: ["LiteRTLM"]
    )
  ],
  targets: [
    .binaryTarget(
      name: "CLiteRTLM",
      url: "https://github.com/google-ai-edge/LiteRT-LM/releases/download/v0.13.1/CLiteRTLM.xcframework.zip",
      checksum: "7ff01c42106b754748b5dd3036a4a57161b25ebf523e705bebc1219061852362"
    ),
    .binaryTarget(
      name: "CLiteRTLM_mac",
      url: "https://github.com/google-ai-edge/LiteRT-LM/releases/download/v0.13.1/CLiteRTLM_mac.xcframework.zip",
      checksum: "ec9ffe230dc39117a7fc8933b1cc15910454027fee6d3041534ab7cf17313981"
    ),
    .target(
      name: "LiteRTLM",
      dependencies: [
        .target(name: "CLiteRTLM", condition: .when(platforms: [.iOS])),
        .target(name: "CLiteRTLM_mac", condition: .when(platforms: [.macOS])),
      ],
      path: "swift"
    ),
  ]
)
