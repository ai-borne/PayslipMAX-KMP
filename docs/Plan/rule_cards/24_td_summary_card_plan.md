# TD entitlements summary card: plan (Phase 0, 2026-11-07)

One card at the top of the Temporary duty area (`td-da` case) that answers "what is my TD entitlement": hotel, taxi, food with its
timings, and CEA / hostel subsidy, for every officer level, at the current DA step. The owner's phone image of a rates sheet
(`td_rates_sheet.json`, S6) prompted it; the sheet is **not** a source (register `23_..._rate_register.md`).

## Owner decisions (2026-11-07)
| # | Decision |
|---|---|
| D1 | Scope: hotel, taxi, food (with timings), CEA and hostel subsidy only. Tele claim, briefcase, JCO/OR rows and every other block of the sheet stay out until an Army letter is read. |
| D2 | The +25% per +50% DA rule is applied by the one existing rule, `PersonalFigureResolver.stepPercent`; the card never types a stepped number. |
| D3 | The whole card is **free**, including the highlighted own-level row. `GUIDE_PAYWALL_ENABLED` is off today, so every user already reads the full Guide. |
| D4 | The free table is labelled with a DA **band** ("DA 50 to 99%"), never a DA number; the step is the same at 60% and 63%, and the Army has not implemented 63% yet. |
| D5 | The shipped food card (`RR-ORD-03`) and CEA card (`RP-085`) are **not edited**; the summary links to them (and to the GST card `RR-ORD-04`). Their static rupee sentences are swept at the next DA step by `rates_report.py`. |
| D6 | The Army letters are not public, so the card cites the MoD letter and, beside it, the DoE OM as the **civil parent order** (register S7). It never cites the DoE OM alone, and never calls it the Army entitlement (its para 8 leaves Armed Forces to MoD). |

## Authority per aspect (what may be cited)
| Aspect | Cite |
|---|---|
| Hotel ceiling, taxi, food base, DA step | MoD letter 12630/Mov C/242/D(Mov)/2017 dt 15-09-2017 (Table E(i), para 2(iv)); civil parent: DoE OM 19030/1/2017-E.IV dt 13-07-2017 |
| Food timing 30 / 70 / 100% | Same letter; PCDA(O) FAQ (Dec 2025) states the three shares and the stepped food rates |
| CEA and hostel subsidy | MoD letter 1(23)/2017/D(Pay/Services) dt 02-08-2018 (as on `RP-085`); PCDA(O) FAQ: CEA Rs 33,750, hostel Rs 1,01,250 from 01-04-2024 |
| Hotel excludes GST | DoE OM 19030/2/2017-E.IV dt 29-06-2018 (as on `RR-ORD-04`), by link only |

## Existing pieces this reuses (read before planning)
`figures.json` (`td_hotel`, `td_taxi`, `food_rate`, `td_food_hours`, `da_step`), `PersonalFigureResolver.stepPercent` (the one DA-step
rule), `preclaim/DaStepRate` (exact paise), `nav.json` case `td-da`, authoring `RR-ORD-03` and `RP-085`, `GuideYourFigure` (one figure per
card, paid half: **not** reused, because a table is not one figure), `23_ta_da_preclaim_rate_register.md`.
Conflict surfaced, not blended: `RP-085` and `RR-ORD-03` print stepped rupees as static text; the summary computes them. Both agree today.

## Phases (each ends with a green build and a Phase Summary; no phase starts before the previous one is verified)
| Phase | Work | Exit gate |
|---|---|---|
| 0 | This plan; baseline of the step maths proved green (existing `PersonalFigureResolverTest` pins steps at each multiple of 50) | shared tests and tool tests green |
| 1 | `figures.json`: add `cea` and `hostel_subsidy` (`da_step`, monthly base 2,250 and 6,750 x 12 per year, authority, evidence, approval), register rows, pin tests (`test_figures.py`, `GuideFiguresContract`, `SyntheticGuideFigures`) | `refresh.py` OK, pins updated deliberately |
| 2 | Shared pure `TdSummaryBuilder` (commonMain): bands x hotel / taxi / food, timing amounts, CEA and hostel, authority lines; tests first for DA 49 / 50 / 99 / 100 / 150, exact paise, missing figures | `:shared:testDebugUnitTest` green |
| 3 | Nav and bundle: a summary entry first in `td-da` (needs a new nav entry kind in `nav.json`, `compile.py`, `bundle.py`, models); bundle regenerated; Kotlin bundle pins | `refresh.py`, `minifyReleaseWithR8`, `check_r8_guide.py` |
| 4 | UI: table composable in a new file, copy in `GuideStrings`, colours from `Theme.kt`, TalkBack reads a row as one sentence, own-level row highlighted, links to the three cards; Pixel walk | `check`, ktlint, iOS framework link, file-size audit |
| 5 | `authoring/changes.txt` entry (no rupee amounts), `HANDOFF.md` and `16_guide_phase_plan.md` update, PR | CI green |

## Risks and controls
- **Release (R8):** new `@Serializable` types in the bundle; verified in Phase 3 with `minifyReleaseWithR8` and `check_r8_guide.py`, not by debug tests alone.
- **iOS:** integer maths only, no regex, no new hot path, so no `iosTest` timing assertion is needed; the iOS link and unit tests still run. No iPhone walk until all features are ready (owner rule).
- **Regression:** additive card; no flag beyond `GUIDE_ENABLED`; retire with `--- retire` if needed. Existing cards are untouched, so no `rev` change and no "updated since your note" for users.
- **Navigation and state:** opens through the existing card route; back, tab re-tap, lock screen and process death are inherited. No new arguments, no storage, no Room change.
- **Stale rates:** at DA 100% the step moves; the builder follows `da_step`, and the bundle's `ratesAsOf` and the staleness nudge already cover a release gap.
- **Not claimed:** the free table states "DA 50 to 99%"; below 50% or above 99% the builder shows the matching step, and shows nothing for a level it has no band for.
