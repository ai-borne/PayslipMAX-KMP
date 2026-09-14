# Platform-Independent Monetization Rollout (v1.1 → v1.2/v1.3)

**Date**: September 11, 2026
**Supersedes**: nothing — this extends [05_launch_strategy_and_resolution.md](05_launch_strategy_and_resolution.md)
Section 4/5 (Step 4 / "Phase 2") with a platform-decoupled sequencing decision, and reflects the
current state after [06_closed_testing_progress_log.md](06_closed_testing_progress_log.md).

## 0. Current state (2026-09-13)

- **iOS**: `v1.1.1` cleared App Review (`READY_FOR_SALE`) on 2026-09-12. Per
  [08_ios_monetization_phaseplan.md](08_ios_monetization_phaseplan.md) Phase 1 (committed `8059f04`
  on `release/ios-1.0.0-v6`), `LaunchFlags.FREE_LAUNCH_MODE` is now split into
  `FREE_LAUNCH_MODE_IOS`/`FREE_LAUNCH_MODE_ANDROID` per Section 2 below — both still `true`, no
  observable behavior change yet. Phase 2 (App Store Connect subscription product) is complete as
  of 2026-09-12: `payslipmax_yearly_premium` (₹199/yr, 1 Year Upfront) exists in ASC with
  metadata/screenshot/review notes filled in, status "Ready for Review" — it will go out bundled
  with the `v1.2` submission in Phase 8. Phase 3 (RevenueCat dashboard wiring) is complete as of
  2026-09-12: the Apple App Store app entry now exists in RevenueCat, `payslipmax_yearly_premium` is
  created as a product attached to the `PayslipMax Premium` entitlement and wired into the `default`
  offering's `$rc_annual` package, and the real production RevenueCat SDK key
  (`appl_NgEonbkizWMfsjfyaFCuTgBLWGx`) has been retrieved and is held, uncommitted, for Phase 4. Real
  install count: 1 in the last 60 days (via the fastlane/App Store Connect API tooling added in
  Phase 0), confirming the free-to-paid conversion exposure is still minimal. Phase 4 (real
  RevenueCat API key shipped in code) is complete as of 2026-09-12:
  `RevenueCatApiKey.ios.kt` now returns the real production key, `FREE_LAUNCH_MODE_IOS` remains
  `true` (paywall still dark for real users), and a regression test guards against ever re-shipping
  the sandbox `test_...` key. Phase 5 (paywall wiring verification) is complete as of 2026-09-12:
  the existing paywall (`PremiumFeaturesScreen.kt`) confirmed sourcing real product/price data from
  RevenueCat's SDK, not a mock. Found and fixed a real gap: `RevenueCatBillingManager`'s
  entitlement-ID constant was `"premium"`, but the dashboard's actual entitlement identifier is
  `"PayslipMax Premium"` (with the space) — a real purchase would never have resolved to `Active`
  under the old constant. Fixed surgically; `FREE_LAUNCH_MODE_IOS` still `true`. Phase 6
  (grandfather clause) required no work — it is a recorded decision (**no** grandfathering), not an
  implementation task. **Phase 7 (sandbox purchase verification) is COMPLETE (2026-09-12)**: a
  TestFlight-only paywall override shipped (`4a74192`, `isTestFlightBuild()` + widened `DevOverride`
  eligibility, `FREE_LAUNCH_MODE_IOS` untouched at `true`). The first sandbox purchase failed with
  "Package yearly unavailable" — the code looked up the RevenueCat package by the literal id
  `"yearly"`, but the dashboard package's identifier is `$rc_annual` (`"yearly"` is the Test Store
  *product* inside it), fixed in `e5ae1f2` via the SDK's typed `Offering.annual` accessor. On
  TestFlight `1.1.2 (3)` the whole pipe then verified end to end on-device: live StoreKit price,
  purchase completed (Apple shows the subscription active, renewing 13 Sep 2026), entitlement
  `PayslipMax Premium` confirmed **Active** on the RevenueCat dashboard server-side, restore working
  on a fresh install, and both offline failure paths degrading gracefully with no crash or hang.
  Two sub-criteria are explicitly **not** verified: cancel-mid-purchase (blocked once subscribed),
  and gate-level unlocking, which is unverifiable by construction while `FREE_LAUNCH_MODE_IOS` is
  `true` — `hasAccess` short-circuits in both override positions and the one `isPremiumEnabled`-bound
  card is suppressed by the same flag, so **no in-app UI exposes entitlement state until Phase 8
  flips it**. Tech debt found and fixed en route: `Info.plist` hardcoded `CFBundleVersion` to the
  literal `2`, making `CURRENT_PROJECT_VERSION` dead (`8d2a18f`), and `resolveYearlyPackage()`
  collapsed three distinct failure modes into one opaque error while the hardcoded price fallback
  made a dead product look like a healthy paywall (`e693581`, diagnostics only). Two open gaps carry
  into Phase 8: RevenueCat's separate *App Store Connect API* key slot is empty ("Store Status:
  Could not check") — now shown empirically **not** to block on-device purchasing — and the ASC
  subscription **group** still has no localization, which is required before submission. Full detail
  in doc 08's Phase 7 Phase Summary. **Phase 8 (flip the real flag + submit) is verified but not yet
  submitted, as of 2026-09-13.** `FREE_LAUNCH_MODE_IOS` is now `false` (`bc25900`) — iOS monetization
  is live in code from `v1.2`, Android untouched at `true`. Both Phase 7 gaps are closed on-device
  via TestFlight `1.2 (5)`/`1.2 (6)`: gate-level unlocking verified in **both** directions on two
  independent Apple IDs (the Settings premium card, suppressed until this flag flipped, now renders
  in both states), and cancel-mid-purchase degrades gracefully with no hang. Purchase unlocks the
  gated features with no app restart, Restore shows its confirmation for ~1.5s before the sheet
  closes, and the paywall reads ₹999 with Apple charging ₹999. One account rendered `$9.99` in-app
  while Apple billed ₹999; this was chased to ground and is a **known TestFlight sandbox limitation**
  (StoreKit product metadata defaulting to USD in beta builds), not a defect — ruled out in our code,
  and ruled out on the RevenueCat dashboard, which recorded that transaction as India/₹999. It
  resolves on production release. En route it exposed one real latent bug, fixed in `caf6fe4`: the
  store price was read once in the ViewModel's `init` and never again, so an early or failed read was
  frozen for the session; it is now re-read whenever the paywall is presented. The ASC subscription
  **group localization** that Phase 7 flagged as a submission blocker now exists (`en-US`, "PayslipMax
  Premium"), and the subscription is `READY_TO_SUBMIT` at ₹999/yr (raised from ₹199 before submission).
  Two stale ₹199 artifacts on the subscription itself (its App Review screenshot and its Review Notes
  text) were found and corrected before submission — both independent of the app-side price display
  bug. **Phase 8 is COMPLETE as of 2026-09-13: `v1.2` (build `1.2 (6)`) is submitted to App Review**,
  confirmed via both the ASC UI and the `review_status`/`subscription_status` fastlane lanes —
  `v1.2` and `payslipmax_yearly_premium` both `WAITING_FOR_REVIEW`. Still open (non-blocking):
  RevenueCat's App Store Connect API key slot is empty (harmless to purchasing, proven across two
  accounts). Full detail in doc 08's Phase 8 Phase Summary.
- **Android**: Closed testing, `9 (1.0.0)` live on Internal testing (not yet promoted), `8 (1.0.0)`
  is the live Closed testing release, mandatory 14-day window running against v8. Still free by
  policy requirement, not choice.

## 1. Core decision: decouple the two platforms' monetization timing

Do **not** wait for both platforms to be ready before enabling either. The two are blocked by
completely different things:

- **iOS** has no external blocker left. Everything needed for Step 4 (App Store Connect
  subscription + RevenueCat Apple app/key + StoreKit sandbox verification) is dashboard/console work
  that can start today.
- **Android** has a hard, sequential, policy-driven blocker chain that cannot be worked around or
  sped up by code:

  ```
  Closed testing → Production promotion (must launch FREE — no live public URL exists yet)
    → live public URL exists → submit to BillDesk for merchant KYC
    → BillDesk approves merchant profile → real-money subscriptions can process
    → ship a follow-up release with the paywall enabled
  ```

  Android **cannot** launch to production already paid — Google Play has no verified payments
  merchant profile behind it until BillDesk clears, and BillDesk cannot verify until a live public
  URL exists (which only exists after a free production launch). This was true in Section 2 of doc
  05 and remains true now; there is no way to route around it. The only lever available is
  minimizing how long Android's production app stays free, by front-loading everything that
  *doesn't* require the verified merchant profile.

Holding iOS back to "stay in sync" with Android gains nothing — it only extends iOS's own
free-to-paid conversion problem, which is currently as small as it will ever be (v1.0 has been
live only ~2 days).

## 2. Do we need separate branches per platform? No.

This does **not** require forking the codebase or defeating SSOT. The confusion comes from
`LaunchFlags.FREE_LAUNCH_MODE` currently being a **single shared `const val`** in `commonMain`
(`shared/src/commonMain/kotlin/com/payslipmax/pdfparser/subscription/LaunchFlags.kt`) that both
platforms compile against. Flipping it today would arm Android's paywall too, against an
unverified merchant profile — that's the actual problem, not the branch structure.

**Fix: split the flag per platform, keep it in the same file, on the same branch.**

```kotlin
object LaunchFlags {
    const val FREE_LAUNCH_MODE_IOS: Boolean = true
    const val FREE_LAUNCH_MODE_ANDROID: Boolean = true
}
```

Each platform's `provideBillingManager()`/`SubscriptionManager.hasAccess()` path reads only its own
constant. Both constants live in `commonMain`, in `main`, at the same time — Android's build simply
never looks at the iOS flag and vice versa. This means:

- iOS can flip to `false` and ship v1.2 while Android's flag stays `true`, from the exact same
  commit — no divergent branch needed.
- Android flips to `false` later, from the same `main`, once BillDesk clears.
- SSOT is preserved: there is still exactly one `RevenueCatBillingManager`, one entitlement mapping,
  one `SubscriptionManager.hasAccess()` gate. Only the *timing* of when each platform's build reads
  `false` differs — that's a release-sequencing concern, not a code-duplication one.

A short-lived branch like the existing `release/ios-1.0.0-v6` (cut for a specific submission,
merged/rebased back into `main` once released) is a normal, harmless pattern and unrelated to this
question — it doesn't fork platform logic permanently. What would actually defeat SSOT is a
**permanent**, never-merged-back platform branch that reimplements billing logic separately. Nothing
here calls for that.

## 3. iOS path — proceed now, independent of Android

No dependency on Android or BillDesk. Sequence:

1. **App Store Connect** — create the auto-renewable subscription (`payslipmax_yearly_premium`,
   ₹999/yr), attach review screenshot; confirm the Paid Applications Agreement is active.
2. **RevenueCat dashboard** — add the Apple App Store app under Project Settings > Apps, link the
   App Store Connect API key/shared secret, then attach the Apple product ID to the `premium`
   entitlement and `yearly` package in the Product Catalog.
3. **Code** — pull the real `appl_...` key into
   [RevenueCatApiKey.ios.kt](../../shared/src/iosMain/kotlin/com/payslipmax/pdfparser/billing/RevenueCatApiKey.ios.kt)
   (currently the Phase 0 `test_...` sandbox key — this was the root cause of the original v1.0.0(2)
   Guideline 2.1 rejection). Split `LaunchFlags` per Section 2 above.
4. **Sandbox-verify in TestFlight before submitting** — actually complete a test purchase
   end-to-end. This is the exact step that was skipped last time and caused "Package yearly
   unavailable" to reach an App Reviewer. Do not submit without this.
5. **Existing-install grandfather decision** — pull actual v1.0 install count from App Store Connect
   analytics before deciding. Given the tiny live window so far, a straightforward paywall re-enable
   with no grandfather clause is the likely call, but confirm with real numbers rather than assuming.
6. **Flip `FREE_LAUNCH_MODE_IOS` to `false`**, ship as its own release (v1.2), through App Review —
   entirely independent of Android's state.

## 4. Android path — prep now during closed testing, launch free (forced), fast-follow paid

Cannot skip the free production window (Section 1). What *can* happen now, during the remaining
closed-testing days, without touching the unverified merchant profile:

1. **Play Console** — create the subscription product/base plan under Monetize > Subscriptions now;
   it can exist in draft/active state before the merchant profile is verified. Reusable fastlane
   tooling for the Play Console API already exists at `composeApp/fastlane/` (mirroring
   `iosApp/fastlane/`'s ASC tooling) — `listing_status` reads the current store listing per locale
   (this app's only listed locale is `en-IN`, not `en-US`) and `add_terms_disclosure_to_listing`
   appends a subscription auto-renewal/terms disclosure line, idempotently. Auth via a
   least-privilege service account key at `~/.play_console/payslipmax-fastlane-supply.json`
   (read-only app info + "Manage store presence" only — no release/financial/order permissions).
   See [08_ios_monetization_phaseplan.md](08_ios_monetization_phaseplan.md)'s 2026-09-14 entry for
   the incident that produced this tooling.
2. **RevenueCat dashboard** — attach the Google product ID to the `premium` entitlement / `yearly`
   package (the `PayslipMax (Play Store)` app already exists in RevenueCat per doc 05 Section 4).
3. **Test the purchase flow now** using Play Console **license testers** — this uses Google's
   test-purchase path and does not require the verified merchant profile, so it's safe to do
   mid-closed-testing.
4. **Have BillDesk paperwork ready in advance** (PAN, bank account proof, video KYC scheduling) so
   it can be submitted the moment a live public URL exists — don't wait until after production
   promotion to start that conversation.
5. Once Day 14 completes and production promotion happens (per
   [06_closed_testing_progress_log.md](06_closed_testing_progress_log.md) checklist): submit the
   live URL to BillDesk immediately.
6. Once BillDesk approves the merchant profile: flip `FREE_LAUNCH_MODE_ANDROID` to `false` and ship
   the paywall-enabled release. Android's real production users only ever see a free window for as
   long as BillDesk KYC takes — everything else is pre-staged.

## 5. Why this order doesn't create a grandfathering problem on Android

Android has **zero production users today** — only closed testers. If steps in Section 4 are
front-loaded correctly, the free-in-production window is short and BillDesk-processing-time-bound
only. There is no large free install base to grandfather on Android by the time the paywall ships,
unlike iOS (which already has real free users from v1.0). Re-evaluate this only if the free
production window ends up stretching for weeks — if a meaningful Android install base accumulates
before BillDesk clears, revisit the grandfather-clause question the same way doc 05 flagged for iOS.

## 6. On Branching

We don't need separate branches — that would actually be the wrong fix. The real issue isn't branch structure, it's that FREE_LAUNCH_MODE is currently one shared const val. Split it into two — FREE_LAUNCH_MODE_IOS and FREE_LAUNCH_MODE_ANDROID — both still living in the same commonMain file, same branch, same commit. Each platform's build only reads its own constant, so you can flip iOS to false while Android stays true, from identical source. SSOT is fully preserved — there's still exactly one RevenueCatBillingManager, one entitlement mapping, one gating function; only the release-timing knob is now per-platform instead of global. A temporary release branch (like your existing release/ios-1.0.0-v6) is fine and unrelated to this — what would actually defeat SSOT is a permanent, never-merged platform fork that duplicates billing logic, which nothing here calls for. Make the LaunchFlags split, so as to launch iOS app monetization.
