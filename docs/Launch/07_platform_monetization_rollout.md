# Platform-Independent Monetization Rollout (v1.1 → v1.2/v1.3)

**Date**: September 11, 2026
**Supersedes**: nothing — this extends [05_launch_strategy_and_resolution.md](05_launch_strategy_and_resolution.md)
Section 4/5 (Step 4 / "Phase 2") with a platform-decoupled sequencing decision, and reflects the
current state after [06_closed_testing_progress_log.md](06_closed_testing_progress_log.md).

## 0. Current state (2026-09-16)

- **iOS**: `v1.2` (build `1.2 (6)`), auto-renewable subscription `payslipmax_yearly_premium`
  (₹999/yr), and subscription group `PayslipMax Yearly Premium` are live in production on the
  iOS App Store (`READY_FOR_SALE`). Monetization is active (`FREE_LAUNCH_MODE_IOS = false`). Next
  maintenance update **`v1.2.1 (3)`** has been submitted from TestFlight and is actively **published**,
  carrying the Gemma ODR fix, Universal Backup & Restore cross-platform interoperability, in-app privacy
  policy link fix, and document in-place safety.
- **Android** (updated 2026-09-24): the mandatory 14-day Closed testing window completed and Google
  granted production access. **`15 (1.0.0)`** has been promoted to the **production** track at 100%
  rollout and is **live on the Google Play Store** (confirmed by the owner 2026-09-24; see
  [06_closed_testing_progress_log.md](06_closed_testing_progress_log.md)). The live public URL now exists,
  so **the next action is submitting it to BillDesk for merchant KYC** (Section 4 step 5). Still free —
  `FREE_LAUNCH_MODE_ANDROID = true` — until BillDesk approves. **Update 2026-09-26:** the Play subscription
  `payslipmax_yearly_premium` with base plan `yearly` (auto-renewing, INR 999.00/yr, ACTIVE) was created
  in Play Console **without any BillDesk block** — setup and activation do not need merchant
  verification. Real charging still presumably does (unconfirmed until BillDesk clears). Verify any time
  with `fastlane android subscription_status` (read-only). Remaining: attach the product in RevenueCat
  (`payslipmax_yearly_premium:yearly` → `premium` entitlement / `yearly` package), add license testers,
  test on the Pixel with a debug paywall override.

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

  **Status (2026-09-24): the first two arrows are done.** Production promotion happened (versionCode 15,
  100% rollout) and the app is live on Google Play, so the public URL exists. The chain is unblocked;
  the next step is the BillDesk submission — this is no longer a deadlock, just a queue.

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
   **Done 2026-09-26:** subscription `payslipmax_yearly_premium` / base plan `yearly`, INR 999.00.
2. **RevenueCat dashboard** — attach the Google product ID to the `PayslipMax Premium` entitlement and
   the `$rc_annual` package of the `default` offering (the `PayslipMax (Play Store)` app already
   exists in RevenueCat per doc 05 Section 4). **Done 2026-09-26** (service-account credentials valid;
   product imported, attached, added to the package). The package id is `$rc_annual`; the code
   resolves it via `Offering.annual`, so `"yearly"` is only the product/base-plan id.
3. **Test the purchase flow now** using Play Console **license testers** — this uses Google's
   test-purchase path and does not require the verified merchant profile, so it's safe to do
   mid-closed-testing.
4. **Have BillDesk paperwork ready in advance** (PAN, bank account proof, video KYC scheduling) so
   it can be submitted the moment a live public URL exists — don't wait until after production
   promotion to start that conversation.
5. **Done (2026-09-24): Day 14 completed and production promotion happened** (per
   [06_closed_testing_progress_log.md](06_closed_testing_progress_log.md)) — versionCode 15 is on the
   production track and **live on Google Play**. **Next action (now):** submit the
   live URL (`https://play.google.com/store/apps/details?id=in.aiborne.payslipmax`) to BillDesk
   immediately — don't wait for a "convenient" moment, this is the actual unblock this whole doc has
   been sequencing toward.
6. Once BillDesk approves the merchant profile: flip `FREE_LAUNCH_MODE_ANDROID` to `false` and ship
   the paywall-enabled release. Android's real production users only ever see a free window for as
   long as BillDesk KYC takes — everything else is pre-staged.

## 5. Why this order doesn't create a grandfathering problem on Android

Android has **zero production users today** (updated 2026-09-24: versionCode 15 just went live, so a
small real install base starts accumulating from here) — only closed testers. If steps in Section 4 are front-loaded correctly, the
free-in-production window is short and BillDesk-processing-time-bound only. There is no large free install base to grandfather on Android by the time the paywall ships,
unlike iOS (which already has real free users from v1.0). Re-evaluate this only if the free
production window ends up stretching for weeks — if a meaningful Android install base accumulates
before BillDesk clears, revisit the grandfather-clause question the same way doc 05 flagged for iOS.

## 6. On Branching

We don't need separate branches — that would actually be the wrong fix. The real issue isn't branch structure, it's that FREE_LAUNCH_MODE is currently one shared const val. Split it into two — FREE_LAUNCH_MODE_IOS and FREE_LAUNCH_MODE_ANDROID — both still living in the same commonMain file, same branch, same commit. Each platform's build only reads its own constant, so you can flip iOS to false while Android stays true, from identical source. SSOT is fully preserved — there's still exactly one RevenueCatBillingManager, one entitlement mapping, one gating function; only the release-timing knob is now per-platform instead of global. A temporary release branch (like your existing release/ios-1.0.0-v6) is fine and unrelated to this — what would actually defeat SSOT is a permanent, never-merged platform fork that duplicates billing logic, which nothing here calls for. Make the LaunchFlags split, so as to launch iOS app monetization.
