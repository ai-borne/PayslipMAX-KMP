# iOS Native Test Coverage Gap

## Problem statement

`shared/src/iosTest/` covers the KMP layer (parser, Gemma, billing) well — 10 test files including
the `ParserUtilsIosPerfTest` regression guard. But `iosApp/iosApp/` (native Swift glue code) is almost
entirely untested: 481 lines across 4 files, only one of which has real coverage.

| File | Lines | Risk | Tested? |
|---|---|---|---|
| `ContentView.swift` | 245 | Nav bridge, edge-swipe-back gate, PDF/backup pickers | No |
| `iOSApp.swift` | 148 | Firebase init, anonymous auth + ID-token retry, telemetry delegates | No |
| `GemmaInferenceBridge.swift` | 88 | LiteRT-LM engine cache/race for Tier 6 inference | No |
| `GemmaOnDemandResourceBridge.swift` | 92 | ODR model fetch + progress observation | Yes (`GemmaOnDemandResourceBridgeProgressTests.swift`) |

`PayslipMaxTests.swift` is the unmodified Xcode template — no real assertions.

This matters because the gap isn't hypothetical: `GemmaOnDemandResourceBridge`'s progress-observation
logic already regressed once in production (commit `b518cb4`, shipped v1.2.1) before a test was
written for it. The same class of bug — a plausible-looking guard that silently breaks a specific
runtime transition — is currently unguarded in the other three files.

## Proposed solutions

Each item: extract the risky logic into a pure/testable unit (matching the
`observeDownloadProgress` pattern already used for the ODR fix), then write an `XCTest` for it.

- [ ] `NavCoordinator.gestureRecognizerShouldBegin` (ContentView.swift) — edge-swipe-back gate;
      verify it blocks when `hasActiveUnsavedSubState()` is true and when stack depth is 1
- [ ] `NavCoordinator.navigationController(_:didShow:)` — verify `onNativePopObserved()` fires only
      when the stack returns to depth 1, not on push
- [ ] `DocumentPickerDelegate` / `BackupPickerDelegate` byte-array marshaling — verify `Data` →
      `KotlinByteArray` round-trips correctly (off-by-one/sign errors in the `Int8`/`UInt8` cast are
      easy to introduce silently)
- [ ] `AppDelegate` `AuthTokenProvider` retry-on-`17999` branch (iOSApp.swift) — verify retry fires
      only on that specific error code, and completion is called exactly once on both success and
      exhausted-retry paths
- [ ] `GemmaInferenceBridge.engine(for:)` cache behavior — verify a second call with the same
      `modelPath` returns the cached engine without re-initializing
- [ ] Replace `PayslipMaxTests.swift` template boilerplate once the above land (delete `testExample`/
      `testPerformanceExample`, keep the file as the home for whichever of the above don't get their
      own file)

## Way ahead

1. Work top-to-bottom — `NavCoordinator` first (highest risk: silently discards unsaved edits, purely
   logic, easiest to isolate from UIKit).
2. Each checked item needs the extraction-for-testability step done first, mirroring
   `GemmaOnDemandResourceBridge.observeDownloadProgress` — don't test through `UIViewControllerRepresentable`
   or `AppDelegate.application(_:didFinishLaunchingWithOptions:)` directly.
3. No CI gate for `PayslipMaxTests` currently exists — once coverage is non-trivial, add
   `xcodebuild test -scheme PayslipMax -destination 'platform=iOS Simulator,name=...'` to
   `scripts/git-pre-push.sh` alongside the existing iOS unit test run.
4. Vendored code (`iosApp/Vendor/LiteRTLM/`) is out of scope — third-party, not maintained here.
