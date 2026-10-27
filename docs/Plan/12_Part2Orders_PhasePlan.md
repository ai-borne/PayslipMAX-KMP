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
- ENCASH, pp.140–141: the days are in Data 2 (col 7; corrected in Phase 8, an earlier draft said col 8: the printed table shows Data 1, 3
  and 4 not required), and the narrative gives a year-wise break-up "during the entire service" (note 2).
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
- Backup v5 adds `part2Drafts` and `part2Orders` (Base64 ciphertext in the metadata document) and, when the vault has pages, binary page
  records after it (phase 3b; the first build put pages in the JSON as Base64 and needed about 13 times the vault in memory):
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
| 2b. Content batch 1: annual set + ration four + R&H; owner review | done, accepted (2026-10-23); the check against the printed pages was delegated and is recorded (DECISIONS Round 15, Phase 11 item 7) | ebf6c124, 09bfbf98, 2d192468 |
| 3. Storage: schema 15, repository, backup v5 with bounded restore | done, accepted (2026-10-23) | 8cb0a40b |
| 3b. Chunked backup: pages as binary records, one page in memory at a time | done, accepted (2026-10-23) | 32ca5316 |
| 4a. Lookup UI (free): Part II home, ribbons, tiles, code pages, search | done, accepted (2026-10-23); Pixel 9 walk done (Phase 11 item 12), hosts folded (item 13) | 19416f6e, 34c2f296 |
| 4b. Leave draft flow + slip preview (Premium), autosave | done, accepted (2026-10-23); Pixel 9 walk done (Phase 11 item 15) | bace0c4b, 1ff29b95 |
| 5. Print + "Check the HRMS draft" | done, accepted (2026-10-24); Pixel 9 walk done (Phase 11 item 16); items 17 and 18 closed | 32c9617c, ad91c255 |
| 6a. Page pipeline: scanner spike, `PageScanner`, `PageEncoder`, merged-manifest guard | done, accepted (2026-10-24); spike and the committed pipeline walked on the Pixel 9 (Phase 11 items 19 and 21) | 76404e21, 1b05efea |
| 6b. Vault UI: order no./date, linking, last-order line, follow-up references | done, accepted (2026-10-25); walked on the Pixel 9 (Phase 11 item 25); follow-ups added the same day (DECISIONS Round 21); the iPhone walk is Phase 11 items 14 and 20 | 6e2db804, 0daea659 |
| 6c. CANCEL / CANCELS: content batch 2 and flow | done, accepted (2026-10-25); follow-ups the same day (DECISIONS Round 23); batch 2 checked against pp.226-227 by Claude and walked on the Pixel 9 (Phase 11 items 27 and 28); the owner's own read of the review pack stays open (item 27); the iPhone walk is items 14 and 20 | 216b14c5, 42b5a46a, d5999a23 |
| 7a. Reminder planner + in-app Due | done, accepted (2026-10-25: DECISIONS Round 27); built, gated and walked on the Pixel 9 including the card on a real import; the owner's answers are DECISIONS Round 25; items 31, 32, 34 and 36 closed in the same sprint (Round 26), 33 and 35 open; the iPhone walk is items 14 and 20 | aceb6dba, 5d2de477 |
| 7b. OS notifications on both platforms | done, accepted (2026-10-26: DECISIONS Round 29); built, gated and walked on the Pixel 9; the owner's answers are DECISIONS Round 28; items 37 and 41 closed, 38 an accepted limit, 39, 40 and 42 open; the iPhone walk is items 14 and 20 | 6421b5bb |
| 8. Rest of Chapter 5, incl. ACCLVE and ENCASH (batch 3) | done, accepted (2026-10-27: DECISIONS Round 32); built, gated and walked on the Pixel 9 (2026-10-26); the owner's answers are DECISIONS Rounds 30 and 31; items 8 (part), 11, 33, 44 and 45 closed; item 43 (prefill screens) is Phase 8b, 46 accepted, 47 open (iPhone) | ca2f74a4 |
| 8b. Leave Bank prefill screens for ACCLVE and ENCASH (Phase 11 item 43) | done, accepted (2026-10-27: DECISIONS Round 34); built, gated and walked on the Pixel 9 (2026-10-27); the owner's answer is DECISIONS Round 33; item 43 closed, 48 open (b to d), items 14, 20 and 47 gain the new screens; ran before Phase 9 (Round 32) | 888945df |
| 9. Area re-grants: HAA, SCCIA, field, TPTA (batch 4) | done, accepted (2026-10-27: DECISIONS Round 36); built, gated and walked on the Pixel 9 (2026-10-27); the owner's answers are DECISIONS Round 35; item 8 closed, items 49 to 54 added (50 closed by the ODP-governs rule), items 14, 20 and 47 gain the area questions | dde30a51 |
| 10. Release hardening, owner content sign-off, iPhone walk, docs | not started | — |
| 11. Pending items (collected from earlier phases) | open: items 14, 20, 22, 27, 35, 38, 39, 40, 42, 46, 47, 48, 49, 51, 52, 53, 54 (38 is an accepted limit; 35 is now only the light theme, accessibility and the iPhone; 49 and 51 to 53 are Phase 9's; items 1-4, 6-10, 12, 13, 15-19, 21, 23-26, 28-32, 34, 36 and 50 closed; 5 not closable) | — |

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
  - **Content:** 24 templates (`annual.json` 8, `ration.json` 4, `rh.json` 12 = STPRH + the 11 GTDRH grant codes), 20 rules (7 checks, 13
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
    judgment calls first; the owner settled each on 2026-10-23 (DECISIONS Round 12). The owner had not yet checked the templates against the
    printed pages when 2b was built; that check was delegated and done 2026-10-23, with no corrections (DECISIONS Round 15, Phase 11 item 7).

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
- Done 2026-10-23 (Phase 3 summary in the session). The owner's decisions are in DECISIONS.md Round 13. Where the build differs from the bullets above:
  - **Layout:** `shared/.../part2/data/` holds `Part2Entities`, `Part2Dao`, `Part2Payloads` (flat `@Serializable`, `v: 1`), `Part2Codec` (seal/open), `Part2Bounds`
    (sizes and shapes only; the engine still judges values), `Part2Repository` + `RoomPart2Repository`, `Part2Backup` + `Part2BackupRows`, `Part2Exceptions`;
    `Part2Draft`/`VaultOrder`/`VaultPageRef` are in `part2/domain`; every cap is in `Part2Limits`.
  - **Rows:** ciphertext is a BLOB (hex would double a 500 KB page); drafts and orders carry their id inside the ciphertext, a page carries both ids in a
    binary frame (no JSON/Base64 inside the database). `part2_pages.position` is a plain column beside the plain `orderId` (an ordering, not content).
    No foreign keys. The order -> draft link, number and date are inside the order's ciphertext.
  - **Caps:** pages 500 KB, 10 per order, payload 128 KB, 1000 drafts, 1000 orders, vault 100 MB ceiling (a write past it is refused). The count/size checks are
    inside the DAO's own `@Transaction` methods. A restore examines at most the cap in rows, checks each row's id, text length and the running totals before
    decoding it, and skips and counts the rest (`Part2Restore.skipped`).
  - **Backup:** v5 adds `part2Drafts/Orders/Pages` (Base64 ciphertext under the backup password; pages moved out of the JSON in phase 3b). `PayslipBackupService` gained one constructor argument
    (`part2Backup`, bound in `sharedModule` whatever the flag says, pinned by `Part2WiringTest`), 180 -> 192 lines. Only `part2Module` (the repository) is flag-gated.
  - **Pinned by test:** the seal overhead (48 bytes) against the real `CryptoHelper` on both platforms; the owner's numbers by literal (`Part2LimitsPinTest`);
    the storage folder is blind to codes and logs nothing (`Part2StorageSourceTest`).
  - **Draft print snapshot:** Phase 5 adds `printed` to the draft payload as an optional field inside `v: 1` (older readers ignore it); no schema change.


### Phase 3b: Chunked backup — pages as binary records, one page in memory at a time
- Goal: a full vault backs up and restores inside a phone-sized heap. Owner decision: DECISIONS Round 14 (closes Phase 11 item 9).
- Unit tests first (`PageArchiveTest`, `Part2BackupTest`, `Part2BackupBoundsTest`): the document is found by its magic and an old single-blob archive
  never is; a file whose lengths lie (including `Int.MAX_VALUE`, which wraps a 32-bit sum) is refused; the archive is the vault's size plus a
  few hundred bytes; a record cut out or added, or a cut file, fails the whole restore with a fixed message and the device is untouched; a record
  over its size, past the count, past an order's ten, past the vault ceiling or without an order is never opened; the page is opened once to verify
  and once more only when the transaction writes it.
- Integration tests: the 120-page scale test runs on the JVM in a 256 MB heap (`maxHeapSize` for every shared unit test) with both databases and the
  archive held at once, and on iOS with the process peak recorded; the real-SQLite export/restore, mid-restore failure rollback and iOS Room tests
  run unchanged on the new format.
- Implement: `PageArchive` + `PageArchiveWriter` (format, one exactly sized buffer), `Part2PageRestore` (header checks before any read,
  verification, lazy re-encryption), `Part2RowPass` (the draft/order rows, moved out of `Part2Backup`), `BackupRows.part2Pages` as a `Sequence`
  inserted in batches of `Part2Limits.PAGE_INSERT_BATCH`, `Part2PageBackup` removed from `PortableBackup`.
- Done 2026-10-23. Measured on the 120 x 500 KB vault: archive 78 MB -> 58 MB; JVM export extra heap 821 MB -> 174 MB and restore 443-1,132 MB -> 102 MB
  (sampled heap in use, garbage included), inside a 256 MB heap; export 1.1 s and restore 1.2 s on the JVM; iOS export 0.6 s, restore 1.0 s, process peak 469 MB (was 1,221 MB). Mutation checks found and closed four gaps: the
  magic, a record length that lies within the page limit, an eager (not lazy) restore, and `8 + Int.MAX_VALUE` wrapping negative (a real bug the new
  test caught before any mutant). One mutant is equivalent and kept: a negative page count in the trailer is also caught by the "records end where the
  trailer begins" check.

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
- Built 2026-10-23 (Phase 4a summary in the session). Where the build differs from the bullets above:
  - **Destinations** are a sealed sub-interface, `Part2Destination` (`Home`, `Ribbon(ribbonId)`, `Case(caseId)`, `Code(code)`), in the Guide package (Kotlin
    requires that of every inheritor of `GuideDestination`), so the Guide's `when` needs one branch for all four. The saver writes `part2|home`,
    `part2|ribbon|<id>`, `part2|case|<id>`, `part2|code|<CODE>`: ids only. `Code` takes any one of a variant serial's codes and opens the whole serial.
  - **Logic is in `shared`**: `Part2LookupIndex` (tiles, case, code page, search; pure, no regex) and the content types in `Part2LookupContent.kt`.
    `Part2LookupViewModel` holds only the typed query (a Koin `single`, like the Guide's own search, so it survives a code page and a tab switch;
    cleared on each entry from Guide Home; cut at 100 characters; no `CrashReporter`) and reads the loaded Guide. Search ranks an exact code or serial
    over a code prefix over a name word, needs every word to match, and reaches all 303 serials including those of unbuilt ribbons.
  - **Entry** has two states, `HIDDEN` and `OPEN` (`part2Entry`): lookup is free, so there is no locked state until 4b. A bundle with no `part2` section
    also hides it. Off, `part2FactoryOrNull` is null and Koin is never asked for the model. A restored Part II stack under a flag-off build or an old
    bundle is cut back to Guide Home, every Part II level at once.
  - **The REJOINAL note** comes from the template's `print` field (`Part2PrintedAs`), not from a name in the UI; `Part2LookupSourceTest` proves the
    screens name none of the 321 codes. "Group not printed in ODP-2026" shows on the code page and the list row when the group is null.
  - **No theme token was needed**: the screens reuse the Guide's tile, pill and card components. Part II copy is in `Part2Strings`.
  - **`GuideTab.kt` grew 257 -> 265, then the owner had the hosts folded** into `GuideHosts` (Phase 11 item 13): now 263 and pinned by a test. It is not "one
    delegation line", because a sealed `when` branch and the factory parameter cannot share one line. All of it is delegation; the logic is in `GuideTabPart2.kt`. `GuideHomeScreen` hit the 50-line composable limit with its two new parameters, so its two trailing notes rows moved into a
    private extension (no behaviour change). See Phase 11 item 13.
  - **Tests**: red first (20 of 20 shared and 36 of 54 composeApp new tests failed on missing behaviour), then 30 deliberate breaks, all killed; three
    survived the first round and each exposed a test gap that is now closed (`5.1` is not `5.10`; digits in names must not rescue `3.2`; no model is made
    for a bundle with no section). `Part2BrowseContract` runs on the real bundle on both platforms: 303 serials, 321 codes and 303 names each found.

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

- Built 2026-10-23 (Phase 4b summary in the session). The owner's answers are DECISIONS Round 17 (drafts list on the Part II home, "Add another occurrence", ack ticks now, prefill of area details only). Where the build differs from the bullets above:
  - **The form is derived, not written per code.** `LinkExpander` (the engine's link expansion, extracted unchanged under its tests) is read by both the engine and the new `LeaveFormPlanner`, so the form asks exactly the slots the orders on the slip print (`Part2DraftEngine.templatesFor`). Yes/no and one-of questions belong to the rule families (`AnswerQuestion`, `RationRules.questions`, `AreaRules.questions`, registered in `Part2Rules.shipped()`); a contract test proves every condition a real link names is answered by exactly one question. `LeaveEventEditor` makes every edit under `Part2Limits`; `VaultOrders.lastOrder` is the SSOT for "last Part II order"; `Part2DraftSummary` words a list row.
  - **Destinations** added to `Part2Destination`: `Locked` (the upgrade panel, no id), `Draft(draftId)` and `Slip(draftId)`, saved as `part2|locked`, `part2|draft|<id>`, `part2|slip|<id>`. `knows` checks the id is a plain token. The plan said `retainKnown` cuts an id the repository no longer has; `retainKnown` runs before any repository answers, so the cut is done by the level once its model answers `GONE` (`GuideNavState.cutDraft`); both are tested.
  - **Held model.** The open draft's model lives in `GuideNavState.part2Draft` like the TA/DA worksheet, closed when the stack leaves the draft levels and by `discardTransientForms()` (which replaced `discardPreClaim()` on the `App.kt` lock line; `PreClaimSecurityContractTest` pins the line and its worksheet half is now proved with a held worksheet).
  - **`Part2Models`** (lookup + drafts list + draft maker) is what the Guide tab is handed, in place of the lookup model alone; `GuideTab.kt` kept its 263 lines (the factory type changed in place). `GuideAccess` gained `part2Unlocked` (its own gate, `PART2_ORDERS`), placed before `onUnlock` so every trailing-lambda caller is unchanged.
  - **Entry decision** `part2DraftEntry(unlocked, hasDrafts)`: OPEN, VIEW_ONLY (no entitlement but a stored draft: open, read, delete) or LOCKED. No "was Premium" flag exists.
  - **Autosave** is one write 300 ms after the last change, from the first change only; leaving the form or a lock writes a pending change at once; delete cancels it. The event is held atomically because the area-details prefill edits from a background coroutine.
  - **`GatedNavigationInvariantTest`** (named in the bullets) walks Smart Insights cards against the paywall catalog and Part II is not a `Screen`, so it was not edited; the same invariant for Part II (no route reaches the form without OPEN) is `Part2DraftAccessUiTest`, one test per entitlement state and per way in.
  - **Seams for Phase 5:** `SlipUi.readyToPrint` is the print gate (no `ack` unticked, ticks never saved, cleared by any edit); Phase 5 adds the Print button and the draft's `printed` snapshot. Nothing here prints, shares or writes a file; `Part2DraftSourceTest` forbids all of it.
  - **Tests**: red first (before the code: 24 of 36 and 3 of 9 new shared tests; 15 of 16 builder, 18 of 19 model, 5 of 6 drafts-list, 7 of 13 navigation and entry, and 17 of 17 screen tests in composeApp, all failing on missing behaviour, none on a compile error), then 77 deliberate breaks (70 planned + 7 for the prefill) with a first-round result of 8 survivors/invalid, each a real gap or a dead line, all closed: answer length and chooseOne cap hidden behind other limits (M06, M08), request code unchecked with room to spare (M10), acknowledge on a gone or closed draft (M50), a lock test that held no worksheet (M51), a held-model test whose model was never made because the Guide had not loaded (M64), an invalid mutant rebuilt (M25) and a prefill case where a stray value would be stored unseen (P06). Two redundant lines were deleted instead (`dirty = false` in `delete`, the `changed == request` shortcut). The `MISSING` status name was a manual code and was caught by `Part2DraftSourceTest` (renamed `GONE`, as in 2a).

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

- Built 2026-10-23 (Phase 5 summary in the session). The owner's answers are DECISIONS Round 18 (blocks per occurrence, the certificate as the manual words it, "printed" is the system's completed job, the snapshot kept through edits and flagged). Where the build differs from the bullets above:
  - **Layout** is `shared/.../part2/print/`: `SlipDocument` (the worded slip, built once by `SlipUi.toDocument()` in composeApp, so the paper and the preview are worded by the same `SlipRowUi.detailLines()`), `SlipLayout` (pure: A4 from `PdfLayoutSpec`, rows kept whole, a heading kept with its first row, a taller-than-a-page row flowing line by line, words wider than the page broken, a page label from a `{page}`/`{pages}` template) and `SlipLayoutSpec` beside `PdfLayoutSpec` in `utils/` (type styles, inks, rule, gaps). The plan's diagram has `Part2Slip -> SlipLayout`; the worded document sits between, because the words are `Part2Strings`' and shared cannot depend on composeApp. `PdfLetterFormatter.wrapLine` was generalized (a `breakLongWords` flag, default off), its old tests untouched; it was not copied.
  - **Printers** are in `shared` (`AndroidSlipPrinter` + `SlipPrintAdapter` + `SlipPdf` in androidMain; `IosSlipPrinter` + `IosSlipPdf` in iosMain; the `SlipPrinter` interface in commonMain, `FakeSlipPrinter` in `shared-test-fixtures`). composeApp's `rememberSlipPrinter()` (expect/actual) builds the Android one from the Activity under `LocalContext`'s wrappers (`findActivity`, shared with the status-bar code); a test or host can provide its own through `LocalSlipPrinter`. A missing Activity prints nothing.
  - **Printed** on Android: `PrintJobWatch` polls the `PrintJob` the print manager returns and only `COMPLETED` counts (`onFinish` cannot tell a printed slip from a cancelled dialog); the watch stops the moment the job ends and gives up after `Part2Limits.PRINT_WATCH_MS` (30 minutes). iOS: the sheet's `completed` flag. The view model's `print()` and `SlipPrintSession` (new file; `LeaveDraftViewModel.kt` 265 -> 288 lines, then 258 once the autosave moved to `DraftSaver.kt`, see item 18) record the snapshot at once on completion, never for a closed or deleted draft, and keep the record in memory (and retry with the next save) if the write fails.
  - **Snapshot** is `Draft.printed` (`PrintedSnapshot`: time, template revision per code, the rendered rows), an optional field inside payload `v: 1` (two new `@Serializable` classes, so the R8 gate reads `part2: 9 models`); `Part2Bounds` and `Part2Limits` (`PRINTED_ROWS_MAX` 170 = 10 requests x (16 links + 1), `PRINTED_TEXT_MAX` 4000, `PRINTED_DATA_MAX` 4) bound it on the way in and out. No schema or backup version change.
  - **Check the HRMS draft** is `Part2Destination.Hrms(draftId)` (`part2|hrms|<id>`), reached from the slip preview, with `GuideNavState` holding the draft's model across slip and check.
  - **The plan's Android integration test** ("the Robolectric `PrintDocumentAdapter` writes a valid PDF") is partly replaced: Robolectric 4.12 has no native PDF code (`PdfDocument` throws "document is closed" there), so the adapter and `SlipPdf` are tested against a recording `SlipPdfDocument` (pages in order and renumbered, ranges, cancel, failure, the document closed) and the real PDF is proven on the Pixel 9 (Phase 11 item 16). iOS builds a real PDF in the simulator (`IosSlipPdfTest`).
  - **The iOS edge-swipe** needs no Swift: the Part II levels are inline in the Guide tab (stack depth 1, where `shouldAllowInteractivePop` is false) and the print sheet is modal. The iPhone walk confirms it (item 14).
  - **Counts** (debug / release / iOS): shared 1556 -> 1639 / 1556 -> 1639 (1 known skip) / 1451 -> 1507; composeApp 1237 -> 1294 / 1207 -> 1268 / 775 -> 820; corpus 139/139; scripts tests 50 and tool tests 411 unchanged; bundle 345,327 bytes; R8 gate `41 models and serializers kept (guide/model: 32, part2: 9)` (two new `@Serializable` classes: `PrintedPayload`, `PrintedRowPayload`); Room schema 15 and `.pcda` v5 unchanged. The first gate run failed on a 317-line test file (split) and, with a test JVM left spinning at 100% CPU for 6 hours by a deliberate break (killed), on three iOS timing tests and two Compose-idle tests; each passed on a quiet machine and the final `check` is green.
  - **Tests**: red first (27 of 74 shared, 15 of 37 Android printer, 4 of 4 iOS and 34 of 67 composeApp new tests failing on missing behaviour, none on a compile error; the `PdfDocument` seam tests were written with the seam after Robolectric ruled out a real PDF, so their red is the mutants'), then 140 deliberate breaks. 19 first-round survivors or invalid mutants each exposed a gap or a dead line and are closed: a page-break guard `!atPageTop` no test reached (a blank first page with an empty header), a block taller than a page leaving a gap, the snapshot's certificate, authority, From and To limits, a word exactly as wide as the line, out-of-order page ranges, the hairline width, a print state published only by chance (`onChange`), printing a draft being deleted, the model held with only the check level on the stack, an HRMS certificate shown under a label, the first order only, the preview's detail lines and an unfinished draft's check level. Four lines were deleted instead: a `minOf` in the heading's keep-with-next (redundant), the single-character guard in the word wrap, the empty-page guard in `newPage` (it made a slip with nothing to draw zero pages, which cannot be written) and `loaded` in `print()`.

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

- Built 2026-10-24 (Phase 6a summary in the session). No owner question was open (DECISIONS Round 19 lists the build-time choices). Where the build differs from the bullets above:
  - **Merged-manifest guard.** `scripts/check_merged_manifest.py` compares the release merged manifest with `scripts/merged_manifest_baseline.json`: uses-permission (and the sdk-23 form), declared permissions with their protection level, `uses-feature` (a camera requirement would hide the app from devices without one), every exported component (an `exported=true`, or a filter and no attribute, anything but a plain `false` counts; a provider only when it says so) with its guarding permission and `enabled`, and `allowBackup`/`debuggable`/`usesCleartextTraffic`. Every baseline entry carries a reason (11 permissions, 1 declared, 0 features, 3 exported components: the launcher, `ProfileInstallReceiver` behind `DUMP`, and Play's disabled `AssetPackExtractionService`). Wired into `ci.yml` and `git-pre-push.sh`, with tests that fail if either wiring goes. CLAUDE.md names it.
  - **Spike (Pixel 9, ML Kit Document Scanner 16.0.0, published 2025-09-29; no advisory found).** (1) The merged release manifest gained no `CAMERA` permission, no `uses-feature` and no exported component; ML Kit adds three components, all `exported="false"`. (2) Scanning works with the phone offline (airplane mode, Wi-Fi and data off, ping unreachable) once an online scan had put the scanner module on the phone; a never-scanned phone offline was not tried (item 19). (3) Output location: `<app cache>/mlkit_docscan_ui_client/<n>.jpg`, a `file://` URI, JPEG, 0.6 to 1.1 MB, no Exif or GPS, gallery image count 0 before and after, no other new file outside the app's own. **ML Kit never deletes the page**: it was still in the cache after the scan ended, so the app must (it does).
  - **Pipeline** is `shared/.../part2/pages/`. Common: `PageScanner` (+ `ScannedPage`, `PageScan`, `ScanFailure`; `discardLeftovers()` for a killed process), `PageEncoder`, `PageCapture` (the one place a page is held in plaintext: reads one page, encodes, stores, discards it, then the next; every page is discarded on success, on any failure and on cancel; raw and stored bytes are overwritten; nothing logged), `PageGeometry` (the long-edge and pixel-limit arithmetic in `Long`), `JpegFit` (the quality ladder 85, step 5, floor 40, judged on the stored bytes) and `JpegMetadata` (cuts APP1 to APP13, APP15, comments and any bytes after the end marker; malformed files are refused). Android: `AndroidPageEncoder` (decode at a power-of-two fraction, Exif orientation applied before it is dropped, scale, step quality), `AndroidPageScanner` (turns the files the source reports into pages; only a regular file that resolves, links and `..` included, into the scanner's own folder is ever read or deleted; sweeps the folder before a scan, on cancel, on failure and on walking away), `MlKitScanSource` (gallery import off, JPEG only, page limit = room left; the two Play services calls are constructor defaults so the rest is unit-tested), `ScanResultBridge`. iOS: `IosPageEncoder` (ImageIO thumbnail at the target size, so a large photo is never decoded in full), `IosPageScanner` over a Swift `DocumentScanPresenter` (VisionKit; pages go to Kotlin as in-memory JPEG data and nothing touches disk). The plan said "PageEncoder/PageScanner interfaces and implementations"; `PageCapture`, `PageGeometry`, `JpegFit` and `JpegMetadata` are the shared parts that keep the two platforms to one rule each (DRY). `composeApp` gets `rememberPageScanner()` (expect/actual, like `rememberSlipPrinter`) and `part2Module` binds `PageEncoder`; nothing uses them until 6b.
  - **Plan vs build.** The plan asked for `PageEncoderTest` on Robolectric native graphics: it works (real JPEG compress, decode, scale and Exif read), so the encoder is proved against real JPEG bytes on the JVM, not through a seam. `NSCameraUsageDescription` is in `Info.plist` and `Part2Strings.cameraUsageDescription`, and `Part2CameraUsageTest` keeps them equal and fails on any other privacy key. The plan's "XCTest for the VisionKit presenter's in-memory result path" is `DocumentScanPresenterTests` (9 cases: the hand-over in order, an unencodable page skipped, one answer only, the unsupported camera, cancel and failure callbacks, dismiss).
  - **Counts** (debug / release / iOS): shared 1642 -> 1769 / 1642 -> 1769 (1 known skip, `GemmaModelPathsAndroidTest`) / 1509 -> 1597; composeApp 1301 -> 1305 / 1275 -> 1279 / 827 -> 828; scripts tests 50 -> 88; tool tests 411 and the bundle 345,327 bytes unchanged; `PayslipMaxTests` XCTest 33 cases, 0 failures; corpus 139/139; R8 gate `41 models and serializers kept (guide/model: 32, part2: 9)` (no new `@Serializable` class); manifest gate 11 / 1 / 0 / 3 as baselined; Room schema 15 and `.pcda` v5 unchanged.
  - **Tests** were run red first (30 of 36 manifest-guard tests; 38 of 43, then 11 of 14, 17 of 21 and 16 of 67 on the pipeline, none on a compile error) and then about 410 deliberate breaks in nine rounds. Survivors each exposed a gap or a redundant line: the metadata cut accepted a file that did not start with the start marker, fill bytes and standalone markers were untested, a scan header with an impossible length, vendor trailers after the end marker, the cap judged on bytes the cut then removed (the codec writes a colour-profile segment), a hard-coded order id, a cancelled capture reported as merely unsaved, a link to a directory outside the scanner folder, an `Int` overflow in the pixel limit (now one shared `Long` function), the iOS quality ladder and `maxPages` never proved, the camera shown off the main dispatcher. Deleted as redundant: a clamp that could never fire, three `toString` overrides, the `recycle()` calls, two positivity checks, the `compress()` return value, a `step` branch in the quality ladder, an empty-input check, a link-clearing in the result bridge. Equivalent mutants kept and documented: `long <= longEdge` vs `<` (a factor of 1 scales to itself) and the two `>=` boundaries of the raw-size and pixel caps (no real image of exactly that size can be made). One of my mutants looped forever and one run died on its timeout; both left the files restored (checked by sha256) and no stray test process.

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

- Built 2026-10-25 (Phase 6b summary in the session). The owner's answers are DECISIONS Round 20 (vault section on the Part II home with scanning from the vault too, linking set once at scan time, the grant picked from the vault). Where the build differs from the bullets above:
  - **No `Part2Vault` level.** The section on the Part II home is the vault (owner's choice); `Part2Destination.Order(orderId)` (`part2|order|<id>`, ids only, cut once the repository answers `GONE`) is the one new level. The order's model lives with its level, so leaving it, the app lock included, writes what was typed and drops the model and any page picture.
  - **Layout.** `ui/screens/guide/part2/vault/`: `VaultViewModel` (the list and the one place a new order is scanned in: it makes the order first, keeps it only if a page was stored, removes an order a killed process left empty when the vault opens, and ignores a second tap while a scan is open), `VaultOrderViewModel` (number and date through `VaultOrderEditor`, saved by the shared `AutoSaver`; more pages; `view(pageId)` that only reads this order's pages and zeroes the bytes), `VaultNotices` (the words for every `CaptureResult`), `VaultSection`/`VaultOrderScreen`/`VaultPageRow` (stateless), `VaultSizeWarning` (the Settings line), `LocalPageScanner` and `PageImageDecoder` (test seams). Shared: `VaultOrders.quotable` (the last-order line is its first element; the grant picker lists it), `VaultOrderEditor`, `Part2Repository.order(id)`, `RestoreReport`. Copy is `Part2VaultStrings` (the camera text stays in `Part2Strings`).
  - **`DraftSaver` became `AutoSaver<T>`** (same behaviour, given its write and delete as functions so it cannot reach storage); the draft form and the order screen share it. `Part2DraftSourceTest` now names the model as the only writer and holds `AutoSaver.kt` to "no repository, no storage". The existing draft tests ran green on it before any vault code.
  - **Entitlement.** `Part2Host.canCapture` (= the Part II entitlement) gates scanning and adding pages; reading and deleting are open to anyone who holds an order. The host's `scanOrder`/`addPages` are tested to refuse without it.
  - **Restore (item 10).** `PayslipBackupService.restore` returns `Result<RestoreReport>`; Settings shows "Restore Complete! N items could not be restored." in the warning colour.
  - **Counts** (debug / release / iOS): shared 1769 -> 1785 / 1769 -> 1785 (1 known skip) / 1597 -> 1613; composeApp 1305 -> 1447 / 1279 -> 1421 / 828 -> 908; scripts tests 88, tool tests unchanged; bundle 345,327 bytes; R8 gate `41 models and serializers kept (guide/model: 32, part2: 9)` (no new `@Serializable` class); manifest gate 11 / 1 / 0 / 3 as baselined; Room schema 15, `.pcda` v5; XCTest 34 cases passed, 0 failed; `App.kt` 297, `GuideTab.kt` 263, `LeaveDraftViewModel.kt` 260, `Part2Strings.kt` 266, no new file near 300.
  - **Tests.** Shared and view-model tests were run red first (8 of 29 shared, 41 of 50 vault, 3 of 6 navigation, 2 of 5 restore status, 2 of 5 size). The screen tests (host, home, slip, order, grant, Settings line) were written before the screens but first run after them, so they have no red run; the deliberate breaks stand in for it. 195 deliberate breaks in five rounds: 167 killed, 4 invalid (did not compile), 3 patterns that no longer matched, 21 survivors. Each survivor exposed a gap or a redundant line and was closed: a boundary at exactly 40 characters, the list's page count after a scan, a tidy-up that started just before a scan, edits after leaving or deleting the order, a scan after the order was deleted or the screen left, the busy state, a delete that wrote the order back, a scan not stopped when the screen was left, the lapsed state's read-only text, the picker's Close button, the unreadable-orders message. Deleted as dead: the `LOCKED` vault state (the entitlement is all the vault needs: `vaultEntry` became `canCapture`), the notice from the section's visibility test. Equivalent mutants kept: `collectLatest` vs `collect` in the list, `remember(id)` vs `remember` in the order level and the page row (the screens are keyed by id), the text box's `error` flag (its supporting text is tested).
  - **Follow-ups (same day, DECISIONS Round 21).** Delete one page, the vault picker on `carry_order`, link and unlink later, and the template revision recompute (Phase 11 items 26 and 6). `GrantPicker` became the generic `ChoicePicker` (used for the vault orders and for the drafts). Counts after them: shared 1798 debug / 1798 release (1 known skip) / 1623 iOS, composeApp 1470 / 1444 / 920; R8 and manifest gates as above; 51 deliberate breaks in the follow-ups (plus 4 re-runs). The screen tests for the follow-ups were written first and run red (3 of 3 page-delete, 3 of 6 link, 1 of 1 carry-forward); the validator's revision check was added before its test was run, so its mismatch test has no red run.
  - **Pixel 9 walk (Phase 11 item 14's Android half).** Done 2026-10-25 with a debug build from a copy of the tree (flag flipped there only); see item 25.

### Phase 6c: CANCEL / CANCELS — content batch 2 and flow
- Unit tests first:
  - Golden CANCEL (col 6 "ALL", cols 8–9, certificate) and CANCELS (code in col 6, serial in col 7, one occurrence only).
  - Time-bar note over 2 years.
  - "Cancel and re-draft" opens a new draft prefilled from the cancelled one.
- Integration test: vault order → cancel → slip → print record → the new draft is linked to both.
- Implement: `templates/cancel.json`, the cancel flow in `VaultViewModel`, the review batch 2 entries.
- Exit: gate green; owner review recorded.

- Built 2026-10-25 (Phase 6c summary in the session). The owner's answers are DECISIONS Round 22 (re-draft copies the linked draft, the cancelled state is derived in two stages, cancel codes share a slip only with each other). Where the build differs from the bullets above:
  - **Content (`templates/cancel.json`, 23 lines; `rules.json` +4 rules).** CANCEL (p.226) and CANCELS (p.227) in the manual's own wording: "ALL" in col 6, the order's number and date in cols 8 and 9 (CANCELS: the code in col 6, the serial in col 7), From and To not required, the certificate as printed (no label, no full stop). Four `warn` checks: `cancel_quoted`/`cancels_quoted` (note 2 of each page) and `cancel_time_barred` (p.226 note 4)/`cancels_time_barred` (p.227 note 6). Bundle 345,327 -> 347,716 bytes (the `part2` section gained 2 templates and 4 rules; 26 templates, 24 rules). The review pack lists two new judgment calls: para 12.3's misprint "CANCELCS" (the slip prints CANCELS) and the time bar counted from the order's date. The own-words copycheck is CLEAN over the six new labels and helps.
  - **Domain (`shared/.../part2/domain`).** `CancelRules` (the family: the two codes as `ALL_OCCURRENCES`/`ONE_OCCURRENCE` so no file outside a `*Rules.kt` names a code, the slot `cancelled`, the four checks), `CancelDrafts` (the cancel event, the re-draft event, `isCancel`), `CancelStatus` (worked out from drafts and orders, never stored: printed slip, then the published cancel order linked to it; a CANCEL outranks a CANCELS). The engine takes the device's day (`draft(input, today)`, `RuleContext.today`) so the time bar compares with it while the engine stays pure; `Part2Rules.VAULT_ORDER_SLOTS` is the one list of boxes that quote a vault order (grant, carry-forward, cancelled). `Part2LookupIndex.draftOptions(sameRibbonAs)` offers the codes of the ribbon a slip started in. `Part2Draft.pairedWith` is an optional `pair` field inside payload `v: 1`, bounded like every id: no schema, backup or R8 change.
  - **Flow (`composeApp`).** `OrderCancelling` (new, `draft/`) saves the cancel slip and the corrected draft together or neither (the corrected one first, removed if the slip cannot be saved); `Part2Drafting.cancel`; `Part2Host.cancelOrder` (refuses unless the officer may draft, opens the cancel slip, says why on the order screen when it cannot); the order screen's "Cancel this order" (once the number and date are typed, otherwise a hint) and "Cancel and re-draft" (only while the linked draft exists); `CancelNotes` words the state for the order screen, the vault row and the slip's linked rows; the slip preview's "Open the corrected draft"/"Open the cancel slip" hop pops when the partner is the level below. The cancel codes are drafted in the same form as the Leave codes (nothing of their own); `Part2CancelStrings` holds the copy. `PreClaimDates.localDay` replaced five copies of "the day where the officer is".
  - **Counts** (debug / release / iOS): shared 1798 -> 1847 / 1798 -> 1847 (1 known skip) / 1623 -> 1659; composeApp 1470 -> 1517 / 1444 -> 1491 / 920 -> 951; scripts tests 88; tool tests 411 -> 428; XCTest 34 cases (see the summary); corpus 139/139; R8 gate `41 models and serializers kept (guide/model: 32, part2: 9)` (no new `@Serializable` class); manifest gate 11 / 1 / 0 / 3 as baselined; Room schema 15, `.pcda` v5; `GuideTab.kt` 263 and `App.kt` 297 untouched; largest touched files `LeaveDraftViewModel.kt` 268, `LeaveDraftStateBuilder.kt` 219, `VaultOrderViewModel.kt` 195.
  - **Tests.** Red first: 16 of 17 new Python tests, 30 of 52 shared (in the first run, none on a compile error), 23 of 28 composeApp view-model tests, 6 of 7 and then 1 of 9 Robolectric screen tests, and the `localDay` test. Written after the code they check, so with no red run (the deliberate breaks stand in): the real-bundle tests (`Part2BatchTwoShipTest`, the golden slips for what was already content, `Part2CancelRealBundleTest`), the refusal-notice screen test and the host guard test (both added for survivors). 75 deliberate breaks in two rounds (30 shared, 36 composeApp, 8 on the template JSON, 1 on `localDay`): 4 first-round survivors and 1 that did not compile, each a gap, all closed and re-run killed (the two-kind status order, a slip holding both cancel kinds, the old last-order line copied into the corrected draft, the host's own entitlement guard, a rebuilt ribbon mutant). No equivalent mutant. Found by the tests, not the breaks: a collector that published before storing the new list. The first full run also needed four guards and contracts updated for the new family (the engine source guard now holds the new files to "no code named", the draft source guard names `OrderCancelling` as the one other writer through `Part2Repository`, the rule and template counts, the eight-code offer).
  - **Follow-ups (same day, DECISIONS Round 23).** Items 29 and 30 and the check of item 27. The two templates gained an optional "Date of the earliest occurrence" box that only the time bar reads (counted from the earlier of it and the order's date); CANCELS gained the `cancels_code_unknown` warning (p.227 note 5); the corrected-draft button follows its partner live; "Cancel and re-draft" is not offered when the linked draft is itself a cancel slip. Counts after them: shared 1857 debug / 1857 release (1 known skip) / 1663 iOS, composeApp 1526 / 1500 / 956, tool tests 431, scripts tests 88, corpus 139/139; bundle 348,306 bytes (26 templates, 25 rules); R8 gate `41 models and serializers kept (guide/model: 32, part2: 9)`, manifest gate 11 / 1 / 0 / 3, Room 15, `.pcda` v5; `LeaveDraftViewModel.kt` 274, `CancelRules.kt` 76; no Swift changed, so the 34 XCTest cases of the 6c run stand. 24 more deliberate breaks (20 Kotlin, 4 template): one first-round survivor (the partner check at load, which the live list cannot replace when it answers first; a slow-repository test now forces that order), all killed; the tests for the occurrence box and the typed code passed on their first run because the domain was already built, so the breaks stand in for them. The Pixel 9 walk (item 28) then found the drafts list calling a finished cancel slip "Not finished"; fixed with `Part2DraftSummary.finished` (test first, 3 more breaks killed).

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
- Built 2026-10-25 (Phase 7a summary in the session). The owner's answers are DECISIONS Round 25 (a Dashboard card after an import with an in-memory "Not now"; the return reminder stops when a rejoining draft falls within the leave; the sixth "add the published order" leaves Due 30 days later). Where the build differs from the bullets above:
  - **Planner (`shared/.../part2/domain`).** `ReminderPolicy` (a data class: hour 9, 14 and 30 days, at most 6, 1 March, nearest 20: the one home of the cadence), `Reminder`/`DueReminder`/`ReminderPlan` (`due` oldest first with an `overdue` flag the planner sets, `upcoming` nearest first and capped; `Reminder.key` is stable across rebuilds so a scheduler can replace by key), `ReminderRules` (the rule family: names the codes by role, so no other file names one; reads `LeaveRules`' slot ids and annual/casual sets instead of copying them) and `ReminderPlanner` (pure: the instant and the device's offset are handed in; the one place a date is worked out). `CivilCalendar` gained `fromDayNumber` and `UNIX_DAYS_TO_2000`, checked against two outside Unix times and by a round trip of every day of 2000-2100. `CancelStatus` hoisted a filter out of its per-draft loop (same answers; it was quadratic).
  - **Due list (`composeApp`).** `Part2Destination.Due` (`part2|due`, no id), `GuideNavState.openPart2Due()`, `DueViewModel` (the plan, worded with the drafts list's own titles; a repeating reminder is one line and not again once due; reads only), `DueScreen`, the "Due" row on the Part II home (`Part2DueRow`), `Part2DueLevel` (the upgrade panel for anyone not entitled), `DueCardModel` + `DueImportCard` (the card; "Not now" is a counter in memory), `DashboardDueCard` (the Dashboard side: flag, entitlement, import succeeded), copy in `Part2DueStrings`. `App.kt` was split first: `DetailContent` moved unchanged to `AppDetailContent.kt` with the `openDue` helper (297 -> 254 lines). `Part2Models` gained `due`; `part2Module` binds the two new models as singletons. No new `@Serializable` class, no stored field, no schema, backup, permission or dependency change.
  - **Seams left for 7b.** The scheduler rebuilds from `ReminderPlan.upcoming` by `Reminder.key`; its text goes in `Part2DueStrings` (generic); a notification tap opens Due through `GuideNavState.openPart2Due()` (and `PendingPart2Link` while locked); `App.kt` has room.
  - **Counts** (debug / release / iOS): shared 1857 -> 1938 / 1857 -> 1938 (1 known skip, `GemmaModelPathsAndroidTest`) / 1663 -> 1743; composeApp 1526 -> 1586 / 1500 -> 1560 / 956 -> 986; scripts tests 88; tool tests 431; corpus 139/139; bundle 348,306 bytes (unchanged; refresh and copycheck CLEAN); R8 gate `41 models and serializers kept (guide/model: 32, part2: 9)`; manifest gate 11 / 1 / 0 / 3 with no baseline change; Room schema 15, `.pcda` v5. The iOS planner timing test measured 5 plans of a full device (1,000 drafts, 1,000 orders, every tenth a cancel slip) in 1.49 s against a 5 s bound. No Swift changed, so the 34 XCTest cases of 6c were not re-run. Largest new files: `ReminderPlanner.kt` 129, `DueViewModel.kt` 91, `DueScreen.kt` 68; `DashboardScreen.kt` 291 (+5; a blank line went to keep `PopulatedDashboard` at 50), `GuideTab.kt` 263, `App.kt` 254, `LeaveDraftViewModel.kt` 274.
  - **Tests.** Red first: 49 of 69 planner tests (every one an empty-plan failure, no compile error), 13 of 17 Due-model and card-model tests, 12 of 28 screen and navigation tests, and the repeating-reminder duplicate (found by a screen test, then a model test first). Written with or after the code they check, so with no red run (the deliberate breaks stand in): `CivilCalendarDayNumberTest` (red for 2 of 4), `GuideNavPart2DueTest` and `Part2DueSourceTest` (the `Due` level and `openPart2Due` were built in one step), `ReminderPlannerRepositoryTest`, `OpenDueTest`, the Koin wiring test. 123 deliberate breaks in seven rounds (56 + 7 + 3 + 4 on the planner, rules and `CancelStatus`; 53 on the Due models, screens, navigation, wiring and Dashboard links), plus one re-run: 9 first-round survivors, each a gap or a dead line, all closed and re-run killed: the window start of a two-leave draft and a non-rejoining request carrying the slot (two tests added); a redundant `coming.isEmpty()` clause, a doubled year range and an unreachable "1 March exists" guard (deleted); a second pass in the clock-offset conversion that only differs inside a skipped hour and a code check in `leaveFrom` nothing exercised (deleted before the run); the locked Due panel's upgrade button not wired (a tap added). One equivalent mutant kept: `floorDiv` against `/` for an instant before 1970, which a reminder never has. Found by the tests, not the breaks: a repeating reminder listed both as due and as coming up. Tests that could not be forced to fail by a break: the colour of an overdue line (a visual token; `UiResourceUsageSourceTest` and the strict audit keep literals out).
  - **Security.** The `security-review` skill was run on the phase diff: no finding. The planner is pure and touches no I/O, log, clipboard, network or crash report; `Reminder`, `ReminderPlan` and `DueUiState` print counts only; the list's strings are generic and name no code, order number or date except a date the planner handed over; `part2|due` carries no id and a malformed entry is cut; the Due level and the card are for an entitled officer only, and the planner and the repository stay ungated (Round 13); "Not now" is held in memory only (a source test forbids storage). The restore path is unchanged. Storage does not bound a number's value, so the planner refuses a leave year outside 2000-2100 before the calendar counts anything, and a date is checked with `CivilCalendar.exists` first (see "Found late" below).
  - **Found late, fixed in this phase.** Storage bounds a draft's size and count but not a number's value (`Part2Bounds` accepts any `NumberValue`), so a crafted `.pcda` could carry a leave year of billions, and the calendar counts years by a loop. I had first deleted `ReminderRules.annualYear`'s upper bound as redundant (a year the calendar cannot hold gave no reminder either way, so five breaks survived); the review of that deletion showed the bound is what stops the loop. It is back (2000 to 2100), pinned on the rule itself (`ReminderRulesTest`, 4 breaks killed), with the reason in its comment. Dates in a draft were already checked with `CivilCalendar.exists` before anything is counted.
  - **Follow-ups (same day, DECISIONS Round 26).** Items 32, 34 and 36 (see Phase 11): a stored "Dismiss" on every Due item (`Part2Draft.dismissed`, `ReminderTask`, `DueViewModel.dismiss`, cleared by a completed print), the extension rule in the planner, and `Part2DueRoomTest`. Counts after them (debug / release / iOS): shared 1938 -> 1963 / 1938 -> 1963 (1 known skip) / 1743 -> 1768; composeApp 1586 -> 1601 / 1560 -> 1575 / 986 -> 997; scripts tests 88; tool tests 431; R8 gate `41 models and serializers kept (guide/model: 32, part2: 9)` and manifest gate 11 / 1 / 0 / 3 unchanged; Room schema 15, `.pcda` v5; `LeaveDraftViewModel.kt` 274 -> 279; the largest new test file is 184 lines (`DueViewModelTest` was split at 319 into itself and `DueViewModelDismissTest`). Red first: 8 of 14 dismissal tests in shared, 4 of 11 extension tests, 9 of 40 in composeApp, each on missing behaviour. The security review of the new storage: the field is inside the encrypted payload, names are plain tokens, count and length are bounded on write and on every read, so a crafted backup can carry neither markup nor a crowd of names; nothing is logged.
  - **Not done in 7a** (see Phase 11): the Dashboard card on a real import and the light theme (item 35), the iPhone walk (items 14 and 20), and the XCTest run (no Swift changed). The Pixel 9 walk of the Due list is done (item 35).

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

- Built 2026-10-26 (Phase 7b summary in the session). The owner's answers are DECISIONS Round 28 (an explainer then the system dialog, a device-local "asked" flag, one fixed generic sentence; then eight closing answers). Where the build differs from the bullets above:
  - **Shared (`shared/.../part2/reminders`).** `ReminderScheduler` (one method, `replaceAll(reminders)`), `ReminderSync` (the one place `ReminderPlanner.plan().upcoming` is copied to the scheduler: on every change to the drafts, the orders or the entitlement, an identical list is not given again, a scheduler that throws is retried, a lapse or Free gives an empty list, `hasReminders` for the prompt, and `rebuildOnce()` for a boot), `ReminderFireCheck` (is this reminder due now, asked of the planner at fire time; `draftExists` for the debug switch), `NotificationPermission`, `ReminderPromptStore` (the device-local flag: private SharedPreferences, `NSUserDefaults`), `ReminderNotificationText` (the injected words) and `ShortDelayReminderScheduler` (the debug-only walk switch, the `DevOverride` pattern: in memory, inert unless `isDebugBuild()`, 20 seconds). Fakes are in `shared-test-fixtures`.
  - **App (`composeApp`).** `PendingPart2Link`/`Part2Link` (Koin-scoped; a known kind and a plain id, checked against the database when taken), `Part2LinkEffect` (the one call added to `App.kt`: 254 -> 255 lines, the pin is 297), `ReminderPromptModel` and `ReminderPromptHost` (mounted in `GuidePart2Level`; `GuideTab.kt` stays 263), `PayslipViewModel.hasAccessFlow`, `startPart2Reminders`, `NavBridge.popToRoot` and `PendingLinkStackReset` (iOS closes every pushed detail before Due), `ReminderDelaySwitch` in Settings (debug builds only), `Part2ReminderStrings`. Koin binds the lot in `part2Module` (loaded only while Part II is on).
  - **Android (`composeApp/androidMain/.../reminders`).** WorkManager 2.11.2 (pinned in `libs.versions.toml`; started on demand: its automatic initializer is removed from the manifest and `PayslipApplication` is the `Configuration.Provider`, so a flag-off build starts as before), `AndroidReminderScheduler` (cancels the queued jobs and queues the planner's list by key; a running job is never cancelled; the delay is the planner's instant minus now), `ReminderWorker` (kind and draft id only, read as untrusted; asks `ReminderFireCheck`; always ends in success), `ReminderNotifier` (one channel, the injected sentence, an explicit immutable PendingIntent per reminder), `ReminderLinkIntent`, `AndroidNotificationPermission` and `ReminderBootReceiver` (not exported, BOOT_COMPLETED only). `ic_stat_reminder` is referenced from code.
  - **iOS.** `IosReminderBridge` (the Swift gateway registered at start-up; the one new allow-list entry in `stateful_singleton_guard.py`, approved), `IosReminderScheduler`, `IosNotificationPermission`, `acceptReminderTap`; Swift `ReminderNotificationGateway` (over `UNUserNotificationCenter`, keyed requests, 20 against the system's 64) and `ReminderTapRouter` (holds a tap that arrives before Kotlin is ready); `NavCoordinator.popToRoot`.
  - **R8 and manifest.** `check_r8_guide.py` now also requires the worker and that it keeps its name (WorkManager stores it by name). With the flag off R8 correctly drops the worker with the rest of Part II (seen on the first run), so the worker check applies only where `PART2_ENABLED` is true and the gate says so; it was proved on a flag-on release built in a copy: `41 models and serializers kept (guide/model: 32 models, part2: 9 models); PayslipDatabase_Impl, Part2Dao_Impl, ReminderWorker kept`, and `ic_stat_reminder` is in that APK. The manifest gate went from 11 / 1 / 0 / 3 to `13 permissions, 1 declared, 0 features, 5 exported components`: `POST_NOTIFICATIONS` (the app's), `RECEIVE_BOOT_COMPLETED` (WorkManager), and two exported WorkManager components behind system-only permissions (`SystemJobService`, `DiagnosticsReceiver`), each a reviewed baseline entry with a reason. The boot receiver is not exported, so the exported count did not change.
  - **Counts** (debug / release / iOS): shared 1963 -> 2001 / 1963 -> 2001 (1 known skip) / 1768 -> 1805; composeApp 1601 -> 1733 / 1575 -> 1707 / 997 -> 1047; scripts tests 88 -> 100; tool tests 431; XCTest `PayslipMaxTests` 34 -> 48 cases; corpus 139/139; Room schema 15, `.pcda` v5, bundle unchanged. Largest new files: `ReminderWorkerTest.kt` 212, `ReminderSyncTest.kt` about 230 (the reboot tests are in their own file), `ReminderNotificationTests.swift` about 207; no file over 300.
  - **Tests.** Red first (each on missing behaviour, no compile error): the sync and fire check (11 of 18), the prompt model (6 of 8), the pending link (4 of 9), the link opener, the pop-to-root (1 of 3), the access flow (3 of 3), the debug switch (2 of 6), the worker, scheduler and notifier on Android (6 of 7, 13 of 22), the permission wrapper (4 of 5), the R8 script, the boot receiver (2 of 5, then 1 of 6), the Compose mount and the suspending-lookup test. Written with or after the code, with no red run, so the deliberate breaks stand in: the prompt store, the link intent, the wiring tests, the integration tests, the iOS Kotlin tests and all the Swift XCTest cases. About 160 deliberate breaks (110 in the first round with 9 survivors, each a gap or a dead line and all fixed or deleted and re-run killed; later rounds on the opener, the walk switch, the boot receiver; 7 on the iOS Kotlin; 12 on the Swift, of which one was my own no-op reorder that proves nothing). Dead lines deleted: an `if (it != null)` in the opener, a key-tag filter in the scheduler, an `answer = null` in the permission wrapper. One clause I had thought dead (`given = null` after a failed call) is needed: a partial failure leaves the system in an unknown state, so the same list must be sent again (test added).
  - **Found on the Pixel 9, not by any test, then fixed with a test that reproduces it.** (1) The debug switch brought jobs forward but the worker's due-check, which asks the real plan, said "not due": nothing posted; the worker now checks only that the draft exists while the switch is on. (2) A tap never opened Due: `take()` cleared the link before its database lookup suspended, which changed the `LaunchedEffect` key and cancelled it mid-lookup; the fake repository answers at once so no test saw it. The opener is now one long-lived collector (`Part2LinkOpenerUiTest.aTapStillOpensWhenTheDatabaseLookupReallySuspends`). (3) After a reboot the system had dropped all seven queued jobs until the app was opened (the plan's "reboot survival" bullet): `ReminderBootReceiver` rebuilds the schedule once from the drafts. Proved after a real restart: seven jobs, the app never opened.
  - **Security.** The `security-review` skill was run on the phase diff: no finding. The launcher activity is exported, so the tap's extras are untrusted: a known kind, a plain id, the database checked when taken, nothing opens before unlock. The notification is one generic sentence with no kind, code, order number or date; the PendingIntent is explicit and `FLAG_IMMUTABLE`; nothing is logged, reported, copied or sent; the two new exported components are behind system-only permissions. A source guard (`Part2ReminderSourceTest`) covers every new reminder file.
  - **Device (Pixel 9, Android 17, a debug build of a copy of the tree with the flag flipped there only; the walk is Phase 11 item 37).** Walked: the explainer on entering Part II the first time, then the system dialog; no second ask after a kill and relaunch; delivery with the app in the background and in airplane mode; one generic sentence and nothing else; a tap on a reminder landing on Due; Dismiss taking the queued job off (1 -> 0); Force Free taking all seven off and Force Premium bringing them back; a tap on a killed app landing on the PIN screen and opening Due after unlock (twice, with logging); and the seven jobs back after a real restart with the app never opened. Not walked: the light theme, TalkBack, the iPhone (items 14 and 20).

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

- Built 2026-10-26 (Phase 8 summary in the session). The owner's answers are DECISIONS Rounds 30 and 31 (the three phase-start questions were settled from the PDF at the owner's instruction; item 43 deferred, the pack read, item 45 built). Where the build differs from the bullets above:
  - **22 serials, 5 template files** (`ch5_furlough.json` 4, `ch5_study.json` 3, `ch5_family.json` 6, `ch5_late.json` 4, `ch5_yearend.json` 5; none over 100 lines); the bundle holds 48 templates and 56 rules (26 -> 48 and 25 -> 56; 36 checks, 14 conditions, 6 derive rules), 348,306 -> 389,509 bytes. Rule families: `LongLeaveRules`, `AcclveRules`, `LtcRules`, `EncashRules` (new), `LeaveRules` and `RationRules` (extended). No file outside a `*Rules.kt` names a code (`Part2EngineSourceTest` green).
  - **Format additions the real rows forced** (each written test-first in Python and Kotlin, mirrored in both validators): a `derived` slot worked out by a named derive rule from the slots it `reads` (the arithmetic, the 300, the 30 and the 60 live in `AcclveCalculator`, never in a template); a `years` slot (the encashment's year-by-year list, stored as the text of one answer); a worked-out day count may be marked `for` a check; a form question may be fed by a check, not only by a link's condition. Limits: template text 600 -> 1,300 (the LTC certificates are the manual's own words, the longest 1,237 characters), a stored text answer 600 (the break-up), `links` stays 16.
  - **The plan's `col 8` for ENCASH was wrong:** the printed table puts the days in Data 2 (col 7). The plan's Facts are corrected.
  - **Counts** (debug / release / iOS): shared 2001 -> 2131 / 2001 -> 2131 (1 known skip, `GemmaModelPathsAndroidTest`) / 1805 -> 1878; composeApp 1733 -> 1735 / 1707 -> 1709 / 1047 -> 1049; scripts tests 100; tool tests 431 -> 443; XCTest 48 (no Swift changed); corpus 139/139; R8 gate `41 models and serializers kept (guide/model: 32 models, part2: 9 models)` unchanged (no new `@Serializable` class; the two new slot fields live in an already kept class); manifest gate `13 permissions, 1 declared, 0 features, 5 exported components` unchanged; Room schema 15, `.pcda` v5. Largest touched files: `GuidePart2TemplateValidatorTest.kt` 299 (was 297, two strings edited), `Part2Strings.kt` 267 (copy for the batch went to the new `Part2LongLeaveStrings.kt`), `OccurrenceDrafter.kt` 224, `GuideTab.kt` 263 and `App.kt` 255 untouched.
  - **Tests.** Red first (each on missing behaviour, no compile error): the rule families (16 of 26), the slot validators in Python and Kotlin, the derived-slot engine (4 of 7), the accumulation rules (9 of 9), the golden slips (18 of 18 against the old bundle), the return reminders (1 of 20). Written with or after the code they check, so with no red run (the deliberate breaks stand in): `YearlyBreakupTest`, `AcclveCalculatorTest`, `LtcAndEncashRulesTest`, `LeaveBankFiguresTest`, `Part2LeaveBankIntegrationTest`, `Part2BreakupBoundsTest`, `Part2BatchThreeFlowTest`/`ShipTest`/`FormTest`/`YearEndGoldenTest`, `Part2YearEndReminderRealBundleTest`. **Mutation:** 79 Kotlin breaks (all killed after one test was added for the one survivor, D71: a derive rule reading a worked-out day count), 3 reminder breaks, 7 template breaks (2 more were refused by the validators and so never reached a test), 7 Python breaks; one survivor was a redundant clause (`r != s.get('id')`), deleted with its Kotlin twin. Other lines deleted as redundant: a negative guard in `AcclveCalculator.accrual` and a `size < 2` guard in `missingYears`. A no-op control survived, so the harness tells the two apart.
  - **Found by the tests, not by the breaks:** a hidden derived slot refused a draft for a year the form typed (the form tries any number; fixed so every number drafts). **Found on the Pixel 9, not by any test:** the ENCASH help text promised "The Leave Bank fills what the statement shows" although the prefill is item 43; the text now says to put a semicolon between years.
  - **Security.** The `security-review` skill was run on the phase diff: no finding. The break-up is read by plain scans with digits, ranges, row count, length and markup refused; derived slots look their rule up in the app's own registry and refuse when it is unknown; Leave Bank rows enter as `LeaveEntry` only and the slip engine still imports no payslip type; nothing is logged, reported or sent. Noted, not reportable: `AcclveRules.alUsed` counts days without checking that a stored date exists (a crafted backup could make the calendar loop; resource exhaustion), and it is not called by any screen yet: whoever wires item 43 must check `CivilCalendar.exists` first.
  - **Pixel 9 walk (2026-10-26; Android 17, serial 4A231VDAQ0001D; a debug build of a copy of the tree with the flag flipped there only, installed over the owner's debug copy without uninstalling; driven over adb with uiautomator, only Part II strings printed).** Observed: the Leave ribbon with all six Chapter 5 cases and "Draft this order" on every code; the FURLEV form (ration and NCC questions, the date boxes, the two cycle-year boxes, the kind-of-leave box that appears only with ration); its slip (one order, Group A and C, FURLEV with the cycle in the narrative and Data 4, SORS, GTDRSHNA with the certificate, TORS 04/11/2026, STPRSHNA, ration rows cited to p.98 notes 1.8 and 4, no R&H row); printing through Save as PDF (a valid one-page A4 PDF whose text matched the preview row for row; deleted from the phone) and "Printed on 26/10/2026"; **item 45 live:** Due listed "Draft your rejoining slip, FURLEV, Due 04/11/2026" and "Add the published order" 09/11/2026; MTRNLV for exactly 180 days with ration: only MTRNLV, no ration rows, no certificate box, and "Leave of 180 days or more: the unit publishes the ration orders quarterly in arrears, so none is drafted here" cited to p.99 note 5; ACCLVE 2024 (282, 0, 19): Data 1 19, Data 2 301, Data 3 282-2023, Data 4 "0 days Annual Leave encashment claimed on LTC during 2024" and "The total is above 300 days. Only 300 days can be encashed." (p.133 (a)), and no "too early" tick because today is past 1 March 2025; ENCASH with the manual's own eight years: "Allowed to encash 220 days ... Total 220" and Data 2: 220; a FURLEV with the NCC answer: "Furlough is not granted to NCC officers or re-employed officers." (p.119 note 7) with a tick box and "Tick every check above to print." The phone's search box swallowed some of my first taps (the Part II home focuses it on entry). Not walked: the reissue warning (EXTNAL, VOLREJ, RECALL, EXTNFUR), the NCC and retirement-date answers on annual leave, EOL, the study, adoption and paternity codes, the LTC codes and the light theme; each is covered by the real-bundle tests. Left on the phone: five synthetic drafts (FURLEV printed, FURLEV with the NCC answer, MTRNLV, ACCLVE, ENCASH), the earlier synthetic ones, and the debug build; the override stays at Force Premium; the tree copy (with the keystore) is deleted; `LaunchFlags.kt` is byte-identical (sha256 989cf71e...ca87a1).

### Phase 8b: Leave Bank prefill screens for ACCLVE and ENCASH (Phase 11 item 43)
Nudge: connect the Pixel 9 (and ask the owner to import a December payslip that prints the leave table, or confirm one is imported).
- Owner decision (DECISIONS Round 33, asked at the start of 8b): the fill is a **button**, "Fill from your Leave Bank". It fills only the boxes that are still empty and never touches what the officer typed. Once a figure of the form is filled the button reads "Refill from your Leave Bank" and asks before it replaces those figures. Settled by earlier rounds, not asked: the fill lives inside the Premium form (Round 13), obeys `LaunchFlags.LEAVE_BANK_ENABLED` (its comment: every Leave Bank surface reads it), compares ACCLVE(Y) with the statement and changes nothing on a mismatch (Rounds 5 and 30), uses the printed drafts for the annual leave used in Y (Round 30), and every figure stays typed or overridable.
- Design (extend, do not shadow):
  - **Rows cross, payslips never do.** `LeaveBankSource` (shared `part2/leavebank`) hands the form `List<LeaveEntry>` as a flow; `LeaveBankFeed` is its one implementation and is attached to the one `PayslipViewModel.leaveBankSummary` (the single derivation of the summary, never recomputed) by `GuideTabRoute`, which already holds that view model. The feed is bound in `part2Module` (a class, not an `object`, so `stateful_singleton_guard.py` is untouched); nothing is collected while no draft is open. `Part2EngineSourceTest` still proves the engine imports no payslip.
  - **The fill is a capability of the rule family, not of a screen.** `LeaveBankFill` (domain, names no code) is registered per code in `AcclveRules` and `EncashRules` and read through `Part2Rules.fill(code)`, the way questions are. It has three pure parts: the values to put in the form (`fill`), the notes the form shows from what is typed (`notes`) and the cross-check (`crossCheck`). It returns facts (`FillNote`, `CrossCheck`), never words. No screen or view model names a code.
  - **What ACCLVE fills** (year Y typed first): the previous total (the statement's ACCLVE row of Y-1, else the total of the officer's own ACCLVE draft for Y-1), the LTC days (the statement's LVELTC rows dated in Y), the annual leave used in Y (the printed drafts, minus any whose order was cancelled in full, through `CancelStatus`) and (c) = `min(30, 60 - used)` from `AcclveCalculator`. **ENCASH:** the break-up from the statement's ACCLVE rows, year by year, with the years it has no row for named (from what is in the box, so a typed year clears its name) and a line when the statement's latest total differs from the break-up's sum.
  - **Cross-check.** On the slip preview, a line for an ACCLVE draft: the statement's figure for the year matches, differs (both figures shown, nothing changed), or the Leave Bank has no row for the year yet (a quiet "not checked" line).
  - **`LeaveDraftViewModel.kt` (279 lines) is split first:** the area-details prefill moves unchanged to `AreaPrefill.kt` and the new fill lives in `LeaveBankFiller.kt`; the model grows by a few lines only. The fill is applied to the form's answers **at the moment the data arrives**, inside one atomic edit, so a source that answers late fills only what is still empty and a year changed meanwhile fills the new year's figures, never the old one's.
  - **The ENCASH break-up box** becomes multi-line (`FieldValue.Years`, `kind: years`). The editor cut every text answer at 200 characters, which would have cut a long break-up mid-row; a break-up now keeps `Part2Limits.BREAKUP_TEXT_MAX` (600), the limit storage and the reader already use.
  - **Security fix carried from Phase 8:** `AcclveRules.alUsed` checks `CivilCalendar.exists` on both stored dates before counting a day (a crafted backup could make the calendar loop). Copy for all of it goes in a new sibling, `Part2LeaveBankStrings.kt`; `Part2Strings.kt` (267) is not touched.
- Unit tests first (each seen failing for the right reason before the code, with the new types stubbed):
  - Shared: `AcclveRulesTest` (a stored date that does not exist counts nothing; the fill's figures), `EncashRulesTest` (break-up fill, notes), `LeaveBankInputsTest` (printed only, cancelled in full excluded, the draft itself excluded), `LeaveBankFiguresTest` (statement total), `LeaveEventEditorTest` (a longer text limit), `CancelStatusLiveTest`, `Part2RulesFillTest` (registered per code, one per code), `LeaveBankFeedTest` (nothing before attach, follows the summary, latest summary wins, nothing collected while unobserved).
  - composeApp: characterization of `LeaveDraftViewModel` before the split (the existing prefill, editing, pair, print and dismissal suites run unchanged on the split); `LeaveBankFillerTest` and `LeaveDraftFillTest` (fills only empty, replace on Refill only, a source that really suspends, a source that answers after the officer typed, the year changed meanwhile, the draft closed meanwhile, no source, an unknown code, each figure from its source); `LeaveDraftStateBuilder` tests (the button states, the notes, the cross-check line for match, difference and no row, nothing when the Leave Bank is off).
- Integration tests: `Part2LeaveBankIntegrationTest` is extended (the real `LeaveBankCalculator` over a synthetic statement, the real bundle, the real `Part2Rules`: fill, then draft, then cross-check); a Robolectric screen test per state (button, Refill dialog, notes, the multi-line break-up box, the cross-check line, a lapsed officer sees no form), `Part2DraftAccessUiTest` unchanged; the Koin wiring test loads `part2Module` directly and proves one feed behind one source.
- Implement: as designed above, then the Pixel 9 walk.
- Exit: gate green + R8 + iOS; flag reverted; item 43 closed by the walk and by the tests.
- Debt watch: a second copy of 30, 60 or 300 (they live in `AcclveCalculator`); a word built in a view model or composable (it comes from `Part2LeaveBankStrings`); a fake that answers instantly hiding an ordering bug (the tests include a source that suspends and one that answers late); `LeaveDraftViewModel.kt` creeping back toward 300.

- Built 2026-10-27 (Phase 8b summary in the session). The owner's answer is DECISIONS Round 33. Where the build differs from the bullets above:
  - **Domain (shared).** `LeaveBankFill` (`domain/LeaveBankFill.kt`, names no code): `fill` (figures), `notes` (facts from what is typed), `crossCheck` (a `YearCheck`: year plus the existing `CrossCheck`), with `FillNote`, `FillOutcome` and `LeaveBankInputs.of` (the live drafts only, this draft left out, through the new `CancelStatus.liveDrafts`, which the reminder planner now reads too). Registered in `AcclveRules.fills` and `EncashRules.fills` and read through `Part2Rules.fill(code)`. `LeaveBankFigures.statementTotal` added; `LeaveEventEditor.setValue` takes a `textMax` (a year list keeps 600, not 200: the editor would have cut a long break-up mid-line). `AcclveRules.alUsed` checks `CivilCalendar.exists` first (the Phase 8 security note). `LeaveBankSource`/`LeaveBankFeed` (`part2/leavebank`): the feed is attached to the one `PayslipViewModel.leaveBankSummary` by `GuideTabRoute` (`attachPart2LeaveBank`), bound once in `part2Module`, collects nothing while no draft is open, and is given to the draft maker only through `leaveBankFor` (the `LEAVE_BANK_ENABLED` kill switch). `FakeLeaveBankSource` is in `shared-test-fixtures`.
  - **Screens (composeApp).** `LeaveDraftViewModel.kt` 279 lines before and after: `AreaPrefill.kt` moved out unchanged (255), then the fill hooks went in. `LeaveBankFiller` (the press, the wait, one atomic edit when the Leave Bank has answered, a removal drops a waiting fill), `LeaveBankView` (words for notes, messages and the check, via `Part2LeaveBankStrings`), `LeaveBankFillView` (button, notes, the Refill question), `YearsBox` (several lines), `CrossChecksView` (under the slip's checks), `FieldValue.Years`, `FillUi`, `SlipUi.crossChecks`. `GuideTab.kt` 263, `App.kt` 255, `Part2Strings.kt` 267 untouched.
  - **Build-time choices (change them if you disagree).** (1) The previous total falls back to the officer's own finished ACCLVE draft **for the year before** only, not "the last ACCLVE draft" whatever its year: a total from another year would be a wrong figure filled quietly. (2) The LTC days are filled only when a statement is held ("0 days encashed" would be a claim, not a fact). (3) A statement or draft figure outside 0 to 9,999 is left for the officer. (4) The cross-check is worded by year ("no row for 2025 yet"), not "the statement of Y+1". (5) The ENCASH note names the statement's own total when it differs from the sum of the lines (the running total is net of encashments, so it can be lower). (6) The ENCASH help no longer tells the officer to use semicolons (the box has several lines; a semicolon still works): the bundle went 389,509 -> 389,474 bytes, the review pack was regenerated.
  - **Counts** (debug / release / iOS): shared 2131 -> 2174 / 2131 -> 2174 (1 known skip, `GemmaModelPathsAndroidTest`) / 1878 -> 1918; composeApp 1735 -> 1783 / 1709 -> 1757 / 1049 -> 1081; scripts tests 100; tool tests 443; XCTest 48 (no Swift changed); corpus 139/139; R8 gate `41 models and serializers kept (guide/model: 32 models, part2: 9 models)` and manifest gate `13 permissions, 1 declared, 0 features, 5 exported components`, both unchanged; Room schema 15, `.pcda` v5 (no stored field, no new `@Serializable` class). Largest files: `LeaveDraftViewModel.kt` 279, `LeaveDraftFillLateTest.kt` 228, `LeaveDraftStateBuilder.kt` 226, `Part2LeaveBankFillUiTest.kt` 225.
  - **Tests.** Red first, each on missing behaviour with the new types stubbed: the stored-date check, `CancelStatus.liveDrafts`, the fill rules, editor limit and statement total (17 of 48), the feed (3 of 4), the strings (6 of 6), the model's fill (12 of 14), the screens (6 of 9). Written with or after the code they check, so with no red run (the deliberate breaks stand in): `LeaveBankInputsTest`, `Part2RulesFillTest`, the integration-test additions, the source guards, `LeaveBankFillerTest` (a thread race, simulated), the wiring tests' bindings, `LeaveBankFillIosPerfTest`. **Mutation:** 106 distinct breaks over four rounds of the work: 103 killed, 1 equivalent (`CancelStatus.liveDrafts` pre-filtering the cancel slips is a speed-up with the same answer), 1 retired (a `forget()` in `cancel()` that a better design made unnecessary) and the no-op control, which survived as it must. First-round survivors that were real gaps, all closed with a test and re-run killed: a newer LTC row hiding the statement total, the cancelled-draft exclusion never exercised from the model, an empty break-up not listed as still needed, and a refused removal wrongly dropping a waiting fill.
  - **Found by building, not by a test:** the editor cut every text answer at 200 characters, which would have cut a 60-year break-up mid-line (`setYears`); a waiting fill belongs to the occurrence it was pressed on, and removing an earlier one shifts the indices (a removal now drops the fill); a fill pressed before the draft had loaded could have published a form with no answers (the model publishes rows only once loaded; tested).
  - **Security.** The `security-review` skill was run on the phase diff: no finding. The one change in a trust boundary is a fix: `alUsed` now refuses a stored date the calendar cannot hold. Figures stay in memory; nothing is logged, reported or sent; `LeaveBankInputs`, `LeaveBankView` and `FillOutcome.Values` print none; three source guards keep it so.
  - **Pixel 9 walk (2026-10-27; Android 17; a debug build of a copy of the tree with the flag flipped there only, installed over the owner's debug copy; driven over adb with uiautomator, printing only fixed strings; the owner's real figures were never recorded).** With the owner's real imported statement: the ACCLVE form offered the button; a press without a year said "Type the leave year first"; with 2025 typed the fill gave four figures and the cap note, and the owner confirmed the previous total and the LTC days equal the statement; a hand-typed figure was kept, "Refill" asked first, "Keep mine" changed nothing and "Replace" restored the statement's figures; the slip for 2025 read "Not checked" (the statement has no 2025 row yet) and for 2024 read the difference line naming both figures; ENCASH filled one line a year in a multi-line box with the years with no line named and the statement-total note (the lines are well-formed, the named years equal the gaps, the note's sum equals the lines); Force Free: the case rows say Premium, "Draft this order" opens the upgrade panel, the slip is read-only with only the HRMS check and Delete; Force Premium put back; the print through Save as PDF gave a valid one-page A4 PDF (deleted) and "Printed on". The match case was first not reached and then walked the same day (item 48a). **Not reached:** see Phase 11 item 48 (b to d).

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

- Built 2026-10-27 (Phase 9 summary in the session). The owner's rule, given at the end of the phase: ODP 2026 is the official document and governs wherever sources differ (item 50 closed on it); the handbooks only fill a gap the ODP leaves (item 51). Where the build differs from the bullets above:
  - **Scope (owner, phase start).** Every leave that returns the officer re-grants: 15 leave templates carry the area links (GTDAL, GTDALCF, GTDCL, GTDSPCL, MTRNLV, SPMTRNLV, GTDCCL, CHADPLVE, PTRNLV, PATLCHAD, FURLEV, EOL, STDLEV, STDLVAL, STDLVFUR); extensions, voluntary rejoining, recall and terminal leave carry none. Special casual leave always re-grants: the ODP excludes only casual leave under 14 days (HAA) or 15 days (field), and the exception names "Casual Leave" alone.
  - **Domain.** `AreaRules` (122 lines) holds the day thresholds once (`HAA_CASUAL_MIN_DAYS` 14, `FIELD_CASUAL_MIN_DAYS` 15), five OneOf questions (R&H, HAA, SCCIA, field, Transport), 31 area conditions and the two derive rules `tran_stop_on` and `tran_grant_on`; `lostMonths` dates the Transport Allowance by whole calendar months, checking that every stored date exists before any arithmetic. A format addition lets a From or To column name a `derived` slot (both validators). New: `templates/area.json` (9 templates) and `tran.json` (STPTRAN and GTDTRAN1-5); 17 new rules; the `links` limit rose 16 -> 31 (item 8 closed).
  - **Screens.** `Part2AreaStrings` (32 lines, a sibling of `Part2Strings`, which stays 267) holds the four questions and the Transport category names; `AreaPrefill` copies other drafts' typed answers but never a date (item 53).
  - **Counts** (debug / release / iOS): shared 2174 -> 2219 / 2174 -> 2219 (1 known skip, `GemmaModelPathsAndroidTest`) / 1918 -> 1937; composeApp 1783 -> 1791 / 1757 -> 1765 / 1081 -> 1086; scripts tests 100; tool tests 443 -> 446; XCTest 48 (no Swift changed); corpus 139/139; bundle 389,474 -> 445,625 bytes (63 templates, 73 rules); Room schema 15 and `.pcda` v5 unchanged. Pins: GuideTab.kt 263, App.kt 255, LeaveDraftViewModel.kt 279, Part2Strings.kt 267, AcclveRules.kt 165, LeaveRules.kt 140; no touched file is over 300 lines; `check_tech_debt_limits.py --strict` clean on the 31 touched `.kt` files and on the full scan of 640.
  - **Tests.** Red first on behaviour with the types stubbed (`AreaRulesTest`, the golden tests of batch 4, the Transport tests, the scope test, the derived-date validator test); 44 deliberate breaks, every real one killed, the no-op control survives, sha256 restore verified for each; the iOS timing test `AreaRulesIosPerfTest` (20,000 passes over every area rule, bound 5 s, measured 0.6 s).
  - **Found by building, not by a test.** Three limits were too small once the links were added: typed boxes per request 16 -> 28, printed rows 170 -> 320, stored payload 128 KB -> 256 KB (worst draft measured 204 KB); each now has a real-bundle test (`Part2LimitsRealBundleTest`, `Part2WorstDraftSizeTest`). The owner accepted the three numbers (Round 35). The prefill would have carried last time's sanction date into a new field-area grant, so it skips dates.
  - **Security.** The `security-review` skill was run on the phase diff: no finding. Every stored date is checked to exist before calendar arithmetic; new free text enters only through the existing slot path (length cap, no markup); no new permission, component, library, schema or backup field.
  - **Gate.** ktlintFormat; `check -x iosX64Test -x iosSimulatorArm64Test` green; iOS link and `iosSimulatorArm64Test` green; full `refresh.py` (three re-dated files restored, `refresh.py --check --allow-no-sources` OK, bundle unchanged at sha prefix 1fe62f5b8f0015b4); script and tool tests; `check_schema_immutability.py`; `:composeApp:minifyReleaseWithR8` and `assembleRelease` green; `check_r8_guide.py` "41 models and serializers kept (guide/model: 32 models, part2: 9 models)"; `check_merged_manifest.py` "13 permissions, 1 declared, 0 features, 5 exported components". `LaunchFlags.PART2_ENABLED` stays false (sha 989cf71e...ca87a1 unchanged).
  - **Pixel 9 walk (2026-10-27; Android 17; a debug build of a copy of the tree with the flag flipped there only, built from d268f934 plus the uncommitted Phase 9 changes, installed over the existing debug copy without uninstalling; driven over adb with uiautomator, printing only fixed strings; synthetic dates in October 2026 and synthetic station, Corps and serial text only; the copy was deleted afterwards).** Casual leave 1 to 13 Oct with GTDHH11 chosen: no HAA boxes, the slip printed GTDCL only; 1 to 14 Oct: the four HAA boxes appeared and the slip printed GTDCL then GTDHH11 from 15/10/2026 with "ODP-2026, p 49, note ii", CAT-III R1H1 wording and the certificate; with SCCIA, field and Transport chosen at 14 days the SCCIA boxes appeared and no field-area or Transport boxes; at 15 days the field-area boxes appeared and the slip was GTDCL, GTDHH11, GTDCIAFD (p 53 note 2) and GTDHAFA (p 55 note 2), each from 16/10/2026 with its certificate, in date order; annual leave 1 to 31 Oct with GTDTRAN3: the Transport boxes appeared (the disability box is required for category 3) and the slip printed GTDAL, STPTRAN from 01/10/2026 (p 71 note v) and GTDTRAN3 from 01/11/2026 (p 71 note iii); 2 to 31 Oct: no Transport boxes and GTDAL only; the print dialog opened and was cancelled (nothing saved). **Not walked** (tests cover them): a draft with ration and R&H beside the areas, the "Seen in practice" label on an area row (no area row carries it), a lapsed Force Free officer (Phase 9 changes no gating), the saved printed PDF, the iPhone (items 14, 20, 47), light theme. The phone keeps the debug build and four more synthetic drafts.

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

1. **Phases 1 and 2a: the R8 gate and the Part II model folder.** CLOSED in Phase 3 (the worker part moves to 7b, below).
   - Evidence: after `assembleRelease`, `scripts/check_r8_guide.py` printed `39 models and serializers kept (guide/model: 32 models, part2: 7 models);
     PayslipDatabase_Impl, Part2Dao_Impl kept; bundle shipped byte-identical`. No keep rule was added. The script now also fails on a registered
     folder with no model (`part2: 0` can no longer pass), accepts `internal`/`private` models, and requires `REQUIRED_CLASSES`; each was written red first
     (`RoomClassesTest`, `VisibilityModifierTest`, `EmptyFolderTest`). Phase 7b adds the reminder worker class to `REQUIRED_CLASSES`. Worker half CLOSED in 7b: required, and required to keep its name, wherever `PART2_ENABLED` is true (with the flag off R8 rightly drops it); proved on a flag-on release built in a copy (see the Phase 7b notes). The real flag-on release is Phase 10's (item 42).
2. **Phase 2a: the rule contract and the copy check had nothing real to compare.** CLOSED in 2b.
   - Evidence: `Part2LookupContract.assertRulesMatchTheRegistry` now compares 20 declared rules with 20 implemented (and 24 templates); a
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
6. **Phase 2a: a template's `rev` is shape-checked in Kotlin, not recomputed.** CLOSED in the Phase 6b follow-ups (owner, DECISIONS Round 21): `Part2TemplateRev` recomputes it on every load (SHA-256 through `CryptoHelper`, the Python tool's own canonical JSON) and `GuidePart2TemplateValidator` refuses a mismatch. Evidence: `Part2TemplateRevTest` (two known-answer values produced by `part2_templates.rev()`, on the JVM and on iOS), `Part2ShippedRevTest` (all 24 shipped templates), the validator tests (a changed word with an unchanged rev is refused; recomputed, it is accepted; a rev of the wrong shape is reported once); 17 deliberate breaks, all killed. The shared fixtures keep readable revisions; the validator suites use sealed copies. Original text: it was `commonMain` has no SHA-256, and the bundle ships inside the
   signed app, so a changed `rev` needs a changed app. The Python stale-bundle guard is the check. Decide at Phase 10 whether to add a
   recompute (`expect`/`actual` hash) or accept; closed by that decision recorded in the Phase 10 summary.
7. **Phase 2b: the check of batch 1 against the printed pages.** CLOSED 2026-10-23 (owner delegated it; DECISIONS Round 15). Evidence: all 24
   templates compared with the rendered pages 46-48, 97-99, 114-118 and 131-132, with Annexure II (serial, name, group) and, mechanically, with every
   narrative fragment in the page text: no correction needed. The 7 fragments the mechanical pass could not find are explained (the PDF text
   interleaves the table cells on p.116; "dated" is the owner's "no. dated date" call in Round 12). Two points were put to the owner: REJOINAL keeps the
   order no. in Data 3 as the row says although note 2 says column 7 (owner: keep), and the NCC / re-employed / retirement-year entitlements (item 11).
8. **Phase 2b: later batches will need decisions on limits and reissues.** CLOSED in Phase 9 (DECISIONS Round 35): the `links` limit rose 16 -> 31 (the longest real template: `GTDAL` and `GTDALCF`, their 16 older links and the 15 area links), in `rules.json` (which ships it, so both validators and the bundle carry it), pinned by `Part2LookupContract`, `Part2BatchFourScopeTest.theAnnualLeaveTemplatesHoldTheMostLinksAndTheLimitIsRaisedToExactlyThat` and `Part2BatchThreeFlowTest`; a deliberate break to 30 is refused by the Python validators (mutant T09). Two storage limits that followed from the old number rose with it, each found by a test and not by a user: `VALUES_PER_REQUEST_MAX` 16 -> 28 (a request's typed boxes were full at the fifth box of the SCCIA re-grant, so the form showed boxes that could not be saved) and `PRINTED_ROWS_MAX` 170 -> 320 (10 requests x (31 links + 1)); `Part2LimitsRealBundleTest` now measures the shipped bundle against all three. Closed in Phase 8: `cl_combination` now covers all 13 other Chapter 5 leave grants (`LeaveRulesTest.casualLeaveBesideEveryOtherChapterFiveLeaveIsAcknowledgedToo`, `Part2BatchThreeFlowTest`), and the reissue is settled from the PDF: para 11 (p.8) forbids amending an order, so an extension, voluntary rejoining or recall asks "Did the earlier order also grant or stop ration or R&H?" and on Yes warns (`reissue_allowances`, p.8, notes "11, 12.3") that the earlier rows are cancelled and notified afresh with CANCELS; no rows are drafted (`Part2BatchThreeFlowTest.anExtensionOrReturnThatTheOfficerSaysMovedEarlierAllowancesPointsAtParaEleven`, 4 codes). Still open for Phase 9: `links` is 16 and GTDAL uses all 16, so the HAA, SCCIA, field and TPTA re-grants raise the limit with its reason (Round 12; no link-group feature). The new Chapter 5 codes carry at most 4 links, so no raise was needed here. Original text: `links` is 16 and `GTDAL` uses all 16; Phase 9 adds the HAA, SCCIA, field and TPTA re-grants to the leave grants, so the limit rises with its reason (owner, Round 12; no link-group feature). `cl_combination` knew only the annual set, and an extension, voluntary rejoining or recall did not move the ration or R&H dates already drafted. What 6c learned: a CANCEL covers every occurrence of an order, so cancelling a leave order cancels its linked ration and R&H rows with it and needs no per-row cancel; and the corrected draft is a copy of the draft's requests (`CancelDrafts.redraftEvent`).
9. **Phase 3: a full vault did not fit the memory of a backup.** CLOSED in phase 3b (owner chose the chunked backup, Round 14). Before: 120 pages of
   500 KB (about 61 MB) needed about 0.8 GB of heap on the JVM and the iOS process peaked at 1,221 MB, because the archive was one JSON string
   (about 13 times the vault). Evidence after: `Part2VaultScaleJvmTest` passes with the source database, the target database and the 58 MB archive
   all in a 256 MB heap (`maxHeapSize` for the shared unit tests), extra heap 174 MB (export) and 102 MB (restore); the iOS twin (`Part2VaultScaleIosTest`) measured export 5.3 s -> 0.6 s,
   restore 2.9 s -> 1.0 s and the process peak resident 1,221 MB -> 469 MB (both databases and the archive included). Still true and not a defect: the caller holds the archive as one `ByteArray` (about the vault's size), so a
   streaming `Sink` API would be the next step if the 100 MB ceiling ever rises.
10. **Phase 3: skipped restore rows are counted but not shown.** CLOSED in Phase 6b (`RestoreReport`, `restoreStatus`; `PayslipViewModelRestoreReportTest` makes a file with unreadable rows and reads the count from the view model's callback, `RestoreStatusTest` pins the sentence). Before: OPEN. `Part2Restore.skipped` counts rows left out (unreadable, oversized, surplus,
   orphaned), but `PayslipBackupService.restore` returns `Result<Unit>` and drops it, so the officer is never told. Why deferred: no screen exists yet.
   How closed: Phase 6b shows "N items could not be restored" and a ViewModel test covers it.
11. **Phase 2b check: annual leave entitlements the 60-day check does not know.** CLOSED in Phase 8 by tests (the owner's own read of the batch 3 pack is item 44). Note 3 (p.115): NCC and re-employed officers get 30 days of annual leave in a year (60 for one retired and re-employed in the same year); note 13 (p.116): the retirement-year entitlement by date. Evidence: `LeaveCapRulesTest` (30 for the NCC answer, 60 with the same-year answer, the retirement date cuts 30 / 58 / 59 / 60 at their edges, a leave of another year or a date the calendar rejects is judged as usual, the tighter limit wins; 14 deliberate breaks, all killed), `Part2BatchThreeFlowTest.theNccAndRetirementCapsReachTheAnnualLeaveCheckThroughTheAnswersAndTheDate` (the real bundle), `Part2BatchThreeFormTest` and `Part2FormRealBundleTest` (GTDAL and EXTNAL ask the two new yes/no questions; the retirement date is an optional field). Built as: two yes/no questions and an optional date field on GTDAL, EXTNAL and STDLVAL, read by `al_year_cap`; "28/29 Feb" is read as the last day of February (58 in a common year, 59 in a leap year).
12. **Phase 4a: the Pixel 9 walk.** CLOSED 2026-10-23 (owner backed up first). The Play 1.5.0 copy was uninstalled, a debug build with `PART2_ENABLED` flipped locally was
   installed and driven over adb (uiautomator for taps and text, screenshots kept in the session scratchpad only). Observed: the Part II row on Guide Home;
   Part II home with Leave and Records tiles only; Leave -> "Annual and casual leave" ("Ch 5, pp 114-116 · 5 codes") -> GTDAL page (Group A, "Financial (other than
   claims)", chapter 5 page 114, serial 5.1) and the "Also see" row; search by code (`rejoinal`: page 131 with the "slip prints REJOIN" note), by serial (`3.28`: "Group not
   printed in ODP-2026" on the row and the page; `5.10` gives EOL only), by variant code (`gtdtran3`: one row with GTDTRAN1-5), by name (`location allowance`: 2
   results), one letter ("Type at least two letters or digits.") and no match; true process death on the EOL page (`run-as ... kill -9`, pid 16037 gone, new pid
   18838, the same page restored, Back to Part II home with an empty search, as the query is memory-only); the Guide tab re-tap from a deep level to Guide Home; light and dark
   (the phone started in dark and was put back). Reverted: `LaunchFlags.kt` is byte-identical (sha256 989cf71e9e52262a480b3823c047510270cc552587db7c679c37c861c1ca87a1),
   the debug build is uninstalled, and the owner reinstalls from Play and restores the `.pcda` backup. Not observed: system Back from the restored page (the on-screen
   Back was used), TalkBack, and a small-screen or large-font layout.
13. **Phase 4a: `GuideTab.kt` wiring grows with each feature.** CLOSED 2026-10-23 (owner: fold the hosts now). `PreClaimHost` and `Part2Host` now sit in one
   holder, `GuideHosts` (`rememberGuideHosts` builds both), so the tab's `when` branches, `GuideHomeRoute` and `GuideDestinationContent` take one `hosts` argument
   and a new feature adds a field there instead of lines here. `GuideTab.kt` is 263 lines: 265 after the first 4a build, 257 before 4a (the 6 that remain are the
   Part II factory parameter, its kdoc, imports and the one `when` branch, which cannot be folded). `GuideTabWiringSourceTest` pins it: at most 263 lines, and the file names
   neither host type nor either builder. The fold edits only call sites in `GuideTab.kt`; `GuideTabPreClaim.kt` is untouched, and every pre-claim and Part II screen test is green on it.
14. **Phase 4a: iOS look and gestures of the Part II screens.** OPEN, a manual gate. Phase 6b adds the vault: the Part II home section, scanning from it and from a slip, the order screen (number, date, viewing and hiding a page, Add pages, Open the draft, delete with its confirmation), the Settings line, and the lapsed (read-only) state; and the restore message. Only the framework link, `iosSimulatorArm64Test` (707 tests) and the
   Native timing of the search walk were observed (794 ms for about 930 searches, budget 1,500 ms). Nothing was driven on an iPhone: the levels are inline in
   the Guide tab like every other Guide level, with the on-screen back header, and the iOS edge-swipe does not apply to a tab. Closed by the Phase 10
   iPhone checklist (browse, search, back at each level, light and dark). Phase 4b adds the draft screens to that list: the form (date picker, chips, order reference), the slip preview, the drafts list with its delete dialog and the upgrade panel. Phase 5 adds: Print (the iOS print sheet, an AirPrint printer and Save to Files; the sheet's `completed` flag after Print and after Cancel), that the edge-swipe cannot dismiss the sheet or leave the slip while it is up, the "Printed on / Changed since printed" lines, and the "Check the HRMS draft" screen, in light and dark. Phase 6c adds the cancel flow: "Cancel this order" and "Cancel and re-draft" on a vault order, the cancel slip and its print sheet, the hop between the two slips, the time-bar note, and the note on the cancelled order. Phase 7a adds the Due list and the import card: the "Due" row on the Part II home (its count, and the "Nothing is due right now." line), the list (Due now with the overdue colour, Coming up, an empty list, the unreadable line), a row opening its slip, the upgrade panel for a lapsed officer, and the Dashboard card after an import ("See what is due" opening Guide, Part II, Due; "Not now"), in light and dark. Phase 8b adds the year-end forms: the "Fill from your Leave Bank" button and its "Refill" question (dialog, Replace, Keep mine), the filled figures and the cap note under it, the multi-line break-up box with its missing-years and statement-total notes, the "Leave Bank check" line on the ACCLVE slip (match, difference, not checked), and the lapsed officer's read-only slip. Phase 6a adds the camera (item 20): the permission prompt and its text, scanning two pages, Cancel, denying the permission, the camera closing when the app locks, and no new image in Photos.
15. **Phase 4b: the Pixel 9 walk.** CLOSED 2026-10-23 (owner: "proceed with phone check"). The Play copy had already been uninstalled after the 4a walk, so no app data was on the
   phone and no backup was needed. A debug build with `PART2_ENABLED` flipped locally was installed and driven over adb (uiautomator for taps and text, screenshots kept in the
   session scratchpad only). Observed on the Pixel 9 (Android, dark theme):
   - Case rows carry "Draft this order"; the GTDAL form shows the ration tick, the R&H chips (None first), the date boxes with the real calendar dialog (dates picked with it), digits and
     text boxes and the half-filled order reference's error with "Clear"; the footer said "Still needed: Serial in the notification" until it was typed, then "See the slip" opened.
   - Slip for AL 06-20 Oct with ration and GTDRH12: one order, 7 rows in date order (SORS, GTDRSHNA, STPRH, GTDAL, TORS, STPRSHNA, GTDRH12), 15 days counted inclusively, the practice rows
     labelled "Seen in practice" and the manual rows "ODP-2026, p 97, note 1.1" etc.
   - Process death: pid 31096 killed with `run-as ... kill -9` after HOME, new pid 631: the app reopened on the same slip with every row; Edit showed every answer (area details included).
     An earlier-taken 50 days raised the 60-day `ack` card ("ODP-2026, p 115, note 2") with its tick; ticking it showed "All checks are ticked".
   - Lock: with the app passcode on, a kill and relaunch showed the lock screen; unlocking returned to the same slip, read back from the database, with the tick cleared (ticks are not saved).
   - Force Free (the lapsed state, one stored draft): the Part II home still listed "Your drafts"; opening it gave the read-only slip with "Your Premium has ended..." and only "Delete draft"; the
     case rows carried "Premium" and "Draft this order" opened the upgrade panel; delete asked first, Cancel kept the draft and Delete removed it.
   - System Back from the form returned to the case; opening a form and leaving it without typing left no draft.
   Reverted: `LaunchFlags.kt` byte-identical (sha256 989cf71e9e52262a480b3823c047510270cc552587db7c679c37c861c1ca87a1), the debug build uninstalled (the phone had no app when the walk
   began), the phone still in dark theme. The owner reinstalls from Play and restores the `.pcda` backup.
   Not observed: the Play upgrade sheet behind "Unlock with Premium" (needs real billing), light theme, TalkBack, a small screen or large font, and the in-process lock (Android locks only on a
   cold start, so the lock path walked was restore-from-database; the in-process discard is proved by `GuideNavPart2DraftTest` and the iOS lock is the Phase 10 iPhone walk).
   Seen and not fixed (no Part II defect): the certificate line reads "Certificate: CERTIFICATE: ..." because the manual's own text begins with that word and the row adds its label; and the lock
   screen showed "Incorrect PIN, please try again" before any PIN was entered (existing lock-screen behaviour, not touched in 4b). Both are for the owner to decide.

16. **Phase 5: the Pixel 9 walk and the real PDF.** CLOSED 2026-10-23 (owner connected the phone; the Play copy was not installed, so no data was at risk). The walk used a debug build made in a copy of the working tree with
   `PART2_ENABLED` flipped, so `LaunchFlags.kt` in the repo was never edited (sha256 989cf71e...a1b3 unchanged); driven over adb (uiautomator for taps and text). Observed on the Pixel 9 (Android, dark theme):
   - GTDAL 06-20 Oct with ration: the slip's 5 rows, then "Print the slip" opened Android's system print dialog (A4 preview "Page 1 of 1", the same text as the screen). Cancel left no "Printed on" line and the button enabled.
   - Printing to "Save as PDF" proposed the file name "Part II slip" (the fixed job name) and, once saved, the screen read "Printed on 23/10/2026". The saved file (53,148 bytes) is a valid one-page A4 PDF (Skia/PDF); `pdftotext` and a
     rendered image matched the preview row for row: title, last-order blank, the notice, "Order 1 · Group A, C", GTDAL with its From/To and Data lines, SORS, GTDRSHNA with the RMA certificate as "CERTIFICATE: RATION MONEY
     ALLOWANCE 1. ..." (no extra label), TORS, STPRSHNA and "Page 1 of 1". The PDF was pulled to the session scratchpad and deleted from the phone.
   - "Check the HRMS draft": "Row 1 of 5" ... "Row 5 of 5", each field under its label (Code, Narrative, From, To, Data 3, Data 4), empty columns not listed.
   - True process death (`run-as ... kill -9`, new pid 14293): the app reopened on the same slip with "Printed on 23/10/2026" read back from the encrypted database.
   - Edit after printing: "Changed since it was printed. Print it again before you hand it over." appeared; printing again replaced the record and removed the note.
   Reverted: the debug build uninstalled, the phone's theme untouched (dark), no PDF left, the copy of the tree (with the keystore) deleted.
   Not observed: a physical printer (the phone offered one), a failed or cancelled job after "Print" was pressed, the lapsed (Force Free) state's slip without Print (proved by `Part2PrintUiTest`), light theme, TalkBack, a small screen or large font.
17. **Phase 5: "Printed on" was a UTC day.** CLOSED 2026-10-24 (the owner asked for the open items to be fixed; earlier they had accepted UTC for now). `localUtcOffsetMillis(atMillis)` (`expect`/`actual` in `shared/.../utils/`: Android
   `TimeZone.getDefault().getOffset(instant)`, iOS `NSTimeZone.defaultTimeZone.secondsFromGMTForDate`) gives the device's offset at the instant, and `PreClaimDates.dayOfInstant` moves the UTC day by it. "Printed on" and the drafts list's
   "edited" date use it (the builder and the drafts model take the offset as a parameter, so tests pass a fixed one). The calendar picker stays UTC midnight on purpose. Evidence: `LocalTimeOffsetAndroidTest` and `LocalTimeOffsetIosTest` (India
   +5:30 at any instant; New York -5 in January and -4 in July), `PreClaimDatesTest` (20:00 UTC on 23 Oct is 24 Oct at +5:30, 23 Oct at -4; the local midnight is the first instant of the new day), `LeaveDraftPrintedStateTest` and
   `Part2DraftsViewModelTest` (the same draft shows 24/10 in India and 23/10 in New York and UTC); six deliberate breaks, all caught. Other dates the Guide shows were not touched.
18. **Phase 5: `LeaveDraftViewModel.kt` was 288 lines (265 before).** CLOSED 2026-10-24. The autosave (the 300 ms pause, the write lock, the dirty and failed flags, the write on leaving, the delete) moved unchanged into
   `DraftSaver.kt` (98 lines); the model is 258 lines, below where Phase 4b left it. The existing `LeaveDraftViewModelTest`, `LeaveDraftEditingTest`, `LeaveDraftPrintTest` and `LeaveDraftPrintRecordTest` ran green on it unchanged; 13
   deliberate breaks of `DraftSaver` found one redundant line (`pending?.cancel()` in `closing()`, since the model cancels its scope right after; deleted) and one gap (a finished save still counted as unwritten: `leavingAfterTheChangeWasSavedWritesNothingMore`).
   `Part2DraftSourceTest` now names the saver beside the model as the only writers and holds both to "through `Part2Repository`, never Room, a DAO or `CryptoHelper`". `Part2Strings.kt` (259) stays on the watch.

19. **Phase 6a: the scanner on a phone that has never scanned, offline.** CLOSED 2026-10-24 by the owner's decision (DECISIONS Round 19): accepted, no pre-download; the message is item 23's. The spike scanned offline only after one online scan, so the Play services scanner module was already on the phone. A phone that has never scanned and is offline probably cannot start the scanner until Play services has downloaded the module once. Not tried (it needs a phone with that state). The app handles it (`MlKitScanSource` maps a scanner that cannot start to `ScanFailure.UNAVAILABLE`, no crash; unit-tested). What remains is 6b's: it words the message (item 23), and the Phase 10 release walk tries a first scan offline to see it. Phase 7b adds the notifications (item 20 lists the iPhone-only checks).
20. **Phase 6a: the iOS camera.** OPEN, a manual gate. The simulator has no document camera (`VNDocumentCameraViewController.isSupported` is false), so only the Kotlin side, the unsupported path, the callbacks and the in-memory hand-over are tested (Swift XCTest 9 cases, Kotlin iosTest 13 scanner cases). Not observed: the camera permission prompt and its text, scanning, closing with Cancel, denying the permission, the camera closing when the app locks, pages beyond the room left being dropped, and that Photos gets no new image. Closed by the Phase 10 iPhone walk; item 14's list gains these (and, from 6c, the cancel flow's own camera use: a published cancel order is scanned like any other). From 7a the iPhone walk also covers the Due screen and the import card (item 14 lists them). Phase 7b adds, on a real iPhone: the notification permission prompt (after our explainer) and a decline never asked again; a reminder delivered with the app closed (use a short test build) showing only "PayslipMax / A Part II reminder is due."; a tap with the app open, with a detail pushed (it must close first), with the app closed, and while the app is locked (it waits, then opens Due); and that pending reminders survive a restart and that Settings -> Notifications shows PayslipMax.
21. **Phase 6a: the committed pipeline on a device.** CLOSED 2026-10-24 (the owner reconnected the Pixel 9 after the 6a commit; nothing of the owner's was on the phone). A harness built from the 6a commit (a copy of the tree, never committed: one button that runs the committed `rememberPageScanner()`, `providePageEncoder()` and `PageCapture` over a repository that writes the stored pages to a folder) was installed and driven over adb. Observed on the Pixel 9 (Android, dark theme), with the owner scanning a public printed book page (never an order), one page per scan, twice:
   - The real ML Kit scanner opened from the committed glue. Each run logged `Saved(1, notSaved=0)` and `cacheLeft=0`; `ls <app cache>/mlkit_docscan_ui_client` was empty afterwards, so the scanner's page file was deleted on success.
   - The stored pages are `page1.jpg` 161,410 bytes (1027 x 1600) and `page2.jpg` 191,744 bytes (1306 x 1600): long edge 1600, well under 500 KB, upright and sharp when viewed. Their JPEG segments are only JFIF, quantisation, frame, Huffman and scan (`E0 DB DB C0 C4 C4 C4 C4 DA`) and end with `FF D9`: no APP1 to APP13, no comment, no colour profile. No "Exif" text in the bytes, and `sips -g all` shows no orientation, make, model, date or GPS (`exiftool` is not installed on the Mac, so the marker walk and `sips` stand in).
   - No new file outside the app's own folders; the page was never offered to the gallery (the harness writes nothing there).
   Reverted: the harness uninstalled (its external folder went with it), the copy of the tree deleted, the phone's theme untouched (dark), `LaunchFlags.kt` byte-identical (sha256 989cf71e...ca87a1).
   Not observed: two pages in one scan (each scan was one page, so the multi-page return of ML Kit and the order of several pages on a device were not walked; unit-tested), a cancelled or failed scan on the device, and the walk-away case.
22. **Phase 6a: plaintext that cannot be overwritten.** OPEN as an accepted residual for the Phase 10 security review. The decoded bitmap (Android) and image buffers (iOS) are freed by the runtime, not overwritten; `NSData` cannot be zeroed; the scanner's cache file is deleted, not overwritten (app-private cache, file-based encryption on Android). Every plaintext `ByteArray` the app owns is overwritten. Nothing is written outside the app's private cache. Closing it would need a custom codec; no change planned.
23. **Phase 6a -> 6b hand-offs.** CLOSED in Phase 6b: `discardLeftovers()` when the Part II home opens, when an order level is restored and when the Guide leaves the screen (`Part2VaultUiTest`: opening, restored level, lock); every `CaptureResult` worded (`VaultNoticesTest`, 11 breaks, 0 survivors); capture gated on the entitlement (`Part2VaultHostTest`, `Part2VaultUiTest`); the 60 MB line in Settings (`VaultSizeWarningTest`, `VaultSizeSettingsUiTest`). Original text: 6b must 6b must: call `PageScanner.discardLeftovers()` when the vault opens and when the app locks (a killed process can leave a page in the cache); word `CaptureResult.OrderFull`, `Failed(UNAVAILABLE)`, `Failed(FAILED)`, `Saved(notSaved)` and the refusals (`VAULT_FULL`, `TOO_MANY`, `NO_SUCH_ORDER`) in `Part2Strings`; gate capture on the entitlement (a lapsed officer can view and delete only); show the 60 MB warning. 6b's tests cover each.
24. **Phase 6a: the colour profile is dropped.** CLOSED 2026-10-24: the owner chose to leave it dropped (DECISIONS Round 19); 2026-10-25 a scanned page was viewed on the Pixel 9 with natural colours (blue table headers, white paper), nothing looked wrong. The metadata cut removes every APP2 segment, which includes a colour profile; iPhone photos in Display P3 are stored without it and show with sRGB colours. Photos of black-and-white text pages are not affected visibly; the owner can look at a scanned page in the 6b walk.
25. **Phase 6b: the Pixel 9 walk.** DONE 2026-10-25 (owner unlocked the phone and scanned; the Play copy was not installed; a debug build from a copy of the tree with the flag flipped there, driven over adb). Observed (Android, dark theme): the "Your published orders" section on the Part II home with the scan button; the real ML Kit scanner opened from it and the order screen opened on return; the number and date typed and shown as the row title; "View page 1" showed the stored page upright and in natural colours; kill -9 (pid 25556 gone, 4966) and relaunch: the order and its row came back from the encrypted database; a Leave draft made with the calendar dialogs and its slip showed "Last Part II order: <number> dated 13/10/2026" from the vault; "Add the published order" on the slip scanned a second page ("1 page saved."; the order is linked to that draft), "Open the draft" returned to the slip, and the slip listed the linked order; Force Free: the slip read "Your Premium has ended...", no Add button, the order screen read-only with no boxes and no Add pages, "Open the draft" and Delete still there; delete asked first, Cancel kept the order, Delete removed it, and the draft was left; the scanner's cache folder was empty after the scans. Reverted: the app uninstalled, the phone's theme unchanged (dark), the copy of the tree (with the keystore) deleted, my screenshots deleted, `LaunchFlags.kt` byte-identical (sha256 989cf71e...ca87a1). The owner scanned a document that showed a real person's details for the first page; it was removed with the app and none of it was kept or recorded. Not observed: backup then restore of the vault and the "N items could not be restored" line on the phone (unit-tested), a cancelled or failed scan on the phone, the first scan on a phone that never scanned, offline (item 19's walk), two pages in one scan, the vault-size line (it needs over 60 MB; unit and Robolectric tested), TalkBack, a small screen or large font, light theme.
26. **Phase 6b: not built, for the owner to ask for.** CLOSED the same day (owner asked for all three, DECISIONS Round 21): (a) one page can be deleted (`VaultOrderPageDeleteTest` 7 cases, `Part2VaultPageDeleteUiTest`); (b) the carry-forward order box offers the vault (`VaultOrderSlotsTest`, `Part2VaultRealBundleUiTest`); (c) an order can be linked to a draft and unlinked later (`VaultOrderLinkTest`, `Part2VaultLinkUiTest`, `VaultOrderEditorTest`). 51 deliberate breaks, 6 survivors, each a gap or a redundant line (two guards deleted, four tests added), and all re-run killed. Not walked on the phone by me; the owner is testing the debug build. Original text: (a) A single page cannot be deleted on its own: a bad page means deleting the order and scanning again. (b) The picker is offered for the grant box only; "Order allowing the carry forward" (`carry_order`, GTDALCF) is also a vault order but was not asked for. (c) An order cannot be linked to a draft after it was scanned (owner chose "at scan time only", Round 20). Each is a small change if the owner wants it.

27. **Phase 6c: the owner's content review of batch 2.** OPEN for the owner's own read; CHECKED by Claude the same day (delegated, DECISIONS Round 23). The pack is `docs/Plan/rule_cards/part2/part2_review.md` (`part2_review.py`). Both templates were compared with pp.226 and 227 read as images and with the page text (serial, group, narratives, columns 4 to 9, both certificates, notes 1-4 and 1-7, the code name CANCELS in note 7): no correction. The exit is therefore "checked, not yet read by the owner". Closed by the owner's reply recorded in DECISIONS, or a template correction with its golden test.
28. **Phase 6c: the Pixel 9 walk of the cancel flow.** DONE 2026-10-25 (owner unlocked the phone and scanned one public printed page; a debug build of `42b5a46a` from a copy of the tree with the flag flipped there, installed over the owner's debug copy without uninstalling; driven over adb with uiautomator). Observed (Android, dark theme): the Part II home with an empty vault and no drafts; a GTDAL draft (5 to 9 Oct 2026) made through the real calendar dialogs, whose "Add another occurrence" listed exactly the eight Leave codes and no cancellation code; "Add the published order" opened the ML Kit scanner and the owner's scan came back as "1 page saved"; the order screen said "To cancel this order, type its number and date above first" with no cancel button until a synthetic number was typed and a date picked, then both "Cancel this order" and "Cancel and re-draft" appeared; "Cancel and re-draft" opened the cancel slip (CANCEL, "Cancelled all occurrence of Part II Order Number ... dated ...", Data 1 "ALL", Data 3 and 4 the order, the certificate, "Open the corrected draft"); Android's print dialog showed the same rows on one A4 page and "Save as PDF" wrote a valid one-page A4 PDF whose text matched, after which the slip read "Printed on 25/10/2026" (the PDF was pulled for checking and deleted from the phone and the Mac); "Open the corrected draft" showed the copied GTDAL slip with "Open the cancel slip", and one Back from the cancel slip went to the order, not round a loop; the order's screen, its row in the vault list and its row on the slip it came from all read "Cancel slip printed on 25/10/2026."; changing the order's date removed the note; with the order dated Oct 2023 "Cancel this order" gave a slip with the time-bar warning "ODP-2026, p 226, note 4" (no tick, printable) and the form had the optional "Date of the earliest occurrence" box; adding a CANCELS offered only CANCEL and CANCELS, a typed code GTDALX raised "The code you typed is not one the manual lists" (p 227 note 5) beside CANCELS' own time-bar line (p 227 note 6); a true kill (`run-as ... kill -9`, pid 27221 gone, new pid 31421) and relaunch restored the same slip with its checks; Force Free: the Part II home without "Scan a published order", the slip read-only with "Your Premium has ended" and only the HRMS check and Delete draft, the order screen read-only with no cancel offer, hint or Add pages (Open the draft and Delete order remained); the override was put back to Force Premium, which it was before. **Found and fixed:** the drafts list said "Not finished" for a finished cancel slip (it has no dates, and the line treated no dates as unfinished); `Part2DraftSummary.finished` now tells them apart and the list says "Ready" (`Part2DraftSummaryRealBundleTest`, `Part2CancelUiTest`, 3 deliberate breaks killed). Seen and not changed: the Part II home puts the cursor in the search box on entry, so the keyboard opens over the lists (existing since 4a), and the cancel slip's last-order line quotes the order being cancelled when it is the newest in the vault (para 6.7 as built). Not observed: the second stage "Cancelled by order ... dated ..." (it needs a second scan of a cancel order; unit and screen tested), light theme, TalkBack, a small screen or large font, the iPhone (items 14 and 20). Left on the phone for the owner: three synthetic drafts and one synthetic order (a public page scanned as its page). My screenshots are deleted.
29. **Phase 6c: the time bar is counted from the order's date.** CLOSED 2026-10-25 (owner: optional box that falls back, DECISIONS Round 23). Both templates have an optional "Date of the earliest occurrence" box (`occurrence_on`, only the check reads it, never printed); the warning counts from the earlier of it and the order's date. Evidence: `CancelRulesTest` (the earlier date decides, a date not chosen or not a date is ignored, the occurrence alone is enough), `Part2CancelGoldenTest.theOccurrenceDateIsNeverPrintedAndNeverRequiredButTheTimeBarReadsIt`, `Part2CancelRealBundleTest` (a young order with an old occurrence warns; the box is optional) and `test_part2_cancel.OccurrenceDate`; 14 deliberate breaks, all killed.
30. **Phase 6c: smaller edges left.** CLOSED 2026-10-25 (DECISIONS Round 23). (a) A typed CANCELS code not in the manual's list (or one the slip prints) raises the `cancels_code_unknown` warning, never a block (`CancelRulesTest`, `Part2CancelGoldenTest`, `Part2CancelRealBundleTest`, `test_part2_cancel`). (b) The corrected-draft button follows the partner live (`LeaveDraftPairTest.theButtonFollowsThePartnerLiveWhenItIsDeletedOrSavedAfterTheSlipLoaded`, and `theButtonAppearsEvenWhenTheListOfDraftsAnswersBeforeTheSlipHasLoaded` for the ordering the first survivor exposed). (c) "Cancel and re-draft" is not offered when the linked draft is itself a cancel slip, and `OrderCancelling` refuses it with its own words (`VaultCancelStateTest`, `OrderCancellingTest`, `Part2CancelUiTest`, `VaultCancelRefusalTest`).
31. **Phase 6c -> 7a hand-off.** CLOSED in Phase 7a. "None after cancellation" reads `CancelStatus.of(order, drafts, orders)` (a printed cancel slip, then the published cancel order linked to it), never a stored flag: `ReminderPlanner` calls it for every order linked to a draft, and a draft whose order is cancelled in full (scope ALL) is void. Evidence (`ReminderPlannerCancelTest`, 16 tests; `ReminderPlannerRepositoryTest`, the same through the real encrypted repository): a printed CANCEL slip voids the draft at once; the published cancel order keeps it void; an unprinted cancel slip cancels nothing; a CANCELS (one occurrence) stops none of the order's other reminders (with and without its own published order) while a CANCEL beside it still wins; a cancel naming another order or date voids nothing and stray spaces in the quoted number do not hide one; the cancel slip is an ordinary printed draft with its own "add the published order" series that its own cancel order ends, and is never itself void; the corrected draft is an ordinary draft with its own reminders once printed; a void accumulation draft does not answer another draft's year end and a void rejoining does not answer a return. **A finding for the owner on the hand-off's third case:** "a cancelled draft's 'add the published order' reminder" cannot arise as a separate effect. A cancel needs the order's number and date, and an order with a number and a date linked to the draft is exactly what "published" means, which already ends that reminder; so `aCancelledDraftHasNoAddTheOrderReminderAtAll` pins the combination (a change to either rule that brought the nagging back would fail it), and the cancel's own effect shows on the return and year-end reminders. Four of the deliberate breaks were of this logic (any cancel voids, nothing is void, cancel slips ignored, the order filter in `CancelStatus`), all killed.
32. **Phase 7a: nothing is dismissed and nothing expires.** CLOSED the same day (owner chose a stored "Dismiss", DECISIONS Round 26). Every Due item, due now or coming up, has a "Dismiss" button; the name of the reminder (`return`, `add-order`, `year-end-<year>`) is stored in the draft's optional `dismissed` list inside the encrypted payload `v: 1` (no schema, backup-version or R8 change, no new `@Serializable` class; the field is bounded by `Part2Bounds`: at most 32 plain-token names), the planner skips the whole series (so a scheduler is given nothing for it), and a completed print of the draft clears its dismissals (the way back; Round 18). Evidence: `ReminderPlannerDismissTest` (9), `Part2DraftDismissedCodecTest` (6: round trip, an older row without the field, ciphertext, the count and token limits, an impossible stored name is unreadable), `fullDraft` now carries two dismissals so the Room, backup and restore round trips prove they travel, `DueViewModelDismissTest` (8: only the `dismissed` field changes, edit time and print record kept, other drafts untouched, a name already dismissed is not written again, a refused write leaves the row), `LeaveDraftDismissedTest` (the form keeps them through a save, a completed print clears them, one that did not complete does not), `Part2DueUiTest` (a button on every item, a dismissal removes the item for good) and `Part2DueRoomTest` (over the real repository, surviving a close and reopen); 26 deliberate breaks of this work plus 3 on the Room test, all killed (one identity edit I wrote by mistake survived as a no-op control; its real form was killed).
33. **Phase 7a -> 8 hand-off: the ACCLVE template.** CLOSED in Phase 8 by `Part2YearEndReminderRealBundleTest` (the real ACCLVE template drafts from the answers a form gives; its code is `ReminderRules.YEAR_END_REQUEST`; its `year` slot is a number slot the rules read; a printed GTDAL of 2026 gives the year-end reminder before an ACCLVE for 2026 exists and none after, even one not printed; an ACCLVE for another year answers nothing). Original text: OPEN, for Phase 8. The year-end reminder is answered by a request whose code is `ReminderRules.YEAR_END_REQUEST` and whose number slot `year` (the slot every annual template shares) is the leave year. Today only a synthetic draft proves it (`ReminderPlannerYearEndTest`), because ACCLVE has no template. Phase 8's template must keep that code and slot id, and a real-bundle test must draft an ACCLVE for year Y and watch the year-end reminder for Y end. Closed by that test.
34. **Phase 7a: a leave that is extended.** CLOSED the same day (owner: build it now, DECISIONS Round 26). A printed leave that starts on or before another leave's return day and returns after it takes over that return reminder (chains too); a duplicate slip for the same days, a leave that ended before that day, an unprinted draft and a leave whose order was cancelled in full do not. The return day used is the one after the suffix, not after the last day of leave. Evidence: `ReminderPlannerExtensionTest` (11); 7 deliberate breaks of the rule (strict start, equal end, unprinted and void spans, among them) all killed. A clause I first wrote (`draftId != this draft`) was dead, since a draft's own leaves never return later than its latest return, so it was deleted rather than tested. Writing the tests also showed that eight earlier fixtures used adjacent or overlapping leaves, which now correctly take each other over; they were rebuilt with leaves three days apart.
35. **Phase 7a: what the Pixel 9 walks did not reach.** The card and the Due list are WALKED; OPEN only for the light theme, TalkBack, a small screen or large font, a Due screen left open past midnight (it is rebuilt when the drafts or orders change or the screen is entered again, not on a timer) and the iPhone (items 14 and 20). Two walks on 2026-10-25 (owner attached the phone, unlocked and awake; debug builds of the working tree that became `aceb6dba` and then of `5d2de477`, each made in a copy with the flag flipped there only, installed over the owner's debug copy without uninstalling; driven over adb with uiautomator; the Dashboard's real payslip data was never printed or recorded and no screenshot was taken). **Walk 1 (`aceb6dba`), Android, dark theme:** the Part II home's "Due" row reading "Nothing is due right now." while the only printed slip was a cancel slip; the Due list with "Coming up: Add the published order, CANCEL, Due 08/11/2026" (its print on 25 Oct plus 14 days); printing the corrected GTDAL draft through Android's system dialog (Save as PDF) gave "Printed on 25/10/2026", after which the row read "1 thing is due." and the list read "Due now: Draft your rejoining slip, GTDAL, Overdue since 10/10/2026" (leave 5 to 9 Oct) with "Coming up": two "Add the published order" lines dated 08/11/2026 and "Draft the year-end leave accumulation for 2026" dated 01/03/2027; tapping the overdue item opened that draft's slip and Back walked slip, Due, Part II home, Guide Home; a true kill (`run-as ... kill -9`, pid 21976 gone, new pid 24309) with the Due list open restored the same list; Force Free: the restored Due level showed the upgrade panel ("Drafting is a Premium feature") and no item, and the Part II home had no Due row while the drafts stayed listed; the override was put back to Force Premium and the row came back. **Walk 2 (`5d2de477`):** a "Dismiss" button on every row, due now and coming up; dismissing "Add the published order" for the CANCEL slip removed that row and left the rest; a true kill (pid 28960 gone, new pid 29137) and a fresh walk to the list showed it still gone (stored in the encrypted database). **The card, with the owner importing the September 2026 payslip on the phone:** the Dashboard showed "Part II reminders / 1 reminder from your slips is due." with "See what is due" and "Not now"; "See what is due" opened the Due list with the overdue return, Back went to the Part II home and then Guide Home; back on the Dashboard the card was still there (tab switches keep it); under Force Free it vanished and under Force Premium it came back; "Not now" hid it. Cleaned up: the saved "Part II slip.pdf" removed from the phone's Downloads, both tree copies (with the keystore) deleted, `LaunchFlags.kt` byte-identical (sha256 989cf71e...ca87a1), the override left at Force Premium. The phone also holds the GTDAL draft I printed (a synthetic draft from the 6c walk) and one dismissed reminder (the CANCEL slip's). The automated side: `DueImportCard`, `DueCardModel`, `openDue` and the Due screens are tested in isolation, `Part2DueRoomTest` runs them over the real repository, and `Part2DueSourceTest` pins the three links that cannot be driven without the real subscription and Koin. Closed by the iPhone walk and, if the owner wants them, the light-theme and accessibility looks.

36. **Phase 7a: the Due screens are not run over the real Room repository.** CLOSED the same day (owner: add the test, DECISIONS Round 26). `Part2DueRoomTest` runs `DueViewModel`, a dismissal and `DueCardModel` over the production schema and `RoomPart2Repository` (encrypted payloads), through publishing an order, dismissing, and closing and reopening the database; 3 deliberate breaks (dismiss writes nothing, orders ignored, "Not now" forgotten) killed by it alone. It needed `androidx.room:room-runtime`, the bundled SQLite driver and `androidx.test:core` on composeApp's `androidUnitTest` classpath only (the app already ships Room through `shared`; no release classpath, manifest or R8 change). Written after the code, so it has no red run.

37. **Phase 7b: the device walk.** CLOSED for Android on the Pixel 9 (2026-10-26; the owner attached, unlocked and restarted the phone; a debug build of a copy of the tree with the flag flipped there only, installed over the owner's debug copy; driven over adb with uiautomator, the Dashboard's real data never recorded; the two PDFs I saved were deleted). It found three real faults, each fixed with a test that reproduces it (see the Phase 7b notes): the debug switch's reminders were silenced by the due-check; a tap never opened Due because the link was cleared before the database lookup finished; and a restart dropped every queued reminder. My first locked-tap attempt landed on the Dashboard and I could not reproduce it: it most likely tapped the notification group's summary row; two later attempts with logging both opened Due after unlock, so it is recorded as inconclusive, not as a pass. The phone now holds the 7b debug build; the app passcode lock is off again and the override is back at Force Premium. OPEN: the iPhone (items 14 and 20).
38. **Phase 7b: accepted limits (owner, DECISIONS Round 28).** (a) Delivery is inexact: WorkManager can be delayed by Doze or battery saver, so "09:00" is "around 09:00". (b) A Premium lapse is noticed when the app next opens; up to 20 queued reminders can fire before that, and the boot rebuild also schedules as for an entitled officer (it has no billing). The text is generic, so nothing leaks. (c) A change of time zone is not followed until the app next opens. Closing any of them is a decision: (b) needs a stored last-known entitlement, (c) a new receiver.
39. **Phase 7b: no way back after a decline.** OPEN by the owner's choice (Round 28: strictly "Due list only, never asked again"). Nothing in the app says that notifications can be turned on in the system settings. A one-line note on the Due screen, with or without a button, is the closing option if the owner asks.
40. **Phase 7b: the Play listing.** OPEN, for Phase 10. The release declares `POST_NOTIFICATIONS` and `RECEIVE_BOOT_COMPLETED` (and WorkManager's two guarded components) even while Part II is off; the Play Console declarations and the privacy-policy wording need updating before the next Play upload. I cannot change the Play Console.
41. **Phase 7b: the `IosReminderBridge.Companion` allow-list entry.** CLOSED: approved by the owner (Round 28). Same pattern and reason as `IosPageScanner.Companion`: Swift registers the notification gateway at start-up.
42. **Phase 7b: the worker proof and a cold worker on the real release.** OPEN, for Phase 10. The R8 gate checks the worker only where `PART2_ENABLED` is true, so it is proved on a flag-on release built in a copy, not on the flag-off build that ships today. Phase 10 runs `check_r8_guide.py` and `check_merged_manifest.py` on the real flag-on release and walks the release APK: a reminder delivered after a kill and a restart (WorkManager starts on demand there, and the receiver and worker run in a cold process).

43. **Phase 8: the ACCLVE and ENCASH forms do not fill their figures from the Leave Bank yet.** CLOSED in Phase 8b (2026-10-27; DECISIONS Round 33; the owner chose a button that fills only empty boxes, with "Refill" asking first). Evidence: `LeaveBankFill` registered in `AcclveRules` and `EncashRules` (`AcclveLeaveBankFillTest`, `EncashLeaveBankFillTest`, `Part2RulesFillTest`, `Part2LeaveBankIntegrationTest`), the source and its wiring (`LeaveBankFeedTest`, `Part2LeaveBankWiringTest`, `Part2WiringTest`), the model (`LeaveDraftFillTest`, `LeaveDraftFillLateTest` with a source that really suspends and one that answers late, `LeaveBankFillerTest`), the screens (`Part2LeaveBankFillUiTest`: button, cap note, override, Refill question, cross-check line in three states, multi-line break-up with its missing years, no fill without the Leave Bank, a lapsed officer) and the Pixel 9 walk in the Phase 8b notes; 106 deliberate breaks. The security note carried from Phase 8 (`alUsed` counting days between stored dates without checking they exist) is fixed and pinned. Original text, kept for the record: the Koin source, the "fill from the Leave Bank" action, the cap note, the cross-check line, the copy and the walk were all missing, and an officer typed the figures.
44. **Phase 8: the owner's own read of the batch 3 review pack.** CLOSED 2026-10-26: the owner read `part2/part2_review.md` (48 templates, the batch 3 judgment calls listed in DECISIONS Round 30) and reported no correction (DECISIONS Round 31). Before it, the 22 serials had been checked against the printed pages by Claude while building (pp.116-141 read as images; the manual's own worked examples on pp.133 and 140 are goldens). Should the owner later find a wording to change, it is a template edit with its golden test.
45. **Phase 8: what the new leaves do not remind.** CLOSED in Phase 8 (owner: build it as proposed, DECISIONS Round 31). The return reminder ("draft your rejoining slip", the day after the later of the last leave day and the last suffix day) is now set by every Chapter 5 leave but terminal leave, which ends in retirement: furlough and its extension, maternity, special maternity, child care, child adoption, paternity (both), EOL, study leave and its two extensions. The year-end reminder stays for annual leave (ACCLVE is annual leave). Evidence: `ReminderPlannerReturnTest.theRestOfChapterFiveSetsAReturnReminderTooExceptTerminalLeaveAfterWhichTheOfficerRetires` (12 codes set one, with a suffix, and TERLEV, OSL, AWL, ACCLVE, ENCASH and LVELTC set none), `Part2ReturnReminderRealBundleTest` (all 17 return grants declare `leave_from` and `leave_to` as date slots in the real bundle); red first (1 of 20), 3 deliberate breaks of `RETURN_GRANTS` all killed.
46. **Phase 8: manual notes not drafted as checks.** OPEN, accepted. Study leave of 6 to 24 months, twice in a career (p.125 note 3); special maternity leave 30 or 60 days by reason, not more than twice, 24 months apart (p.119 note 11); maternity not in spells (note 9); child care leave 360 or 180 days in a career (p.120 note 12.1); the undertaking for a non-regular officer's furlough (p.118 note 6); ration for study leave in India (p.98 note 1.9). Each needs service history the app does not hold or a question nobody has asked; the note stays on the manual's own page. Closed by the owner naming one and a test.
47. **Phase 8: the iPhone.** OPEN (items 14 and 20 gain the new form fields (and, from Phase 9, the HAA, SCCIA, field-area and Transport questions and their boxes; and, from 8b, the fill button, the Refill question, the cap note, the cross-check line and the break-up box): the retirement date, the two NCC questions, the earlier-allowances question, the ACCLVE figures and the break-up box, and the return reminder after a furlough). Phase 9 adds the four area questions, their boxes (corps, notification, serial, station, operation, sanction date, Transport station and disability) and the area rows with certificates to the iPhone check. The Pixel 9 walk of Phase 8 is DONE and recorded in the Phase 8 notes; it did not reach the reissue warning, the annual-leave NCC and retirement answers, the LTC codes and the light theme (tests cover them).
48. **Phase 8b: what the Pixel 9 walk did not reach.** OPEN (a is CLOSED). (a) CLOSED the same day: the "Matches" line was first not seen on the owner's real statement (for 2024 it stayed "differs" after the owner corrected one box). Checked without showing a figure: the LTC days were zero, the drafted total was exactly previous total plus accumulated days, and the statement's total was larger by an amount equal to none of the three. Then, with the accumulated days set to the statement row's own days (read inside a script from the break-up the same statement had just filled, never printed, the temporary file deleted), the line read "Matches": the earlier difference was the number typed, and the check did its job (both figures, nothing changed). (b) The state with no Leave Bank data (typed by hand, then the fallback from the year before's own ACCLVE draft) was not walked: the phone holds a statement, and removing it would have meant deleting the owner's payslips. Covered by `AcclveLeaveBankFillTest` and `LeaveDraftFillTest`. Closed by a walk on a phone with no statement. (c) The owner did not confirm the ENCASH lines against the statement (they asked me to judge); the structure was checked (one `YYYY D` line a year, ascending, the named gaps equal the real gaps, the note's total equals the lines) and one year has more than 30 days, which the slip will flag with the existing `encash_year_over_30` check. (d) Not walked: light theme, TalkBack, a small screen or large font, the iPhone (items 14, 20, 47).
49. **Phase 9: the area re-grants after hospitalisation, courses and any other absence that is not a leave order.** OPEN (owner, Round 35: leave open for Phase 11 until the owner names where such an absence is drafted). The manual re-grants HAA (p.49 note ii), SCCIA (p.53 note 2) and the field-area allowances (p.55 note 2) "on rejoining from leave, hospitalization, courses etc."; the field-area exception also names hospitalisation of fewer than 15 days. This release drafts the re-grants only from a leave order (15 leave templates carry them). The 15 area templates have no draft entry of their own (the code pages and the case lists offer none, so none was invented), so an officer back from hospital or a course cannot draft them, and the "or hospitalization" half of the field-area exception is not applied. Closed by the owner naming where such an absence is drafted (a Medical or Courses ribbon, or a draft entry for the area codes), with its tests.
50. **Phase 9: two thresholds where the printed ODP page and the handbooks disagree.** CLOSED by the owner (2026-10-27): ODP 2026 is the official document and governs wherever sources differ, so no change. The slip follows the printed page (DECISIONS Round 30): HAA re-granted after a casual leave of 14 days or more (p.49 note ii: "less than 14 days"), the field-area allowances after 15 days or more (p.55 note 2). The Handbook on Pay & Allowances 2023 (p.141) lets an absence of up to 14 days continue HAA, so 14 days would need no re-grant, and the MoD letter of 24 Jan 2020 (handbook p.136, PCDA(O) FAQ) lets casual leave of up to 20 days continue the field-area allowances. The handbook figures are recorded only so the difference is known. Each number lives once, in `AreaRules` (`HAA_CASUAL_MIN_DAYS`, `FIELD_CASUAL_MIN_DAYS`), pinned in `AreaRulesTest` and the golden tests.
51. **Phase 9: the Transport Allowance dates are a gap the ODP leaves, filled from the handbook.** OPEN; the owner accepted the dating (Round 35) and a real order is the only remaining check (the owner's rule: the ODP governs, so this is the only place the app goes beyond it). The ODP says only "from the date admissible" (p.70) and "absence covering a full calendar month" (p.71 notes iii and v). The handbook (pp.124-125) and the PCDA(O) FAQ say the allowance is lost only for a calendar month wholly covered by leave and a part month is paid in full, so STPTRAN is dated the first day of the first month covered in full and the grant the first day of the month after the last (`AreaRules.lostMonths`, the derive rules `tran_stop_on` and `tran_grant_on`). No HRMS order was available to check it. Closed by the owner's reply, or a real order's pattern, recorded in DECISIONS (a different rule is one function and its pins).
52. **Phase 9: the certificates of the area re-grants.** OPEN (the owner has not read the batch 4 pack and asked the agent to read it, Round 35: the agent compared the columns of GTDHH11, GTDCIAFD, GTDHAFA, GTDTRAN1-5 and STPTRAN with the printed tables on pp.48-55 and 70-71 and found no difference; the certificates cannot be checked because Annexure IV is not in the PDF, so the owner's read of those stays). Annexure IV Sr 1 and 1A, which HAA notes iv and v name, are not in the PDF, so the one printed HAA certificate is used, with only the category line of the officer's code; item v (daily allowance on temporary duty), the OR alternative of the SCCIA certificate for local units, the Transport Allowance course line (iii) and the blanks the officer or clerk fills (height, Div/Bde, posting date, staff-car date) are left to the clerk. All are listed in the review pack's judgment calls. Closed by the owner's read, or a template correction with its golden test.
53. **Phase 9: two details of the prefill.** OPEN, accepted. (a) The Transport Allowance station is not prefilled from an earlier draft: its box appears only once the dates cover a whole month, and the prefill runs when the code is chosen. (b) A date is never brought back (the sanction period of a field-area grant would carry last time's end date into this one); `AreaPrefill` now skips dates, which `LeaveDraftPrefillTest` and `Part2AreaFormRealBundleTest` pin. Closed by the owner asking for (a), with a test.
54. **Phase 9: two stale sentences found, not touched.** OPEN. The review pack's judgment call "Casual leave beside another leave is checked against the annual set only ... the rest of Chapter 5 joins the check in phase 8" has been untrue since Phase 8 (`cl_combination` covers all 13 other leave grants), and the nav case "Back from leave, early or recalled" lists the area serials 3.13 to 3.25 and 3.53 as "also relevant" but not 3.54 (STPTRAN), which is homed in Moves. Closed by correcting the sentence in `part2_review.py` and, if the owner wants it, adding 3.54 to that case's `also` list (a nav test pins its length).

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