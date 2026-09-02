# Tax Planner — "Gold Standard" Execution Plan

**Feature:** Insights → Tax Planner (PRO)
**Status:** Phases 0-7 done (12 Aug 2026); Phase 8 next
**Author:** Architecture review, 11 Aug 2026
**Branch:** to be cut from `feature/security` as `feature/tax-planner-gold`
**Evidence base:** Apr 2026 payslip (live parse) + 126 committed corpus fixtures carrying PCDA's own tax page
**Governing rules:** `CLAUDE.md` — phase independence, TDD, MVVM/SOLID/DRY/SSOT, 300-LOC limit, no hardcoded
strings/colors, security, Phase Handoff Protocol.

---

## 0. Executive summary

The Tax Planner currently overstates this user's annual tax by **₹1,56,269 (+24.9%)** and his monthly TDS by
**+40%**, fires a false "high TDS spike" alarm, and presents a regime-switch nudge that — if acted on via the
PCDA(O) website utility, which is genuinely available — would **cost him money**.

The errors are not estimation noise. They are four independent, individually provable defects:

| Source | Rupee impact on this payslip |
|---|---|
| New-regime slabs frozen at FY 2023-24 vintage | **₹1,14,400** |
| One-off arrears + reimbursements annualised ×12 | **₹41,868** |
| §10(14) exemption applied uncapped | ₹63,367 (mis-sizes regime gap ~8×) |
| PCDA's own `Total Tax Payable` destroyed by a bad sanity guard | corrupts **17 committed fixtures** |

Correcting the first two alone lands the projection at **₹6,29,025 vs PCDA's ₹6,27,975 — 0.17%**. Gold standard
is reachable; the engine just has to be right.

**Two discoveries during evidence-gathering changed the shape of this plan:**

1. **The committed corpus already contains PCDA's official tax computation for 126 payslips spanning FY 2015-16
   → FY 2026-27.** This is a ready-made, CI-runnable golden gate — the strongest correctness harness this
   feature could have, and it costs nothing to build. It is the backbone of every phase below.
2. **PCDA is not always current-law-correct, and the rule pack must be FY-versioned back to 2015-16.**
   `TaxRuleKnowledgeBase` holds 3 financial years, all with identical stale slabs, and silently falls back to
   FY 2026-27 rules for any unknown year. Since the corpus spans 11 FYs, *most* of the user's history is
   currently scored against the wrong year's law. See §3 ADR-2.

---

## 1. Evidence base

All figures below are reproducible; none are estimated.

### 1.1 Ground truth — Apr 2026 payslip, page 4

```
Assessment Year 2027-2028. Period 01/04/2026 to 31/03/2027   (New Tax Regime)
 6. Total Taxable Income      3487744      10. Total Tax Payable    603822
 8. Standard Deduction          75000      11. Income Tax Deducted   99567
 9. Net Taxable Income (6-7-8) 3412740     12. Ed. Cess Deducted      3983
```

`603822` is reproduced **exactly** by the Finance Act 2025 slabs (4/8/12/16/20/24L @ 5/10/15/20/25/30%) on
`3412740`. PCDA's own arithmetic is the independent confirmation of which slab table is correct.

### 1.2 What the app produces (live parse, `WealthOptimizationEngine.analyzeLedger`)

| Field | PCDA | App | Δ |
|---|---|---|---|
| Taxable income | 34,87,744 | 36,21,936 | +1,34,192 |
| Net taxable | 34,12,740 | 35,46,936 | +1,34,196 |
| **Annual tax (incl. cess)** | **6,27,975** | **7,84,244** | **+1,56,269 (+24.9%)** |
| Monthly TDS runway | ~47,675 | 66,711 | +40% |
| Spike warning | none warranted | "+32%" fired | false positive |
| §10(14) exemption | ≤ 50,400 (Rule 2BB) | 2,53,500 | +2,03,100 |

Error decomposition — the two causes sum to the total exactly:
`110,000 base (slabs) × 1.04 = 114,400` + `134,192 over-projected income × 0.312 = 41,868` = **156,268**.

### 1.3 Corpus validation (126 fixtures with a tax page)

- **14 fixtures reproduce PCDA exactly** under the corrected slabs (FY 2024-25 Dec-onward + all FY 2025-26).
- **67 pre-FY2023-24 fixtures reproduce exactly** under old-regime slabs.
- **A `+12,500` constant delta** on FY ≤ 2016-17 fixtures proves the 2.5–5L band was **10%** before FY 2017-18,
  not 5%. The rule pack must version this.
- **A `+10,000` constant delta** across Apr–Nov 2024 fixtures, vanishing from Dec 2024, is exactly the
  FY2023-24 → FY2024-25 first-15L difference. PCDA applied the Finance (No.2) Act 2024 slabs from the
  **December 2024** payroll, not from April. This is real, expected divergence and must be modelled, not
  "fixed".
- **17 fixtures carry a corrupted `totalTaxPayable`** equal to `round(grossSalaryYtd × 0.30)` — 13 March
  fixtures and 4 April fixtures. See D3.

Reproduce §1.3 with the analysis script committed in Phase 0.

---

## 2. Verified defect register

Severity: **S1** = produces materially wrong money on screen · **S2** = wrong advice or false alarm ·
**S3** = correctness latent / architectural · **S4** = copy, precision, hygiene.

| ID | Sev | Defect | Location |
|---|---|---|---|
| D1 | S1 | New-regime slabs are FY 2023-24 vintage (3/7/10/12/15L). ₹1,10,000 flat base overcharge above ₹24L taxable. | `DualRegimeEngine.kt:123-141` |
| D2 | S1 | Slab tables in `TaxRuleKnowledgeBase` are **never read** — computation hardcodes `if` chains. A *third* divergent copy exists in `deriveMarginalRate`. SSOT violation ×3. | `TaxRuleKnowledgeBase.kt:53-69`, `DualRegimeEngine.kt:109-141`, `WealthOptimizationEngine.kt:157-177` |
| D3 | S1 | Sanity guard compares an **annual** liability against a **YTD** gross; when it misfires it replaces PCDA's real figure with `grossSalaryYtd × 0.30`. Corrupts 17 committed fixtures and every Mar/Apr parse. | `TaxParserUtils.kt:99-104` |
| D4 | S1 | Arrears never detected: matches `"ARREAR"` but PCDA codes are `ARR-*`; and scans only `rawEarnings`, which is empty because `ARR-*` maps to **structured** fields. One-off arrears annualised ×12. | `TaxLedgerAggregator.kt:61-71` |
| D5 | S1 | Reimbursements (`ETKT`) annualised and taxed; PCDA excludes them. | `TaxLedgerAggregator.kt:120-123` |
| D6 | S1 | Only 3 FYs in the rule pack; unknown FY **silently** falls back to FY 2026-27. Corpus spans FY 2015-16 → 2026-27, so most history is scored against the wrong year. | `TaxRuleKnowledgeBase.kt:114-121` |
| D7 | S1 | §87A new-regime rebate still ₹7L/marginal-relief-off-₹7L. Current law: ≤ ₹12L total income, cap ₹60,000. A junior officer at ₹11L is told he owes ~₹65k instead of ₹0. | `DualRegimeEngine.kt:154-167` |
| D8 | S2 | §10(14) exemption applied **uncapped** (₹2,53,500 vs Rule 2BB max ₹50,400/yr). The correct caps exist in `DEFAULT_DEFENCE_SECTION_10` and are dead data. | `DefenceTaxExemptionEngine.kt:72-96`, `TaxRuleKnowledgeBase.kt:40-51` |
| D9 | S2 | "Min Old Regime Deductions Needed ₹4,33,333" vs "Total Old Regime Deductions ₹4,03,500" implies a ₹29,833 gap to a better regime. True gap ≈ ₹2,32,933. Actionable via the PCDA(O) utility → real money loss. | `DualRegimeEngine.kt:169-186` + `TaxExemptionBreakdownCard.kt` |
| D10 | S2 | Opportunities gated only on headroom, never on active regime. §80CCD(1B) advice shown to a NEW-regime user, where the section does not exist. | `WealthOptimizationEngine.kt:93-115` |
| D11 | S2 | YTD tax counts only the current payslip's ITAX line (₹50,425) and **omits cess**, while PCDA's parsed `taxDeductedYtd`+`cessDeductedYtd` (₹1,03,550) sit unused. Under-counts tax paid by ~₹53,000. | `TaxLedgerAggregator.kt:125`, `TdsRunwayEngine.kt` |
| D12 | S2 | Spike warning fires on a projection that is wrong in **direction** — real TDS drifts slightly down. | `TdsRunwayEngine.kt:33-39` |
| D13 | S2 | Two incorrect static tips: §10(14) described as uncapped + "ensure PCDA excludes them"; §80C described as automatic — both false for a NEW-regime user. Note tip 3 **correctly** cites the ₹12L threshold the engine doesn't implement. | `AppStringsPremium.kt:99,102,104` |
| D14 | S3 | No surcharge anywhere; `TaxPlannerResult.surcharge` hardcoded `0.0`. Wrong above ₹50L. | `DualRegimeEngine.kt`, `TaxPlannerResult.kt:36` |
| D15 | S3 | "Peer Benchmark" is the user vs himself, built from four magic numbers incl. a hardcoded `253500`. Returns a value identical to the displayed rate whenever NEW wins — structurally cannot inform. Its caption describes an old-regime scenario attached to a new-regime number. | `ConversationalTaxNarrativeEngine.kt:69`, `TaxNarrativeBenchmarkCard.kt:35` |
| D16 | S3 | Dead parallel implementation (`TaxPlannerResult` + `TaxPlannerResultBuilder` + its test) that disagrees with the live path (counts DSOP only vs DSOP+AGIF for §80C). Reachable only from a test. | `TaxPlannerResultBuilder.kt`, `TaxPlannerResult.kt`, `WealthOptimizationEngine.kt:138-155` |
| D17 | S4 | Rupee-exact figures from 1 month of data; unsatisfiable nudge ("upload remaining 11 months" in April); `₹` missing in spike text; `₹50000` unformatted; footer asserts "Version 2026.1 (Verified 2026-02-01)" over 2023-vintage slabs. | multiple |
| D18 | S4 | Hardcoded interpolated UI string violates Rule 4. | `TaxNarrativeBenchmarkCard.kt:35` |

---

## 3. Architectural decisions

**ADR-1 — Surcharge lands in Phase 1, not deferred.**
It is part of "the tax computation is correct", shares the same file, test harness and rule-pack schema as the
slabs, and deferring means reopening `DualRegimeEngine` twice. Zero effect on this user's payslips; required for
correctness above ₹50L and for anyone with house-property or capital-gains income. Includes marginal relief.

**ADR-2 — The rule pack is FY-versioned from FY 2015-16 and the silent fallback is removed.**
The corpus spans 11 FYs and the app displays historical payslips. `getRulesForFy` currently returns FY 2026-27
rules for FY 2017-18 with no signal. Replace with an explicit `TaxRuleLookup` returning
`Resolved(rules)` / `OutOfRange(nearestKnownFy)`; the UI must degrade visibly ("rules unavailable for FY X")
rather than silently compute a wrong number. **Fail loud.**

**ADR-3 — Two tracks, not one number. Current law is authoritative; PCDA is authoritative for TDS.**
This replaces the earlier "anchor everything on PCDA" idea, which §1.3 disproves — PCDA lagged the Finance
(No.2) Act 2024 by eight months, so blind anchoring would import their error into our headline.

| Track | Source of truth | Answers |
|---|---|---|
| **TDS track** | PCDA page 4, as parsed | "What will actually be deducted from my pay?" |
| **Liability track** | Our engine, current law for that FY | "What will I finally owe when I file?" |

Where they diverge, show both and explain the delta as refund or top-up. This is not a workaround — it is the
correct model of the two-decision structure in §3 ADR-4, and it makes the reconciliation both a **feature** and
the **golden test's** assertion mechanism.

**ADR-4 — Regime switching is feasible; model it as two decisions with different reversibility.**
Confirmed from the payslips themselves: PCDA(O) has advertised a self-service regime-switch utility in every
payslip since Jan 2023, with the wording flipping from "opt for new" to "opt for old" exactly when §115BAC(1A)
made the new regime the default in FY 2023-24.

| | Intimation to PCDA (TDS) | Election in the ITR (final) |
|---|---|---|
| Mechanism | PCDA(O) website utility | Return filed u/s 139(1) |
| Reversible mid-year? | **No** (CBDT Circular 4/2023) | **Yes**, independent of what PCDA was told |
| Trap | — | **Old regime is unavailable in a belated return** |

Consequence: the single "Switch to X Regime" opportunity is too crude and must be split. Also note the Dec 2025
payslip states LTC is included in gross for NEW-regime tax — regime-conditional taxability, so the two regimes
cannot share one gross figure.

**ADR-5 — No file exceeds 300 LOC; splits are planned up front, not retrofitted.** See per-phase file plans.

---

## 4. Phase plan

Every phase: build green → all tests pass → tech-debt resolved → Phase Summary → only then proceed.

Per-phase command gate:
```bash
./gradlew ktlintFormat && ./gradlew ktlintCheck
./gradlew :shared:testDebugUnitTest :composeApp:testDebugUnitTest
python3 scripts/check_tech_debt_limits.py --strict <changed .kt files>
./gradlew check -x iosX64Test -x iosSimulatorArm64Test     # phase exit
```
Phases touching `commonMain`: additionally `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`.

---

### Phase 0 — Golden harness (no production code changes)

**Objective.** Build the gate that every later phase is measured against, and freeze today's wrong behaviour so
regressions are impossible to miss.

**Scope.**
- `shared/src/androidUnitTest/.../tax/PcdaTaxParityTest.kt` — for every corpus fixture with a tax page, compute
  tax on PCDA's own `netTaxableIncome` and compare to PCDA's `totalTaxPayable`.
- Three explicit, documented exclusion lists (data, not `@Ignore`):
  - `CORRUPTED_TOTAL_TAX` — the 17 D3-affected fixtures, to be **deleted** in Phase 4.
  - `PCDA_LAG` — Apr–Nov 2024 (+₹10,000), with the legislative reason recorded per entry.
  - `MARCH_SEMANTICS` — March fixtures pending the §5 investigation.
- `scripts/analyze_tax_corpus.py` — reproduces §1.3 verbatim.

**Tests (TDD).** The test is the deliverable. It must **fail loudly** if a fixture silently leaves an exclusion
list, and must assert list sizes so shrinkage is visible.

**Acceptance.** Test runs in CI, green, with today's engine, and prints a parity scoreboard
(`exact / excluded / mismatched`). Baseline recorded in this document's changelog.

**Debt risk.** None — additive test-only.

---

### Phase 1 — Tax computation engine correctness (D1, D2, D6, D7, D14)

**Objective.** One slab-driven, FY-versioned, SSOT computation core that reproduces PCDA across 11 financial
years.

**Scope.**
- Make `TaxRuleKnowledgeBase` genuinely authoritative: computation iterates `TaxSlab` lists. **Delete all three
  hardcoded slab copies** (`computeOldSlabTax`, `computeNewSlabTax`, `deriveMarginalRate`), which resolves D2.
- Populate real slabs FY 2015-16 → FY 2026-27, including the pre-FY2017-18 10% band and the FY2023-24 →
  FY2024-25 → FY2025-26 new-regime transitions.
- §87A per FY (old ₹5L/₹12,500; new ₹7L/₹25,000 for FY 23-24 & 24-25; **₹12L/₹60,000 from FY 25-26**) with
  correct marginal relief.
- Surcharge + marginal relief (ADR-1): old 10/15/25/37%, new capped at 25%.
- `TaxRuleLookup` replacing the silent fallback (ADR-2).

**File plan (300-LOC rule).** `DualRegimeEngine.kt` is 187 LOC and cannot absorb this. Split:
```
shared/.../tax/TaxRuleModels.kt              data classes only
shared/.../tax/rules/OldRegimeSlabTable.kt   FY2015-16 → FY2026-27
shared/.../tax/rules/NewRegimeSlabTable.kt   FY2023-24 → FY2026-27
shared/.../tax/rules/Section10ExemptionTable.kt
shared/.../tax/TaxRuleKnowledgeBase.kt       lookup + explicit out-of-range
shared/.../tax/SlabTaxCalculator.kt          pure slab math
shared/.../tax/RebateCalculator.kt           §87A + marginal relief
shared/.../tax/SurchargeCalculator.kt        surcharge + marginal relief
shared/.../insights/DualRegimeEngine.kt      orchestration only (shrinks)
```

**Tests (write first).**
- `SlabTaxCalculatorTest` — boundary at every slab edge, each FY.
- `RebateCalculatorTest` — ₹12L cliff, marginal-relief band, both regimes, per FY.
- `SurchargeCalculatorTest` — 50L/1Cr/2Cr/5Cr thresholds + marginal relief + new-regime 25% cap.
- `TaxRuleLookupTest` — known FY resolves; FY 1999-2000 returns `OutOfRange`, never silent rules.
- **`PcdaTaxParityTest` exclusion lists shrink**: `CORRUPTED_TOTAL_TAX` unchanged, `PCDA_LAG` unchanged,
  everything else exact to ±₹1.

**Acceptance.** Apr 2026 → **₹6,03,822 exactly**. Jan 2026 → **₹5,93,097 exactly**. All non-excluded fixtures
exact. No file > 300 LOC.

---

### Phase 2 — §10(14) caps and regime gating (D8, D9, D10)

Promoted ahead of projection work: this is the only defect that produces **actionable wrong advice** a user can
execute on the PCDA website today.

**Scope.**
- Wire the dead `Section10Rule` monthly caps into `DefenceTaxExemptionEngine`; exemption = `min(received, cap ×
  months)` per allowance.
- Map PCDA allowance codes (`RH11`…`RH33`, `SICHA`, field/high-altitude codes) to the right Rule 2BB category —
  the current blanket keyword sweep (`"RHA"|"RISK"|"FIELD"|"HARDSHIP"`) cannot be category-correct.
- Every exemption/deduction/opportunity becomes **regime-conditional**. Under NEW: §80C, §80CCD(1B), §10(14)
  field, §10(13A) HRA all → ₹0, with an explicit "not available under your regime" state (not a silent zero).
- Recompute break-even against **capped** deductions so D9's contradiction disappears; render the two figures in
  one place so they can never diverge again.
- Regime-conditional gross (LTC taxable under NEW — ADR-4).

**File plan.** `insights/Section10CapPolicy.kt` extracted; `DefenceTaxExemptionEngine.kt` (97) stays under limit.

**Tests.** RH12 @ ₹21,125/mo → exemption ≤ ₹50,400, **not** ₹2,53,500. NEW-regime user → zero opportunities
requiring old-regime sections. Break-even ≥ capped deductions ⇒ recommendation flips only when genuinely better.
A test asserting the *reason* a section is unavailable, so it cannot pass on a coincidental zero.

**Acceptance.** Old-regime deductions for Apr 2026 ≈ ₹2,00,400. No §80CCD(1B) card for a NEW-regime user.

---

### Phase 3 — Income projection accuracy (D4, D5, D11)

**Scope.**
- Arrears detection reads **structured** `arrears*` fields (SSOT: derive the code list from
  `PayslipPatternConfig`, never a second hardcoded keyword list) **and** handles the `ARR-` prefix in
  `rawEarnings` for unmapped codes.
- Exclude reimbursements (`ETKT` and same-name credit/debit pairs) from taxable projection.
- `monthsAvailable` derived from the latest payslip's **position in the FY**, not the payslip count.
- YTD tax = PCDA's `taxDeductedYtd + cessDeductedYtd` when available, falling back to the summed ledger; cess
  included everywhere.
- Re-tune `TdsRunwayEngine` thresholds against corrected inputs (D12 should disappear as a consequence, not by
  suppressing the warning).

**File plan.** Extract `insights/IncomeProjectionPolicy.kt`; `TaxLedgerAggregator.kt` (190) stays under limit.

**Tests.** Apr 2026: arrears detected = **₹10,086** (currently 0.0 — a test that would have caught D4);
projected gross ≈ ₹34,91,106; YTD tax = ₹1,03,550; **no spike warning**. Multi-month FY cases. A reimbursement
appearing on both sides nets to zero.

**Acceptance.** End-to-end Apr 2026 total tax **₹6,29,025 vs PCDA ₹6,27,975 — within 0.2%**.

---

### Phase 4 — Parser guard fix + fixture regeneration (D3)

**Scope.**
- Replace the guard: compare `totalTaxPayableRaw` against `totalTaxableIncome`, never `grossSalaryYtd`. Never
  fabricate a value — on genuine implausibility return `null` and mark the field unavailable.
- Investigate **March semantics**: `grossSalaryYtd` is ~1 month's gross in March fixtures, so field 1 changes
  meaning at FY-end. Document the finding; handle explicitly.
- **Regenerate the 17 corrupted fixtures** via `CorpusCaptureTest` + `CorpusScrubber`. Diff-review every
  regenerated file before commit.
- Delete `CORRUPTED_TOTAL_TAX` from Phase 0's exclusion list.

**Security gate.** Fixture regeneration is the one phase that touches PII-adjacent data. Mandatory: scrubber run
verified, manual diff of all 17 files for name/PAN/account leakage, gitleaks clean. **No real PDF or unscrubbed
dump is committed.**

**Tests.** April/March parses retain PCDA's real `totalTaxPayable`. A regression test pinning the guard to
`totalTaxableIncome`. `PcdaTaxParityTest` exclusion list shrinks by 17 — enforced by the asserted list size.

---

### Phase 5 — Two-track model & the insights worth paying for (ADR-3, ADR-4)

**Scope.**
- TDS track vs liability track as first-class domain concepts, with a reconciliation delta.
- **Refund/top-up insight:** "PCDA will deduct ₹X under NEW; filing under OLD recovers ₹Y." The single most
  valuable statement this screen can make, currently impossible.
- **§139(1) trap warning:** old regime is unavailable in a belated return.
- **DSOP-waste insight:** ₹4,80,000/yr DSOP + ₹1,50,000 AGIF earning **₹0** tax benefit under NEW — correct,
  high-value, and the exact opposite of today's §80CCD(1B) advice.
- Arrears transparency ("₹10,086 was Jan–Mar arrears, excluded from projection").
- Mid-year regime-change detection across an FY.
- Split the crude "Switch regime" opportunity into the two ADR-4 decisions with their different reversibility.

**Tests.** Divergence classified correctly as refund vs top-up; belated-return warning shown only where old
regime wins; DSOP-waste insight appears only under NEW with non-zero DSOP.

---

### Phase 6 — UI rebuild (D15, D17, D18)

**Scope.**
- **Delete the Peer Benchmark card** (D15) — it cannot produce information. Replace the hero with liability +
  regime verdict + next-month TDS.
- New cards: PCDA Official Computation (mirrors page 4), reconciliation delta, DSOP-waste.
- Explicit cess and surcharge lines.
- "Next month's net pay ≈ ₹X".
- Precision: round to ₹1,000 and show a range at low coverage; keep the honest "1 of 12" chip.
- Fix nudge copy, `₹` symbols, number formatting via `formatIndianCurrency` (SSOT).

**Rule 4 compliance.** All new copy → `ui/theme/AppStringsTaxPlanner.kt` (new file; `AppStringsPremium.kt` is
173 LOC and would breach 300). Zero literals in composables — fixes D18. Colors from `Theme.kt` tokens only.

**File plan.** One card per file, each < 150 LOC; `TaxPlanningScreen.kt` stays an assembler.

**Tests.** `composeApp` view-state tests per card; a lint-style test asserting no literal `"₹"` or bare string
in the tax screens.

---

### Phase 7 — Debt removal, copy correctness, disclaimers, ADR-2 wiring (D13, D16, D17)

**Scope.**
- **Delete** `TaxPlannerResult.kt`, `TaxPlannerResultBuilder.kt`, `WealthOptimizationEngine.buildTaxPlannerResult`,
  `TaxPlannerViewStateTest.kt` (D16).
- Rewrite tips 1, 2 and 4 (§10(14) capped + regime-conditional; §80C not automatic under NEW; no December
  regime deadline — the utility is live year-round, per the April payslip. Reframe as "intimate early to avoid
  Q4 TDS bunching"). Make tips regime-aware.
- Footer version/verified-date from the rule pack — no hardcoded `"Version 2026.1"` (D17).
- **Advice disclaimer**: verify with PCDA(O)/a CA before switching regime. Appropriate for a paid feature that
  now recommends executable financial actions.
- **Wire ADR-2's fail-loud path to production** (gap found during Phase 4's deep-check, deliberately deferred
  here rather than fixed as a drive-by): `TaxRuleKnowledgeBase.resolve()` returns the proper
  `TaxRuleResolution.Resolved`/`OutOfRange` sealed result, but every production call site
  (`DualRegimeEngine.calculateOldRegimeTax`/`calculateNewRegimeTax`/`marginalRate`, `TaxPlanningScreen`) still
  calls the silently-falls-back-to-nearest-FY `getRulesForFy`. Thread `TaxRuleResolution` through
  `DualRegimeEngine`'s core functions (a return-type/signature change, not additive) up through
  `WealthOptimizationEngine`/`ConversationalTaxNarrativeEngine` to `TaxPlanningScreen`, with an explicit
  "tax rules unavailable for FY X" UI state on `OutOfRange` instead of a silently-wrong number. Low severity
  today (every FY the corpus/UI can currently reach resolves cleanly) but real once the rule pack ages past
  its last-covered FY without a code update.

**Acceptance.** No dead tax code. No production call site of `TaxRuleKnowledgeBase` can silently compute
against the wrong FY's rules — `getRulesForFy` either becomes unused (all callers migrated to `resolve()`) or
is documented as intentionally retained only for a specific, named legacy caller. `docs/AI_INSIGHTS_PIPELINE.md`
updated with the two-track model and a changelog entry.

---

### Phase 8 — Comprehension rebuild: BLUF hierarchy, active-month indicator, regime-label fix (U1, U2, U3)

**Objective.** Phases 0-7 made the numbers correct. This phase makes the screen understandable at a glance to
a reader with no tax background — a BLUF ("Bottom Line Up Front") layout instead of a flat card stack — without
changing any tax computation.

**Evidence — UX comprehension gap register** (same rigor as §2: verified against the live screen and code, not
estimated).

| ID | Sev | Gap | Location |
|---|---|---|---|
| U1 | S2 | Raw `TaxRegime.name` ("NEW"/"OLD") is rendered directly as the small regime badge under the TDS/Liability figures, instead of routing through the SSOT labels already defined and already used correctly one card over (`AppStringsPremium.taxPlanningNewRegimeLabel`/`taxPlanningOldRegimeLabel` = "New Tax Regime"/"Old Tax Regime", used by `TaxLiabilityVerdictHeroCard.kt:31`). Confirmed on-screen as the bare "NEW" tag under "PCDA Will Deduct (TDS)" / "You'll Finally Owe (Liability)". A second, domain-layer instance of the same defect class: `TaxTrackReconciliation.message` (the reconciliation card's primary sentence, not just a badge) string-templated the same enum directly (`"...under $tdsRegime"`), defaulting to its bare `.name`. `shared/` can't reach composeApp's `AppStringsPremium` SSOT, so fixed with a local literal-string mapping matching this same file's own pre-existing convention (`belatedReturnTrapWarning`'s hardcoded "Old Tax Regime" text). | `TaxReconciliationDeltaCard.kt:44,50,91`; `TwoTrackReconciliationEngine.kt:43-51` |
| U2 | S2 | No active-month indicator on the Tax Planner screen. It renders whichever payslip is `selectedPayslip` on the shared `PayslipViewModel` — state that only the Dashboard's month-picker sets (`DashboardComponents.kt:110,127`) — with nothing on this screen showing which month that is. A user who picked April on the Dashboard, then later parses May, keeps seeing April's PCDA-mirror/next-month figures on Tax Planner with no way to tell. | `TaxPlanningScreen.kt`, `PayslipViewModel.kt:65-124` |
| U3 | S3 | ~10 cards (verdict, PCDA mirror, TDS-vs-liability, two regime-comparison cards, DSOP-waste, deductions audit, TDS runway, exemption breakdown) render with equal visual weight; none distinguishes "informational" from "needs a decision." No single plain-language sentence states the year-end liability before the reader has to synthesize it themselves. Jargon (TDS, YTD, §10(14), cess, AGIF, regime) appears with no inline gloss anywhere on the screen. | `TaxPlanningScreen.kt` and all `Tax*Card.kt` |

**Scope.**
- **U1 fix.** Replace `TaxRegime.name` in `TrackFigure`'s call sites with the existing SSOT label constants (a
  `TaxRegime -> String` mapping in one place, e.g. alongside `taxPlanningNewRegimeLabel`/`OldRegimeLabel`), so no
  future card can reintroduce a raw enum name.
- **U2 fix.** New `TaxDataAsOfBanner` composable pinned at the top of `TaxPlanningScreen.kt`: "Data as of: <Month
  Year> · X of 12 months" — sourced from `activePayslip`/`fySummary`, both already computed. No new data
  plumbing; this only surfaces state that already exists but isn't shown.
- **U3 — BLUF hero rebuild.**
  - New `TaxBlufSummaryCard.kt`: one plain-language headline sentence for the annual liability, generated by
    extending `ConversationalTaxNarrativeEngine` with a `buildBluf(...)` method (matching its existing
    sentence-generation pattern — no hand-written strings inside the composable), plus one "no action needed" /
    "action flag" line driven entirely by signals the engine already computes (`needsReview`,
    `dsopWasteInsight != null`, `midYearRegimeChange?.detected`, reconciliation `type != MATCHED`). No new
    business logic — a presentation-layer read of existing state.
  - Reuse the existing `InsightSeverity` / `severityColor()` SSOT (already driving the Dashboard's Smart
    Insights alerts) for the no-action-vs-flag visual distinction, rather than adding a second color scheme.
  - New generic `ExpandableDetailSection.kt` (title + chevron + collapsed-by-default content slot) wrapping the
    PCDA Official Computation card, the month-by-month ledger, and the full Old-vs-New comparison table. Their
    content is unchanged — only default visibility moves behind one tap.
  - Inline glossing: first occurrence of TDS/YTD/regime/cess in the new BLUF copy gets a short plain-language
    parenthetical (content-only, added to `AppStringsTaxPlanner`).
  - Regime labels in all new/touched copy read "New Tax Regime" / "Old Tax Regime" in full — never bare
    "New"/"Old" (per user direction; also closes U1).

**File plan (300-LOC rule).**
```
composeApp/.../ui/screens/TaxDataAsOfBanner.kt        new, small
composeApp/.../ui/screens/TaxBlufSummaryCard.kt       new, < 150 LOC
composeApp/.../ui/screens/ExpandableDetailSection.kt  new, generic, reusable
shared/.../insights/ConversationalTaxNarrativeEngine.kt  + buildBluf(); re-check 300-LOC, extract if needed
composeApp/.../ui/screens/TaxReconciliationDeltaCard.kt  U1 fix only, no structural change
composeApp/.../ui/screens/TaxPlanningScreen.kt           wires banner + BLUF card; wraps 3 existing cards in ExpandableDetailSection
```

**Tests (write first).**
- `TaxReconciliationDeltaCardTest` (extends the Phase 6 card test) — rendered regime text is never the literal
  `"NEW"`/`"OLD"`, always the full label.
- `TaxDataAsOfBannerTest` — renders the active payslip's month/year and FY coverage count.
- `ConversationalTaxNarrativeEngineTest` — new `buildBluf` cases: matched reconciliation → "no action needed"
  sentence; `dsopWasteInsight` present → flag sentence; amount rounding/range consistent with the existing hero
  card's Phase 6 precision rule.
- `TaxPhase8CardsTest` (Robolectric) — `ExpandableDetailSection` starts collapsed, expands on click, wrapped
  content matches the existing Phase 6 card tests unchanged.
- All existing tax-computation tests (`PcdaTaxParityTest`, `TaxScreensNoLiteralCurrencyTest`, etc.) stay green
  untouched — this phase is presentation-only.

**Acceptance.** No screen text is a raw regime enum name. The active month is visible on the Tax Planner screen
itself, without navigating to Settings/Dashboard. Above the fold on a standard phone viewport (no scroll): data-
as-of line, one BLUF sentence, one no-action/flag line. Full detail (PCDA mirror, ledger, comparison table) is
reachable in one tap — present in full, not deleted or reduced. Zero change to any tax computation output: every
existing tax-engine test remains green, confirming this phase touched presentation only.

---

## 5. Cross-cutting compliance

**SSOT registry** — after this work, exactly one owner each:

| Concept | Owner |
|---|---|
| Slabs / rebate / surcharge / cess | `TaxRuleKnowledgeBase` + `rules/*` |
| §10(14) caps | `Section10ExemptionTable` |
| Allowance code → field | `PayslipPatternConfig` |
| Currency formatting | `TaxLedgerAggregator.formatIndianCurrency` |
| Tax Planner copy | `AppStringsTaxPlanner` |
| Colors | `Theme.kt` |
| Action-vs-informational visual weight | `InsightSeverity` + `severityColor()` (Phase 8; no second color scheme) |

**Security (Rule 5).** No new network calls — all rules ship offline. No new PII surface: tax figures derive
from already-encrypted `ParsedPayslip`; nothing new persisted unencrypted; no PII in logs or exception
messages. Phase 4 carries the scrub gate. Pre-commit gitleaks + pre-push scan unchanged. Threat note: the
reconciliation card displays PCDA figures already visible in the user's own PDF — no new exposure.

**Regex safety.** Phase 3/4 touch `commonMain` string handling. No lookarounds/backreferences (pre-commit flags
these); any new hot path needs an `iosTest` timing assertion per the `ParserUtilsIosPerfTest` precedent — the
JVM corpus proves correctness, not Kotlin/Native performance.

---

## 6. Risks

| Risk | Mitigation |
|---|---|
| Fixture regeneration leaks PII | Phase 4 scrub gate + manual diff of all 17 + gitleaks |
| Corrected slabs change historical screens users have seen | Expected and correct; call it out in release notes |
| FY 2026-27 slabs change in a future Finance Act | ADR-2's versioned pack makes this a data edit, not a code change |
| March semantics unresolved | Explicitly scoped in Phase 4; fixtures stay excluded until understood — **not** silently included |
| Scope creep into the wider Insights screen | Out of scope: retirement, DSOP corpus, pension. Tax Planner only |

---

## 7. Open items

1. **March `Total Tax Payable` semantics** — resolve in Phase 4 before including those fixtures.
2. **Rule 2BB category mapping for `RH11`–`RH33`** — needs a source for which 7th-CPC RHA cells map to which
   notified category. If unresolvable, apply the **most conservative** cap and disclose the assumption on screen
   rather than guessing generously.
3. **§80CCD(2)** (employer NPS, available under NEW) — out of scope while this user is on DSOP/OPS, but required
   before the feature ships to post-2004 NPS officers. Track separately.

---
