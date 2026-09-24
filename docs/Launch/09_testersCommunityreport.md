# 09 — Testers Community Reports (Closed Testing, Day 10)

Source: three reports from Testers Community, received on day 10 of Android closed testing,
located at `/Users/sunil/Downloads/PayslipMax ASO Reports/`:

- `in.aiborne.payslipmax_aso_score.pdf` — ASO store-listing audit (70/100, grade B)
- `in.aiborne.payslipmax_feedback.pdf` — device/functionality testing report
- `in.aiborne.payslipmax_production.pdf` — draft answers for Google's production-access questionnaire

**Note on source quality:** the feedback report is generic boilerplate — it reports zero
device-specific findings ("no crashes, no bugs, everything worked as intended") and its 4
recommendations just restate points already covered by the ASO report. The ASO report and the
production questionnaire are the two documents with concrete, actionable content.

## READ FIRST — SSOT rule for store listing copy (both platforms)

This app ships on Google Play (`in.aiborne.payslipmax`, Android) **and** the Apple App Store
(`in.aiborne.payslipmax`, iOS) under the same identity. Store listing copy — app name/title,
subtitle/short description, and full description — must stay a **single source of truth**
across both platforms, adapted only for each store's field constraints. This mirrors the
project's SSOT rule for in-app config objects (see root `CLAUDE.md`), applied to store metadata.

**Before changing store listing copy on either platform, an agent must:**
1. Read this section and the "Shipped copy, for reference" section below for the current
   canonical copy.
2. Change both platforms in the same session — Play via `composeApp/fastlane/Fastfile`
   (`play_service`, `listing_status`, `apply_aso_relaunch_listing` pattern), iOS via
   `iosApp/fastlane/Fastfile` (`asc_api_key`, `app_description`, `add_eula_link_to_description`
   pattern, extended with equivalent name/subtitle/keywords/description lanes).
3. Adapt for each store's field limits, not duplicate verbatim:
   - Play title ≤30 chars vs. iOS App Name ≤30 chars + separate Subtitle ≤30 chars.
   - iOS has a dedicated Keywords field (≤100 chars, comma-separated, not user-visible) with no
     Play equivalent — Play relies on keyword density inside the visible title/description.
   - iOS App Name/Subtitle live in `appInfoLocalizations` (App Information); iOS Keywords/
     Description/Promotional Text live in `appStoreVersionLocalizations` (tied to a specific,
     editable app version) — confirm the current version is in an editable state (e.g.
     "Prepare for Submission" / "Waiting for Review", not "Ready for Distribution") before
     attempting a write, same way Play requires an open `edit` before `commit_edit`.
   - Full description body/structure (sections, FAQ, tone) should match across platforms; only
     the character budget differs (Play 4000 vs. ASC 4000 — same limit, but ASC keywords field
     is a separate consideration Play doesn't have).
4. Log the shipped copy for both platforms in this doc's reference section, same as the Android
   entry below, so the next agent has both as ground truth.
5. Confirm with the user (dry-run diff first) before committing live to either store — same
   safety bar as any public-facing listing change.

## Priority 1 — Store listing text (no code changes) — Android DONE, iOS partial (2026-09-17)

- [x] **Add "payslip" to the app title** — highest-leverage single fix (+5 pt on ASO score).
      Title previously didn't contain the primary keyword and only used 10/30 characters.
      Shipped: `PayslipMax: Payslip Manager` (27/30 chars).
- [x] **Rewrite the full description**:
  - Expanded from 1,659 → 3,420 characters (Play indexes this field for search).
  - Broken into headed sections with bullets (was previously a wall of text).
  - Removed all 7 emoji; professional tone throughout (fits the PCDA(O)/defence audience).
  - Added a "Who PayslipMax is for" use-case section and an FAQ section for extra keyword
    surface and conversion copy.
  - Fixed a pre-existing typo ("fpayslips" → "payslips") and corrected deduction terminology
    to match actual PCDA(O) payslips ("PF, NPS" → "DSOP, Taxes, AGIF, Arrears etc").
  - Subscription auto-renewal disclosure line preserved verbatim (compliance requirement).

Shipped live to the `en-IN` Play Console listing via a new `apply_aso_relaunch_listing` fastlane
lane (`composeApp/fastlane/Fastfile`), reusing the existing `play_service`/edit-commit pattern
from `add_terms_disclosure_to_listing`. Run with `fastlane android apply_aso_relaunch_listing`
from `composeApp/`; verify anytime with `fastlane android listing_status`.

Expected effect: ASO score moves from 70/100 (B) toward high 80s/90s. Policy compliance was
already 100%, so no compliance risk from these changes.

### Shipped copy, for reference

**Title:** `PayslipMax: Payslip Manager`

**Short description:** unchanged — `Privacy-first PCDA(O) payslip parser, salary analysis & tax
planning calculator.` (already scored 4/4 on both short-description checks, no edit needed)

**Full description:**

```
PayslipMax is a privacy-first, on-device salary and payslip management application designed to help PCDA(O) users to understand earnings, verify deductions, and optimize tax savings with total security.

100% ON-DEVICE PRIVACY, ZERO CLOUD UPLOADS
Your payslips never leave your phone. All PDF parsing, salary computations, and AI insights are performed locally on your device with zero server uploads. Nothing is sent to any server, ever.

KEY FEATURES

Smart Payslip Parsing
- Instantly read and parse complex PCDA(O) payslip PDFs with high accuracy.
- Automatically organize earnings (Basic Pay, DA, HRA, Allowances) and deductions (DSOP, Taxes, AGIF, Arrears etc).
- Interactive digital replica viewer to inspect and audit your original payslip layout, line by line.

In-Depth Salary Breakdown and Trends
- Track month-over-month income variations, net salary growth, and deduction changes over your entire service.
- Visual charts for earnings vs. deductions distribution, so you can see exactly where your money goes.
- Intelligent Anomaly Detection automatically flags a missing allowance or an unexpected deduction spike.

Intelligent Tax and Savings Calculators
- Compare Old vs. New Tax Regimes with accurate, updated slabs for the current financial year.
- Calculate income tax liabilities, standard deductions, and 80C/80D investment impacts.
- DSOP Wealth Simulator: project your retirement fund growth with interactive interest projections, compounding curves, and custom contribution scenarios.

Private On-Device AI Insights
- Contextual analysis of salary components and personalized tax optimization opportunities.
- Powered by on-device intelligence, with your private financial data never shared with third parties.

Security You Can Trust
- Secure local biometric or PIN app lock protection.
- No account registration or external tracking required.
- AES-256 encryption for all stored payslip data.

WHO PAYSLIPMAX IS FOR
PayslipMax is built for serving PCDA(O)-administered users who want clarity on their pay without handing financial documents to a third-party service. It is equally useful for:
- A user posted to a new unit who wants to quickly confirm a changed allowance or HRA slab is reflected correctly.
- Personnel comparing DSOP balances month over month ahead of retirement planning.
- Anyone deciding between the Old and New Tax Regime who wants a clear estimate based on their own payslip figures rather than generic examples.
- Families who track household salary trends but do not want payslip PDFs stored in email or cloud drives.

FREQUENTLY ASKED QUESTIONS

Is my payslip data uploaded anywhere?
No. PayslipMax parses your PDF entirely on your device. No payslip, PDF, or extracted salary figure is ever uploaded to a server.

Which payslip formats are supported?
PayslipMax is purpose-built for PCDA(O) payslip formats and is regularly updated as formats change across statement periods.

Which tax regimes are supported?
Both the Old and New Tax Regimes are supported, with slabs kept current for the applicable financial year.

Do I need an account to use the app?
No account or sign-in is required. Your data stays on your device, tied to your device alone.

What if the app misreads a value from my payslip?
You can review and correct any parsed field directly in the app; your correction is saved without altering the original parsed document.

PayslipMax Premium is an auto-renewing subscription; see Terms of Use and Privacy Policy for billing and cancellation details.
```

### iOS App Store — divergence found, partial sync done (2026-09-17)

Pulling the live iOS listing (`fastlane ios ios_listing_status` in `iosApp/`) surfaced that iOS and
Android copy had **already diverged** — iOS had its own distinct, decent description (naming
"DSOP Wealth Simulator" and "Intelligent Anomaly Detection" as features) that was never ported to
Android, and vice versa the Android relaunch above was never ported to iOS. Decision: merge the
best of both into one canonical description (now folded into the Android text above — the two
DSOP/Anomaly bullets came from iOS) and treat *that* as the SSOT going forward for both stores.

**Apple constraint that shapes the rollout plan:** unlike Play (which lets you open a listing
`edit` and commit it anytime), Apple locks **App Name, Subtitle, Keywords, and Description** to
the currently *editable* app version (e.g. "Prepare for Submission" / "Waiting for Review"). The
live iOS version (`1.2.1`, state `READY_FOR_DISTRIBUTION`) is not editable for those fields — they
require a new version submission to change. **Promotional Text is the one exception**: Apple
allows editing it on a live version without a new build/review, so that shipped immediately.

**Shipped now** (live, via new `update_promotional_text` lane in `iosApp/fastlane/Fastfile`, run
with `fastlane ios update_promotional_text`):
- Promotional Text: was `Turn your snooze-fest PCDA(O) payslips into powerful financial insights.`
  → now `Turn your PCDA(O) payslip into clear salary, tax and DSOP insights, 100% on-device.`

**Shipped 2026-09-18, to TestFlight-only `1.2.2` — not yet submitted for App Store review.** The next
iOS build needed for TestFlight testing (`1.2.1 (3)` was already `READY_FOR_SALE`, so its pre-release
train was closed to new builds — Apple rejected a same-version upload attempt) turned out to be the
"next real, code-driven app update" this section said to wait for, so the queued copy below shipped
into it rather than sitting queued further:
- App Name: `PayslipMax` → `PayslipMax: Payslip Manager` (27/30 chars, matches Android title)
- Subtitle: `Payslip details in your pocket` → `Salary, Tax & DSOP Insights` (27/30 chars,
  keyword-focused instead of tone-focused — the old subtitle contributed no extra indexed terms)
- Keywords: `payslip, salary, defense payslip, tax planner, dsop, army,payslip parser, income tax`
  → `payslip,salary,payslip manager,tax planner,dsop,army,payslip parser,income tax,salary tracker`
  (93/100 chars)
- Description: same canonical text as the Android full description above (3,605 chars live, fits
  ASC's 4,000-char limit) — includes the standard EULA link line Apple's Guideline 3.1.2 fix
  already relies on, so it wasn't dropped.

**How it shipped:** `iosApp/fastlane/Fastfile` gained `create_app_store_version` (opens a new
`PREPARE_FOR_SUBMISSION` version — does not submit for review) and `apply_relaunch_aso_copy`
(dry-run diff by default, `apply:true` to write; PATCHes `appInfoLocalizations` for Name/Subtitle
and `appStoreVersionLocalizations` for Keywords/Description in one lane). Dry-run diff was shown
and confirmed before applying. `1.2.1` (`READY_FOR_SALE`) is completely untouched by this — the new
copy lives only on the `1.2.2` version object, which stays unsubmitted until an explicit future
`submit_for_review` action.

## Priority 2 — Screenshots

- [x] Redo Play Store screenshots to be feature-focused with short captions/annotations
      (smart parsing, salary breakdown, tax insights) instead of plain screens. Flagged
      independently by both the ASO report and the feedback report. **Shipped live 2026-09-17**
      (Android). **iOS shipped 2026-09-18** to TestFlight-only `1.2.2` (see resolution below) —
      both platforms now done.

Scope decision (2026-09-17): both platforms, in the same pass — Android via a Play Console
screenshot upload, iOS screenshots via App Store Connect (screenshots aren't locked to an
editable-version state the way App Name/Subtitle/Keywords/Description are, so this doesn't need
to wait for the next iOS version submission like the queued copy above does).

Production split: user supplies the raw device screenshots (real parsed-payslip screens, scrubbed
data only, no real PII); the agent designs the caption/annotation overlay layer and prepares the
upload lanes. Same SSOT spirit as the listing-copy rule above — caption *messaging* (what each
screenshot claims) should be consistent across platforms even though each store's screenshot
pixel dimensions/aspect ratios differ and are handled separately per store.

### Phase-wise plan

**Phase 0 — Baseline & spec.** Confirm current live screenshot sets on both stores (count, order,
dimensions) via `fastlane android listing_status` / `fastlane ios ios_listing_status` (extend
either lane if it doesn't already surface screenshot metadata). Confirm required Play/ASC
dimensions for each device class being targeted. No asset changes. Exit: current-state documented,
target spec confirmed with user.

**Phase 1 — Raw screenshots.** User supplies 3 raw screens per platform (Smart Parsing result,
Salary Breakdown/Trends, Tax Insights/regime comparison), captured from real app UI with a scrubbed
fixture — never a real payslip. Agent verifies each image is clean (no debug UI, no real PII,
correct device frame/resolution) before moving on.

**Phase 2 — Caption/annotation design.** Draft one short benefit-led caption per screenshot (e.g.
"Every deduction, decoded" / "Old vs New regime, instantly") — same three messages reused across
both platforms' screenshots, adapted only for each store's caption space convention. Overlay design
pulls colors/type from `Theme.kt` tokens for visual consistency with the in-app UI (no hardcoded
ad-hoc colors, matching the project's resource-management rule even though these are store assets,
not app code). Dry-run the composited images for user sign-off before any upload.

**Phase 3 — Upload tooling.** Extend `composeApp/fastlane/Fastfile` and `iosApp/fastlane/Fastfile`
with screenshot-upload lanes, mirroring the existing `apply_aso_relaunch_listing` /
`add_eula_link_to_description` edit-commit patterns — reusable automation, not a one-off manual
Play Console / ASC dashboard upload.

iOS done (2026-09-17): `iosApp/fastlane/Fastfile` gained two lanes reusing fastlane's own
battle-tested `deliver` screenshot uploader rather than hand-rolling the raw ASC API's
reserve/checksum/multipart-upload screenshot flow —
- `stage_screenshots source:<folder>` — copies numerically-prefixed captioned PNGs (order =
  upload order) into `fastlane/screenshots/<locale>/` (gitignored staging dir, already covered by
  the repo's `.gitignore`). Dry-run/inspectable before anything touches ASC.
- `upload_screenshots locale:<locale>` — calls `deliver` with `skip_metadata`/`skip_binary_upload`/
  `submit_for_review: false` so it touches *only* the screenshot set for that locale, nothing else.

Verified: `stage_screenshots` run against the 8 approved captioned PNGs (source captured from
`ios_6.5_display_screenshots copy/`, composited via an HTML/CSS template using `Theme.kt`'s color
tokens, rendered at exact 1284×2778 through a headless-browser pipeline) — all 8 copied correctly
in order to `fastlane/screenshots/en-US/`.

`upload_screenshots` attempted live (2026-09-17) and **blocked**: `deliver`'s screenshot uploader
(`Deliver::UploadScreenshots#upload`) always calls `app.get_edit_app_store_version` and errors
("Could not find a version to edit") if none exists — it will not touch a `READY_FOR_SALE` live
version regardless of `use_live_version:`. All 5 iOS versions (`1.2.1` down to `1.0`) are
`READY_FOR_SALE`; none is in an editable pre-submission state. **This is the same restriction the
doc already found for App Name/Subtitle/Keywords/Description above — Apple applies it to
screenshots too** (this corrects the plan's earlier assumption that screenshots were the
exception). Practically: screenshots can't ship until the next iOS version is opened for
submission, same gate as the queued copy changes — so bundle both into that same submission rather
than treating them as separate events.

Checked 2026-09-17 against `08_ios_monetization_phaseplan.md`: all 5 iOS versions (`1.0` through
`1.2.1`) are `READY_FOR_SALE` — `1.2.1` build 3 (the Backup/Restore + `privacyPolicyUrl` fix)
already cleared review and shipped, so it wasn't an open window either. The phase plan has no
further phase scheduled (Phase 9/Distribution is the last one, currently in post-launch
monitoring) — there is no next build currently planned to carry this. **Decision (2026-09-17,
confirmed with user): wait for the next real, code-driven app update** (a bug fix or feature that
needs a new version anyway) and bundle the queued ASO copy + these 8 screenshots into that
submission, rather than opening a metadata-only version now just to ship marketing assets. Next
agent picking this up: check `fastlane ios review_status` first — if a new version is open for any
other reason, that's the trigger to also apply this queued copy/screenshots, not a separate event.

**Resolved 2026-09-18:** the next TestFlight build needed for testing (`1.2.2`) turned out to be
exactly that "next real update" trigger — see the Priority 1 "Shipped 2026-09-18" note above for
why `1.2.2` had to open in the first place (Apple closed `1.2.1`'s pre-release train once it went
`READY_FOR_SALE`). `deliver`'s `upload_screenshots` lane was tried again against the newly-opened
`1.2.2` version and **still uploaded nothing** — it logged "Successfully uploaded screenshots" both
with and without `overwrite_screenshots: true`, but ASC showed 0 screenshots landing either time
(root cause not fully diagnosed — `deliver`'s folder/device-type auto-detection silently no-op'd
against this project's flat-locale `fastlane/screenshots/<locale>/*.png` layout). Also found: Apple
auto-copies the *previous* version's screenshots onto a newly created version — `1.2.2` inherited
1.2.1's old, uncaptioned raw screenshots (`IMG_9762.png` etc.) by default, which would have shipped
silently if untouched.

Replaced with a new lane, `upload_screenshots_direct`, calling `Spaceship::ConnectAPI::AppScreenshot.create`
directly (the same lower-level library `deliver` itself wraps) instead of going through `deliver`'s
uploader. Also added `clear_screenshot_set` (deletes an `appScreenshotSet`'s contents — used first
to clear Apple's auto-copied stale screenshots) and `create_app_store_version` (see Priority 1).
`upload_screenshots` is kept in the Fastfile marked DEPRECATED, for reference only — do not use it.

Verified live: all 8 approved captioned screenshots below uploaded to the `APP_IPHONE_65` set on
`1.2.2` and reached `COMPLETE` processing state. The `APP_IPHONE_58` set (5.5"/8-Plus-class) was
cleared of its stale auto-copied screenshots but left empty — this project only ever composited the
6.5" canvas, so there's nothing correctly-sized to put there yet; worth checking before any actual
submission in case that display class is a mandatory slot.

Approved screenshot set + order (light/dark alternating for variety, parsing/privacy leads):
1. Digital Replica, light — "Every payslip line, explained"
2. Digital Replica, dark — "Your data never leaves your device"
3. Dashboard, light — "Your salary, decoded at a glance"
4. Tax Planner — "Know what you'll really owe"
5. Anomaly Checks — "Catches what you'd miss"
6. DSOP Simulator — "Watch your DSOP grow"
7. Dashboard + donut, dark — "See where your money goes"
8. Retirement Entitlement Summary — "Plan retirement with real numbers"

Android — DONE and shipped live (2026-09-17): reused the same 8 iOS-captured screens/captions,
recomposited at 1440×2778 (Play caps aspect ratio at 2:1; the iOS 1284×2778 canvas is 2.16:1 and
would have been rejected — widened the canvas rather than cropping content). `composeApp/fastlane/Fastfile`
gained `screenshot_status` (read-only count/list, throwaway-edit pattern like `listing_status`) and
`upload_screenshots source:<folder>` (deletes the existing `phoneScreenshots` set for the locale,
uploads the new set in filename order, commits — uses the Android Publisher API's
`upload_edit_image`/`deleteall_edit_image` directly, same `play_service` auth as the rest of this
file). Unlike iOS, Play has no editable-version lock (per the SSOT section above) — the edit/commit
went live immediately, verified via `screenshot_status` before and after (8 old images → 8 new
image IDs for en-IN).

**Phase 4 — Ship & verify.** Run the upload lanes, confirm via `listing_status` lanes on both
platforms, update this doc's checkbox and log the shipped screenshot set (same as the Priority 1
"Shipped copy" reference section) so the next agent has ground truth. Re-evaluate Priority 4 (the
production questionnaire is holding on this).

Each phase ends with the same tech-debt-resolved / build-status handoff style used in
[08_ios_monetization_phaseplan.md](08_ios_monetization_phaseplan.md), even though this track has
no compiled build to break — "build" here means the fastlane lanes run clean and the dry-run
images are approved before committing live.

## Priority 3 — In-app features (real code work, follow normal phase-wise process)

- [x] **In-app rating prompt** — trigger after a positive moment (e.g. a successful payslip
      parse), not on launch/cold-start. Shipped on `release/ios-1.0.0-v6` (versionCode 13, commits
      `9300feb`/`60815ca`/`ad486de`), verified end-to-end on a Pixel 9 — see
      [06_closed_testing_progress_log.md](06_closed_testing_progress_log.md) v13 workstream 6.
- [x] **Onboarding walkthrough for first-time users** — with a skip option and an accessible
      help/FAQ section. 3-slide skippable carousel (privacy/trust → on-device-AI-parsing → Get
      Started/FAQ link), rendered as a centered pop-over `Card` dimmed over the real Dashboard
      (not a full-screen takeover) so a first-time user gets spatial orientation, plus a separate
      one-time coachmark on the Dashboard's upload FAB. Gated by a shared-module
      `OnboardingManager`/`OnboardingStorage` SSOT (Android `SharedPreferences` / iOS
      `NSUserDefaults` actuals, mirroring `RatingPromptStorage`). Shipped together in the commit
      that flips this checkbox — see that commit's diff for the exact file list. Verified via the
      full `check` gate (Android + common build, ktlint, tech-debt audit,
      `linkDebugFrameworkIosSimulatorArm64`, `iosSimulatorArm64Test`) **and** manually end-to-end on
      a physical Pixel 9 — see
      [06_closed_testing_progress_log.md](06_closed_testing_progress_log.md) workstream 7 for the
      on-device verification note.

Both require going through the project's normal phased build process (`AppStrings.kt` for any
copy, `Theme.kt` for styling, 300-line file limit, tests, etc.) — not something to rush in just
to answer the production form.

## Priority 4 — Production Access Questionnaire

The production report is a fill-in-the-blank draft for Google's actual 10-question
closed-testing-graduation form.

- Q1 (tester recruitment) — draft answer matches reality (paid provider + PCDA(O) outreach),
  usable as-is.
- Q4 / Q8 ("what changes did you make based on feedback") — draft **presumes** the ASO copy,
  screenshots, rating button, and walkthrough are already shipped. All four are now live on
  Android: Priority 1 (ASO copy, 2026-09-17), Priority 2 (screenshots, 2026-09-17), Priority 3
  (rating prompt + onboarding walkthrough, shipped in versionCode 13 → Internal testing,
  2026-09-18 — see [06_closed_testing_progress_log.md](06_closed_testing_progress_log.md)). The
  gate this note used to describe is cleared; still worth promoting v13 to Closed testing and
  letting it stabilize before submitting the questionnaire, so Google's listing-history
  cross-check reflects the same build testers are actually on.
- **Resolved (2026-09-24):** the mandatory 14-day Closed testing window completed and Google granted
  production access automatically — no separate manual questionnaire submission was required or
  found in Play Console during this pass (the "Production Access Questionnaire" concept in the draft
  above may describe an older/different Play Console flow than what this app actually went through).
  versionCode 15 is now promoted to the production track and in Google's review — see
  [06_closed_testing_progress_log.md](06_closed_testing_progress_log.md), "PROMOTED TO PRODUCTION."
  This section's action items are complete; kept here as historical record.

## Not worth acting on

The feedback report's "Additional Recommendations" (community engagement, financial-education
content, generic accessibility audit) have no specific finding behind them — skip rather than
spend cycles here.

## Strategic advisories (from ASO report, ongoing / off-listing)

- Drive review velocity: prompt for ratings after a positive in-app moment, not on launch.
- Track store-listing conversion rate in Play Console (Grow users → Store presence); target
  ≥25% visitor-to-install.
- Use Play Console's free store-listing experiments to A/B test first 3 screenshots + short
  description.
- Localize the listing for top 3 markets once English listing is optimized.
- Ship a small update every 6–8 weeks to stay favored by store ranking/freshness signals.
