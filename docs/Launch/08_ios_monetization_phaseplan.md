# iOS Monetization Rollout — Phase-Wise Execution Plan (v1.1.1 → paid)

**Date**: September 11, 2026
**Scope**: iOS only. Executes Section 3 of
[07_platform_monetization_rollout.md](07_platform_monetization_rollout.md) as an explicit,
gated, phase-by-phase plan per this repo's non-negotiable execution rules (CLAUDE.md §0–2).
Android is untouched by this plan (still blocked on BillDesk KYC per doc 07 §4).

**Starting state**: `v1.1.0` free, live in App Store production. `v1.1.1` (UI changes only, no
monetization) is currently in App Review. `FREE_LAUNCH_MODE` is a single shared `const val = true`
in `shared/.../subscription/LaunchFlags.kt`. `RevenueCatApiKey.ios.kt` returns a **sandbox test
key** (`test_QOma...`) — this is the exact cause of the earlier Guideline 2.1 rejection on
`v1.0.0(2)`: RevenueCat could not resolve a real product because there was no real Apple app/key
behind it.

**Non-negotiable outcome**: no phase after this one ships to App Review with an IAP that isn't
100% wired end-to-end. Per RevenueCat's own rejection postmortems, the near-universal cause of
IAP-related 2.1 rejections is submitting before a sandbox purchase has actually been completed —
not a code defect. Phase 9 exists specifically to make that impossible to skip.

Each phase below ends with a **Phase Summary** (tech debt incurred / how it was resolved / build
+ test status) before the next phase starts, per CLAUDE.md's Phase Handoff Protocol. No phase
starts until the previous one is fully green.

---

## Phase 0 — Baseline

**Goal**: a frozen, verifiable snapshot of current state before anything changes.

- Confirm `v1.1.1` App Review status; do not touch `LaunchFlags`, `RevenueCatApiKey.ios.kt`, or
  any billing file while it's in review (a mid-review binary change is a different build than what
  was submitted — don't invalidate the pending review).
- Record current values as the rollback reference:
  - `LaunchFlags.FREE_LAUNCH_MODE = true`
  - `RevenueCatApiKey.ios.kt` → `test_QOmayJNDtTWprZuKRLJcsQKOOjW`
  - App Store Connect: no subscription product exists yet
  - RevenueCat dashboard: confirm whether an Apple app entry already exists (unclear from repo
    state alone — check the dashboard directly)
- Pull real v1.0/v1.1 install counts from App Store Connect Analytics (needed for the
  grandfather-clause decision in Phase 6 — doc 07 §3 step 5 flagged this as "confirm with real
  numbers, don't assume").
- No code changes in this phase. Exit criteria: baseline documented, `v1.1.1` review outcome known.

**Phase Summary** (completed 2026-09-12): no tech debt — no code touched. Baseline recorded with
real data (via new fastlane/App Store Connect API tooling under `iosApp/fastlane/`, added this
phase instead of manual dashboard checks each time): `v1.1.1` was `WAITING_FOR_REVIEW` at baseline
(cleared to `READY_FOR_SALE` before Phase 1 started); RevenueCat has no Apple app entry yet (only
`PayslipMax (Play Store)` exists); 1 real install in the last 60 days. Build/tests unaffected, still
green from `v1.1.1`'s own CI run.

---

## Phase 1 — Split the flag (mechanism only, still defaults to free)

**Goal**: make iOS/Android independently controllable without changing observable behavior yet.

- Edit `LaunchFlags.kt`:
  ```kotlin
  object LaunchFlags {
      const val FREE_LAUNCH_MODE_IOS: Boolean = true
      const val FREE_LAUNCH_MODE_ANDROID: Boolean = true
  }
  ```
- Update every call site (`SubscriptionManager.hasAccess()`, `BillingProvider` wiring, any
  feature-gate check) to read the platform-specific constant via `expect`/`actual` or a
  platform-scoped accessor — grep for `FREE_LAUNCH_MODE` first (`SubscriptionManagerTest.kt`,
  `SubscriptionManagerBillingTest.kt` reference it and need updating alongside).
- **This phase changes nothing about app behavior** — both flags stay `true`. This is purely the
  SSOT-preserving refactor from doc 07 §2, done as its own reviewable, testable step rather than
  bundled into the flag flip itself.
- Tests: update/extend `SubscriptionManagerTest` to assert the two constants are read
  independently (e.g. a test double that flips one without the other and checks only the matching
  platform gate responds).

**Exit criteria**: `./gradlew :shared:testDebugUnitTest :composeApp:testDebugUnitTest` green,
`ktlintCheck` green, app behavior on both platforms unchanged (still fully free).

**Phase Summary** (completed 2026-09-12, commit `8059f04` on `release/ios-1.0.0-v6`): tech debt =
none — mechanical constant split plus a new `isFreeLaunchModePlatform()` expect/actual (mirroring
the existing `isDebugBuild()` pattern in the same package), all call sites migrated in the same
commit (verified via grep, no stale `LaunchFlags.FREE_LAUNCH_MODE` references left). Added a test
proving the two platform flags are read independently rather than coupled. `:shared:testDebugUnitTest`,
`:composeApp:testDebugUnitTest`, `ktlintCheck`, the tech-debt/file-size audit, and the iOS framework
link check are all green (verified manually and re-verified by the pre-commit hook on commit).
Behavior confirmed unchanged — both flags still `true`.

---

## Phase 2 — App Store Connect: create the subscription product

**Goal**: the real product exists in ASC, independent of any app code.

- Confirm the Paid Applications Agreement is active for the developer account.
- Create an auto-renewable subscription group + subscription (`payslipmax_yearly_premium`, ₹199/yr
  per doc 07 §3, or whatever pricing is finalized — confirm price tier with the user before
  creating, this is a business decision not a technical one).
- Fill every required metadata field: display name, description, review screenshot (a screenshot
  of the actual paywall screen, not a placeholder — Apple rejects on missing/mismatched review
  screenshots), localized pricing.
- Get the product to **"Ready to Submit"** status in ASC — RevenueCat's own checklist calls this
  out explicitly as a precondition; a product stuck in "Missing Metadata" will silently fail to
  resolve later even if the code is correct.
- No code changes this phase — pure App Store Connect console work.

**Exit criteria**: subscription product shows "Ready to Submit" (or better) in ASC.

**Phase Summary** (completed 2026-09-12): no tech debt — pure ASC console work, no code changes.
Re-verified live state before starting: `v1.1.1` still `READY_FOR_SALE` (via `fastlane ios
review_status`), Paid Apps Agreement still Active in ASC → Business → Agreements. Created
subscription group `PayslipMax Yearly Premium` (group ID `22378910`) and subscription
`PayslipMax Yearly Premium` (product ID `payslipmax_yearly_premium`, 1 Year Upfront duration,
₹199.00/year confirmed against the ASC-generated territory price list — the ₹199 India tier maps to
$1.99 USD as the base equivalent). Added English (U.S.) localization (display name "PayslipMax
Premium", description) and a review screenshot of the actual in-app paywall (captured by
temporarily flipping `FREE_LAUNCH_MODE_IOS` to `false` in a local, uncommitted build — confirmed via
`git diff` that the flag was reverted to `true` before ending the session, no code change persisted)
run on iPhone 17 Pro Simulator via `idb`, navigating Settings → Upgrade to PayslipMax Premium.
Added review notes describing the subscription, its unlock path, and confirming Restore Purchases
presence. Subscription status is **"Ready for Review"** (ASC's current label for what this doc
calls "Ready to Submit") — confirmed via the "Item Ready to Submit" panel; ASC correctly refuses a
standalone submission ("Your first auto-renewable subscription must be submitted with a new app
version") since subscriptions can only go out bundled with an app version submission, which is
Phase 8's job, not this phase's. Build/tests unaffected — no code touched by the end of the phase.

---

## Phase 3 — RevenueCat dashboard: wire the Apple app

**Goal**: RevenueCat can resolve the real product, not the sandbox placeholder.

*(This is the phase where `claude-in-chrome` is useful for driving the RevenueCat dashboard UI —
offer to drive it live once you're at the dashboard and want a second pair of hands on the
click-path, per the screenshot workflow you referenced.)*

- In RevenueCat dashboard → Project Settings → Apps: confirm/create the Apple App Store app entry
  (check whether one already exists from `v1.0.0` sandbox testing — don't create a duplicate).
- Attach the App Store Connect API key (or shared secret) so RevenueCat can validate receipts
  server-side.
- Product Catalog: attach the real Apple product ID from Phase 2 to the `premium` entitlement and
  a `yearly` package/offering.
- Copy the real **production** RevenueCat API key (`appl_...`) — this replaces the `test_...` key,
  but **do not commit it yet** (Phase 4 is the code change; keep Phase 3 as dashboard-only so it's
  independently verifiable/reversible).

**Exit criteria**: RevenueCat dashboard shows the entitlement resolving to a real, non-sandbox
Apple product; API key retrieved and held for Phase 4.

**Phase Summary** (completed 2026-09-12): no tech debt — pure RevenueCat dashboard work, no code
changes. Re-verified live state before starting: `v1.1.1` still `READY_FOR_SALE` (via `fastlane ios
review_status`). Created the Apple App Store app entry in RevenueCat under the `PayslipMax` project
(bundle ID `in.aiborne.payslipmax`), uploading the App Manager ASC API key
(`AuthKey_J87P2YJ2PS.p8`, copied to `SubscriptionKey_J87P2YJ2PS.p8` locally only to satisfy
RevenueCat's upload filename check — original untouched, no repo change) with Key ID `J87P2YJ2PS`
and the Issuer ID from `iosApp/fastlane/.env` — RevenueCat confirmed "Valid credentials". Note: the
actual p8 file upload was done by the user manually (browser-automation credential-file upload was
correctly blocked by the harness's safety classifier as file-exfil risk) — this is the one step in
the phase that isn't reproducible by an agent alone. Created product `payslipmax_yearly_premium`
under the new PayslipMax (App Store) app (RevenueCat's "Import" found nothing yet — ASC metadata
sync lag — so it was added manually with the exact product ID from Phase 2), attached it to the
existing `PayslipMax Premium` entitlement, and wired it into the `default` offering's `$rc_annual`
package (previously only had a Test Store product) alongside the untouched Play Store slot. Product
"Store Status" shows "Could not check" — expected propagation delay, not a config error. Retrieved
the real production RevenueCat SDK key (`appl_NgEonbkizWMfsjfyaFCuTgBLWGx`) and am holding it,
uncommitted, for Phase 4 (per this phase's own instruction not to commit it yet). Build/tests
unaffected — no code touched.

---

## Phase 4 — Code: real API key + platform flag still `true`

**Goal**: ship the real key through the pipe, but keep the paywall dark until Phase 8.

- Replace `RevenueCatApiKey.ios.kt`'s return value with the real `appl_...` key. Per this repo's
  security rule, confirm this key is safe to commit (RevenueCat public SDK keys are designed to be
  client-embedded, unlike the ASC API key from Phase 3 which must never enter the repo) — verify
  against RevenueCat's own docs on which key is which before committing.
- `FREE_LAUNCH_MODE_IOS` **stays `true`** in this phase — the point is to prove the RevenueCat SDK
  initializes against the real backend without yet exposing any paywall to real users.
- Add/extend a unit test asserting `revenueCatApiKey()` no longer returns a string prefixed
  `test_` (cheap regression guard against ever re-shipping the sandbox key to production — this
  exact mistake was the root cause of the earlier rejection).

**Exit criteria**: iOS build initializes RevenueCat against the production project; app still
100% free-behaving; tests green.

**Phase Summary** (completed 2026-09-12): no tech debt — a two-line key swap plus one new
regression test, no structural change. Re-verified live state before starting (didn't trust the
Phase 3 memory copy blindly): re-fetched the key from the RevenueCat dashboard directly
(Project Settings → API keys → "PayslipMax (App Store)", created Sep 12, 2026) via
`claude-in-chrome` and confirmed it matches the memory value exactly:
`appl_NgEonbkizWMfsjfyaFCuTgBLWGx`. Also re-confirmed `v1.1.1` is still `READY_FOR_SALE` via
`fastlane ios review_status` (no App Review in flight to conflict with a mid-review binary
change). Replaced `RevenueCatApiKey.ios.kt`'s return value with the real key (public SDK key, safe
to commit per RevenueCat's own docs — distinct from the ASC API key which must never enter the
repo) and refreshed the stale doc comment on the `expect fun` that still described it as the
Phase 0 Test Store key. `FREE_LAUNCH_MODE_IOS` left untouched at `true` (confirmed via `grep`
after the change — still `true` in `LaunchFlags.kt`). Added
`RevenueCatApiKeyTest.iosShipsRealProductionKeyNeverSandboxTestKey()` under `shared/src/iosTest/`
asserting the key is not `test_`-prefixed and is `appl_`-prefixed — the exact regression guard the
phase calls for, guarding against ever re-shipping the sandbox key (the root cause of the earlier
Guideline 2.1 rejection). Exit-criteria commands run for real: `ktlintCheck`,
`:shared:testDebugUnitTest`, `:composeApp:testDebugUnitTest`, `iosX64Test`,
`iosSimulatorArm64Test`, `check_tech_debt_limits.py --strict` on the three touched files, and
`:composeApp:linkDebugFrameworkIosSimulatorArm64` — all green. App behavior confirmed unchanged
(still 100% free).

---

## Phase 5 — Verify the existing paywall wires up to the real product

**Goal**: confirm the already-built paywall works against Phase 3's real offering — this is a
verification/gap-fill phase, not a build-from-scratch phase.

- The paywall already exists: `composeApp/.../ui/screens/PremiumFeaturesScreen.kt`, driven by
  `PremiumFeaturesCatalog.kt` (SSOT `FeatureGate` → display-metadata mapping) and wired to
  `viewModel.launchPurchaseFlow(onResult)` / `viewModel.restorePurchases(onResult)`
  (`SubscriptionAccess.kt`). Apple's restore-button requirement is already satisfied
  (`onRestoreClick` exists). **Do not rebuild this** — Phase 5 is about proving it, not
  re-implementing it.
- Verify `launchPurchaseFlow`/`restorePurchases` actually source price/product data from
  RevenueCat's `Offering` (the Phase 3 real product), not a hardcoded/mock value — grep their
  implementation in the ViewModel and `RevenueCatBillingManager` to confirm.
- Run existing `InsightCardGatingTest` / `GatedNavigationInvariantTest` and confirm they still pass
  once Phase 3/4's real product ID is in play — these tests currently exercise the gating logic
  against whatever fixture/fake `BillingManager` they use, so check whether they need a new case
  for the real product ID or remain valid as-is.
- All copy/styling already presumably go through `AppStrings.kt`/`Theme.kt` per CLAUDE.md — spot
  check `PremiumFeaturesScreen.kt` for any drift before this phase closes.

**Exit criteria**: existing paywall confirmed to render real product/price data from the Phase 3
offering, restore-purchases confirmed present and functional, still gated off from real users
(`FREE_LAUNCH_MODE_IOS = true`), full test suite green. No new UI code unless a genuine gap is
found (e.g. price not sourced from the real offering) — if so, fix only that gap, surgically.

**Phase Summary** (completed 2026-09-12): tech debt = a real wiring gap found and fixed, no other
debt. Re-verified live state before starting: `v1.1.1` still `READY_FOR_SALE` via `fastlane ios
review_status` (no review in flight), `FREE_LAUNCH_MODE_IOS` still `true` via `grep`. Traced
`launchPurchaseFlow`/`restorePurchases`/price display end-to-end: `PremiumFeaturesScreen.kt` →
`PayslipViewModelExtensions.kt` → `RevenueCatBillingManager` (`shared/.../billing/`) →
`Purchases.sharedInstance.getOfferings()`/`.purchase()`/`.restorePurchases()` — all real SDK calls
against the live `default` offering, no hardcoded/mock product or price (confirmed the price flow
specifically: `PayslipViewModel._premiumPriceState` starts at an `AppStrings` static fallback and
is overwritten by `billingManager.getFormattedPrice()`, exercised by
`PayslipViewModelBillingTest.premiumPriceState_updates_from_live_billingManager_price`). While
verifying the offering/package/entitlement chain directly against the live RevenueCat dashboard
(not just trusting the Phase 3 memory summary) via `claude-in-chrome`, found a real gap:
`RevenueCatBillingManager.REVENUECAT_ENTITLEMENT_ID` was hardcoded to `"premium"`, but the actual
entitlement's dashboard **Identifier** field (Product catalog → Entitlements → PayslipMax Premium)
is the literal string `"PayslipMax Premium"` (with the space) — confirmed via zoomed screenshot of
the identifier field, not just eyeballing the list view. Since `entitlements.active[id]` is an
exact-string lookup, this meant a real completed purchase would never have resolved to `Active`
(`mapCustomerInfoToSubscriptionState` would always return `Inactive`) — a rejection-adjacent bug
Phase 7's sandbox test would otherwise have caught the hard way. Confirmed the package-identifier
side was *not* a bug: the offering's package identifier is `"yearly"` (the `$rc_annual` label is
just the package's duration type, not its identifier), matching `RevenueCatBillingManager`'s
existing `REVENUECAT_PACKAGE_ID = "yearly"` exactly, correctly attached to product
`payslipmax_yearly_premium` from Phase 2/3. Fixed the gap surgically: changed
`REVENUECAT_ENTITLEMENT_ID` to `"PayslipMax Premium"` with a doc comment explaining it must match
the dashboard's literal identifier field, not a slugified guess
(`shared/src/commonMain/kotlin/com/payslipmax/pdfparser/billing/RevenueCatBillingManager.kt`). No
other call site or test hardcodes `"premium"` (verified via grep) — existing tests reference the
constant, not a literal, so no test changes were needed. Verified
`InsightCardGatingTest`/`GatedNavigationInvariantTest` still pass with the real product/entitlement
config in play — both test the `FeatureGate`/`hasAccess` gating layer, which is independent of the
RevenueCat entitlement ID string, so they were never at risk from this bug but are re-confirmed
green regardless. Exit-criteria commands run for real: `:shared:testDebugUnitTest`
`:composeApp:testDebugUnitTest` (including `InsightCardGatingTest`, `GatedNavigationInvariantTest`,
`PayslipViewModelBillingTest`, `RevenueCatBillingManagerTest`, `SubscriptionManagerBillingTest`,
`RevenueCatApiKeyTest`), `ktlintCheck`, `check_tech_debt_limits.py --strict` on the touched file,
`iosX64Test`/`iosSimulatorArm64Test`, and `:composeApp:linkDebugFrameworkIosSimulatorArm64` — all
green. `FREE_LAUNCH_MODE_IOS` untouched, still `true` (paywall still dark for real users).

> **Correction (2026-09-12, during Phase 7)**: the package-identifier claim in the Phase 5 summary
> above is **wrong**, and it was the direct cause of Phase 7's first sandbox purchase failing. The
> summary states the offering's package identifier is `"yearly"` and that `$rc_annual` is "just the
> package's duration type, not its identifier." The opposite is true. Verified against the live
> dashboard: the `default` offering contains exactly one package whose **identifier is
> `$rc_annual`**; `"yearly"` is the identifier of the *Test Store product inside* that package. The
> RevenueCat KMP SDK's `Offering.getPackage(identifier)` matches on `Package.identifier`
> (`availablePackages.firstOrNull { it.identifier == identifier }`, read from the SDK source), so
> `getPackage("yearly")` could never match and **no purchase could ever have succeeded**. Note that
> Phase 3's record ("wired into the `default` offering's `$rc_annual` package") was correct all
> along — Phase 5 "corrected" a non-bug into a wrong conclusion. Fixed in commit `e5ae1f2` by
> resolving through the SDK's typed `Offering.annual` accessor instead of any literal identifier.
> **Lesson for future phases: reading a dashboard label is not verification — confirm which field
> the SDK actually matches on.**

---

## Phase 6 — Grandfather-clause: confirmed **no**

**Decision (confirmed 2026-09-11)**: existing free installs do **not** keep free access. Every
user hits the real paywall once `v1.2` ships, regardless of install date — no separate
grandfather-check code path is built. Rationale: v1.1.x's live window is small (per doc 07 §3 step
5's own framing), so the cost of a clean cutover is low and avoids permanently carrying
install-date-branching logic in the gating path.

- No new code in this phase — this supersedes the "decide with Phase 0 data" framing; the decision
  is made. Phase 0's install-count pull is still worth doing for its own sake (understanding
  conversion exposure), but it no longer gates a decision here.
- Nothing in `SubscriptionManager.hasAccess()` should branch on install date. If such a branch is
  ever proposed later, treat it as a deliberate reversal of this decision, not a silent addition.

**Exit criteria**: this section stands as the recorded decision; no implementation work required.

---

## Phase 7 — Sandbox purchase verification (TestFlight)

**Goal**: prove the entire pipe works end-to-end before it's anywhere near App Review — this is
the single step whose absence caused the original rejection, per doc 07 §3 step 4.

- Ship a TestFlight build with `FREE_LAUNCH_MODE_IOS` flippable via a debug-only override (not the
  production flag itself) so the paywall is reachable in TestFlight without affecting the
  still-free production binary.
- Using a **sandbox Apple ID** (not a real account), complete an actual purchase through the
  TestFlight build:
  - Product fetch succeeds (no `STORE_PROBLEM`/generic `SKError`)
  - Purchase completes without error
  - Entitlement unlocks the gated feature immediately
  - Restore Purchases works on a fresh install/reinstall
- Also test failure paths: cancel mid-purchase, network-off during purchase — the app must fail
  gracefully, not crash or hang (a hang here is a 2.1 rejection risk independent of IAP config).

**Exit criteria**: a documented, successful sandbox purchase + restore, screenshotted, with no
manual workaround needed. Do not proceed to Phase 8 without this artifact existing.

**Phase Summary** (completed 2026-09-12): the sandbox purchase pipe is verified end to end on a
real device via TestFlight `1.1.2 (3)`.

**What was verified (exit-criteria artifacts, all screenshotted on-device):**

- **Product fetch** — paywall rendered `₹ 199` formatted by StoreKit, not the hardcoded
  `AppStrings.settingsPremiumPlanPrice` (`"₹199 / Year"`). The absent `" / Year"` suffix is the
  discriminator: a displayed price alone never proves the product resolved.
- **Purchase completed** — Apple's Manage Subscription shows `PayslipMax Premium`, ₹199/year,
  renewing 13 Sep 2026; a second purchase attempt returned "You are currently subscribed to this".
- **Entitlement resolves** — confirmed *server-side* on the RevenueCat dashboard, not merely
  inferred: customer `$RCAnonymousID:ff9a…62d0` (India) shows *"Started a subscription of PayslipMax
  Yearly Premium (payslipmax_yearly_premium) for INR 199 from offering default"* and entitlement
  **`PayslipMax Premium` → Active**. Product catalog → Entitlements shows Identifier and Display
  Name are the *same* string `PayslipMax Premium`, so Phase 5's entitlement finding was correct
  (unlike its package finding) and `REVENUECAT_ENTITLEMENT_ID` needs no change.
- **Restore on a fresh install** — app deleted, reinstalled from TestFlight, Restore Purchases run
  with no local state: the sheet auto-dismissed with no error, which is reachable only via
  `PurchaseSheetOutcome.Success`, which in turn requires
  `entitlements.active.containsKey("PayslipMax Premium")`.
- **Failure paths degrade gracefully** — Airplane Mode purchase → inline *"Purchase failed: Error
  performing request."*; Airplane Mode restore → *"Restore failed: Error performing request because
  the internet connection appears to be offline."* Sheet stays open, controls re-enable, no crash
  and **no hang** — i.e. the `suspendCoroutine` in `launchBillingFlow`/`restorePurchases` does get
  its callback on network failure. A hang here would have been a 2.1 risk independent of IAP config.

**Explicitly NOT verified (do not record these as passed):**

1. **Cancel mid-purchase.** Once subscribed, tapping Unlock yields Apple's "already subscribed"
   alert instead of a cancellable purchase sheet, so the sub-criterion became untestable in this
   session. The offline path exercises the same in-app recovery behaviour, but this specific
   interaction remains untested.
2. **Gate-level unlocking is unverifiable on this build by construction.** With
   `FREE_LAUNCH_MODE_IOS = true`, `SubscriptionManager.hasAccess` short-circuits in *both* override
   positions — `FORCE_FREE` returns `false` unconditionally, `FOLLOW_FLAG` falls through to the
   free-launch check and returns `true` unconditionally — so the `billingManager.subscriptionState`
   branch is unreachable. The Settings premium card, the one UI bound to `isPremiumEnabled` rather
   than `hasAccess`, is itself suppressed by `if (!isFreeLaunchModePlatform())`
   (`SettingsSectionComponents.kt:39`). **No in-app UI exposes entitlement state while the free-launch
   flag is true**; gated cards are flag echoes, not evidence. This resolves in Phase 8 when the flag
   flips, and must be re-tested there.

**Tech debt incurred and resolved this phase:**

1. **`Info.plist` hardcoded `CFBundleVersion` as the literal `2`** while `CFBundleShortVersionString`
   correctly used `$(MARKETING_VERSION)` — so `CURRENT_PROJECT_VERSION` was dead and every archive
   ever produced shipped as build 2, the `1.1.2 (4)` bump in `e5ae1f2` being a no-op. Xcode's
   "Manage Version and Build Number" masked it by auto-incrementing at upload (hence TestFlight
   `1.1.2 (3)`). **Resolved** in `8d2a18f`: wired to `$(CURRENT_PROJECT_VERSION)`, verified
   resolving to 4 in both Debug and Release via `xcodebuild -showBuildSettings`.
2. **`resolveYearlyPackage()` collapsed three failure modes into one opaque error**, and
   `getFormattedPrice()` failed identically for all three by falling back to the hardcoded price —
   a dead product rendering as a healthy paywall, which is what made the first failure cost a full
   debugging session. **Resolved** in `e693581`: `YearlyPackageResolution` (`Resolved` /
   `OfferingsUnavailable` / `NoCurrentOffering` / `NoAnnualPackage`), each failure naming its own
   cause, a `getOfferings` failure carrying the SDK's message. Purely diagnostic — resolution logic
   unchanged, so the verified purchase path is untouched. Scope was confirmed with the user before
   implementing, and deliberately excludes the price fallback (see carried debt below).

**Tech debt carried forward (recorded, not silently dropped):**

- **The hardcoded price fallback stays.** `AppStrings.settingsPremiumPlanPrice` still renders as if
  it were live store data when RevenueCat returns nothing. Fixing it (show no price until live data
  arrives) touches `PayslipViewModel`, both premium composables and their tests — a wider UI
  decision deferred by explicit user choice, not an oversight.
- **Restore's success message is never visible.** `onRestoreClick` sets `feedbackStatus` and calls
  `onDismissRequest()` in the same branch (`PremiumUpgradeBottomSheet.kt:96-104`), so the
  confirmation renders into a sheet being torn down. The user sees a spinner, then silence, on a
  *successful* restore. Cosmetic, but it actively misleads during testing.
- **No regression test for the `Info.plist` fix.** `iosApp` has no test target; asserting a
  build-config placeholder would mean standing up test infrastructure for a file with no runtime
  behaviour. Stated rather than pretended.

**Build/test status:** `ktlintCheck`, `:shared:testDebugUnitTest`, `:composeApp:testDebugUnitTest`,
`iosSimulatorArm64Test`, the tech-debt audit and the iOS framework link check were all run for real
and are green; the full pre-push gate (both variants, corpus regression, iOS suite, gitleaks over
the pushed range) passed on `8d2a18f`. `FREE_LAUNCH_MODE_IOS` remains `true` — production is
untouched and the paywall stays dark until Phase 8.

**Open gaps carried into Phase 8:**

1. **RevenueCat's App Store Connect API key slot is still empty** (product Store Status "Could not
   check"). Now demonstrated *empirically harmless* to on-device purchasing — the full purchase,
   entitlement and restore chain worked without it — so it is a server-side sync/display concern
   only, not a blocker. Still worth filling; needs a manual `.p8` upload by the user.
2. **The ASC subscription group has no localization.** Unchanged, and required before submission.

## Phase 8 — Flip the real flag + submit

**Goal**: the actual production monetization switch.

- Flip `FREE_LAUNCH_MODE_IOS = false` in `main`. `FREE_LAUNCH_MODE_ANDROID` remains `true`,
  untouched (doc 07 §1/§5) — this is the one line that changes in this phase.
- Full regression: `./gradlew check -x iosX64Test -x iosSimulatorArm64Test`,
  `iosX64Test iosSimulatorArm64Test`, ktlint, corpus regression — this is a release build, run the
  full gate, not the incremental pre-commit subset.
- Submit as `v1.2` through App Review. Attach the Phase 7 sandbox-purchase evidence in App Review
  notes if there's any ambiguity in the paywall's placement (reviewers occasionally re-request
  this).
- This is the **second-to-last phase overall** the user asked for ("payment testing" phase) — note
  Phase 7 is the *sandbox* payment test (pre-submission) and this phase is the *submission* itself;
  if App Review comes back with a rejection, treat it as looping back to Phase 7, not forward.

**Exit criteria**: `v1.2` in App Review with monetization live, full test suite green, sandbox
evidence attached.

**Phase Summary** (verification complete 2026-09-13; submission pending — see "Remaining" below):
monetization is live in code and the entire purchase pipe is verified on-device with the real flag
flipped for the first time.

**The one production line** (`bc25900`): `FREE_LAUNCH_MODE_IOS = false`. `FREE_LAUNCH_MODE_ANDROID`
stays `true`, untouched. Shipped as TestFlight `1.2 (5)`, then `1.2 (6)` after the fix below. Both
landed at their declared build numbers with no Xcode auto-increment — the Phase 7 `CFBundleVersion`
fix holding across real archives, which Phase 7 could only assert from `xcodebuild -showBuildSettings`.

**Exit-criteria evidence (all on-device, all screenshotted).** Every check below was run with the
`DevOverride` at `FOLLOW_FLAG`, so `hasAccess` fell through to `billingManager.subscriptionState` —
this is the first phase in which any of it is real evidence rather than a flag echo:

1. **Gate-level unlocking, both directions** — Phase 7's headline gap, now closed. Locked: the
   Settings card reads "Upgrade to PayslipMax Premium" and premium tools are shut. Unlocked: the
   same card reads "Premium Plan Activated · Subscribed (Auto-Renewing Subscription Active)" and
   Tax Planner / DSOP Simulator / Claim Generator / Retirement Calculators all open. Verified on
   **two independent Apple IDs**. The chain was traced in code, not inferred: RevenueCat entitlement
   → `subscriptionState.Active` → `setPremiumEnabled(true)` (`PayslipViewModelExtensions.kt:70`) →
   Room → `uiState.isPremiumEnabled`.
2. **The Settings premium card renders at all** — suppressed in every prior build by
   `if (!isFreeLaunchModePlatform())` (`SettingsSectionComponents.kt:39`). Confirmed in both states.
3. **Cancel mid-purchase** — untestable in Phase 7 and now closed: dismissing Apple's auth prompt
   mid-flow leaves the sheet open, the spinner clears on its own, nothing unlocks, no hang.
4. **Purchase → immediate unlock** — Apple's "You're all set", then the Settings card flipping to
   activated **without an app restart**. This is RevenueCat's #3 rejection cause, directly disproven.
5. **Restore shows its confirmation for ~1.5s before the sheet closes** — the Phase 7 debt fix
   verified by eye. There is deliberately no automated coverage (a real `ModalBottomSheet` spans
   three Robolectric windows and the assertion cannot find the message node); only the pure
   `dismissDelayMsFor()` rule is unit-tested, so on-device was the only possible verification.
6. **Price reads ₹999** — confirmed on an Indian-storefront account, and Apple's own TestFlight
   purchase sheet charged "₹ 999 per year" on **both** accounts.

**The price-display investigation (the substantive work of this phase).** On one of the two Indian
accounts the in-app paywall rendered `$9.99` while Apple's purchase sheet charged `₹ 999`. This was
chased to ground rather than waved off, because every user of this app is on an Indian storefront:

- Ruled out in our code: `getFormattedPrice()` returns `storeProduct.price.formatted`
  (`RevenueCatBillingManager.kt:137-142`) verbatim — there is no currency logic anywhere in the app.
- Ruled out in RevenueCat (checked live on the dashboard): the App Store product carries no
  RevenueCat-side price at all, and the Test Store product is USD **79.99**, not 9.99 — so the app
  was not accidentally resolving the test product. The sandbox transaction for that very purchase is
  recorded as **India, $10.45 USD-normalised** — i.e. ₹999. RevenueCat's server had it right.
- `$9.99` is exactly **our own configured USA price point** in ASC, so StoreKit itself handed the app
  a US-storefront product while billing the Indian account correctly.
- **Root cause: a known TestFlight sandbox limitation**, not a defect — StoreKit product metadata
  can default to USD in beta builds while the payment sheet uses the real localised price. It is
  reported by other developers with this exact symptom (RevenueCat community: "TestFlight iOS 18.5
  UI shows USD but purchase sheet shows INR") and resolves on production release. That it appeared
  on one Indian account and not the other, on the identical binary, is the instability itself.

**Tech debt incurred and resolved this phase:** one, found by the investigation above and fixed in
`caf6fe4`. `loadPremiumPrice()` was private and called **once**, from the ViewModel's `init`, so a
price read at cold start was frozen for the whole session — a failed or early fetch could never
recover. It is now the public `refreshPremiumPrice()`, fired again from a `LaunchedEffect` as
`PremiumUpgradeBottomSheet` opens (the point at which a quoted price becomes a commitment), wired at
all four paywall call sites via a new `onPresented` parameter. A fetch returning `null` now leaves
the last known price in place rather than blanking it, so a transient offerings failure while the
sheet is open cannot disable Unlock mid-decision. Two tests cover it, both carrying the TestFlight
observation in a comment so the *why* survives:
`refreshPremiumPrice_rereads_the_store_so_a_late_storefront_replaces_the_startup_price` and
`refreshPremiumPrice_keeps_the_last_known_price_when_the_store_returns_nothing`.

**Recorded honestly: this fix did not resolve the `$9.99` symptom.** It was written while the
one-shot-`init` read was still the leading hypothesis; the account still showed `$9.99` after it
shipped in `1.2 (6)`. It is kept because a price read once and never re-read is a genuine latent
bug and the tests are sound — but it earns no credit for the currency behaviour, whose cause is the
sandbox limitation above.

**Tech debt carried forward (recorded, not silently dropped):**

- **RevenueCat's App Store Connect API key slot is still empty** (product Store Status "Could not
  check"). Demonstrated harmless to purchasing across two accounts and two builds; needs a manual
  `.p8` upload by the user.
- **No regression test for the `Info.plist` `CFBundleVersion` fix** — `iosApp` has no test target.
  Now at least empirically confirmed twice: builds 5 and 6 both uploaded at their declared numbers.
- **`DeveloperOverrideSection`'s segmented control overflows** — the "Force Premium" label renders
  in a raised box over its neighbours. Debug/TestFlight-only UI that never ships to production; left
  alone deliberately so as not to invalidate the build already under test.

**Build/test status:** the full gate was run for real on `caf6fe4`, not assumed:
`./gradlew check -x iosX64Test -x iosSimulatorArm64Test`, `iosX64Test iosSimulatorArm64Test`,
`ktlintCheck`, the tech-debt audit on all seven touched files, and the iOS framework link check all
green. The exhaustive pre-push gate (both variants, full corpus regression, full iOS suite, Room
schema immutability, gitleaks over the pushed range) passed on the push of `caf6fe4`.

**Live App Store Connect state, re-verified this phase via fastlane rather than trusted from memory:**
`v1.1.1`/`v1.1.0`/`v1.0` all `READY_FOR_SALE` (nothing in review); subscription
`payslipmax_yearly_premium` **READY_TO_SUBMIT** with the group localisation (`en-US`, "PayslipMax
Premium") that Phase 7 flagged as the blocker now present; prices IND ₹999.00 (proceeds ₹719.62),
USA $9.99, GBR £9.99, `startDate=current`, `preserved=false`.

**Submission closeout (2026-09-13).** Before submitting, two stale-₹199 artifacts from the earlier
price point were found and fixed on the live subscription (not just in screenshots evidence):

- The subscription's **App Review screenshot** (uploaded in Phase 2) still showed the old
  hardcoded-fallback paywall — literally `"₹199 / Year"`, the exact fallback string Phase 7's debt
  fix later deleted from the app. Replaced with a fresh on-device TestFlight capture reading
  `"₹ 999"`. Editing it required removing the subscription from its (already-`READY_TO_SUBMIT`)
  draft submission first — ASC makes Review Information read-only while an item sits in a draft
  submission — then re-adding it afterward; it returned to `READY_TO_SUBMIT` unchanged.
- The subscription's **Review Notes** text itself also hardcoded `"(₹199/year)"` in prose, a second
  independent mismatch the doc's earlier caveat-in-review-notes plan hadn't anticipated. Corrected
  to `₹999/year` in the same edit pass.
- Also verified live (no fix needed): the app's License Agreement is Apple's Standard License
  Agreement, and both `Privacy Policy URL` (`https://www.ai-borne.in/privacy-policy`) and
  `Support URL` (`https://www.ai-borne.in/support`) resolve to real, current, functioning pages —
  closing out the Guideline 3.1.2 metadata risk raised before submission.

**`v1.2` created and submitted to App Review (2026-09-13).** Build `1.2 (6)` attached. Hit one
ASC-specific snag not documented anywhere: adding the subscription alone to the version's draft
submission left it blocked with *"New subscription groups must be submitted with an auto-renewable
subscription from within that group"* — a first-ever subscription group must be added to the same
draft submission **as its own separate item**, not just its subscription. Fixed by opening the
group's own page (`.../subscription-groups/22378910`) and using its own "Add for Review" control to
attach the group itself to the same draft submission, bringing "Items Ready to Submit" from 2 to 3
(iOS App 1.2, the subscription, and the group). Submitted; confirmed independently via both the ASC
UI and the `review_status`/`subscription_status` fastlane lanes: `v1.2` and
`payslipmax_yearly_premium` both show **`WAITING_FOR_REVIEW`**, group status "Waiting for Review".

**Exit criteria met: `v1.2` is in App Review with monetization live, full test suite green (this
session and the prior one), sandbox evidence attached with the price-change caveat noted in the
submitted review notes.** Phase 8 is complete.

**If App Review rejects, loop back to Phase 7, not forward to Phase 9.** Next session should check
`fastlane ios review_status` for the outcome before doing anything else.

**`v1.2 (6)` REJECTED (2026-09-14) — Guideline 3.1.2, metadata only, not a Phase 7 gating issue.**
Apple: the App Store product page had no functional Terms of Use (EULA) link. Fixed without a new
build via a new fastlane lane, `add_eula_link_to_description` (`iosApp/fastlane/Fastfile`), which
appended the standard Apple EULA link
(`https://www.apple.com/legal/internet-services/itunes/dev/stdeula/`) to the App Description
through the ASC API; verified live both via `fastlane ios app_description` and directly in the ASC
UI. Resubmitted via "Update Review" on the App Version item — all 3 items (`iOS App 1.2`,
`PayslipMax Premium`, `PayslipMax Yearly Premium`) show **Waiting for Review** as of 2026-09-14.

**`v1.2 (6)` APPROVED (2026-09-15):** All 3 submission items (`iOS App 1.2 (6)`, subscription
`PayslipMax Premium`, and subscription group `PayslipMax Yearly Premium`) cleared App Review with
status **Approved** / **Review Completed**.

**Separately found and fixed while investigating this rejection (does NOT require resubmission,
queued for the *next* build instead):** `LegalStrings.privacyPolicyUrl`
(`composeApp/src/commonMain/kotlin/com/payslipmax/pdfparser/ui/theme/LegalStrings.kt`) was wired to
the Support URL (`https://ai-borne.in/support`) instead of the actual Privacy Policy page — the
in-app "Privacy Policy" link in the purchase sheet's legal footer (`UpgradeLegalFooter`, rendered
inside `PremiumUpgradeBottomSheet`) was silently opening the wrong page. Fixed to
`https://www.ai-borne.in/privacy-policy`, with a regression test (`LegalStringsTest.kt`) guarding
against it recurring. This is a `commonMain` fix, live for both iOS and Android once built. Not
part of `1.2 (6)`'s binary (fixed in the working tree after that build was already submitted) — it
needs to ship in the **next** iOS build (`1.2 (7)` or whatever version follows), not this
resubmission, since Apple's 3.1.2 rejection was metadata-only and this fix doesn't change any
metadata a reviewer checks.

---

## Phase 9 — Distribution

**Goal**: paid app live in production.

- On approval: release `v1.2` to production (staged rollout if desired, to limit blast radius of
  any missed edge case).
- Monitor Crashlytics + RevenueCat dashboard (transaction success rate, entitlement grant latency)
  for the first 24–48 hours — RevenueCat's own guidance notes up to 24h propagation delay for new
  products, so don't treat early sandbox-vs-production discrepancies as a bug before that window
  passes.
- Close out this doc with actual production purchase metrics once available; update doc 07's
  "Current state" section to reflect iOS as paid-live.

**Exit criteria**: `v1.2` live in production, first real transactions confirmed successful in
RevenueCat dashboard, no elevated crash rate attributable to the billing path.

**Phase Summary** (completed 2026-09-15): `v1.2 (6)` is released to the iOS App Store (`READY_FOR_SALE`)
alongside auto-renewable subscription `payslipmax_yearly_premium` (₹999/yr) and subscription group
`PayslipMax Yearly Premium`. Monetization is live in production with `FREE_LAUNCH_MODE_IOS = false`.
Monitoring phase active (Crashlytics + RevenueCat dashboard telemetry).

**v1.2.1 (build 2) — Gemma ODR regression fix** (2026-09-15): Gemma offline-model download-banner
regression discovered in v1.2.1 (build 1) TestFlight testing (broken progress observer). Fixed and
verified on real device with unit test regression guard. Build 2 queued for TestFlight re-upload.

**v1.2.1 (build 3) — Universal Backup & Restore Cross-Platform Interoperability** (2026-09-16):
Standardized cryptographic SSOT strictly on PBKDF2-HMAC-SHA256, resolved iOS PRF and UTF-8 byte-length
discrepancies, registered `.pcda` under `UTExportedTypeDeclarations` and `CFBundleDocumentTypes` for
Gmail and Files attachment support, added backup password UX guidance hint, added in-app `privacyPolicyUrl`
destination fix in `LegalStrings.kt`, explicitly declared `LSSupportsOpeningDocumentsInPlace = false`,
and added automated test suites. Build 3 uploaded to TestFlight and submitted to App Review on 2026-09-16.
Cleared review and shipped — `v1.2.1 (3)` is the live, `READY_FOR_SALE` production build as of this
entry (confirmed via `fastlane ios review_status`/`testflight_builds`, 2026-09-18).

**v1.2.2 (build 6, then 7) — TestFlight-only, not yet submitted** (2026-09-18): needed a new TestFlight
build to test the accumulated changes since `1.2.1 (3)`. Apple had closed `1.2.1`'s pre-release train
once it went `READY_FOR_SALE` (rejected a same-version re-upload attempt with "Invalid Pre-Release
Train"), so `1.2.2` was opened as a new version instead of just bumping the build number — this also
made it the "next real update" trigger doc 09 was waiting for, so the queued relaunch ASO copy and 8
approved screenshots shipped into it too (see doc 09's Priority 1/2 for that detail). `iosApp/fastlane/Fastfile`
gained `build_and_upload_testflight` (archive + sign + upload, target-scoped build-number bump via the
`xcodeproj` gem to avoid `agvtool`'s all-targets bug), `create_app_store_version`, `apply_relaunch_aso_copy`,
and `upload_screenshots_direct`. Build 6 shipped the ASO relaunch; build 7 added a profile-settings
keyboard UX fix (`b44a46f`, `commonMain`, applies to both platforms). `1.2.2` sits in
`PREPARE_FOR_SUBMISSION` — no `submit_for_review` call has been made; it is not visible on the public
App Store. `1.2.1 (3)` remains the live production build.

**v1.2.2 (7) submitted to App Review** (2026-09-19): `WAITING_FOR_REVIEW`. Prepared and submitted entirely via
fastlane, two new lanes in `iosApp/fastlane/Fastfile`: `prepare_submission version:1.2.2 build:7` (attaches the
build, sets What's New and review notes from `fastlane/release_notes/1.2.2/`; refuses a build that isn't `VALID`
or a version that isn't editable) and `submit_for_review version:1.2.2 confirm:true` (reviewSubmissions API;
refuses without `confirm:true`). Pre-submit state verified live: build 7 `VALID`, export compliance
(`usesNonExemptEncryption=false`) set, 8 captioned screenshots `COMPLETE` in the `APP_IPHONE_65` set (the
`APP_IPHONE_58` set is empty — not needed while 6.5" is supplied), editable appInfo already carried the new
name `PayslipMax: Payslip Manager` / subtitle `Salary, Tax & DSOP Insights`, and keywords/description from the
ASO relaunch. Gaps found and fixed before submitting: no build attached, `whatsNew` empty, and review notes
were stale copy from 1.2.1. Release notes deliberately omit the privacy-overlay and backup work (the iOS overlay
change was an SSOT alignment, and backup shipped in 1.2.1) so the copy claims only what is new on iOS.
`releaseType` is `AFTER_APPROVAL`, so it auto-releases on approval. No IAP change in this version, so the
subscription is not part of this submission. If rejected, fix and resubmit via the same two lanes.

**Status (2026-09-19 → 2026-09-20): `1.2.2 (7)` APPROVED and RELEASED** (confirmed by the owner, on-device verification complete). 1.2.2 is now `READY_FOR_SALE` on the App Store.

**Status (2026-09-20): `1.2.3 (1)` on TestFlight (processing `VALID`), pending internal test** — uploaded via
`fastlane ios build_and_upload_testflight bump_version:1.2.3`. It is `1.2.3`, not `1.2.2 (8)`: the first attempt at `1.2.2 (8)`
was rejected by App Store Connect (`Invalid Pre-Release Train … '1.2.2' is closed for new build submissions`, 90186) because
1.2.2 is already `READY_FOR_SALE`; a released version's train is closed, so the fix needed a new marketing version. Same shared
Report an Issue fix as Android v14 (dialog + privacy notice → `mailto:` to `founder@ai-borne.in`, falls back to the share sheet if
the URL can't be opened; bug-report-only redaction of PAN/amounts/account numbers/emails). Details:
`docs/Launch/06_closed_testing_progress_log.md` (versionCode 14). iOS gate green locally (full `iosSimulatorArm64Test`, framework
link). **Unverified on iOS:** simulator (no Mail → share-sheet fallback) and a real iPhone with Mail; `iosX64Test` on CI pending.

**Status (2026-09-21): `1.2.3 (2)` on TestFlight (processing `VALID`), pending the device gates below** — uploaded 17:23 via
`fastlane ios build_and_upload_testflight` (no `bump_version`; build 1 → 2 of the still-open `1.2.3` train, confirmed via
`testflight_builds`: `2 (1.2.3)` and `1 (1.2.3)` both `VALID`). Built from `50445866` (LTC / leave-encashment paycodes on top of the
tech-debt list below); ships with Android versionCode 15 (`docs/Launch/06_closed_testing_progress_log.md`). Same Gradle gate as Android
was green before archiving. **dSYM handled by the lane:** the archive's `PayslipMax.app.dSYM.zip` was uploaded to Crashlytics
("Successfully uploaded dSYM files to Crashlytics") *before* the TestFlight upload, and its UUID `7D601080-0B36-3C4C-A9A6-71EB44B857A7`
was checked equal to the shipped binary's (`dwarfdump --uuid` on the extracted IPA vs the dSYM). Kotlin is statically linked into that
binary, so no separate Kotlin dSYM exists. **Gaps, recorded not fixed:** the vendor `CLiteRTLM.framework` ships no dSYM of ours, so a crash
inside it stays unsymbolicated; and a `dwarfdump --debug-info` scan of the dSYM found no `.kt` compile units, so Kotlin frames likely
symbolicate by symbol name only, not file:line (not confirmed against a real Kotlin crash). The Crashlytics test crash is now
debug/TestFlight-only, so it can verify this on this build. `1.2.3 (1)` remains on TestFlight, superseded.

**Shipped in `1.2.3 (2)` (2026-09-21) — none of it was in `1.2.3 (1)`:** the tech-debt work (`docs/Plan/08_01_TechDebt_Explanation`)
was committed after the `(1)` upload. iOS-visible changes:
- Developer Sandbox gated to debug/TestFlight builds (`shouldShowDeveloperSandbox`); it must still appear after 7 taps on TestFlight.
- Firebase anonymous sign-in, the token bridge and both `FirebaseAuth` Xcode product refs removed.
- Room destructive-migration fallback removed: an unmigratable database now fails loudly instead of wiping payslips.
- `EXCLUDED_ARCHS[sdk=iphonesimulator*] = x86_64` so generic-simulator builds link (`CLiteRTLM.xcframework` is arm64-only).
- Gemma installer is now a Koin `single` and `install()` collapses overlapping ODR triggers (tech-debt 1.2). The Swift bridge
  (`GemmaOnDemandResourceBridge.swift`) is unchanged.

Device gates for `1.2.3 (2)` (all still open, unverified on a real iPhone; the simulator relaunch over an existing database was clean):
1. Clean launch over the existing `1.2.3 (1)` database, i.e. the upgrade path the strict Room policy now depends on.
2. Sandbox appears after 7 taps. **✅ Passed on TestFlight `1.2.3 (2)` (owner-reported, 2026-09-21).** It closes when the user leaves Settings and
   reappears after 7 more taps: unlock state is `remember { mutableStateOf(false) }` in `SettingsScreen.kt`, per screen visit and never
   persisted. That predates the tech-debt work (present before July 2026) and is left as is, since the section holds destructive actions.
3. Crashlytics still receives events with auth removed.
4. Gemma model banner: fresh install progresses to Installed; background and foreground mid-download; force a failure (airplane
   mode) and confirm the banner's retry starts a new fetch.

**Next steps (as of 2026-09-21; supersedes the 2026-09-19 list, whose `1.2.2 (7)` review question is closed — it was approved and released):**
1. Internal-test `1.2.3 (2)` (`(1)` is superseded; release commit `5ff62394`) on a real iPhone with Mail: Report an Issue should open a
   pre-filled compose sheet with the redaction applied. The simulator only exercises the share-sheet fallback.
2. ~~Cut the next TestFlight build~~ Done 2026-09-21 as `1.2.3 (2)`. Run the four device gates listed above on it.
3. Submit the build that passes with the same two lanes (`prepare_submission`, then `submit_for_review confirm:true`). While
   `1.2.3` is not yet `READY_FOR_SALE` its train is still open to new builds; once it is released, any further change
   needs a new marketing version (see the 90186 rejection above). A metadata-only rejection needs no new build.

---

## Rejection-avoidance checklist (carried through every phase, source: RevenueCat's App Store
rejection guidance)

- [ ] Never submit an IAP-bearing build without a completed sandbox purchase first (Phase 7 gates
      Phase 8 on this).
- [ ] ASC product status is "Ready to Submit" before any submission referencing it (Phase 2).
- [ ] Production RevenueCat key (`appl_...`), not the sandbox `test_...` key, ships in the
      submitted binary (Phase 4, with a regression test).
- [ ] Restore Purchases is present and functional on the paywall (Phase 5/7).
- [ ] Purchase-flow failure paths (cancel, offline) degrade gracefully, not crash/hang (Phase 7).
- [ ] Entitlement unlocks the gated feature synchronously on purchase success — no dead-end where
      payment succeeds but content stays locked (Phase 7) — this is RevenueCat's #3 listed rejection
      cause.

---

## Android mirror status (2026-09-26)

This plan is iOS-only, but Android's Phase 3 (RevenueCat wiring) and Phase 7 (test purchase) equivalents
are done — no code change was needed, the existing debug `DevOverride` (Settings → Developer · Premium
Override → Force Free) reaches the real paywall. Details of the wiring are in
[06_closed_testing_progress_log.md](06_closed_testing_progress_log.md).

**Verified on a Pixel 9, debug build at versionCode 15 (Play-signed build uninstalled first):**

- **Product fetch** — paywall shows `₹999.00` (no hardcoded fallback exists any more; only "Pricing
  unavailable"), so the Play product resolved through `Offering.annual` (`$rc_annual`).
- **Purchase** — Play sheet said "Test card, always approves … You will not be charged" (license-tester
  account); "Payment successful". A debug-signed APK *did* work with Play Billing because package and
  versionCode match a published build — no new internal-track build was needed.
- **Server-side entitlement** — RevenueCat customer (Sandbox data): "Started a subscription of
  `payslipmax_yearly_premium:yearly` for INR 999 from offering default"; entitlement `PayslipMax Premium`
  → Active (test subscriptions renew every 30 min).
- **Restore on a fresh state** (`pm clear`) — sheet auto-dismissed, `Purchase history retrieved`, no error.
- **Failure path (offline)** — restore returned `NetworkError` ("Error performing request"); sheet stayed
  open, buttons re-enabled, no crash or hang. Tapping Unlock offline also did not crash or hang.

**Not verified (do not record as passed):** cancel mid-purchase (subscription already active); gate-level
unlocking (`FORCE_FREE` and `FREE_LAUNCH_MODE_ANDROID = true` both short-circuit `hasAccess`, and the
"Everything Included" card is bound to the flag, not the entitlement); the inline offline error text
itself (not captured in the UI dump — only the SDK log line was); Google developer notifications topic.

**Gotchas:** the paywall sheet blocks `screencap` (black image) — read it via `uiautomator dump`. The Play
purchase-verification prompt after a purchase cannot be dismissed with Back; it needs an owner choice.
The Pixel is on wireless adb, so `svc wifi disable` drops the connection — use USB (`adb -s 4A231VDAQ0001D`).
Carried debt: stale header comment in `RevenueCatApiKey.kt` (says Android ships the Test Store key).
