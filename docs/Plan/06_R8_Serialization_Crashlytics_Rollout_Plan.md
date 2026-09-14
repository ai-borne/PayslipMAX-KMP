# R8 Keep Rules — Phase 9–11 Rollout Plan (Serialization + Crashlytics cleanup)

**Date**: 2026-09-10
**Branch**: `release/ios-1.0.0-v6` (commit directly — this is the current closed-testing baseline,
already at `versionCode 8` from the prior R8 Phase 1–8 pass; see `git log --oneline --grep="r8" -i`
on this branch for that history).
**Scope**: `composeApp/proguard-rules.pro` only. No `shared`/`composeApp` Kotlin source changes.
**Source analysis**: `docs/Plan/05_R8_Keep_Rules_Recommendations.md`, findings #1–#4 and #8.
**Addresses now**: #1–#4 (kotlinx.serialization redundant keep rules) and #8 (Firebase Crashlytics
redundant keep rule).
**Explicitly deferred** (not in this plan — higher regression risk, needs a dedicated
native/JNI-focused pass): #5 (Koin annotation scope), #6 (global native-methods keep), #7 (LiteRT
wildcard keep). Revisit after this phase ships clean.

## Why this baseline, not `main`

`release/ios-1.0.0-v6` is what real closed testers are running today. It already contains a shipped,
real R8 optimization pass (Phase 1–8, commits `ef9c3c2`…`556fa9a`, released at `versionCode 8` per
`48d2fe6`) that `main` does not have. Branching from `main` would silently drop that prior work from
the baseline. This plan continues the existing phase numbering (Phase 9 onward) and commit style
(`chore(r8): ... (Phase N)`) directly on this branch.

## Non-negotiables carried into this plan

Per `CLAUDE.md`: phases execute in order, one at a time, no skipping ahead; every phase ends with a
fully successful build; no automated test suite exercises R8-stripped behavior in this repo (minify
is off in debug/unit tests), so **manual release-build device verification is the actual gate for
every phase below**, not `./gradlew check`.

---

## Phase 9 — Baseline capture (no commit) — ✅ DONE (2026-09-11)

Purpose: establish a known-good "before" state to compare against, and a rollback point.

1. Confirm clean working tree on `release/ios-1.0.0-v6` (`git status`).
2. `./gradlew :composeApp:assembleRelease` — archive the resulting `mapping.txt` as the
   pre-change baseline.
3. Install the baseline release build on a device:
   - Parse one payslip end-to-end, confirm persistence/read-back.
   - Force one test crash, confirm it appears correctly symbolicated in the Firebase Crashlytics
     console.

**Exit criteria**: baseline release build succeeds; baseline crash appears correctly in Crashlytics.
**Phase Handoff**: no tech debt (no code change this phase); build confirmed green.

**Result**: `versionCode 10` release build succeeded; `mapping.txt` archived
(70,508,186-byte baseline APK). On-device via the already-live Play Internal testing install
(installerPackageName=com.android.vending, integrity check passing): force-stop + relaunch proved
Room read-back survives a fresh process (multi-month dashboard reloaded correctly, no
`SerializationException`). Forced test crash (`PayslipMax Test Crash: Observability Verification`)
confirmed correctly symbolicated in the Crashlytics console (readable
`DeveloperSandboxSectionKt.DeveloperSandboxSection$lambda$0$2$0`, not raw `r8-map-id-...` symbols).

---

## Phase 10 — Remove redundant kotlinx.serialization keep rules — ✅ DONE (2026-09-11)

**Commit**: `chore(r8): remove redundant kotlinx.serialization keep rules (Phase 10)` (`0b985c9`)

1. Edit `composeApp/proguard-rules.pro`, delete the 4 blocks under "Kotlinx Serialization":
   - `-keepclassmembers class * implements kotlinx.serialization.KSerializer { *** INSTANCE; }`
   - `-keepclassmembers class * { @kotlinx.serialization.SerialName <fields>; @kotlinx.serialization.Serializable <fields>; }`
   - `-keepclassmembers @kotlinx.serialization.Serializable class * { *** Companion; }`
   - `-keepclasseswithmembers class * { kotlinx.serialization.KSerializer serializer(...); }`
   - Leave the `-keepattributes *Annotation*,ElementValuePairs` line above them untouched (it's not
     a keep rule for these classes, it's a general attribute retention needed by the library).
2. `./gradlew check -x iosX64Test -x iosSimulatorArm64Test` — cheap correctness gate.
3. `./gradlew :composeApp:assembleRelease`.
4. Device verification (the real gate): install the release build, parse payslips from at least two
   different grammar eras (e.g. one legacy statement, one modern grid), confirm the parse completes,
   persists via Room, and re-reads correctly — no `SerializationException` / `ClassNotFoundException`.

**Exit criteria**: release build succeeds; both grammar-era parse+persist checks pass on-device.
**Phase Handoff**: tech debt = none (pure deletion); build green; device verification passed
(record which two grammar eras were tested).

**Result**: `check` and `assembleRelease` both green. **Device verification for this specific
change is explicitly deferred to Phase 12**, not skipped — Play refuses a second Internal testing
upload at the already-consumed `versionCode 10`, so an intermediate Phase-10-only build cannot be
installed on the test device (the release-only `AndroidAppIntegrityChecker` install-source gate
blocks any non-Play install regardless of `versionCode`). User decision 2026-09-11: verify Phase 10
and 11 together on the real `versionCode 11` shipping artifact at Phase 12 rather than burn a
versionCode on a non-shipping intermediate build. Recorded here per the fail-loud rule so this is
never mistaken for a completed device check.

---

## Phase 11 — Remove redundant Firebase Crashlytics keep rule — ✅ DONE (2026-09-11)

**Commit**: `chore(r8): remove redundant Firebase Crashlytics keep rule (Phase 11)` (`1834c67`)

1. Edit `composeApp/proguard-rules.pro`, delete:
   - `-keepclassmembers class com.google.firebase.crashlytics.** { *; }`
   - Leave `-dontwarn com.google.firebase.crashlytics.**` untouched — separate, unrelated to this
     removal.
2. `./gradlew :composeApp:assembleRelease`.
3. Device verification: install the release build, force a test crash, confirm it appears in the
   Firebase Crashlytics console within the usual latency, correctly symbolicated against the new
   `mapping.txt` (the Crashlytics Gradle plugin, applied in `composeApp/build.gradle.kts:16`,
   uploads this automatically on release build — confirm the upload actually happened, don't
   assume).

**Exit criteria**: forced test crash captured and correctly symbolicated on the new build.
**Phase Handoff**: tech debt = none; build green; crash-capture verification passed.

**Result**: `assembleRelease` green. Device verification deferred to Phase 12 alongside Phase 10,
same rationale as above.

If either Phase 10 or Phase 11 verification fails, bisect by reverting only that phase's commit —
they are independent changes to independent rule blocks.

**Measured size impact (Phase 9 baseline vs. post-Phase-11 build, both `versionCode 10`-config
artifacts)**: APK 70,508,186 → 70,442,383 bytes, **≈65.8 KB smaller (~0.09%)**. Modest and expected —
these were narrow rules pinning a handful of serialization-support and Crashlytics classes that
library-consumer rules already covered functionally, not blanket keeps unlocking large dead-code
removal. A materially bigger size win would come from the deferred, higher-risk findings #5–#7
(Koin annotation scope, global native-methods keep, LiteRT wildcard keep) — intentionally out of
scope for this plan.

---

## ⏸ Paused after Phase 11 (2026-09-11)

Phase 10 and Phase 11 are committed and build-verified; their on-device verification is bundled into
Phase 12 as documented above. **User decision: hold before starting Phase 12 and revisit in
3–4 days** (i.e. around 2026-09-14/15). Nothing further should proceed on this plan until then —
resume by re-reading this doc's current state (don't assume it's still accurate) and continuing at
Phase 12 below.

---

## Phase 12 — Version bump + release candidate

> **Update (2026-09-11)**: versionCode 9 was already consumed by an unrelated UI-fix release
> (published to Internal testing before Phase 10/11 of this plan started; it carries none of this
> plan's proguard-rule changes). versionCode 10 was then consumed by a further UI-only closed-testing
> release, also unrelated to this plan. This plan's proguard-rule changes (Phase 10 + Phase 11) will
> therefore ship as **versionCode 11**, not 9. The step below is updated accordingly; no other phase
> in this plan changes. Phase 10 and Phase 11's deferred device verification (see those phases'
> Result notes) happens here, on this exact artifact.

**Commit**: `chore(release): bump versionCode to 11 for R8 keep-rule release`

1. Bump `versionCode` (whatever it is at the time — currently 10) → 11 in
   `composeApp/build.gradle.kts:168` (mirrors the convention used for the prior R8 pass, `48d2fe6`).
2. Full release build: `./gradlew :composeApp:assembleRelease` (or `bundleRelease` if shipping via
   Play App Bundle).
3. Re-run both device checks from Phase 10 and Phase 11 against this exact build artifact (not the
   intermediate per-phase builds) — this is the artifact that actually ships.

**Exit criteria**: `versionCode 11` release build succeeds; both parse/persist and crash-capture
checks pass on this exact artifact.
**Phase Handoff**: tech debt = none; build green; both verifications repeated and passing on the
shipping artifact.

**Result (2026-09-14)**: `versionCode` bumped to 11 (`836920a`); `:composeApp:assembleRelease` green
(70,442,187-byte APK, matching the Phase 9-11 measured size, confirming no drift). Sideloaded onto
the physical Pixel 9 (installer spoofed to `com.android.vending`, existing v10 uninstalled first for
the signature mismatch, restored to the real Play-delivered v10 afterward — device left clean).

- **Phase 11 check (crash symbolication) — ✅ PASSED.** Fired the Developer Sandbox's "Background
  Thread Crash (IO/Default)" trigger. Crashlytics issue `fb358c61fc9f50dc53b69babeee934f3` shows a
  fully readable title —
  `com.payslipmax.pdfparser.telemetry.TestCrash_androidKt$triggerBackgroundTestCrash$1.invokeSuspend`
  — not obfuscated (`r8-map-id-...`) garbage, and is explicitly tagged by Crashlytics as
  "regressed... in version 1.0.0 (11)", confirming the mapping upload matched this exact build.
- **Phase 10 check (two-grammar-era parse/persist) — NOT DONE, genuinely deferred.** The sideload
  wiped local app data, and importing a real PDF through the file picker needs a human with an
  actual payslip file — this environment has no such file and no way to drive the system file
  picker blindly. **This is the same class of gap the closed-testing progress log already flags as
  "Outstanding" for the bundled Sign-In/purchase checks — not silently skipped, but not verified
  either.** Someone with device access and a real (or corpus) PDF needs to run this before
  promoting past Internal testing.

---

## Phase 13 — Staged rollout to closed testing (no repo commit)

1. Upload the `versionCode 9` build to the lowest-exposure test track first (internal testing, not
   the full closed-testing group).
2. Hold a monitoring window of 24–48h, watching the Crashlytics dashboard for:
   - Any new exception signature not present in the Phase 9 baseline.
   - Crash-free-users rate vs. the Phase 9 baseline.
3. Promote to the full closed-testing group only after that window is clean.

**Exit criteria**: no new crash signatures attributable to this change; crash-free rate stable or
better vs. baseline.

---

## Phase 14 — Documentation close-out

**Commit**: `docs: mark R8 rules #1-4 and #8 addressed (Phase 9-11)`

1. Update `docs/Plan/05_R8_Keep_Rules_Recommendations.md`: mark findings #1–#4 and #8 as **done**,
   dated, with a one-line note on how each was verified (device parse test / forced-crash test).
2. Leave #5, #6, #7 explicitly flagged as still pending/deferred, with a pointer to this doc's
   rationale for deferring them (native/JNI regression risk, needs its own dedicated pass).

**Exit criteria**: doc reflects actual shipped state; nothing marked done that wasn't verified
on-device.

---

## Rollback

Each phase is one isolated commit on `release/ios-1.0.0-v6`. A regression traced to Phase 10 or
Phase 11 can be reverted independently (`git revert <sha>`) without touching the other, since the
two rule removals are unrelated. A regression traced to the version bump alone (Phase 12) reverts
cleanly without touching the rule changes.
