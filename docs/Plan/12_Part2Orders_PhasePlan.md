# Part II Orders, Leave release — Phase Plan

Status: draft for owner sign-off. Written 2026-10-22; revised 2026-10-23 into a strict phase-wise execution plan (see
"Review"). Nothing built. Branch `feature/part2-orders` off `main` @ `d9274bdd`, created in Phase 0 only after
sign-off. This file is the plan; phase status is tracked in the phase table.

## Review (2026-10-23): what changed and why

The first draft was checked against the owner's execution directives (phase independence, TDD, MVVM/SOLID/DRY/SSOT,
300 LOC, resources, security, R8, Phase Handoff). Findings, each now built into the phases below:

| # | Finding | Change |
|---|---|---|
| 1 | Phases 2, 4, 6 and 7 each mixed several risky changes, so one failure would block a large phase | Split into 2a/2b, 4a/4b, 6a/6b/6c, 7a/7b. Each sub-phase ends green and is committed on its own |
| 2 | Adding `FeatureGate.PART2_ORDERS` breaks the build: `featureMeta` in `PremiumFeaturesCatalog.kt` has an exhaustive `when`. It would also advertise a dark feature, because `premiumFeatureCatalog()` lists every gate, and `PremiumFeaturesCatalogTest` asserts the catalog size equals the gate count | Phase 1 adds the catalog entry and `isAdvertised(PART2_ORDERS) = isPart2Enabled()`, and updates that test first |
| 3 | The directive allows a file over 300 lines when SRP requires it, with a comment. The enforced gates do not: Gradle `checkFileSizes` and `check_tech_debt_limits.py --strict` hard-fail any `.kt` over 300, composables over 50 lines, and raw dp/sp/colors/strings in `ui/` | The stricter rule wins. No `.kt` file uses the exception. For non-Kotlin files (Python, Swift, JSON), a new tool test enforces 300 lines, and the exception needs a header comment. None is planned. A file budget per phase keeps every file under the limit |
| 4 | Platform services were `expect fun`s, which are hard to fake and break DIP | They become interfaces in `commonMain` with platform implementations injected by Koin or bound in composition. Fakes live in `shared-test-fixtures` |
| 5 | ViewModels were not named; MVVM boundaries were implicit | A module map names every ViewModel, its state class and its inputs. Composables stay stateless |
| 6 | "Integration tests" were implied, not listed | Every phase lists unit and integration tests separately. Integration tests always use the real shipped bundle, real crypto and in-memory Room |
| 7 | R8 proof relied on a manual mapping grep | `check_r8_guide.py` is extended to the Part II model folders, Room `_Impl` and the WorkManager worker, and runs in CI |
| 8 | New libraries can add permissions or exported components silently | A new `scripts/check_merged_manifest.py` compares the release merged manifest with a committed baseline. CI runs it; any change needs a reason in the baseline |
| 9 | A crafted `.pcda` v5 file could exhaust memory (huge pages, huge counts) | Restore checks sizes and counts against the `Part2Limits` SSOT before decoding any Part II row |
| 10 | Some limits and texts appeared in more than one place (page caps, reminder cadence, notification text, plist string) | One SSOT per concern: `Part2Limits`, `ReminderPolicy`, `Part2Strings`. Platform code receives text by injection. The iOS plist string is the one platform-mandated copy |
| 11 | An `ActivityHolder` or held deep-link `object` would trip `stateful_singleton_guard.py` | The Activity comes from composition (`LocalContext`); the held deep link lives in a Koin-scoped holder, never an `object` with a `var` |
| 12 | The security review was a checklist only | Each code phase also runs the `security-review` skill on its diff and records the result in the Phase Summary |

## Context

Inside the Claim Guide, the officer picks a ribbon (first: Leave) and fills in the details. The app drafts a clerk slip:
occurrence rows only (code, narrative, From/To, Data 1–4), an authority line where the manual requires one, and the
officer's last Part II order no./date. The slip carries no identity data. The app checks the manual's computable rules
and adds the linked orders. The officer prints the slip through the system print dialog. Later they scan the published
order into an encrypted vault and type in its order no. and date. Reminders come from the drafts themselves. Code lookup
is free; drafting, printing, the vault and reminders are Premium. The whole feature is behind `LaunchFlags.PART2_ENABLED`,
which also needs `GUIDE_ENABLED`.

Sources: `docs/Part2Orders/DECISIONS.md` (Rounds 1–10; it wins on any conflict) and `docs/Part2Orders/RIBBON_LAYOUT.md`
(10 ribbons, 48 tiles, 303 serials). Manual = ODP-2026; page numbers are the printed ones, checked in
`full_text_layout.txt` on 2026-10-22. A clickable mock of the flow exists as a private artifact; it is illustrative
only, and its open choices are settled in Phases 2b and 5.

The Leave release drafts **59 serials**:

| Set | Serials | Basis | Phase |
|---|---|---|---|
| Annual leave set | 5.1 GTDAL, 5.2 GTDALCF, 5.3 EXTNAL, 5.4 GTDCL, 5.5 GTDSPCL, 5.23 REJOINAL (printed `REJOIN`), 5.24 VOLREJ, 5.25 RECALL | pp.114–117, 131–132 | 2b |
| Ration four | 3.86 SORS, 3.82 GTDRSHNA, 3.87 TORS, 3.83 STPRSHNA | ration notes 1.1, 1.5, 3, 4, pp.97–98 | 2b |
| R&H | 3.12 STPRH; re-grant with the officer's own code from 3.1–3.11 GTDRH11…GTDRH60 | seen in practice; templates pp.46–48 | 2b |
| Cancellation | 14.1 CANCEL, 14.2 CANCELS, offered from any vault order | para 11 p.8; pp.226–227 | 6c |
| Rest of Chapter 5 | 5.6–5.22, 5.26 ACCLVE, 5.27–5.29, 5.30 ENCASH (22 serials) | pp.117–141 | 8 |
| Area re-grants | HAA 3.13–3.15; SCCIA 3.19–3.21; field 3.23–3.25; TPTA 3.54 STPTRAN + 3.53 GTDTRAN1–5 | p.49 note ii; p.53 note 2; p.55 note 2; p.71 notes iii, v | 9 |

## Facts established before planning

Manual (checked in the text dump):
- GTDAL notes, pp.115–116:
  - 1: days are counted inclusively (05/07–04/09 is 62 days, not 60).
  - 2: AL availed, extended, accumulated for encashment, adjusted against sick leave and study-leave extension together
    must not exceed 60 days a year.
  - 4: the dates tally with the narrative.
  - 7: EXTNAL quotes the grant order.
  - 8: AL is taken before joining time.
  - 9: prefix + suffix over 5 days → the unit checks.
  - 10: two years granted together → two occurrences.
  - 13: entitlement in the retirement year.
- CL cannot be combined with any leave except special CL (p.117). The notes after 5.11 (p.118) say From/To exclude
  prefix and suffix, and Data 1/2 carry the first prefix date and the last suffix date.
- REJOINAL, VOLREJ and RECALL quote the grant order (pp.131–132). The slip prints `REJOIN`, as HRMS does (Round 9).
- ACCLVE, pp.133–134:
  - The narrative is (a) − (b) + (c), capped at 300.
  - Note 6: (c) is at most 30.
  - Note 5: (c) + AL availed + AL adjusted against sick leave + study-leave extension is at most 60.
  - Note 7: published on or after 1 March of the next year, or on retirement.
  - The notes are numbered 1, 2, 3, 5, 6, 7 (there is no note 4).
- ENCASH, pp.140–141: col 8 holds the days, and the narrative gives a year-wise break-up "during the entire service"
  (note 2).
- Ration, pp.97–99:
  - Leave that is 180 days or more (maternity, CCL, adoption, study) is published by the unit quarterly, in arrears
    (note 5). It is not drafted on the leave slip.
  - The grant order carries the Ration Money Allowance certificate (note 8).
- R&H grants (pp.46–47) need station code, rate, and Corps notification no./date + serial (cols 6–9), plus a
  certificate (p.48). The manual never says leave stops R&H; the stop/re-grant is practice only.
- HAA (p.49 note ii): re-granted on rejoining, except after CL under 14 days. Corps notification in cols 6–9 (note i);
  certificate as Annexure IV Sr 1A (p.50 note v).
- SCCIA (p.53 note 2): re-granted on rejoining from leave, hospitalisation or courses, with no CL threshold;
  certificate p.53.
- Field (p.55 note 2): re-granted on rejoining, except after CL or hospitalisation under 15 days; certificate p.56.
- TPTA (p.71):
  - Note v: STPTRAN for an absence covering a full calendar month.
  - Note iii: the grant is published on rejoining after such an absence.
  - Cols 3 and 7 give the reason in both cases.
- CANCEL (p.226) and CANCELS (p.227):
  - Each carries the certificate "previous status may be restored".
  - Cols 8–9 hold the cancelled order's no. and date.
  - CANCEL puts "ALL" in col 6. CANCELS puts the code in col 6 and the serial in col 7, and covers one occurrence only
    (note 7).
  - An order more than 2 years old needs a time-bar sanction.
- Para 3 (p.4) defines the groups: A financial (other than claims); AM the same, required by MISO; B financial (claims);
  C non-financial; CM non-financial, required by MISO. Para 6.4: all occurrences of one event go in one order, in date
  order. Para 6.7: every order quotes the last Part II order. Para 7.1 (p.5): Group B claims are never combined with
  A/AM/C/CM.

Code (read 2026-10-22/23):
- Files near the 300-line limit: `App.kt` 297, `GuideModels.kt` 285, `AppStrings.kt` 288, `GuideTab.kt` 257,
  `PayslipDao.kt` 204. New code goes in new files.
- The mechanical gates:
  - Gradle `checkFileSizes` is part of `check`: every `**/src/**/*.kt` must be ≤ 300 lines, with no exemption.
  - `check_tech_debt_limits.py --strict` runs in pre-commit and CI. It enforces 300 lines, composables ≤ 50 lines, no raw
    `dp`/`sp`, colors or copy in `ui/screens` and `ui/components`, and no risky regex in `commonMain`.
  - `stateful_singleton_guard.py` forbids new `object`s that hold mutable state (closed allow-list).
  - `check_schema_immutability.py`, gitleaks and ktlint also run.
- `PremiumFeaturesCatalog.featureMeta` is an exhaustive `when (gate)`, and `isAdvertised` decides which gates the
  paywall lists. `PremiumFeaturesCatalogTest` and `PayslipViewModelSubscriptionTest` loop over `FeatureGate.values()`.
- The Guide bundle is 252 KB against `GuideBundleParser.MAX_CHARS` 2,000,000. The pattern for a new bundle section is
  `preclaim.json` → `preclaim.py` → additive section → `GuidePreClaimValidator`. `check_r8_guide.py` scans every
  `@Serializable` class in `guide/model/` and runs in CI after `assembleRelease`.
- Navigation precedents: `GuideDestination.PreClaim`, `GuideNavStateSaver` (ids only), `retainKnown`,
  `rememberPreClaimHost`, and `discardPreClaim()` (called from `App.kt` on lock). `PreClaimModule` is loaded only while
  its flag is on.
- Room is at schema 14 with no destructive fallback. `PayslipDatabaseUpgradeTest` and the schema-immutability check
  guard migrations.
- `.pcda` is at v4:
  - `GuideNoteEntity`/`GuideNoteBackup` is the pattern for an encrypted row (id inside the ciphertext; fixed exception
    with no cause).
  - The archive is one JSON document built in memory, and a `ByteArray` serializes as a JSON number array (about 3.5×).
  - `clearAllUserData`/`replaceWithBackup` must learn about every new table.
- `CryptoHelper.encrypt` runs PBKDF2 (10,000 rounds) on every call. `sharePdf` uses `ContextHolder.context`, which is
  the Application, but `PrintManager.print()` needs an Activity. `PdfLayoutSpec` is the A4 geometry SSOT.
- There is no notification, scheduling, camera or scanner code or library. minSdk 26, targetSdk 36. The iOS `Info.plist`
  has no usage descriptions. XCTest exists (`iosApp/PayslipMaxTests`).
- `DutyDays`/`CalendarDate` (`guide/preclaim`, internal) count inclusive civil days with no date library. There is no
  kotlinx-datetime.
- Leave Bank: `LeaveEntry` gives ACCLVE `days`/`totalDays` and LVELTC `days`, each with a `casDate`.
  `LeaveBankCalculator.summarize`/`latestAccrual` are the SSOT. ACCLVE(Y) matches the statement of Y+1.
- The `Officer` model holds only name, CDA A/c and a masked PAN. `ParserUtils` returns `"AR*****90G"` when no PAN is
  found. The slip engine never receives a payslip.

## Owner decisions

All decisions live in `docs/Part2Orders/DECISIONS.md`; this table only maps them to the plan.

| Topic | Round | What it means for this plan |
|---|---|---|
| Clerk slip, no identity data, print only | 5, 7 | The slip model has no identity fields; `SlipPrinter` only; no share sheet (Phase 5) |
| Computable rules, linked orders, manual vs practice labels | 5, 7, 9 | Rule ids in the templates, logic in Kotlin; every link tagged `manual(page, note)` or `practice` (2a, 2b) |
| Vault: scan, encrypted, linked to draft, order no./date typed | 5, 7, 10 | Doc scanners, re-encoded pages, Room rows (3, 6a, 6b) |
| Backup v5, app lock | 5, 10 | One schema bump (14→15) and one backup bump (v4→v5), both in Phase 3 |
| Reminders from drafts, local, generic text | 6, 10 | `ReminderPlanner` (7a) + platform schedulers (7b) |
| Lookup free; `PART2_ORDERS` gate; lapsed users keep read access | 5, 10 | Gate and catalog guard in Phase 1; entry rules in 4a/4b |
| Tiles only for built ribbons (+ Records lookup); search reaches all 303 | 8 | Data in Phase 1, UI in 4a |
| Blank groups show "Group not printed in ODP-2026" | 8 | Validator allows a `null` group for 3.28 and 11.8 only |
| Ration four, R&H, HAA/field/SCCIA re-grant, TPTA, REJOIN, CANCEL | 9, 10 | 2b, 6c, 9 |
| Dataset rebuilt by script; flawed JSON never committed | 4 | Phase 0 |
| Templates in hand-written JSON | 10 | `docs/Plan/rule_cards/part2/templates/*.json` + `tools/part2_templates.py` |
| Dark launch `PART2_ENABLED` (+ `GUIDE_ENABLED`) | 4 | Flag in Phase 1, flipped only on the owner's word (Phase 10) |

## Architecture

```
~/Downloads/rulecards_workdir/part2/ (outside git): master.tsv, full_text_layout.txt
     │ tools/part2_codes.py (local only)                 docs/Plan/rule_cards/part2/
     ▼                                                     codes.json            (generated, committed)
tools/part2_{codes_check,nav,templates}.py ◄────────────── nav.json              (authored from layout.py)
     │ validate + for_bundle                               templates/<batch>.json (hand-written, ≤ 300 lines each)
     ▼ bundle.py: additive `part2` section, BUNDLE_VERSION 1
guide_bundle.json ─► GuideBundleParser ─► guide/model/Part2*Models.kt ─► GuidePart2*Validator
                                                │
shared/part2/domain (pure)                      ▼
  LeaveEventInput ─► Part2DraftEngine ─► Part2Slip ─► SlipLayout (pure page model)
        uses: Part2Rules registry, OccurrenceRenderer, SlipOrdering (para 6.4/7.1), CivilCalendar, LeaveBankCalculator
  ReminderPlanner(drafts, ReminderPolicy) ─► List<Reminder>
shared/part2/data: Part2Repository (interface) ─► RoomPart2Repository ─► Part2Dao ─► 3 entities (schema 15)
                   Part2Backup (export/restore, bounded by Part2Limits) ─► PortableBackup v5
platform services (interfaces in commonMain; implementations per platform):
  SlipPrinter · PageScanner · PageEncoder · ReminderScheduler · NotificationPermission
composeApp ViewModels ─► stateless composables in ui/screens/guide/part2/
```

**Module map and file budget.** Every file stays ≤ 300 lines; composables ≤ 50 lines.

| Layer | File(s) | Responsibility | Phase |
|---|---|---|---|
| Content tools | `part2_codes.py` (local generator), `part2_codes_check.py`, `part2_nav.py`, `part2_templates.py`, `part2_review.py` + one test file each | One concern per file; `part2_templates.py` only validates and ships templates | 0–2b |
| Bundle models | `guide/model/Part2LookupModels.kt` (codes, groups, nav), `Part2TemplateModels.kt` (templates, rules, links) | `@Serializable` shape only | 1, 2a |
| Bundle checks | `GuidePart2LookupValidator.kt`, `GuidePart2TemplateValidator.kt` | Recheck on load; problems name ids only | 1, 2a |
| Domain | `CivilCalendar.kt` (moved), `LeaveEventInput.kt`, `Part2DraftEngine.kt`, `OccurrenceRenderer.kt`, `SlipOrdering.kt`, `Part2Rules.kt` + one file per rule family (`LeaveRules.kt`, `RationRules.kt`, `AreaRules.kt`), `Part2Slip.kt`, `SlipLayout.kt`, `ReminderPlanner.kt`, `Part2Limits.kt`, `ReminderPolicy.kt`, `AcclveCalculator.kt` | Pure Kotlin, no platform, no payslip input | 2a–8 |
| Data | `Part2Entities.kt`, `Part2Dao.kt`, `Part2Payloads.kt`, `Part2Repository.kt`, `RoomPart2Repository.kt`, `Part2Backup.kt` | Encrypted persistence and backup | 3 |
| Platform | `SlipPrinter` (`AndroidSlipPrinter`, `IosSlipPrinter`), `PageScanner` (ML Kit, VisionKit), `PageEncoder` (Android/iOS), `ReminderScheduler` (WorkManager, UNUserNotificationCenter), `NotificationPermission` | Interfaces in `commonMain`; Activity-bound ones built in composition | 5–7b |
| ViewModels | `Part2LookupViewModel` (home, ribbon, case, code, search), `LeaveDraftViewModel` (form → slip, autosave, print record), `VaultViewModel` (orders, capture, order no./date, cancel), `DueViewModel` (planner output) — each with its own `*UiState.kt` | Read and write only through the repository and the engine; no platform types | 4a–7a |
| UI | `GuideTabPart2.kt`, `Part2HomeScreen.kt`, `Part2RibbonScreen.kt`, `Part2CodeScreen.kt`, `LeaveDraftScreen.kt` + section files, `SlipPreviewScreen.kt`, `HrmsCheckScreen.kt`, `VaultScreens.kt`, `DueScreen.kt` | Stateless; state hoisted to the ViewModels | 4a–7a |
| Resources | `ui/theme/Part2Strings.kt` (all copy, incl. notification and print-job text), theme tokens in `Theme.kt` for the practice chip and the Due card | SSOT for copy and colors | 1 onward |
| DI | `di/Part2Module.kt` (loaded only while enabled); shared bindings for repository and scheduler | Composition root | 3–7b |
| Fakes | `shared-test-fixtures`: `FakePart2Repository`, `FakeSlipPrinter`, `FakePageScanner`, `FakeReminderScheduler`, `SyntheticPart2Drafts` | Shared by both modules' tests | 3 onward |

Design rules:
- **Slip.** `Part2Slip` holds orders, each with its group (split by para 7.1) and occurrences in date order (para 6.4).
  An occurrence is: printed code, narrative, From, To, Data 1–4, an optional authority line and an optional certificate.
  The header carries the last order no./date, or a blank. No field can hold a name, number, PAN or account, and the
  engine's input has no payslip.
- **Drafts** store the officer's inputs and the template `rev`s, never rendered text. Printing stores a snapshot of the
  rendered rows. If a template changes after printing, the draft shows "template updated since printed".
- **Rules.** Each check has an id, a citation and a severity:
  - `warn`: shown on the slip preview.
  - `ack`: the manual says "will be rejected"; the officer must acknowledge it before printing. Owner to confirm at 2b.
  - A contract test keeps the bundle's rule ids and the Kotlin registry identical.
- **Area details** (station code, Corps notification no./date/serial) are typed into the draft and prefilled from the
  last draft of the same code. There is no profile store.
- **Last Part II order** = the vault order with the latest order date; blank when the vault is empty.
- **SSOT:**
  - `CivilCalendar` (moved from `DutyDays`, behaviour unchanged).
  - `LeaveBankCalculator` for every Leave Bank figure.
  - `codes.json` for code names, groups and group meanings (para 3 is content, so it ships in the bundle).
  - `Part2Limits` for page, order, payload and backup caps.
  - `ReminderPolicy` for cadence.
  - `Part2Strings` for all copy, passed into platform code. The one exception is iOS `NSCameraUsageDescription`, which
    Apple requires in `Info.plist`; it is checked against `Part2Strings` by a test.
  - Theme tokens for every color. The slip PDF reads its ink and rule colors from `SlipLayoutSpec`, which sits next to
    `PdfLayoutSpec`.
- **SOLID:**
  - Engine, planner and layout are pure and injected with their policies.
  - Platform services sit behind interfaces (DIP), and each rule family is its own class (open for new families
    without editing the engine, OCP).
  - ViewModels depend on interfaces only.
  - No new stateful `object`.

## How each directive is enforced

| Directive | Mechanism | Evidence in each Phase Summary |
|---|---|---|
| 1. Every phase ends green | Gate (below) run in full at phase end; no next phase until green; one commit per phase after the owner accepts | Commands, exit status, test counts per module |
| 2. TDD, 100% pass | Tests are written and failing first. Unit and integration tests listed per phase. None skipped, none `@Ignore`d. Corpus 139/139. Each guarded rule is broken on purpose to show its test fails, then restored | Test counts before and after; the list of deliberate breaks and the test that caught each |
| 3. MVVM, SOLID, DRY, SSOT, 300 LOC | Module map above. `checkFileSizes` + `check_tech_debt_limits.py --strict` (Kotlin, composables). New `tools/test_part2_file_sizes.py` enforces ≤ 300 lines on every Part II Python, JSON and Swift file. `stateful_singleton_guard.py`. Review of every touched file against its single responsibility | Line counts of every touched file; any duplicate found and removed |
| 4. No hardcoded strings or colors | `check_tech_debt_limits.py` in `ui/`. All copy in `Part2Strings`, all colors as theme tokens or `SlipLayoutSpec` constants. Platform code receives text by injection | Grep of the diff for string and color literals outside the SSOT files |
| 5. Security | Per-phase threat checklist (Cross-cutting checks) + the `security-review` skill on the phase diff + gitleaks via the hooks + merged-manifest check from 6a | Findings and fixes, or "no findings" |
| 6. R8 | `minifyReleaseWithR8` every phase. `assembleRelease` + extended `check_r8_guide.py` in phases that add serialized models, Room entities or libraries (1, 2a, 3, 6a, 7b, 10). Narrow keep rules only when the check shows a missing class | Check output; any keep rule added and why |

**Gate (run in full at every phase end):**
1. `./gradlew check -x iosX64Test -x iosSimulatorArm64Test` (debug and release unit tests, ktlint, `checkFileSizes`,
   corpus 139/139).
2. `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64` and `./gradlew iosSimulatorArm64Test`.
3. `python3 docs/Plan/rule_cards/tools/refresh.py` (copycheck CLEAN), then `refresh.py --check`.
4. `python3 scripts/check_tech_debt_limits.py --strict <touched .kt>`, `python3 -m unittest discover -s scripts/tests`.
5. `./gradlew :composeApp:minifyReleaseWithR8 -PallowPlaceholderGemmaModel=true`; in the phases listed above also
   `assembleRelease` + `check_r8_guide.py` (+ `check_merged_manifest.py` from 6a).

## Cross-cutting checks

**R8 / release.**
- `check_r8_guide.py` gains a second model folder (`shared/.../part2/`). It checks for the Room
  `PayslipDatabase_Impl`/`Part2Dao_Impl` and, from 7b, the reminder worker class.
- New libraries: `androidx.work` (7b) and `play-services-mlkit-document-scanner` (6a). Versions are pinned in
  `libs.versions.toml`, at least two weeks old, and checked against published advisories before adding.
- The notification icon is referenced from code, so resource shrinking keeps it. The release APK walk in Phase 10 is
  the final proof.

**iOS / Kotlin-Native.**
- No regex in `commonMain` hot paths; `iosTest` timing bounds (the `ParserUtilsIosPerfTest` pattern) for the engine
  (59 templates × 200 drafts), the planner and the slip layout.
- JPEG re-encoding runs in native code, off the main thread. Backup export with a full vault is measured for time and
  peak memory.
- Swift code (VisionKit presenter, notification delegate) gets XCTest cases in `iosApp/PayslipMaxTests`.

**Navigation and process death.**
- New `GuideDestination`s: `Part2Home`, `Part2Ribbon(id)`, `Part2Case(id)`, `Part2Code(code)`, `Part2Draft(draftId)`,
  `Part2Vault`, `Part2Order(orderId)`, `Part2Due`. The saver writes ids only (random UUIDs, never data). `retainKnown`
  cuts unknown codes and ribbons; missing draft/order ids are cut once the repository answers.
- Drafts autosave from the first step, so process death restores from the database. A scan in flight is lost; the
  officer rescans.
- On lock, `discardPreClaim()` becomes `discardTransientForms()` (a one-line change in `App.kt`).
- Android back uses `GuideNavState.pop`; tab re-tap pops to Guide Home; iOS edge-swipe is blocked while a scan or print
  sheet is up.
- A notification tap opens Guide → Part II → Due (Android intent extra; iOS `UNUserNotificationCenterDelegate` →
  `NavBridge`). It is held in a Koin-scoped `PendingPart2Link` until unlock. It carries only a reminder kind and a draft
  id, checked against the database.

**Security, PII and telemetry (threat checklist for every phase).**
- *Identity:* the slip type cannot hold identity data. A test scans rendered slips for the PAN placeholder and for text
  the officer did not type.
- *Input:* officer input is bounded by `Part2Limits` and plain text (angle brackets refused). Bundle input is
  revalidated on load.
- *Untrusted files:* restoring a v5 backup checks every Part II row's size and count before decoding. Oversized or
  surplus rows are skipped and counted, never decoded.
- *Photos:* pages are re-encoded, which drops all metadata (tested: no APP1/Exif, no GPS). They are never written to
  the gallery or passed through the `FileProvider`. ML Kit cache files are deleted at once, including on failure.
- *At rest:* the device key; in backups, the backup password. Fixed-message exceptions with no cause.
- *Intents and components:*
  - Notification `PendingIntent`s are explicit and `FLAG_IMMUTABLE`.
  - The worker input holds a reminder id only.
  - No new exported component; `check_merged_manifest.py` proves it.
- *Telemetry:* nothing about a draft, code, date or order no. goes to `Logger`, `CrashReporter` or an exception message.
  Counts only, through `TelemetrySanitizer`.
- *Platform:* no network, no WebView, no clipboard writes; `allowBackup="false"` stays. No `FLAG_SECURE` (the app has
  none today; not decided).
- *Repo:* synthetic dates and order numbers only; no identifier from the owner's orders in any file; gitleaks.

**Storage.**
- Schema 14 → 15 adds three tables in one `AutoMigration(14, 15)`. Only ids and the order/page link are plaintext
  columns; everything else is a versioned encrypted payload. Later changes therefore change the payload, not the schema,
  and Phase 3 is the only schema bump.
- Pages are ≤ 500 KB each (under Android's 2 MB row limit) and ≤ 10 per order. Deleting an order deletes its pages;
  deleting a draft keeps its orders.
- `clearAllUserData` and `replaceWithBackup` empty the new tables.
- Backup v5 adds `part2Drafts`, `part2Orders` and `part2Pages`, with pages as Base64 ciphertext:
  - A v4 file restores with an empty vault; a v6 file is refused.
  - MERGE matches by id, newer `updatedAt` wins, and a tie goes to the backup.
  - Settings warns above 60 MB of vault.

**Versioning.** `BUNDLE_VERSION` stays 1; the `part2` section has its own `version: 1`. Payloads carry `v: 1`. DB schema
15, backup 5, each bumped once (Phase 3).

## Phase Summary template (end of every phase)

```
Phase N — <name>                                  Commit: <sha after acceptance>
1. Tech debt incurred:   <each item, or "none">
2. Resolved by:          <exact steps, file:line, for every item above; nothing deferred>
3. Build and tests:      gate commands + exit status; tests per module (debug/release/iOS), before → after;
                         corpus 139/139; deliberate breaks and the test that caught each
4. 300 LOC:              line count of every touched file (largest first)
5. Resources:            string/color literal grep of the diff: clean
6. Security:             threat checklist + security-review skill result
7. R8:                   minify result; check_r8_guide / merged-manifest output where run; keep rules added (why)
8. Owner items:          decisions needed before the next phase, if any
```

The next phase starts only after the owner accepts this summary.

## Phases

| Phase | Status | Commit |
|---|---|---|
| 0. Branch, baseline, occurrence dataset (tools only) | done, accepted (2026-10-23) | see git log |
| 1. Lookup bundle section, models, validator, flag, gate + catalog guard | done, accepted (2026-10-23) | 8bcf9065 |
| 2a. Template format, validators, engine core (synthetic templates), `CivilCalendar` move | done, accepted (2026-10-23) | 534ba5cc |
| 2b. Content batch 1: annual set + ration four + R&H; owner review | built 2026-10-23; **owner review against the PDF not done, so 2b exit is not claimed** | see git log |
| 3. Storage: schema 15, repository, backup v5 with bounded restore | not started | — |
| 4a. Lookup UI (free): Part II home, ribbons, tiles, code pages, search | not started | — |
| 4b. Leave draft flow + slip preview (Premium), autosave | not started | — |
| 5. Print + "Check the HRMS draft" | not started | — |
| 6a. Page pipeline: scanner spike, `PageScanner`, `PageEncoder`, merged-manifest guard | not started | — |
| 6b. Vault UI: order no./date, linking, last-order line, follow-up references | not started | — |
| 6c. CANCEL / CANCELS: content batch 2 and flow | not started | — |
| 7a. Reminder planner + in-app Due | not started | — |
| 7b. OS notifications on both platforms | not started | — |
| 8. Rest of Chapter 5, incl. ACCLVE and ENCASH (batch 3) | not started | — |
| 9. Area re-grants: HAA, SCCIA, field, TPTA (batch 4) | not started | — |
| 10. Release hardening, owner content sign-off, iPhone walk, docs | not started | — |
| 11. Pending items (collected from earlier phases) | open: items 1, 6, 7, 8 (items 2-4 closed in 2b; 5 not closable) | — |

Each phase leaves the app shippable: `PART2_ENABLED` is `false` until Phase 10, so a flag-off build behaves exactly as
before. Each phase's "Debt watch" lists the debt most likely to appear and how it is cleared inside that phase.

### Phase 0: Branch, baseline, occurrence dataset (tools only, no app code)
- Goal: a tested, reproducible `codes.json` for all 303 serials.
- Setup: create `feature/part2-orders`; commit this plan and the DECISIONS Round 10 edit. Record the baseline: the full
  gate, test counts per module, corpus 139/139, line counts of every file the plan touches.
- Unit tests first (`tools/test_part2_codes.py`):
  - Extractor on synthetic layout snippets (no manual text): a wrapped code, a `/` variant list, a blank group, a page
    header.
  - Generated `codes.json`:
    - 303 serials, contiguous per chapter; per-chapter counts 19/16/102/5/30/15/13/10/20/10/21/6/34/2.
    - 321 distinct codes, unique across serials; 8 variant serials with their exact variants.
    - Groups A 204, AM 23, B 11, C 53, CM 10, and `null` only for 3.28 and 11.8; para 3 group meanings present.
    - The 14 serials missing from the flawed JSON are present; 9.7 WTHDPAY detail p.171; 5.23 is `REJOINAL`.
    - No code contains `/`, lowercase or a space.
  - No `part2_occurrences_2026.json` anywhere in the repo.
- Integration test: run the generator against the local sources in a temp dir; `--check` is clean against the committed
  file. Local only; skipped with a visible message in CI, which has no sources (the `refresh.py` convention).
- Implement: `tools/part2_codes.py` (generator, local); `tools/part2_codes_check.py` (shape checks, runs in CI);
  `tools/test_part2_file_sizes.py` (300-line rule for Part II tool, JSON and Swift files).
- Owner spot check: 25 seeded random rows with detail pages, checked against the PDF and recorded in the summary.
- Exit: gate green, app gate unchanged from the baseline.
- Debt watch: the known column-wrap misses must be an explicit list, never a loose tolerance.
- Done 2026-10-23 (see the Phase 0 summary in the session): the explicit list has 12 entries, not 13; each serial has one `page` (its
  Annexure III row, which is the index's Page column), not two; names are the manual's own Annexure II wording, typos included
  ("Guest Rom", "Arm Holiday Homes"); a name is capped at 300 characters (a limit chosen here, not the manual's).

### Phase 1: Lookup bundle section, models, validator, flag, gate + catalog guard (no UI)
- Goal: the bundle carries all 303 code pages and the 10-ribbon layout; the app reads and validates them; the flag and
  gate exist with no user-visible change.
- Unit tests first:
  - `test_part2_nav.py`:
    - Every serial homed once; 10 ribbons, 48 cases; 2–16 codes per case; ≤ 8 cases per ribbon; titles ≤ 7 words.
    - Lookup-only marks; `built` only on Leave and Records.
  - `test_bundle.py`: the section ships with whitelisted keys only, deterministically; the stale guard works.
  - `GuidePart2LookupValidatorTest` refuses each of:
    - an unknown group letter, a duplicate code, an empty name, angle brackets;
    - a `null` group outside 3.28/11.8;
    - an unknown section version.
  - `Part2AvailabilityTest`: all four flag combinations.
  - `SubscriptionManagerTest`: `PART2_ORDERS` under free launch, `FORCE_FREE`, `FORCE_PRO`.
  - `PremiumFeaturesCatalogTest` (updated first): `PART2_ORDERS` is catalogued, and is advertised only while
    `isPart2Enabled()`. The catalog-size assertion counts advertised gates.
- Integration tests:
  - `GuideBundleParser` loads the real shipped bundle: 303 code pages, 321 codes, 10 ribbons.
  - A bundle without `part2` is still valid (old-bundle compatibility).
- Implement:
  - `part2/nav.json`; `tools/part2_nav.py`, wired into `compile.py`, `bundle.py` and `refresh.py`.
  - `Part2LookupModels.kt`, `GuidePart2LookupValidator.kt`.
  - `LaunchFlags.PART2_ENABLED = false`; `Part2Availability.kt`.
  - `FeatureGate.PART2_ORDERS`; the `featureMeta` branch with copy in `Part2Strings.kt`; the `isAdvertised` rule.
  - `check_r8_guide.py` extended to the Part II folders.
- R8: `assembleRelease` + `check_r8_guide.py`; the bundle ships byte-identical.
- Exit: gate green; with the flag off the paywall catalog is unchanged (test).
- Debt watch: `GuideModels.kt` (285) must not grow; the new models go in their own file.
- Done 2026-10-23 (Phase 1 summary in the session). Where the build differs from the bullets above:
  - Case `sub` lines (chapter and pages) are not shipped. Nothing reads them before Phase 4a, which can derive them from the code pages.
  - The nav limits (title words, ribbon and case sizes) live in `nav.json` and ship in the section, as the Guide's `nav.json` does,
    so the Kotlin validator holds no second copy. The text limit (300) comes from `part2_codes_check.NAME_MAX`.
  - `refresh.py` has no new step: `compile.py` runs `part2_nav.check()`, so `refresh.py --check` fails on a broken layout (shown with a
    deliberate break). `bundle.py` refuses to build from one.
  - The Kotlin models are in `guide/model/Part2LookupModels.kt`, so the existing R8 gate already covered them. The Part II folder
    `shared/.../part2/` does not exist yet; `check_r8_guide.py` now walks it with each file's own package, and says so when it is
    absent. See Phase 11.
  - `GuideBundle` needed one wiring field (`part2`), which added 2 lines to `GuideModels.kt`. To keep the file from growing, the two
    change-log models moved to `GuideChangeModels.kt` (same package, no caller change): 285 → 273 lines.
  - Two helpers that would have been third copies, `isGuideId` and `duplicateIdProblems`, now live in `GuideValidationHelpers.kt`;
    `GuideBundleValidator` and `GuidePreClaimValidator` call them instead of their private copies (behaviour unchanged, their tests green).
  - `built` is true only on `leave` and `records`, as the plan says, although no Leave draft exists yet. Tiles are a Phase 4a matter.
- Owner decisions on the Phase 1 doubts (2026-10-23), all as recommended, so no code changed:
  - `built` means "tiles show"; each ribbon is flipped as its drafts ship.
  - The validator keeps reading its limits from the shipped section; the real-bundle contract test pins the agreed values.
  - The R8 gap for `part2/` stays in Phase 11 with no placeholder model.
  - Catalog copy stays as is until the Phase 10 paywall-copy review.
  - The no-group serials (3.28, 11.8) stay mirrored in Python, Kotlin and the contract test, pinned by tests.
  - The `GuideChangeModels.kt` move and the shared validation helpers are accepted.
  - The case `sub` line is derived in Phase 4a from the code pages, not authored.
  - `ack` versus `warn` for the manual's "will be rejected" rules is decided at Phase 2b.

### Phase 2a: Template format, validators, engine core, `CivilCalendar` move
- Goal: the engine turns a `LeaveEventInput` into a `Part2Slip` using *synthetic* templates. Content comes in 2b.
- Unit tests first:
  - `CivilCalendarTest`: characterization tests of `DutyDays`, written and green *before* the move, unchanged after;
    the pre-claim tests stay green.
  - `test_part2_templates.py` refuses each of:
    - an unknown code, a slot used but not declared, a filled "not required" Data column;
    - an unknown rule id, a missing page citation, a link without a `manual`/`practice` basis;
    - a printed-code override other than REJOINAL → REJOIN.
    - It also checks that copycheck skips narratives and checks help text.
  - `GuidePart2TemplateValidatorTest`: the Kotlin recheck of the same rules.
  - `OccurrenceRendererTest`: slot filling, blanks for missing references, plain-text output.
  - `SlipOrderingTest`: date order (para 6.4); a synthetic Group B occurrence is split into its own order (para 7.1);
    the order group is A/AM when any occurrence is A/AM.
  - `Part2RuleContractTest`: bundle rule ids = registry ids.
  - `Part2SlipPrivacyTest`: no PAN placeholder, no text the officer did not type.
  - Determinism: the same input gives an identical slip.
- Integration tests: synthetic templates go through the real Python tools → bundle JSON → `GuideBundleParser` → engine,
  producing a slip.
- Perf: `Part2EngineIosPerfTest` timing bound.
- Implement: the template JSON schema, `part2_templates.py`, `Part2TemplateModels.kt`, the validator, the domain files
  listed in the module map (engine, renderer, ordering, rules registry, slip), `Part2Limits.kt`.
- R8: `assembleRelease` + `check_r8_guide.py` (template models).
- Exit: gate green.
- Debt watch: the engine must not know any specific code. Code-specific behaviour lives only in templates and rule
  families; a test fails if the engine source names a code.
- Done 2026-10-23 (Phase 2a summary in the session). Where the build differs from the bullets above:
  - **Rule model.** `part2/rules.json` declares every rule (`check` with severity `warn` or `ack`, or `condition`; basis `manual` + page, or
    `practice`) and the template text limits. It ships in the section as `rules` and `template_limits`, so Kotlin holds no second copy.
    Logic lives in `Part2Rules` (a registry of `CheckRule`/`ConditionRule`). Both severities are supported, so the 2b decision is a
    one-word JSON edit. The real `rules.json` and `Part2Rules.shipped()` are empty; the bundle grew 299,350 -> 299,476 bytes (three keys).
  - **Template format.** One template per code: `narrative` with `{slot}` / `{slot.no}` / `{slot.date}` placeholders, slots (`date`,
    `number`, `text`, `ref`, `days` counted between two date slots), columns `from`/`to`/`data` x4 (`required`, `optional`, `none`),
    optional `authority` and `certificate`, `rules`, `links` (`manual` + page, or `practice`, with an optional condition rule). The
    placeholder grammar has one home, `guide/data/Part2Placeholders.kt`, read by the validator and the renderer. Python:
    `part2_rules.py`, `part2_slots.py`, `part2_templates.py` (split to stay under 300 lines); `part2_synthetic.py` writes the Kotlin
    integration fixture `shared/src/androidUnitTest/resources/part2/synthetic_section.json` through the real `for_bundle`, and a tool test
    fails if it is stale. Templates carry a `rev` (8 hex of SHA-256 of the shipped template); Kotlin checks its shape only.
  - **Engine.** `LeaveEventInput(requests, lastOrder)`: each `OccurrenceRequest` has a code, slot values and yes/no answers; two leave
    years are two requests. `Part2DraftEngine` expands links (each code once per request; a link whose template does not exist is listed
    in `Part2Slip.undrafted`, never dropped), drafts through `OccurrenceDrafter`, arranges with `SlipOrdering` (date order; claims in a
    separate order; family FINANCIAL when any A/AM), and runs the checks. A slot is required when a text or a required column uses it; an
    order reference is never required and prints `____`. Refusals name the template and slot, never the value.
  - **CivilCalendar.** `calendar/CivilCalendar.kt` holds `CalendarDate` and the arithmetic. `DutyDays` stays in the pre-claim package as a
    thin adapter that words refusals as `PreClaimIssue`, and `typealias CalendarDate` keeps every pre-claim import unchanged.
    `CivilCalendarTest` (7 tests, through the old names) was green before the move and is unchanged after. `PreClaimDates.format` and
    `.YEARS` now read `CivilCalendar`, removing two copies.
  - **Shared helper.** `textProblems` and `hasAngleBrackets` moved into `GuideValidationHelpers.kt`; the lookup validator's private copy of
    `textProblems` was deleted (messages identical, its tests green).
  - **A guard that paid for itself.** `Part2EngineSourceTest` (no engine file names any of the 321 codes) caught that my enum constant
    `MISSING` is also a manual code, so it is now `NOT_ANSWERED`.
  - **Mirrored constants.** `PRINT_OVERRIDES` (REJOINAL -> REJOIN) is held in Python and in the Kotlin validator, pinned by a test on each
    side, as the no-group serials are.
  - **Not done in 2a** (see Phase 11): the first real rule/template (2b); no `@Serializable` class lands in `part2/`.

### Phase 2b: Content batch 1 — annual set + ration four + R&H; owner review
- Owner decisions at phase start:
  - Prefix/suffix printing: the manual's first/last-date columns, or the practice date lists.
  - Whether `ack` checks block printing or only need acknowledging.
  - R&H scope beyond AL (practice evidence is AL only; a 9-day CL had no stop).
- Unit tests first (`LeaveRulesTest`, `RationRulesTest`, golden slips on synthetic dates):
  - 30 days of AL with prefix in an R&H station, on ration strength:
    - SORS + GTDRSHNA and STPRH from the first prefix day;
    - TORS + STPRSHNA and GTDRHxx from rejoining;
    - one A-group order.
  - Note 1's example counts 62 days.
  - 40 days earlier + 25 now → `ack` citing note 2 p.115.
  - Two leave years → two GTDAL occurrences.
  - Prefix + suffix > 5 days → `warn`.
  - CL with AL → refused.
  - A 9-day CL: the ration four only, and no R&H rows (R&H rows are always labelled practice).
  - EXTNAL/REJOIN/VOLREJ/RECALL quote the grant order, or print a blank; VOLREJ/RECALL cancel the remaining days.
  - Not on ration strength → no ration four.
- Integration test: every batch-1 template in the *real shipped bundle* renders its golden slip.
- Implement: `templates/annual.json`, `ration.json`, `rh.json` (each ≤ 300 lines); the `LeaveRules` and `RationRules`
  families; `tools/part2_review.py` → `part2/part2_review.md`.
- Owner reviews batch 1 against the PDF; corrections are applied in this phase.
- Exit: gate green; owner review recorded.
- Debt watch: a narrative typo found in review is fixed in the JSON, and its golden test is updated in the same commit.
- Done 2026-10-23 (Phase 2b summary in the session). The owner's decisions are in DECISIONS.md Round 11 (2a accepted; one order per claim, the
  header lists group letters, identical rows merge; the manual's prefix/suffix columns; `ack` blocks printing until ticked; R&H from annual
  leave only). Where the build differs from the bullets above:
  - **The format needed five additions** that real rows forced, each written test-first in Python and Kotlin and mirrored in both validators:
    computed date slots (`earliest`, `latest`, `next_day`, reading only slots declared before them, so no loop can exist); `if_missing` (an
    optional date, number or text and the word printed without it, "Nil"); `for` (an input only a check reads, printed nowhere); column
    `text` (Data 4 "order no. and date", the fixed R&H rate word, "Corps, notification no., date"); and `CivilCalendar.nextDay`. The engine
    still names no code (`Part2EngineSourceTest`).
  - **Content:** 24 templates (`annual.json` 8, `ration.json` 4, `rh.json` 12 = STPRH + the 11 GTDRH grant codes), 19 rules (6 checks, 13
    conditions). The R&H re-grant is one link per GTDRH code, each with its own condition (`rh_gtdrh12`, ...), so the officer's own code is
    the only one drafted. `GTDAL` and `GTDALCF` carry 16 links.
  - **Rule families:** `LeaveRules` and `RationRules` as planned, plus `AreaRules.kt` (the R&H conditions), started here instead of in
    Phase 9 because the re-grant needs it; Phase 9 extends it. `Part2Rules.shipped()` is their only registration point.
  - **Slip model:** `Part2Order.groups` (the header letters); each Group B claim is its own order.
  - **Limits confirmed against the longest real text** (Phase 11 item 3): narrative 600 (longest 570), label 40 (36), help 160 (156), slots 10
    (8), links 16 (16), rules 4 (2), note 20 (14). Pinned by `Part2LookupContract.assertRulesMatchTheRegistry`.
  - **Two guards had to learn that `from` is a template column:** the Python bundle test and `GuideSecurityContractTest` forbid a `"from"`
    key (card provenance); both now skip the `part2` section only, so cards, figures and worksheets are still held to it.
  - **Owner review pack:** `part2/part2_review.md` (24 sections, generated by `tools/part2_review.py`, stale-checked by a test). It lists ten
    judgment calls first; the owner settled each on 2026-10-23 (DECISIONS Round 12). The owner has not yet checked the templates against the
    printed pages.

### Phase 3: Storage — schema 15, repository, backup v5 with bounded restore
- Goal: drafts, orders and pages persist encrypted, survive upgrade, and travel in v5 backups safely.
- Unit tests first:
  - Payload round-trip; a tampered row, or one moved to another id, throws the fixed exception with no cause.
  - The repository refuses a page over 500 KB, more than 10 pages, or an oversized payload (`Part2Limits`).
  - `Part2Backup`:
    - REPLACE and MERGE semantics; unreadable rows skipped.
    - Oversized or surplus rows in a crafted backup are skipped before decode.
    - v4 → empty vault; v6 refused.
- Integration tests (real crypto, in-memory Room, Android and iOS):
  - `PayslipDatabaseUpgradeTest` reaches 15 from every exported schema.
  - `clearAllUserData` and `replaceWithBackup` empty the new tables.
  - Export → restore round trip including pages.
  - 120 synthetic 500 KB pages export and restore within a measured time and peak memory (recorded).
- Implement:
  - `Part2Entities.kt`, `Part2Payloads.kt`, `Part2Dao.kt`, `AutoMigration(14, 15)` + the schema export.
  - `Part2Repository` + `RoomPart2Repository`, `Part2Backup`, the v5 fields in `PortableBackup` and `BackupRows`.
  - The fakes in `shared-test-fixtures`; Koin wiring.
- R8: `assembleRelease` + `check_r8_guide.py` (Room `_Impl`, payload serializers).
- Exit: gate green.
- Debt watch: `PayslipDao.kt` (204) must not grow — the new DAO is separate; `PayslipBackupService.kt` (180) stays
  under 300 by delegating to `Part2Backup`.

### Phase 4a: Lookup UI (free) — Part II home, ribbons, tiles, code pages, search
Nudge: connect the Pixel 9.
- Unit tests first:
  - `GuideNavStateSaverTest`: the new lookup destinations round-trip (ids only); `retainKnown` and the flag cut unknown
    or disabled levels.
  - `Part2LookupViewModelTest`: ribbons (built only + Records), case codes, code page fields, "Group not printed in
    ODP-2026", the REJOINAL note, search by code and by name.
  - The entry decision: HIDDEN when the flag is off; lookup open for everyone.
- Integration tests: Robolectric Compose with the real bundle — Guide Home row → Part II home → Leave → case → code page;
  search → code page; back at each level; flag off → no row.
- Implement: `GuideTabPart2.kt` (the `GuideTabPreClaim` pattern), the Part II home, ribbon and code screens,
  `Part2LookupViewModel`, `Part2Module`, the theme tokens and `Part2Strings` entries.
- Device: browse, search, process death on a code page, tab re-tap, light and dark; flag flipped locally and reverted.
- Exit: gate green + R8; screenshots kept in the session scratchpad only.
- Debt watch: `GuideTab.kt` (257) gets at most a delegation line; anything more moves to `GuideTabPart2.kt`.

### Phase 4b: Leave draft flow + slip preview (Premium), autosave
Nudge: connect the Pixel 9.
- Unit tests first:
  - `LeaveDraftViewModelTest`: inputs → slip; checks and linked orders; autosave from the first step; restore from a
    draft id; discard on lock.
  - The entry decision: LOCKED for free users; lapsed users can open and delete but not create or edit.
- Integration tests:
  - Robolectric: case → form → slip preview with checks and links; process death mid-form restores the draft; lock
    discards the in-memory model.
  - `GatedNavigationInvariantTest` walks the Part II entry.
- Implement: the form and slip screens (sections split to keep composables ≤ 50 lines), `LeaveDraftViewModel` +
  `LeaveDraftUiState`, `discardTransientForms()`.
- Device: draft AL with ration and R&H, kill the process and restore, lock and unlock, check "Force Free".
- Exit: gate green + R8; flag reverted.
- Debt watch: a validation message must never be built in the composable. Messages come from the rule's id via
  `Part2Strings`.

### Phase 5: Print + "Check the HRMS draft"
Nudge: connect the Pixel 9.
- Unit tests first:
  - `SlipLayoutTest` (pure): A4 from `PdfLayoutSpec`, block layout, wrapping, page breaks, the blank last-order field,
    colors from `SlipLayoutSpec`.
  - `LeaveDraftViewModelTest`: printing records the snapshot only after the printer reports completion.
- Integration tests:
  - Android: the Robolectric `PrintDocumentAdapter` writes a valid PDF with the expected page count; no `ACTION_SEND` and
    no `FileProvider` URI in the path; a missing Activity is a no-op.
  - iOS: the rendered PDF data is non-empty with the expected page count.
- Implement: `SlipLayout`, `SlipLayoutSpec`, the `SlipPrinter` interface; `AndroidSlipPrinter` (built in composition from
  `LocalContext`'s Activity) and `IosSlipPrinter` (`UIPrintInteractionController`, in-memory PDF data); the HRMS check
  screen. The print job name comes from `Part2Strings`.
- Owner decision at phase start: slip layout (blocks per occurrence, as in the mock, or the manual's table).
- Device: print to "Save as PDF" and compare field by field with the preview.
- Exit: gate green + R8; flag reverted.
- Debt watch: `PdfLetterFormatter.wrapLine` is reused, not copied. If the slip needs a variant, the shared function is
  generalized with its existing tests kept green.

### Phase 6a: Page pipeline — scanner spike, `PageScanner`, `PageEncoder`, merged-manifest guard
Nudge: connect the Pixel 9.
- Step 1, before any dependency:
  - Add `scripts/check_merged_manifest.py` + `scripts/merged_manifest_baseline.json` (permissions and exported
    components of today's release build) + its tests in `scripts/tests`, wired into CI.
  - Then a spike: ML Kit's merged manifest adds no `CAMERA` permission and no exported component; scanning works with
    the device offline; its output location is known. If any check fails, stop and bring the owner the CameraX
    fallback.
- Unit tests first:
  - `PageEncoderTest`: no APP1/Exif or GPS; long edge ≤ 1600 px; ≤ 500 KB by stepping quality down (Robolectric native
    graphics on Android; `iosTest` on iOS).
  - The scanner result is mapped to pages; the cache files are deleted on success *and* on failure.
- Integration tests:
  - `FakePageScanner` → `PageEncoder` → repository → backup round trip.
  - XCTest for the VisionKit presenter's in-memory result path.
- Implement: `PageScanner`/`PageEncoder` interfaces and implementations; the ML Kit dependency (pinned);
  `NSCameraUsageDescription`, plus a test that it matches `Part2Strings`.
- R8: `assembleRelease` + `check_r8_guide.py` + `check_merged_manifest.py`.
- Exit: gate green; the baseline diff is reviewed and explained.
- Debt watch: no plaintext page may outlive the encoder call. A test asserts the temp directory is empty afterwards.

### Phase 6b: Vault UI — order no./date, linking, last-order line, follow-up references
Nudge: connect the Pixel 9.
- Unit tests first:
  - `VaultViewModelTest`: order no./date bounded and validated; linking to a draft.
  - The last-order line is the latest order date, or a blank.
  - Follow-up codes (EXTNAL/REJOIN/VOLREJ/RECALL) take the grant's published no./date.
  - Lapsed users can view and delete, but not capture.
- Integration tests: Robolectric draft → "Add the published order" → fake scan → save → vault list → the rejoining slip
  shows the order number; backup → restore keeps it.
- Implement: the vault screens, `VaultViewModel` + its state, the Settings vault-size warning (60 MB).
- Device: scan a printed test page (never a real order); check the exported page with `exiftool`; back up and restore.
- Exit: gate green + R8; flag reverted.

### Phase 6c: CANCEL / CANCELS — content batch 2 and flow
- Unit tests first:
  - Golden CANCEL (col 6 "ALL", cols 8–9, certificate) and CANCELS (code in col 6, serial in col 7, one occurrence only).
  - Time-bar note over 2 years.
  - "Cancel and re-draft" opens a new draft prefilled from the cancelled one.
- Integration test: vault order → cancel → slip → print record → the new draft is linked to both.
- Implement: `templates/cancel.json`, the cancel flow in `VaultViewModel`, the review batch 2 entries.
- Exit: gate green; owner review recorded.

### Phase 7a: Reminder planner + in-app Due
- Unit tests first (`ReminderPlannerTest`, policy from `ReminderPolicy`):
  - Return date at 09:00.
  - 14 days after printing, then every 30 days, at most 6.
  - ACCLVE on 1 March of Y+1.
  - None after publication or cancellation.
  - Nearest 20; idempotent rebuilds.
- Integration tests: the Due list and the post-import Due card from real repository data (in-memory Room); open each item
  to the right screen.
- Implement: `ReminderPlanner`, `ReminderPolicy`, `DueViewModel`, the Due screen and the import card.
- Exit: gate green + R8.
- Debt watch: the planner owns all timing. Neither the UI nor the scheduler computes a date.

### Phase 7b: OS notifications on both platforms
Nudge: connect the Pixel 9.
- Unit tests first:
  - The scheduler rebuilds from the planner's output (fakes).
  - The text comes from `Part2Strings` and is generic.
  - Permission is asked only at the first reminder and never again after a decline.
  - Deep link: an invalid or unknown id is ignored; held in `PendingPart2Link` while locked.
- Integration tests:
  - WorkManager `TestDriver`: enqueue → run → the notification is posted with an explicit, immutable `PendingIntent` →
    tap → navigation.
  - XCTest for the iOS delegate → `NavBridge`.
- Implement:
  - `ReminderScheduler` (WorkManager + channel; `UNUserNotificationCenter`) and `NotificationPermission`.
  - The Swift delegate; the pinned `androidx.work` dependency.
  - A debug-only short delay for device walks, inert in release (the `DevOverride` pattern).
  - If `App.kt` needs more than one line, split it first.
- R8: `assembleRelease` + `check_r8_guide.py` (worker class) + `check_merged_manifest.py` (WorkManager's permissions
  reviewed and recorded in the baseline with reasons).
- Device: prompt, delivery, tap while locked and unlocked, reboot survival.
- Exit: gate green; flag reverted.

### Phase 8: Rest of Chapter 5, incl. ACCLVE and ENCASH (batch 3)
- Owner decisions at phase start: R&H for furlough, study, maternity and other leave; stale note references (TERLEV
  note 10, p.119).
- Unit tests first: golden slips for the 22 serials.
  - `AcclveCalculatorTest`:
    - (c) = min(30, 60 − AL used in Y), with an officer override.
    - (a) from Leave Bank ACCLVE(Y−1) or the previous ACCLVE draft; (b) = LVELTC days dated in Y.
    - (d) ≤ 300, with "(Restricted to 300)".
    - A date before 1 March of Y+1 → `ack`.
  - ENCASH: break-up prefilled from Leave Bank, missing years typed, total ≤ 300 in col 8.
  - Leave ≥ 180 days: no ration four, and the p.99 note is shown.
  - Furlough after AL is ordered AL first.
- Integration test: the cross-check against the statement of Y+1 through the real `LeaveBankCalculator` with the
  synthetic Leave Bank fixture. A mismatch shows both figures and changes nothing.
- Implement: `templates/ch5_*.json` (split to stay ≤ 300 lines each), `AcclveCalculator`, the ENCASH input builder.
- Exit: gate green; owner review of batch 3 recorded.

### Phase 9: Area re-grants — HAA, SCCIA, field, TPTA (batch 4)
- Unit tests first (`AreaRulesTest`):
  - HAA: CL 13 → none, 14 → re-grant.
  - Field: CL or hospitalisation 14 → none, 15 → re-grant.
  - SCCIA: always re-granted.
  - Certificates and Corps notification fields present; no stop rows for HAA, field or SCCIA.
  - TPTA: 05 Jul–03 Aug → none; 25 Jun–05 Aug → STPTRAN, then GTDTRANx on rejoining, with the reason in cols 3 and 7.
- Integration test: AL in a field area with R&H and ration → one order in date order, from the real bundle.
- Implement: `templates/area.json`, the `AreaRules` family, the area questions in the form.
- Exit: gate green; owner review of batch 4 recorded.

### Phase 10: Release hardening
Nudge: connect the Pixel 9.
- Owner sign-off on all 59 templates and a spot check of the code pages.
- Android: back up first, then install the `assembleRelease` APK with `-i com.android.vending`, flag flipped locally.
  Run `check_r8_guide.py` and `check_merged_manifest.py`. Walk the full loop: draft → print → scan → order no. →
  reminder → backup/restore v5 → cancel.
- iOS: link + `iosSimulatorArm64Test` + XCTest; give the owner an iPhone checklist (VisionKit, print, notification
  prompt and tap, native back, lock).
- Final `security-review` over the whole branch diff.
- Docs: plan status; a Part II section in `18_content_update_runbook.md`; one bullet in CLAUDE.md "Feature areas"
  (stays ≤ 200 lines); the paywall catalog copy reviewed by the owner.
- `PART2_ENABLED = true` only on the owner's word, in its own commit. Push/PR only when asked.

### Phase 11: Pending items (collected from earlier phases)
Each item says what is open, why it was deferred, and how it is closed. Every phase adds its leftovers here instead of leaving
them in its summary. An item closes with a test or a command output, not a statement.

1. **Phases 1 and 2a: the R8 gate and the Part II model folder.** (2a part closed; Room and worker parts open.)
   - Closed in 2a: `shared/.../part2/` exists. `check_r8_guide.py` now prints the count per folder and fails on any `@Serializable`
     declaration of a kind it does not prove (sealed class, enum, object, interface, custom serializer);
     `UnlistedSerializableTest` covers it, including a real-tree test and a deliberate `@Serializable sealed class` that failed it.
     Release output: `32 models and serializers kept (guide/model: 32 models, part2: 0 models)`.
   - Still open: no `@Serializable` class landed in `part2/` in 2a (template models live in `guide/model`, covered), so `part2: 0`.
     Phase 3 adds the `PayslipDatabase_Impl`/`Part2Dao_Impl` check with the DAO, and Phase 7b the worker check. Closed by the
     `check_r8_guide.py` output on the release build naming them.
2. **Phase 2a: the rule contract and the copy check had nothing real to compare.** CLOSED in 2b.
   - Evidence: `Part2LookupContract.assertRulesMatchTheRegistry` now compares 19 declared rules with 19 implemented (and 24 templates); a
     deliberate break of the shipped registry failed it. `refresh.py` with the local sources present printed `copy check: CLEAN` over the
     real help text and labels (narratives, certificates and column text are the manual's own wording and are skipped on purpose).
3. **Phase 2a: limits were provisional.** CLOSED in 2b, with one caveat that is item 8.
   - Confirmed or tightened against the longest real text, and pinned by the real-bundle contract test: narrative 600 (longest 570), label
     40 (36), help 160 (156), slots 10 (8), links 16 (16), rules 4 (2), note 20 (14). `Part2Limits` (typed text 200, order no. 40, number
     9,999, requests 10) are the officer's input bounds, not content, and stay.
4. **Phase 2a: engine behaviours the owner had not seen.** CLOSED in 2b (owner, 2026-10-23): each Group B claim is an order of its own; the
   order header lists the group letters present; identical rows merge. Pinned by `SlipOrderingTest` (`eachClaimGetsItsOwnOrderInDateOrder`,
   `theOrderHeaderListsEachGroupLetterPresentOnceInTheManualsOrderAndInfersNone`, `twoIdenticalRowsAreOneRow`). The print layout (phase 5)
   decides how the header letters look, not which print.
5. **Phase 2a: the Kotlin tests were not run red before the implementation.** NOT CLOSABLE for 2a. From 2b on it was done: every new Kotlin
   test (calendar, ordering, computed slots, validator features, rule families, golden slips) was run and seen to fail for the missing
   behaviour before the code was written (22, 9 and 30 failures respectively; the model fields and rule families were stubbed first so the
   failures were behaviour, not compile errors).
6. **Phase 2a: a template's `rev` is shape-checked in Kotlin, not recomputed.** `commonMain` has no SHA-256, and the bundle ships inside the
   signed app, so a changed `rev` needs a changed app. The Python stale-bundle guard is the check. Decide at Phase 10 whether to add a
   recompute (`expect`/`actual` hash) or accept; closed by that decision recorded in the Phase 10 summary.
7. **Phase 2b: the owner has not reviewed batch 1 against the printed pages.** OPEN. Why: it needs the owner with the PDF; the build is
   finished and the pack is ready. How closed: the owner works through `part2/part2_review.md` (ten settled judgment calls first, then 24
   sections each naming its printed page); corrections go in the template JSON and the golden tests in the same commit, and the owner's
   word is recorded here. Until then 2b's exit ("owner review recorded") is not met.
8. **Phase 2b: later batches will need decisions on limits and reissues.** OPEN. `links` is 16 and `GTDAL` uses all 16; Phase 9
   adds the HAA, SCCIA, field and TPTA re-grants to the leave grants, so the limit rises with its reason (owner, Round 12; no link-group feature). `cl_combination` knows only the annual set (GTDAL, GTDALCF, EXTNAL), and an extension, voluntary rejoining or recall
   does not move the ration or R&H dates already drafted; phase 8 adds the rest of Chapter 5 and settles those reissues (owner, Round 12: nothing drafted until then). Closed
   by the phase 8 and 9 summaries.

## Risks and controls

| Risk | Control |
|---|---|
| A template misstates the manual | Page/note on every template; owner review per batch; `part2_review.md` side by side |
| Practice presented as a rule | `practice` basis is mandatory on non-manual links; "Seen in practice" label |
| Identity or the PAN placeholder reaches a slip | The type has no identity fields; no payslip input; text-scan test |
| Photo metadata or a gallery copy leaks location | Re-encode drops metadata (tested); scanners never write to the gallery; temp files deleted (tested) |
| A crafted backup exhausts memory | Bounded restore against `Part2Limits` before decode (Phase 3) |
| A library adds a permission or exported component | `check_merged_manifest.py` baseline in CI (from 6a) |
| A dark feature shows in the paywall | `isAdvertised(PART2_ORDERS)` follows the flag (Phase 1 test) |
| ML Kit needs permission or network | 6a spike; CameraX fallback brought to the owner |
| Android print crashes without an Activity | Printer built from composition; no-op test |
| R8 strips models, Room or workers | Extended `check_r8_guide.py` in CI; release APK walk |
| Schema bump breaks upgrades | One bump; payloads carry later changes; `PayslipDatabaseUpgradeTest` |
| Kotlin/Native slowness | No regex hot paths; `iosTest` timing bounds |
| A file creeps past 300 lines | File budget per phase; Gradle and Python gates; Part II non-Kotlin size test |
| Grant order unpublished for months (practice lag 5–8 months) | Follow-ups print a blank reference; reminder to add the published order |

## Verification (end to end)

1. The full gate (above) on the final commit.
2. `scripts/check_r8_guide.py` and `scripts/check_merged_manifest.py` on the release build.
3. Pixel 9: debug walks in 4a–7b; release APK walk in Phase 10.
4. Owner iPhone walk in Phase 10.
