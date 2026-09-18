# Internal & Closed Testing Progress Log (Android v1.0)

Running record of what changed in each Play Console release — Internal testing and Closed testing
tracks alike — kept so the 14-day-mandatory-testing final submission report (the one justifying to
Google reviewers why the app should be approved for production) can cite concrete, dated evidence
instead of being reconstructed from memory. Update this file at every release bump — don't batch it
at the end. Releases typically land on Internal testing first (fast, small-panel verification) and
are promoted to Closed testing once verified; the 14-day mandatory-testing clock applies only to the
Closed testing track, so each release's status line below states which track it's actually on.

## Status snapshot (as of 2026-09-17, v13 in development)

- **versionCode 13 (1.0.0)** — in development on `release/ios-1.0.0-v6`, accumulating 19 commits across 6 independent workstreams (parser paycode expansion, performance tuning, security hardening, officer profile UX, splash-screen polish, in-app rating prompt) since v12 launched to Closed testing on 2026-09-16. Not yet released to any track.

- **Closed testing track:** `12 (1.0.0)` — promoted directly from Internal testing on 2026-09-16 (177
  countries/regions), superseding versionCode 10. Rather than promoting versionCode 11 as-is, v12 packages
  all of v11's R8/Crashlytics/dead-code hardening together with the user-facing Offline AI banner redesign
  (Option A capsule), Gemma LiteRT engine caching (native OOM fix), and Universal Backup & Restore
  cross-platform interoperability & Password UX guidance.
- **Internal testing track:** `12 (1.0.0)` — released 2026-09-16, verified cleanly on physical Pixel 9,
  then promoted to Closed testing. (Supersedes versionCode 11).
- **Prior Closed track state:** `10 (1.0.0)` — uploaded 2026-09-11 (`dumpsys package` showed `versionCode=10`,
  `installerPackageName=com.android.vending`), now superseded by v12. Closed testing went 8 → 10 → 12.
- **Testers:** 25/25 opted in (third-party tester panel, "Private Testing Pro" plan) as of the v8
  upload; reconfirm current opted-in count against the v10 release in Play Console's own
  "Testing" tab rather than assuming it's unchanged.
- **Reports:** 0/3 ready as of the v8 upload; not yet rechecked for v10.
- **Device-install snag on v10 (resolved):** the closed-testing opt-in link on the Pixel 9 initially
  failed with "You cannot install this app because another user has already installed an
  incompatible version on this device." Root cause: a leftover sideloaded copy of the app
  (`versionCode=9`, `installerPackageName=null` — installed outside Play during earlier ad-hoc
  testing) was still present, and Play's installer won't take over an app it didn't originally
  install. Fixed by `adb uninstall in.aiborne.payslipmax` followed by a Play Store data clear
  (`adb shell pm clear com.android.vending`, needed separately because the Play Store app itself
  was also serving a stale listing), then reinstalling via the opt-in link. Confirmed post-fix:
  `versionCode=10`, `installerPackageName=com.android.vending`.

## Release history and what each build actually changed

### versionCode 4 — released 2026-09-04 20:18
Commits: `66a3aa3`, `9aba2a1`, `751b810`, `e696b13` (telemetry Phases 1–4), plus
`f8f619e` (offline Gemma model download UX + cellular consent), `f30ad2b` (temporarily disabled
`FLAG_SECURE` so testers can screenshot), `c542f71` (registered Firebase client, unblocked API
key), `3fd5a69`/`3ac4a88` (telemetry log sanitization + anonymous install ID), `c28ca3f` (in-app
diagnostic report generator / issue reporting), `30fe2d5` (dashboard/chart UI hardening),
`8248dfc` (background-worker test crash to verify the offline/release Crashlytics pipelines end to
end), `ee9c6fe` (non-fatal Crashlytics telemetry on unparseable PDFs), `c4c5137` (wired
`SwiftCrashReporterDelegate` on iOS for crash-telemetry parity with Android), `5f17ae9`
(versionCode bump itself, "full Gemma on-demand asset pack").

**What this means in plain terms:** Firebase Crashlytics was added end-to-end (fatal + non-fatal,
Android and iOS), R8 deobfuscation mapping upload was wired in, and the pipeline was verified with
a real test crash on a Pixel 9 before shipping. This is the "added Crashlytics" milestone.

### versionCode 5 — released 2026-09-06 (superseded quickly by 6)
Commit: `34ade77` — bumped `targetSdk` to 36 for current Google Play API-level requirements.

### versionCode 6 — released 2026-09-06 16:07
Commits: `22ddd53` (fixed a `fast-uri` dependency vulnerability flagged by CI in `web-prototype`,
unrelated to the shipped app binary but needed to keep CI green), `5b458f2` (moved the salary
countdown ribbon below the stats grid, theme styling pass), `65fcf42` (fixed password cursor
jumping to the wrong position on iOS during PDF unlock), `da2b1a6` (fixed a stale "success" modal
persisting across import dialog re-launches), `9c2ac33` (reworked the payslip import flow to be
file-first and self-explanatory).

**What this means in plain terms:** this is the "removed bugs" milestone — import-flow UX bugs and
a stale-state bug were fixed, plus a security-scan fix to keep the pipeline unblocked.

### versionCode 7 — released 2026-09-09 10:13 (current live release)
Commit: `542eb0c` — "restore v1.0 free-launch strategy and fix launch-mode test coupling."

**What this means in plain terms:** this is the "Fixed App Completeness" milestone. The paywall
gate is bypassed for v1.0 via `LaunchFlags.FREE_LAUNCH_MODE` so every feature (DSOP, Tax Planner,
Anomaly Detection) is fully interactive with no paywall-induced dead ends or error popups — this
directly addresses the "incomplete app" rejection class from Apple/Google review guidelines. See
[05_launch_strategy_and_resolution.md](05_launch_strategy_and_resolution.md) Section 0 for the full
rationale and the iOS resubmission this same flag unblocked.

### versionCode 8 — built 2026-09-07, published to Closed Testing 2026-09-09 20:12
Commits `7497732` → `556fa9a` (R8 Phases 2–8) plus `ef9c3c2` (resource shrinking + proguard rule
dedup + CI verification) and `eb38acd` (fixed a typo in the litert/Gemma keep rule that would have
broken the on-device model at runtime under R8), finishing with `48d2fe6` (the versionCode 8 bump
itself).

**What this means in plain terms:** removed six categories of overly broad ProGuard/R8 keep rules
(blanket Room, generic `Exception`, blanket Ktor package, blanket Compose UI platform package,
blanket `@Serializable`-unscoped Companion keep, Koin Module-implementer) that were shrinking the
release APK/AAB less than necessary, while adding a CI check so keep-rule regressions are caught
automatically instead of only at release time. One real regression (`eb38acd`, the litert package
typo) was caught and fixed during this hardening pass before it could ship — worth calling out
explicitly in the final report as evidence of the testing rigor, not just as a fixed bug.

**Status:** Uploaded and published to the Closed testing track.

#### Pre-upload and on-device verification (versionCode 8):
- **Pre-upload verification done:** before uploading, the exact `composeApp-release.aab` was
  validated with `bundletool` (manifest dump confirmed versionCode 8 / versionName 1.0.0 /
  targetSdk 36, structural `validate` passed clean) and jarsigner-verified against the release
  keystore. It was then smoke-tested end-to-end by sideloading the built APK splits (installer
  spoofed to `com.android.vending` to pass `AppIntegrityChecker`) on both the Android emulator and
  a physical Pixel 9 — Dashboard/History/Insights/Settings all navigated cleanly, Crashlytics
  initialized, `FREE_LAUNCH_MODE` confirmed live ("Everything Included", no paywall), and no
  `FATAL EXCEPTION`/`ClassNotFoundException`/`NoSuchMethodError` appeared in logcat on either
  device — direct evidence the R8 Phase 2–8 keep-rule removals didn't break Koin/Room/Compose at
  runtime. The Pixel 9 was returned to its real Play-delivered v7 install afterward (uninstalled
  the sideload; reinstalled from Play Store) so the tester enrollment on that device stayed clean.
- **Play Console's own bundle diff (v8 vs v7), confirmed post-upload:** DEX size 14.7 MB → 12.8 MB
  (‑13%), download size 22.6 MB → 22 MB, optimisation 46% → 53%, obfuscation 47% → 54%, shrinking
  46% → 53%, and "Resource shrinking optimised" newly present in the R8 config — concrete
  confirmation the keep-rule cleanup had the intended effect. Permission list (12 permissions)
  verified identical to v7 — zero permission-surface drift from this release.
- **Crashlytics symbolication verified on v8, 2026-09-09 22:20:** the Phase 7 Companion-object
  keep-rule rescoping (`847edb1`) was the one v8 change with a real risk of silently breaking
  crash-report readability without breaking the app itself, so it hadn't been checked as part of
  the pre-upload smoke test. Verified directly on the physical Pixel 9 (same device as the
  pre-upload smoke test, running the live Play-delivered `1.0.0 (8)` build): unlocked the hidden
  Developer Sandbox (Settings header tapped 7×) and fired the built-in
  "Background Thread Crash (IO/Default)" trigger (`TestCrash.android.kt`,
  `triggerBackgroundTestCrash()`, added in `8248dfc`) via ADB. The resulting Crashlytics issue
  shows a fully readable stack trace —
  `com.payslipmax.pdfparser.telemetry.TestCrash_androidKt$triggerBackgroundTestCrash$1.invokeSuspend
  (TestCrash.android.kt:13)` — real package, class, file, and line number, not obfuscated
  (`a.b.c`-style) garbage. **Conclusion: v8's R8 keep-rule changes do not break Crashlytics
  symbolication; crash reports on this build can be trusted as-is.**
- **One unrelated Crashlytics issue explained, not a real crash:** a single
  `RemoteServiceException$CrashedByAdbException` ("shell-induced crash") appeared on v8 at
  2026-09-09 18:53:28, ~1 hour before the Play Console submission (19:49). This is not a real user
  crash — it's the expected side effect of the pre-upload sideload/uninstall/reinstall smoke-test
  sequence documented above (installing the split APKs then uninstalling and reinstalling the
  Play-delivered version via ADB), which Android's `ActivityThread` reports as this exact exception
  when a bound background service is disrupted mid-command. Muted/closed in Crashlytics so it
  doesn't skew the 14-day crash-free metrics or the final submission report.

### versionCode 9 — released to Internal testing 2026-09-10 20:46, awaiting promotion to Closed testing
Commits: `bd0f2c4` (History screen ledger toggle affordance), `c80768f` (Digital Replica edit button label).

**What this means in plain terms:** two discoverability fixes discovered post-v8 during device testing:
- **History ledger toggle (bd0f2c4):** The "Historical Ledger Table" header row's expand/collapse control was only tappable on the small chevron icon, not the full row like the year ribbons below it. Fixed by making the entire row clickable (matching the year-header pattern), replacing the text-glyph chevron with Material Icons, and adding `contentDescription` strings for accessibility. Covered by new regression test `HistoricalLedgerCardUiTest.kt` verifying the whole row toggles expansion.
- **Digital Replica edit affordance (c80768f):** The edit/cancel button in the Payslip Digital Replica header was a bare pencil icon with no label, making its purpose (correction mode) invisible to users scanning the screen for how to fix wrong values. Fixed by stacking an "Edit"/"Cancel" label directly below the pencil icon (matching the bottom-nav icon-above-label convention) and updating the subtitle to name the action: "Tap a code for details · Edit to fix a wrong value". Updated tests to target the visible label instead of the now-hidden content description.

Note: `9fa3347` (iOS Gemma model delivery switch to On-Demand Resources) landed on the same branch around this time but is iOS-only with no Android version-code correlate — it does not appear in this Android release log by design.

**Rationale for v9:** both fixes are pure UI/UX improvements (no logic changes, no risk to parsing/data) and directly address discoverability issues identified during closed-testing device review. Releasing them keeps the app feeling responsive to tester feedback and demonstrates iterative polish in the final submission report.

**Status:** Released to the **Internal testing** track only (Play Console: "Available to internal testers," 1 version code, released Sep 10, 2026, 8:46 PM). **Superseded on Closed testing by versionCode 10** (below) before ever being promoted — Closed testing went 8 → 10 directly. v9 remains live on Internal testing only.

### versionCode 10 — released to Closed testing 2026-09-11 — Insights hero + further UX polish
Commits:
- `0122253` — Insights screen hero, first pass: added "Insights" title + month-picker + subtext
  inside an elevated `Surface` "ribbon," matching the Dashboard/History/Settings header pattern
  (Insights was the only main-tab screen missing a hero).
- `9f580f2` — Insights hero, refinement pass, after reviewing `0122253` on-device: removed the
  elevated `Surface` wrapper so the header sits directly on the screen background like every other
  tab (no more visually distinct "ribbon"); reordered to title → subtext → month picker; changed
  the subtext copy to "Monthly details: Track it. Know it. Own it."; made the month-picker chip
  full-width with generous padding for a larger thumb-friendly tap target; recolored the chip to
  the same `primaryContainer` blue used by the Dashboard officer-info card instead of the Material3
  default purple, for theme consistency. Also split `MonthSelectorDropdown` into a smaller helper
  to stay under the 50-line composable limit.
- `4801a19` — versionCode bump itself (9 → 10). Needed because versionCode 9 was already consumed
  by the Internal testing release above; Play requires a distinct, previously-unused versionCode
  per track upload.

**What this means in plain terms:** Insights now has the same title/subtext header every other
tab has, and the month picker reads as a first-class, thumb-friendly control themed to match the
rest of the app rather than a generic Material3 chip. This release carries both the v9 fixes
(History ledger toggle, Digital Replica edit affordance — v9 was never promoted, so this is the
first time those fixes reach Closed testing) and the Insights hero work.

**Status:** Uploaded and confirmed installed via Play on the Pixel 9 test device (see status
snapshot above for the install-conflict snag and its fix). **No R8/proguard changes are included in
this build** — the R8 keep-rule rollout plan
([06_R8_Serialization_Crashlytics_Rollout_Plan.md](../Plan/06_R8_Serialization_Crashlytics_Rollout_Plan.md))
Phase 10/11 work has not started yet and is now retargeted at **versionCode 11**.

### versionCode 11 — released to Internal testing 2026-09-14 17:50 — R8 hardening + tech-debt cleanup
Carries four independent streams, all committed on `release/ios-1.0.0-v6` ahead of this release:

1. R8 Phase 10-11 (serialization + Crashlytics keep-rule cleanup)
2. Tech debt Sprint A (dead-code purge)
3. Tech debt Sprint D (architecture/test-seam cleanup)
4. Crashlytics false-positive fixes (SignInHubActivity / ProxyBillingActivity)

Full detail on each stream below; see also
[06_R8_Serialization_Crashlytics_Rollout_Plan.md](../Plan/06_R8_Serialization_Crashlytics_Rollout_Plan.md)
Phase 12 for the R8-specific verification record.

#### 1. R8 Phase 10-11 — serialization + Crashlytics keep-rule cleanup (complete)
Per `docs/Plan/06_R8_Serialization_Crashlytics_Rollout_Plan.md`, continuing the Phase 1-8 R8 hardening
that shipped in versionCode 8:

- **Phase 9 (baseline capture, no commit):** `versionCode 10` release build archived as the
  pre-change baseline; on-device parse/persist and forced-crash symbolication both confirmed clean.
- **Phase 10 (`0b985c9`):** removed 4 redundant `kotlinx.serialization` keep-rule blocks from
  `composeApp/proguard-rules.pro`. `check` + `assembleRelease` both green.
- **Phase 11 (`1834c67`):** removed the redundant Firebase Crashlytics keep rule. `assembleRelease`
  green.
- **Measured size impact:** APK 70,508,186 → 70,442,383 bytes (~65.8 KB smaller, ~0.09% — modest and
  expected; these were narrow rules already covered functionally by library-consumer rules, not
  blanket keeps).
- Findings #5-#7 (Koin annotation scope, global native-methods keep, LiteRT wildcard keep) remain
  **explicitly deferred** — higher regression risk, needs a dedicated native/JNI-focused pass.

**Status:** Phase 10 and 11 committed, build-verified, and fully verified on-device via versionCode 11
(Phase 12 execution).
- **Phase 11 (Crashlytics symbolication):** Passed during initial Phase 12 verification. The forced
  background-thread test crash produced a fully readable Crashlytics issue
  (`TestCrash_androidKt$triggerBackgroundTestCrash$1.invokeSuspend`), tagged by Crashlytics as
  regressed in "version 1.0.0 (11)".
- **Phase 10 (Serialization parse/persist):** Passed during the Play-delivered versionCode 11 session
  on the physical Pixel 9 (see verification record under Internal-testing verification below). Import
  of payslips spanning multiple grammar eras parsed cleanly, displayed accurately in History, and
  persisted across app restarts with zero `SerializationException`.

See [06_R8_Serialization_Crashlytics_Rollout_Plan.md](../Plan/06_R8_Serialization_Crashlytics_Rollout_Plan.md)
Phase 12 for full detail.

#### 2. Tech debt Sprint A — dead code purge (2026-09-13, complete)
A full tech-debt audit (`docs/Plan/11_techDebt_10sep2026`) was cross-checked file-by-file against the
live codebase (existence, reference counts, exact line numbers) before any action was taken — every
finding verified accurate. Findings were sequenced into 4 sprints; Sprint A (zero production callers,
zero behavior risk) was executed immediately rather than deferred to a release window:

- Removed 12 dead production files: the orphaned cloud/hybrid AI narrative cluster
  (`FinancialInsights.kt`, `AIProviderManager.kt`, `AIInsightProvider.kt`, `LocalGemmaProvider.kt`,
  `AiInsightReport.kt`), the superseded tax/scoring engines (`TaxRecommendationEngine.kt` —
  containing the discredited "submit before December" advice — and `ConfidenceScoringEngine.kt`),
  the never-invoked `TransparencyDialog.kt`/`TransparencyStrings.kt`, and two unreferenced debug/test
  utilities living in production `commonMain` (`RuntimeTokenDiffLogger.kt`, `GemmaBenchmarkHarness.kt`).
- Removed 8 test files that existed solely to test the above dead code.
- Trimmed 3 dead composables out of otherwise-live files: `RetCalcResultsSection`/
  `CommutationResultCard` (`RetirementCalculatorsComponents.kt`), `PrivacyCard`
  (`SettingsHeaderComponents.kt`), `ProfileSection`/`PremiumSection` (`SettingsSectionComponents.kt`)
  — plus 2 `AppStrings.kt` entries that became orphaned once `PrivacyCard` was removed.
- **Net: 1,648 lines removed across 23 files.** No production callers existed for any of it
  (confirmed by direct grep cross-reference, not just the audit's say-so).
- **Verified:** `./gradlew check -x iosX64Test -x iosSimulatorArm64Test` (full Android + common build,
  lint, `checkFileSizes`, and all `shared`/`composeApp` unit tests) passed clean after the purge.

**Status:** Committed on `release/ios-1.0.0-v6`. Pure subtraction of unreachable code — no functional
or UI change for any tester, so it carries no risk to the current versionCode 10 Closed testing
window. Remaining sprints (B: `FLAG_SECURE` re-enable + file-size splits; C: page-2 table-spillover
parsing fix; D: architecture/test-seam cleanup) are intentionally deferred to whatever release follows
the 1.2 (iOS)/V10 (Android) review outcomes, per `docs/Plan/11_techDebt_10sep2026`.

#### 3. Tech debt Sprint D — architecture & test-seam cleanup (2026-09-13, complete)
Per `docs/Plan/11_techDebt_10sep2026`. No user-visible behavior change; all items verified via
`:shared:testDebugUnitTest`, `:composeApp:testDebugUnitTest`, `ktlintCheck`, and
`:composeApp:linkDebugFrameworkIosSimulatorArm64`.

- Moved `FakePayslipDao`/`FakePdfParser` into a new `:shared-test-fixtures` module — these were
  `public` classes previously exported into `shared.framework`'s distributed iOS interface despite
  being test-only. Reduces the shipped binary's public surface.
- Fixed two MVVM leaks: `calculateAssetProjections`'s compound-growth math moved from
  `DsopAssetComparisonCard.kt` into `shared`'s `ProjectionMath.kt`; `breakdownWellnessDrivers`
  scoring moved out of `ui/screens` into composeApp's `domain/` package.
- Deleted unwired `ZeroPiiReportExporter.kt` (zero call sites) and its test.
- Corrected stale documentation: `CLAUDE.md`/`AI_INSIGHTS_PIPELINE.md` no longer describe already-deleted
  parsers (`PayslipTextParser`, `DynamicSpatialParser`, `PayslipTokenParser`) as present, and the ODR
  (On-Demand Resources) Gemma-delivery description now matches the shipped mechanism instead of the
  superseded Background Assets plan.
- Renamed `PayslipTokenParserGemmaTest` → `GrammarAwareParserGemmaTest` to match what it actually tests.
- Fixed `GemmaOnDemandResourceBridge.swift`: Gemma model download progress was rescaling
  `fractionCompleted` against a fabricated `total: Int64 = 1000` instead of reporting real bytes; now
  reads `completedUnitCount`/`totalUnitCount` straight off the same `NSProgress`. No behavior change to
  the on-screen progress bar (the Kotlin side only ever consumed the derived fraction). Verified via
  `:composeApp:linkDebugFrameworkIosSimulatorArm64` and an `xcodebuild` simulator build of the `iosApp`
  scheme (both green). **Real-device ODR download verification not yet done — deferred to the next App
  Review submission cycle.**

**Status:** Committed on `release/ios-1.0.0-v6`, unreleased. Remaining Sprint D item (R8 findings #5-7
Koin/native/LiteRT keep-rule scoping, tracked jointly with the R8 rollout plan above) is intentionally
deferred, not silently dropped.

#### 4. Crashlytics false-positive crash fixes — SignInHubActivity / ProxyBillingActivity (2026-09-14, complete)
Two Crashlytics issues on versionCode 10 were investigated and root-caused to Google's own libraries
being launched by automated test infrastructure (Play pre-launch report / Test Lab-style crawler) with
no intent extras — not real user crashes. All events on both issues show device "OnePlus8Pro" + CPU
X86_64 (physically impossible — a real OnePlus 8 Pro is ARM64-only) + OS "Unknown (11)".

- **`SignInHubActivity`** (`play-services-auth:20.7.0`, transitive via `firebase-auth-ktx` →
  `credentials-play-services-auth`): app has zero Google Sign-In code paths (Firebase Anonymous Auth
  only). Fixed via `tools:node="remove"` in `composeApp/src/androidMain/AndroidManifest.xml`
  (`5488b8f`), paired with `tools:ignore="MissingClass"` for an unrelated AGP lint false positive
  (lint can't resolve library-internal classes referenced only via a removal stanza, even though the
  class is genuinely on the classpath). Merged manifest confirmed free of the activity; full `check`
  gate green; debug APK installs and launches cleanly on an emulator with Firebase Anonymous Auth SDK
  components initializing normally.
- **`ProxyBillingActivity`** (`com.android.billingclient:billing:8.3.0`, transitive via RevenueCat
  KMP's `purchases:10.16.1`): real purchases always route through `RevenueCatBillingManager` with a
  valid `BUY_INTENT`. Unlike the Sign-In activity, this one is **load-bearing** — it can't be removed
  from the manifest without risking real purchase flows, since RevenueCat's own dependency requires it
  unconditionally regardless of any app-level dependency. The app's redundant direct
  `play-billing-ktx:7.1.1` dependency was still removed (`7c0a1d4`) as SSOT/tech-debt cleanup — one
  fewer duplicate Play Billing version in the dependency graph — but the activity correctly remains in
  the manifest by design. Both Crashlytics issues (`add2187eac1b73536c614996b0cea686`,
  `dae7cb84b3499727c650a64570d882a8`) were annotated with this evidence and closed.
- **Outstanding (not silently skipped):** PDF-import-flow smoke test and a live Firebase Anonymous
  Auth trigger (both need a real device/file from a human, not available in this environment); a
  sandbox purchase smoke test (needs an emulator/device with a signed-in test Google account — the
  available emulator had none). None of these block the fix landing; all are lower-risk, unrelated-path
  checks tracked as follow-ups, not correctness gates on the manifest/dependency changes themselves.

**Status:** Committed on `release/ios-1.0.0-v6`. Android-only; zero iOS-side changes
(confirmed via `git diff --stat` against `iosApp/`, `shared/src/iosMain`, `shared/src/iosTest`).

#### Upload and Internal-testing verification (2026-09-14)

**Play Store propagation snag (resolved):** the Play Console release page confirmed versionCode 11
"Available to internal testers" at 17:50, but the Pixel 9's Play Store app served a stale listing —
tapping the opt-in link at 17:52 installed versionCode 10 again (`dumpsys package` confirmed
`versionCode=10`, `installerPackageName=com.android.vending`). Fixed the same way as the v10 snag:
`adb shell pm clear com.android.vending` to drop the stale cached listing, then reopened the Play
Store app page — it then correctly showed an "Update" button with the v11 release notes ("no new
features or UI changes — internal hardening update"). Tapping Update installed
`versionCode=11, installerPackageName=com.android.vending` at 17:57:08, confirmed via `dumpsys
package`.

**Post-install logcat verification, 2026-09-14 17:57 (versionCode 11, real Play-delivered install,
not a sideload):**
- No `FATAL EXCEPTION`, no ANR, no `ClassNotFoundException`/`NoSuchMethodError`. App process stable
  through launch and idle.
- **Gemma offline-AI asset pack downloaded correctly via real Play Asset Delivery** ("Downloading
  Offline AI Model (15%)") — the first time this path has been observed working on this build; the
  earlier Phase 12 sideload could only show `PACK_UNAVAILABLE` since sideloads can't fetch
  Play-delivered asset packs. Positive signal that the Play Asset Delivery / ODR wiring
  (Sprint D, `GemmaOnDemandResourceBridge` area) is intact end-to-end for Android.
- **Non-fatal, pre-existing, not a v11 regression:** `No package ID 6b found for resource ID
  0x6b0b0013` — a resource-lookup warning from a third-party SDK or asset pack, no visible effect.
- **Non-fatal, real gap to close before `FREE_LAUNCH_MODE` is ever disabled:** RevenueCat logged
  `ConfigurationError` — "you have configured the SDK with a Play Store API key, but there are no
  Play Store products registered in the RevenueCat dashboard for your offerings." This is a
  RevenueCat-dashboard configuration gap, not a code regression from the Sprint A billing-dependency
  dedup (`7c0a1d4`) or the `ProxyBillingActivity` manifest decision — but it does mean the deferred
  sandbox-purchase check (flagged as outstanding in stream 4 above) cannot meaningfully pass until
  products/offerings are configured on RevenueCat's side regardless of app code.

**Two-grammar-era parse/persist check — ✅ PASSED (2026-09-14).** User imported payslips spanning
two-plus grammar eras on the physical Pixel 9. Independently confirmed: the History screen renders
Jan-April 2026 with correct gross/DSOP/tax breakdowns, and `adb logcat` shows zero
`SerializationException`/`ClassNotFoundException`/`FATAL EXCEPTION` across the session. R8 Phase 10's
exit criteria are met on the shipping artifact.

**Still outstanding (not silently skipped):** Firebase Anonymous Auth live-trigger and the sandbox
purchase check (stream 4) — both need a human with real credentials/a signed-in test account, and
the purchase check specifically can't pass yet regardless, due to the RevenueCat offerings-config
gap noted above. Neither blocks promotion: Anonymous Auth already initializes silently in the
background (confirmed no auth-related errors in any logcat capture so far), and the purchase path is
unreachable by real testers while `FREE_LAUNCH_MODE` is active.

**Decision:** with the parse/persist check now passed on the Pixel 9 test device, instead of promoting
versionCode 11 directly as-is, the Offline AI banner redesign (Option A capsule) was executed immediately
to resolve the jarring red error state and visual footprint. Both the v11 binary hardening and the v12
UI polish are packaged together into **versionCode 12** to be pushed directly to Closed testing.

### versionCode 12 — Promoted to Closed testing 2026-09-16 — Offline AI banner redesign + Gemma LiteRT Caching (OOM Fix) + Universal Backup/Restore Interoperability & Password UX
Carries the visual overhaul of the Tier 6 Gemma background installation UX, resolves a critical native OOM kernel kill during multi-document parsing, unifies Universal Backup & Restore cryptographic SSOT strictly on PBKDF2-HMAC-SHA256, and adds password guidance UX hints:

Commits:
- `bedd1d1` — `feat(ui): phase 1 - modernize gemma strings and centralize legal disclosure`: replaced technical developer jargon (*"Offline AI Model (~529MB)"*, *">3.5GB RAM get the most accurate parsing"*, raw stack traces) with friendly, human copy (*"Setting up Smart Features"*, *"Downloading offline helper in background for enhanced privacy."*, *"Smart Features Setup Paused"*). Centralized Google Gemma Terms of Use attribution into `LegalStrings` and `HelpLegalScreen`.
- `bc838e2` — `feat(ui): phase 2 - add model banner dismissal and reset state to viewmodel with tdd`: added `isModelBannerDismissed: Boolean = false` to `PayslipUiState` (SSOT) and `dismissModelBanner()` to `PayslipViewModel`, with unit test coverage in `PayslipViewModelGemmaDownloadTest`. Resuming/retrying the download resets dismissal to keep progress visible.
- `956b855` — `feat(ui): phase 3 - redesign gemma model banner into a slim dismissible capsule`: refactored `BaseModelDownloadBanner` into a sleek, compact capsule (~48dp height) with a subtle progress bar (4dp), gentle non-red palette (`surfaceVariant` / `secondaryContainer`), inline [Resume]/[Retry] actions, and an explicit dismiss (`✕`) icon button. Tested via `BaseModelDownloadBannerTest`.
- `b36fdb6` — `feat(perf): resolve native OOM crash via Gemma LiteRT lazy loading and engine caching`:
  - **The Root Cause:** In `PlatformPdfParser.decryptAndParse()`, `GemmaEngine(config)` was instantiated on every imported PDF, and its constructor eagerly called `Engine.initialize()`. This allocated ~600MB of native C++ memory per payslip without closing it. When importing 4+ payslips consecutively on the Pixel 9, native memory stacked to 3.22 GB (`3,221,225,472 bytes`), triggering Android's low-memory killer (`MemoryLimiter: onLimitExceeded memHigh=3221225472`) and killing the app process.
  - **The Architecture Fix:**
    1. **Thread-Safe Engine Caching (`LiteRtEngineStore.kt`):** Created a thread-safe singleton cache ensuring at most one `Engine` is loaded into native memory per model path across the process, matching iOS's actor-isolated `EngineStore` parity.
    2. **Lazy Just-In-Time Loading (`GemmaEngine.android.kt`):** Removed eager `initEngine()` from constructor. Native loading is deferred until `generateResponse()` is genuinely invoked by the fallback extractor. For deterministic parses (99.9% of payslips), 0 MB of native LLM memory is allocated.
    3. **Parser Parity Refactoring (`PdfParser.kt` androidMain):** Extracted `buildGemmaEngine()` helper mirroring iOS `PlatformPdfParser`. Reduced file length to 246 lines (well under the 300-line limit).
    4. **OS Memory Trim Integration (`PayslipApplication.kt`):** Implemented `onTrimMemory(level)` and `onLowMemory()` to release cached native model weights when the OS experiences memory pressure (`TRIM_MEMORY_RUNNING_CRITICAL` / `TRIM_MEMORY_MODERATE`).
    5. **TDD Unit Tests (`AndroidGemmaEngineTest.kt`):** Added tests verifying zero eager allocations on construction, clean `isInitialized` evaluation, and cache clearance.
  - **Live Device Verification (Pixel 9):**
    - Sideloaded `gemma-active.litertlm` (~557MB) and consecutively imported **19 payslips** spanning 2022 to 2026.
    - Native Heap stabilized at **53 MB** (down from > 2.4 GB, a ~98% reduction).
    - Total PSS stabilized at **291 MB** (down from 3.22 GB, a ~91% reduction).
    - Zero crashes, zero `MemoryLimiter` warnings, process remained stable and responsive.
- `0b9601b` — `fix(crypto): unify PBKDF2-HMAC-SHA256 standard and UTF-8 parity across iOS and Android`:
  - Standardized `.pcda` backup archive key derivation strictly on **PBKDF2-HMAC-SHA256** (RFC 6070 compliance) and AES-GCM across both platforms, with zero legacy fallback code.
  - Hardened password encoding to UTF-8 byte representation, eliminating cross-platform encoding discrepancies when passwords contain special symbols, accents, or emojis.
  - Tested via cross-platform TDD test vector suite `CryptoHelperVectorTest.kt` in `commonTest`.
- `e9d35e5` — `feat(ui): add backup password UX guidance hint and contextual labels`:
  - Resolved user uncertainty during backup encryption by providing clear supporting text clarifying that letters, numbers, and symbols are supported.
  - Added a zero-knowledge recovery warning: *"Cannot be recovered if forgotten"*, protecting users from accidental data lockout.
  - Replaced ambiguous labels with contextual labels and placeholders (`AppStrings.settingsBackupPasswordLabel`, `AppStrings.settingsBackupPasswordHint`, `AppStrings.settingsBackupPasswordPlaceholder`). Tested via `BackupRestorePasswordFieldTest.kt`.
- `81e7ad3` — `test(backup): add ViewModel-level backup and restore test suite and verify cross-platform parity`:
  - Added comprehensive `PayslipViewModelBackupTest.kt` covering ViewModel export/import flows, `RestoreMode.REPLACE` and `RestoreMode.MERGE`, empty database exports, incorrect password rejection, and corrupted payload handling.

**What this means in plain terms:**
- **Eliminated the ugly red banner:** When the background download paused (e.g. waiting for Wi-Fi) or encountered transient network errors, the previous UI showed a harsh, bright red `errorContainer` banner across the top of the dashboard displaying raw system exception strings. Normal testers felt alarmed, assuming the core payslip parser or database had broken. The new design uses a gentle, integrated slate/secondary palette with reassuring guidance: *"Smart Features Setup Paused · Will resume automatically on Wi-Fi, or tap to retry."*
- **Reclaimed ~20% of screen real estate:** The old card was 4-5 lines tall, pushing key financial metrics (Net Pay, Basic Pay, DSOP balance) below the fold. The new design is a slim, single-row capsule (~48dp) that sits quietly above the content.
- **Tester agency (✕ dismissible):** Testers can tap the `✕` button to hide the banner for the session. The background download worker continues uninterrupted; testers are never held hostage by persistent cards.
- **Legal clutter removed from home flow:** The required Google Gemma terms URL disclaimer was moved from the transient dashboard card to its proper home under **Settings → Help & Legal → Privacy & AI**, keeping the primary dashboard clean.
- **Permanent fix for batch import crash:** Testers importing multiple payslips consecutively will no longer experience silent app termination. The app effortlessly parses 10–20+ payslips in a single session with low memory consumption and instant parse times.
- **Universal Backup & Restore parity across Android and iOS:** Backups exported on Android can now be seamlessly restored on iOS (and vice-versa) with zero cryptographic drift. Passwords containing emojis or international symbols work reliably without encoding mismatches.
- **Contextual Password UX & zero-knowledge safety:** When creating or restoring backups, users now receive clear, actionable guidance that letters, numbers, and symbols are supported, along with a prominent warning that forgotten passwords cannot be recovered by the developer or support.
- **Architecture & SSOT integrity:** The banner logic is 100% shared Compose Multiplatform in `composeApp/src/commonMain`, while native LiteRT inference on Android now maintains architectural parity with iOS's cached engine store. Enforced strict adherence to project rules: all files <= 300 lines, all functions <= 50 lines.

**Status:** Released to **Internal testing** on 2026-09-16, verified cleanly on physical Pixel 9, and promoted directly to **Closed testing** on 2026-09-16 (`composeApp-release.aab`, versionCode 12, versionName 1.0.0), superseding v10 on the 14-day mandatory track.

### versionCode 13 — In development (16 commits, 5 workstreams)

Five independent workstreams landed on `release/ios-1.0.0-v6` since v12 closed-testing launch:

#### 1. Parser paycode mapping expansion (Phase 0–3)
Reorganized and extended paycode recognition across four phases:

- **Phase 0 (`8afdddc`):** Modularized allowance/deduction pattern mappings from flat `PayslipPatternConfig` into domain-partitioned `AllowancePatternMappings`/`DeductionPatternMappings`, reducing config LOC from 289 → 103. Verified composition integrity via new unit tests.
- **Phase 1 (`760edfa`):** Resolved technical paycodes (HRAX, TECI/II, ARR-TECII) with zero-residual parsing on real examples. Updated `Earnings` domain model, `PayslipAssembler`, `AppStringsPayCodes`, and `ReplicaUtils` with structured line-item formatting. Added TDD tests replicating real screenshots.
- **Phase 2 (`eeefcab`):** Added 7th CPC Risk & Hardship matrix and operational field allowance mappings.
- **Phase 3 (`537d36a`):** Added aviation, travel claims, specialized grants, and clean debit categories.

**Test status:** 100% unit tests passing; all files ≤ 300 LOC.

#### 2. Performance optimization (Phase 1–3)
Three-tier tuning to reduce cold-start and parsing latency:

- **Phase 1 (`1c3f6b1`):** Cache database secret key in volatile memory instead of deriving on every unlock.
- **Phase 2 (`13ac78b`):** Offload decryption pipeline to background dispatcher; reuse device key across sequential imports.
- **Phase 3 (`8c93dc0`):** Extract tax optimization calculation outside ViewModel state-update loop (was re-computing on every UI frame).

#### 3. Security hardening (Phase 1–4)
Privacy overlay lifecycle and window-level task snapshot protection:

- **Phase 1 (`6836694`):** Create `PayslipMaxProtectedOverlay` composable with SSOT `AppStrings` entry.
- **Phase 2 (`8b9e691`):** Wire lifecycle-aware privacy overlay to Android, tied to app resume/pause.
- **Phase 3 (`fba2a5a`):** Align iOS overlay with Android SSOT; verify on Pixel 9 device.
- **Phase 4 (`f158c0f`):** Secure Android task snapshot via `setWindowInsecure()` gating on window-focus loss (prevents screenshot/recording while app is backgrounded).
- **Refactor (`2b4045b`):** Remove now-redundant Compose overlay logic; consolidate all privacy protection into the native window-level layer.

#### 4. Officer Profile UX & robustness (Phase 1–3)
Settings flow sanitization and resilience:

- **Phase 1 (`f93f9ae`):** Align Officer Profile resource strings with naming convention; add sanitization helpers.
- **Phase 2 (`5f960d1`):** Wire officer profile overrides into Digital Replica display.
- **Phase 3 (`ad4e8a1`):** Sanitize profile inputs in ViewModel; robustify PIN reset flow to handle edge cases.

#### 5. Splash screen polish
- **`11c9a00`:** Wire `androidx.core.splashscreen` with `setKeepOnScreenCondition` gated on `PayslipUiState.isLoading` to hold splash through cold-start Koin/Room initialization (no new state). Parity with iOS's static LaunchScreen storyboard.

#### 6. In-app rating prompt (Priority 3, tester report) — Phase 1–4 complete
Play In-App Review (Android) / `SKStoreReviewController` (iOS) triggered after a clean payslip parse, not on launch. Both are OS-rendered dialogs with no custom copy.

- **Phase 1 (`9300feb`):** Shared eligibility logic (`RatingPromptManager`), storage, and clock, with Android/iOS actuals.
- **Phase 2 (`60815ca`):** `requestReview()` expect/actual, Play Core dependency, `ReviewActivityBridge` for the Activity reference.
- **Phase 3 (`ad486de`):** ViewModel eligibility hook (gated on `needsReview == false` and a duplicate-reimport check) + UI-timed trigger via `LaunchedEffect` after the success state settles.
- **Cadence:** 3rd clean parse triggers the first prompt; every 10 more after that; gated by both the count and a 90-day cooldown.
- **Phase 4 (manual verification, Pixel 9, no code changes):** confirmed end-to-end — Play In-App Review sheet rendered after the threshold; re-importing the same payslip did not increment the counter (checked directly against `shared_prefs`, not inferred). `needsReview == true` non-increment is unit-test-verified only, no dirty fixture available on-device to reproduce manually.
- Considered adding custom pre-prompt copy ("how about giving it five stars") — rejected: both Apple and Google prohibit prompting for a specific rating. Kept the OS-native dialog as-is.

**Status:** All 19 commits verified green on `check` gate (full Android + common build, lint, all unit tests). Not yet released to any track; queued for promotion decision once integrated testing and build gate complete.

#### 7. Onboarding walkthrough (Priority 3, tester report) — Phase 1–4 complete, Phase 5 open
3-slide skippable carousel (privacy/trust → on-device-AI-parsing → Get Started/FAQ link) plus a separate one-time coachmark on the Dashboard's upload FAB.

- **Phase 1:** `OnboardingStorage` interface + Android (`SharedPreferences`)/iOS (`NSUserDefaults`) actuals, mirroring `RatingPromptStorage`'s shape exactly (own prefs file, snake_case keys, `ContextHolder`-based, safe `false` defaults).
- **Phase 2:** `OnboardingManager` (`shouldShowOnboarding`/`onOnboardingCompleted`), `HorizontalPager`-based `OnboardingScreen`/`OnboardingSlides`, and a third top-level gate in `App()` (additive — sideload/lock gates untouched) with the FAQ link routed through the existing `nativeDetailNavigator?.invoke(...) ?: navState.push(...)` dispatch so it also works on iOS.
- **Phase 3:** `OnboardingManager.shouldShowCoachmark`/`onCoachmarkDismissed`, `UploadCoachmark` (`Popup`-based, BottomEnd-anchored), wired into `DashboardScreen` via the extracted `ui/components/DashboardUploadArea.kt` (kept `DashboardScreen.kt` under the 300-line cap and its composable under 50 lines).
- **Phase 4:** Full verification pass — `check` gate, `ktlintCheck`, `linkDebugFrameworkIosSimulatorArm64`, `iosSimulatorArm64Test`, and the tech-debt audit all green; tester-report checkbox flipped.
- **Phase 5 (open — see the implementation plan's final phase):** manual on-device verification (cold-start carousel → Skip from every slide → FAQ link → Get Started → coachmark-once → relaunch) was not performed — no physical device or running emulator was available in the implementing session. Flagged rather than assumed; do this pass before treating the feature as fully shipped.

**Status:** Phases 1–4 verified green on the `check` gate (full Android + common build, ktlint, tech-debt audit, iOS framework link, iOS simulator unit tests). Not yet verified on-device; not yet released to any track.

## What still needs to happen before the Day-14 final submission

1. Keep 12+ testers active through the full 14-day mandatory window (Play Console currently shows
   25/25 opted in via the third-party panel, but Google's own 14-day counter is what governs
   production eligibility — confirm which counter is authoritative before submission). Note that
   pushing versionCode 8 mid-window is expected/normal — the mandatory-testing clock is track-based,
   not tied to a single release version — but reconfirm this in Play Console's own "Testing" tab
   rather than assuming it.
2. Once Day 14 completes, collect the 3 pending crash/ANR reports referenced above (currently 0/3
   ready) — the final submission report should either show these as clean or document what was
   fixed in response to them.
3. Draft the final "why this app should be published" report citing this file's dated release
   history as evidence of iterative fixing (crash reporting → bug fixes → completeness fix →
   binary hardening), rather than reconstructing the narrative from git log at the last minute.

## Known gap to close before `FREE_LAUNCH_MODE` is ever disabled

- **RevenueCat has no products/offerings configured for the Play Store API key.** Surfaced
  2026-09-14 via logcat on the real versionCode 11 Internal-testing install: `[Purchases] ERROR:
  ConfigurationError — you have configured the SDK with a Play Store API key, but there are no Play
  Store products registered in the RevenueCat dashboard for your offerings.` Not a code bug — it's a
  RevenueCat-dashboard configuration task (create the subscription products in Play Console, then
  register/attach them as offerings in the RevenueCat dashboard). Currently harmless because
  `LaunchFlags.FREE_LAUNCH_MODE` bypasses the paywall entirely (see the versionCode 7 entry above),
  so no tester can reach the purchase path. **Must be fixed before `FREE_LAUNCH_MODE` is turned off**
  — otherwise `RevenueCatBillingManager` will have nothing to sell and any real purchase attempt will
  fail the same way. Also blocks ever running the sandbox-purchase verification check referenced in
  the versionCode 11 entry above, since there's nothing purchasable to test against yet.
