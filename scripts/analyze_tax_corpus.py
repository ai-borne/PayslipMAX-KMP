#!/usr/bin/env python3
"""Phase 0 golden-harness analysis (docs/Plan/04_TaxPlannerGoldStandard.md §1.3).

Reproduces the corpus-wide tax parity evidence that motivates the Tax Planner
"Gold Standard" plan: for every committed corpus fixture that carries a PCDA
tax page, compute tax on PCDA's own `netTaxableIncome` using the *correct*
FY-versioned slab tables (the same tables `shared/.../tax/rules/*` implements
in Phase 1) and compare to PCDA's own `totalTaxPayable`.

This is read-only: it never touches production Kotlin code and has no
side effects beyond printing a report. Run from the repo root:

    python3 scripts/analyze_tax_corpus.py

Classification per fixture (mirrors PcdaTaxParityTest's exclusion lists):
  - CORRUPTED_TOTAL_TAX: totalTaxPayable == round(grossSalaryYtd * 0.30),
    the D3 sanity-guard bug (TaxParserUtils.kt) misfiring and destroying
    PCDA's real figure. Fixed by Phase 4; excluded until then.
  - PCDA_LAG: Apr-Nov 2024 fixtures, where PCDA's own payroll system used
    stale FY2023-24 slabs for TDS until switching in the December 2024
    payroll run (the Finance (No.2) Act 2024 slabs apply to the whole of
    FY2024-25 in law, so our engine will legitimately diverge from PCDA's
    under-withheld TDS in this window).
  - MARCH_SEMANTICS: March fixtures not already corrupted, where
    `grossSalaryYtd` (field 1) appears to print only the current month's
    gross rather than a cumulative YTD figure -- excluded pending the
    Phase 4 investigation rather than silently trusted.
  - everything else: expected to reproduce PCDA's totalTaxPayable exactly
    (+/- Rs 1) once the FY-versioned slab tables are correct.
"""
from __future__ import annotations

import glob
import json
import os

CORPUS_DIR = os.path.join(os.path.dirname(__file__), "..", "shared", "src", "androidUnitTest", "resources", "corpus")
TOLERANCE = 1.0


def financial_year(year: int, month_num: int) -> str:
    start = year if month_num >= 4 else year - 1
    end_short = (start + 1) % 100
    return f"{start}-{end_short:02d}"


# --- Old regime: unchanged since FY2017-18, 10% band before that (evidence: +Rs12,500 delta <= FY2016-17). ---
def old_regime_slabs(fy: str):
    start_year = int(fy.split("-")[0])
    mid_rate = 0.10 if start_year <= 2016 else 0.05
    return [
        (0.0, 250_000.0, 0.0),
        (250_000.0, 500_000.0, mid_rate),
        (500_000.0, 1_000_000.0, 0.20),
        (1_000_000.0, None, 0.30),
    ]


def old_regime_rebate(net_taxable: float, tax: float) -> float:
    if net_taxable <= 500_000.0:
        return max(0.0, tax - 12_500.0)
    return tax


# --- New regime: FY2023-24, FY2024-25 (annual law, not PCDA's mid-year TDS switch), FY2025-26+. ---
def new_regime_slabs(fy: str):
    start_year = int(fy.split("-")[0])
    if start_year <= 2023:
        return [
            (0.0, 300_000.0, 0.0),
            (300_000.0, 600_000.0, 0.05),
            (600_000.0, 900_000.0, 0.10),
            (900_000.0, 1_200_000.0, 0.15),
            (1_200_000.0, 1_500_000.0, 0.20),
            (1_500_000.0, None, 0.30),
        ]
    if start_year == 2024:
        return [
            (0.0, 300_000.0, 0.0),
            (300_000.0, 700_000.0, 0.05),
            (700_000.0, 1_000_000.0, 0.10),
            (1_000_000.0, 1_200_000.0, 0.15),
            (1_200_000.0, 1_500_000.0, 0.20),
            (1_500_000.0, None, 0.30),
        ]
    return [
        (0.0, 400_000.0, 0.0),
        (400_000.0, 800_000.0, 0.05),
        (800_000.0, 1_200_000.0, 0.10),
        (1_200_000.0, 1_600_000.0, 0.15),
        (1_600_000.0, 2_000_000.0, 0.20),
        (2_000_000.0, 2_400_000.0, 0.25),
        (2_400_000.0, None, 0.30),
    ]


def new_regime_rebate(net_taxable: float, tax: float, fy: str) -> float:
    start_year = int(fy.split("-")[0])
    max_income = 1_200_000.0 if start_year >= 2025 else 700_000.0
    cap = 60_000.0 if start_year >= 2025 else 25_000.0
    if net_taxable <= max_income:
        return tax - min(tax, cap)
    diff = net_taxable - max_income
    return diff if tax > diff else tax


def slab_tax(net_taxable: float, slabs) -> float:
    tax = 0.0
    for lo, hi, rate in slabs:
        if net_taxable > lo:
            upper = min(net_taxable, hi) if hi is not None else net_taxable
            tax += (upper - lo) * rate
    return tax


def compute_tax(net_taxable: float, regime: str, fy: str) -> float:
    """PCDA's own `Total Tax Payable` field is base tax after rebate/marginal-relief, WITHOUT cess
    (cess is a separate printed field, `Ed. Cess Deducted`) -- confirmed directly against the Apr 2026
    ground truth: 603822 == slab tax on 3412740 under Finance Act 2025 slabs, with no +4% applied."""
    if regime == "NEW":
        raw = slab_tax(net_taxable, new_regime_slabs(fy))
        base = new_regime_rebate(net_taxable, raw, fy)
    else:
        raw = slab_tax(net_taxable, old_regime_slabs(fy))
        base = old_regime_rebate(net_taxable, raw)
    return round(base)


def detect_regime(fixture_id: str) -> str:
    """Mirrors TaxParserUtils.parseTaxAndSavings: regime comes from the raw tax-page text, not the
    (currently uncaptured) `taxAndSavings.taxRegime` ground-truth field -- see Phase 0 findings."""
    input_path = os.path.join(CORPUS_DIR, f"{fixture_id}.input.json")
    if not os.path.exists(input_path):
        return "OLD"
    with open(input_path) as fh:
        d = json.load(fh)
    tax_text = d.get("taxPageText") or d.get("fullText") or ""
    return "NEW" if "new tax regime" in tax_text.lower() else "OLD"


def load_fixtures():
    fixtures = []
    for path in sorted(glob.glob(os.path.join(CORPUS_DIR, "*.expected.json"))):
        with open(path) as fh:
            d = json.load(fh)
        tax = d.get("taxAndSavings")
        if tax and tax.get("netTaxableIncome", 0) > 0:
            tax = dict(tax)
            tax["taxRegime"] = detect_regime(d["id"])
            fixtures.append((d["id"], d["year"], d["monthNum"], tax))
    return fixtures


# apr_14: totalTaxableIncome/standardDeduction/totalTaxPayable/cessDeductedYtd are all 0 despite a
# populated netTaxableIncome -- the tax-page ground truth was never fully captured for this fixture.
# Not a computation defect; there is no real PCDA figure here to reproduce.
GROUND_TRUTH_GAP_IDS = {"apr_14"}


def classify(fixtures):
    corrupted, pcda_lag, march_semantics, ground_truth_gap, normal = [], [], [], [], []
    for fid, year, month, tax in fixtures:
        gross_ytd = tax.get("grossSalaryYtd", 0.0)
        ttp = tax.get("totalTaxPayable", 0.0)
        is_corrupted = gross_ytd > 0 and abs(round(gross_ytd * 0.30) - ttp) < TOLERANCE
        if is_corrupted:
            corrupted.append(fid)
        elif fid in GROUND_TRUTH_GAP_IDS:
            ground_truth_gap.append(fid)
        elif year == 2024 and 4 <= month <= 11:
            pcda_lag.append(fid)
        elif month == 3 and tax["netTaxableIncome"] > 250_000.0:
            # Below the basic exemption threshold, tax is trivially 0 regardless of the March
            # grossSalaryYtd anomaly -- doesn't actually exercise it, so don't exclude on that basis
            # (03_march_2022 is net=44,333 and matches exactly; see PcdaTaxParityTest's staleness check).
            march_semantics.append(fid)
        else:
            normal.append((fid, year, month, tax))
    return corrupted, pcda_lag, march_semantics, ground_truth_gap, normal


def main():
    fixtures = load_fixtures()
    print(f"Fixtures with a tax page: {len(fixtures)}")

    corrupted, pcda_lag, march_semantics, ground_truth_gap, normal = classify(fixtures)
    print(f"CORRUPTED_TOTAL_TAX (D3 guard bug): {len(corrupted)} -> {corrupted}")
    print(f"PCDA_LAG (Apr-Nov 2024): {len(pcda_lag)} -> {pcda_lag}")
    print(f"MARCH_SEMANTICS (pending Phase 4): {len(march_semantics)} -> {march_semantics}")
    print(f"GROUND_TRUTH_GAP (tax page never fully captured): {len(ground_truth_gap)} -> {ground_truth_gap}")
    print(f"Normal (must reproduce exactly): {len(normal)}")

    exact, mismatched = [], []
    for fid, year, month, tax in normal:
        fy = financial_year(year, month)
        regime = tax.get("taxRegime") or "OLD"
        net_taxable = tax["netTaxableIncome"]
        computed = compute_tax(net_taxable, regime, fy)
        pcda = tax["totalTaxPayable"]
        if abs(computed - pcda) <= TOLERANCE:
            exact.append(fid)
        else:
            mismatched.append((fid, fy, regime, net_taxable, computed, pcda, computed - pcda))

    print(f"\nExact (+/- Rs 1): {len(exact)} / {len(normal)}")
    if mismatched:
        print(f"Mismatched: {len(mismatched)}")
        for row in mismatched:
            print(f"  {row[0]} fy={row[1]} regime={row[2]} net={row[3]} computed={row[4]} pcda={row[5]} delta={row[6]}")

    apr_2026 = next((t for t in fixtures if t[0] == "04_apr_2026"), None)
    if apr_2026:
        tax = apr_2026[3]
        computed = compute_tax(tax["netTaxableIncome"], tax.get("taxRegime") or "OLD", "2026-27")
        print(f"\nApr 2026 sanity check: computed={computed} PCDA totalTaxPayable(page)=603822 (fixture's stored value is D3-corrupted: {tax['totalTaxPayable']})")


if __name__ == "__main__":
    main()
