# Claim Guide content update runbook

How a correction, a new rate or a whole new rule gets from a report to a released app. The Python and Gradle commands below
were run as written on 2026-10-10 (section 7 on a scratch copy of the repo); the store lanes in section 6 were not run, because
they upload. The Guide ships only inside a normal app release (owner decision: no over-the-air updates), so
the last step is always an app release, and **nothing is uploaded or submitted without the owner saying so**.

```
report -> intake row (17) -> edit authoring/*.txt or figures.json -> refresh.py -> changes.txt entry
       -> branch, PR, gate -> release with the fastlane lanes (owner approves each upload)
```

All commands run from the repo root. `T=docs/Plan/rule_cards/tools` below is only shorthand.

## 1. Intake: turn a report into a row
Both kinds of report end up as one row in `17_expert_review_intake.md` (columns: Card ID, Verdict, Correction, Authority,
Owner decision, Applied in). That file is committed, so **a row may never hold a name, PAN, service number, account number or a
payslip value**; cut those out before you save.

- **The expert's reply** (format on the hand-out's cover page: `Card ID | OK / Wrong / Outdated / Missing | Correction | Authority`):
  paste each line as a row, exactly as sent.
- **A user's "Suggest a correction" email.** The app never sends anything; the user's mail app opens a draft to
  `founder@ai-borne.in` (`AppStringsSupport.supportEmail`). You can recognise it by its subject, `[Guide] <card id>`
  (for example `[Guide] RB-SS-T181`). The body has only these lines, built from an allow-list (`GuideSuggestion`):
  ```
  Guide correction suggestion
  Card: RB-SS-T181
  Title: Food charges on TD: how much do I get?
  Guide data: 2026-10-04
  Card revision: 7df629d0
  App version: 1.3.0

  Suggestion:
  <the user's own words, at most 1000 characters, not rewritten>
  ```
  Make the row like this: Card ID from `Card:`; Verdict = your reading (Wrong / Outdated / Missing) or `User` until you have
  checked; Correction = the user's words, shortened; Authority = empty until you find the order. **A user's text is a lead, not a
  source:** nothing changes until the authority is found and read. Compare `Card revision:` with the card's `rev` in the
  current bundle to see whether they saw the text you have now:
  ```
  python3 -c "import json;b=json.load(open('composeApp/src/commonMain/composeResources/files/guide/guide_bundle.json'));print([c['rev'] for c in b['cards'] if c['id']=='RB-SS-T181'])"
  ```
  A different `rev` means the card was already edited after they wrote; check the change log first.

Verdict meanings are in the intake file. Rejected or unclear rows stay in the table with the reason; never delete them.

**The owner does by hand:** decide accept / reject / ask for every row, find or confirm the authority, and send the expert PDF
(section 8).

## 2. Edit the source (never a generated file)
Card text lives only in `authoring/*.txt`; rupee figures only in `figures.json`. Never edit `rulebook.json`, `RULEBOOK.md`,
`guide_bundle.json`, `13_review_queue.md` or `tools/ids_lock.json` by hand.

- Find the card: `grep -n "RB-SS-T181\|from=SS-T181" docs/Plan/rule_cards/authoring/*.txt` (a card's id is `RB-<first from id>` unless
  its `===` line has `id=`).
- Edit in our own words (copy guard), keep within the compiler's limits (README and `HANDOFF.md`: title 14 words, answer 25,
  3 bullets of 12 words, visible text 90, details 120), and put the authority in `C:`. If the row settles an open point, delete
  the card's `O:` line; if the order could not be read, keep it and say so.
- A rate on a card: the sentence in the card text **and** the figure in `figures.json` are two places (no templating, EP 22).
  Change both, give the figure its `authority`, `evidence` and, once the owner agrees, `approved` date.
- A card that must go away: add `--- retire RB-x reason` in an authoring file. Without it `compile.py` refuses to drop a shipped id.
- A new card: `=== topic=... from=... [id=RB-x] [chips=...]`; it is homed by its topic (`nav.json`) and needs an entry in
  `facets.json` (`"RB-x": "Q" | "H" | "C" | "L"`): `compile.py` stops with `has no valid facet` otherwise.

## 3. Rebuild everything with one command
```
python3 docs/Plan/rule_cards/tools/refresh.py
```
It runs compile, id lock update, `RULEBOOK.md`, the app bundle, the review queue, all tool tests and the own-words guard
(`copycheck`), and stops at the first failure. It ends with `refresh OK`.

- `copycheck` needs the copyrighted source text in `~/Downloads/rulecards_workdir/` (or `RULECARDS_SOURCES=/path`). Without it
  the run prints `SKIPPED copycheck (sources missing)` and exits 2. **Only the owner's machine can run the guard**; CI uses
  `refresh.py --check --allow-no-sources`, which writes nothing and fails if the lock or the bundle is stale.
- Commit the authoring change **together with** the regenerated `rulebook.json`, `RULEBOOK.md`,
  `guide_bundle.json`, `ids_lock.json` and `13_review_queue.md`.
- Three tool tests are deliberate **pins** on the shape of the dataset and fail on purpose when it changes. Update them in the same
  commit, and read the diff to be sure the new number is the one you meant:
  `test_bundle.py` `test_matches_the_compiled_dataset` (card counts per domain, areas, cases),
  `test_bundle.py` `test_approved_figures_ship_for_the_four_personal_cards` and `test_figures.py`
  `test_exactly_the_four_personal_cards_have_a_figure` (the set of figure keys and cards). The unverified-card count (37) and the
  no-cite count (31) in `test_bundle.py` are pins too.

## 4. Record the change for users
`authoring/changes.txt` is the log users read as "What's new in the Guide (<month>)". Add one dated block when a card's
**meaning** changes (a new dated rule, a corrected rate or condition), not for a typo:
```
== 2026-11-15
- Food charges now follow the 8th pay commission [RB-SS-T181-8cpc, RB-SS-T181]
```
Rules (enforced by `compile.py`): a real calendar date, card ids that exist, no rupee amount in the text (free users read it).
Write it in our own words, short. The newest 12 entries ship; the cards in the newest entry get an **Updated** chip. It is free for
every user. Any change to a card's shipped text changes its `rev`, so a user's private note on that card then says "This card was
updated since your note": that is intended, even for a typo fix that has no log entry.

## 5. Branch, pull request, gate
```
git checkout -b feature/guide-<topic>
python3 -m unittest discover -s docs/Plan/rule_cards/tools -p 'test_*.py'
python3 docs/Plan/rule_cards/tools/bundle.py --check
python3 docs/Plan/rule_cards/tools/refresh.py --check --allow-no-sources
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
./gradlew check -x iosX64Test -x iosSimulatorArm64Test
```
The pre-commit hook runs the same checks on staged files and the pre-push hook runs the full gate (both build variants, the
corpus, the whole iOS suite, a secret scan); never bypass them, never run two Gradle commands at once.

Kotlin tests that read the real bundle pin the dataset too, and fail on purpose when it changes. Rehearsed on 2026-10-10 with a
405-card bundle (one replaced pair, one change entry, one new figure): `GuideBundleContract.assertMatchesCompiledDataset` pins the card
total, the travel count (and with it pay), the unverified (37) and no-cite (31) counts and `ratesAsOf`; `GuideFiguresContract`
pins the set of figure keys and personal cards and each figure's value against the owner-approved letter, and
`SyntheticGuideFigures` (shared-test-fixtures) must equal what ships. Update them in the same commit, taking a new figure's expected
value from the order, never from the code. Everything else in the contract passed unchanged with a replaced card in the bundle (tiles,
feeds, search, chips, paywall rule, Pay Audit links, pins and share notes), after three checks that wrongly assumed no card was
replaced were corrected in M8. If another test fails after a content change, read what it protects before editing it.

A content-only change adds no model, so
R8 needs no keep rule, but run the cheap proof anyway:
```
./gradlew :composeApp:assembleMinifiedTest -PallowPlaceholderGemmaModel=true && python3 scripts/check_r8_guide.py
./gradlew :composeApp:minifyReleaseWithR8 -PallowPlaceholderGemmaModel=true
scripts/run_guide_minified_smoke.sh      # needs the Pixel on adb; installs over the app, never uninstalls
```
Open a PR to `main`; merge when the gate is green.

## 6. Release (existing fastlane lanes; the owner approves every upload)
Version rules: the marketing version is `version.properties` (`MARKETING_VERSION`); the Android `versionCode` in
`composeApp/build.gradle.kts` is raised by hand and **never reused**; the iOS build number is raised by the lane (commit the
`project.pbxproj` change afterwards). Release notes live in `iosApp/fastlane/release_notes/<version>/` and
`composeApp/fastlane/release_notes/<version>/whats_new_en-IN.txt` (Play limit 500 characters).

- **iOS** (Xcode must be signed in to the Apple account): `cd iosApp && set -a && source fastlane/.env && set +a`, then
  `fastlane build_and_upload_testflight changelog:"..."`.
  App Store: `create_app_store_version version:X.Y.Z`, `prepare_submission version:X.Y.Z build:N`, then
  `submit_for_review version:X.Y.Z confirm:true` only on the owner's explicit OK (it publishes on approval);
  `fastlane review_status` and `testflight_builds` to read the state.
- **Android**: `./gradlew :composeApp:bundleRelease -PgemmaModelSourcePath=<the Gemma model FILE>` (the owner keeps the path;
  `docs/Launch/05_launch_strategy_and_resolution.md` section 7; never the placeholder flag), then
  `cd composeApp && fastlane upload_to_track aab:<path> track:internal notes:"..."`, check with `fastlane track_status`, and later
  `fastlane promote_release version_code:N to:alpha|production`. The AAB is over 400 MB; a network blip discards the edit, rerun.
- Before the build goes to users, the owner walks the open iOS list (`16_guide_phase_plan.md`, "iOS walkthrough checklist").

## 7. The 8th CPC playbook
Rehearsed on 2026-10-10 on a scratch copy of the repo (the food card replaced by a dated successor with its own figure):
after the steps below `refresh.py` ended `refresh OK` and `copycheck` said CLEAN.

1. **List what could change.** `python3 docs/Plan/rule_cards/tools/rates_report.py` prints every current card that has a RATES chip,
   a digit, a `%` or `Rs` (about 234 of 404 today, deliberately too many rather than too few) and every figure with its effective date
   and evidence. Work down it against the commission's orders; the review PDF carries the same list in its appendix.
2. **Decide per card.** A rule whose old version still matters (arrears are worked out under the old rule) gets a **new dated
   card that replaces the old**. A card that was simply wrong, or only quotes a number, is **edited in place** (section 2).
3. **New dated card.** Add to an authoring file, same `topic=` as the old card, and `id=` because it starts from the same source:
   ```
   === topic=RR-ORD-03 from=SS-T181 id=RB-SS-T181-8cpc effective=2027-01-01 replaces=RB-SS-T181 chips=RATES personal=level:food_rate_8cpc
   ```
   then `T:`, `A:`, `K:` and `C:` as usual. `replaces=` needs `effective=` (it becomes the old card's `until` date), the new date must be
   later than the old card's, and a card has one successor. The old card stays in the data and reaches users through "Earlier
   rule"; it leaves the tiles, search and feeds, so **`nav.json` needs no edit** (the new card is homed by the same topic).
4. **Facet and figure.** Add `"RB-SS-T181-8cpc": "H"` to `facets.json`. If the card shows "Your figure", add a new key to
   `figures.json` (copy the old one: `card` = the new id, `effective_from`, `authority`, `evidence`, then the owner's `approved` date).
   **Keep the old figure on the old card**: `compile.py` requires a figure for every card with a `personal=` spec.
5. **Change log.** Add the dated block to `changes.txt` naming both ids (section 4).
6. **Bump `RATES_AS_OF`** in `docs/Plan/rule_cards/tools/config.py` (`'YYYY-MM'`, the month the figures are current to). It is the
   "Rates as of" chip, and nine months after it (`GuideStaleness.STALE_AFTER_MONTHS`) cards show the "rates may have changed"
   nudge. A plain DA step (no commission) changes only this value and the DA card.
7. `python3 docs/Plan/rule_cards/tools/refresh.py`, then update the pins (section 3), then sections 5 and 6.

## 8. What the owner does by hand (not scriptable)
- **Send the expert PDF.** It is still unsent. Regenerate it first if any card changed:
  `python3 docs/Plan/rule_cards/tools/review_pack.py --pdf` writes `docs/Plan/rule_cards/review/guide_review_<date>.pdf`
  (git-ignored: a hand-out, not a source; needs Google Chrome; without `--pdf` only the HTML is written).
- Decide every intake row; approve each new or changed figure (`approved` date in `figures.json`); confirm the evidence label.
- Run the own-words guard (`refresh.py` on the machine that has the source text).
- Approve every store upload and submission; run the iOS walkthrough list.
- Reply to the user who wrote in, if you want to (the app collects no address).
