"""Master orchestrator for PCDA(O) Data Factory.

Executes all 8 extractors and the rules sanitizer to generate the 9 verified JSON data packs in scripts/pcdao_factory/output/.
"""

import json
import os
import sys
import time
from typing import Any, Callable, Dict

# Ensure local imports work cleanly
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, SCRIPT_DIR)

from extractors.allowances_extractor import extract_allowances_data
from extractors.pay_matrix_extractor import extract_pay_matrix_data
from extractors.promotion_workflow_extractor import extract_promotion_and_workflow_data
from extractors.risk_hardship_extractor import extract_risk_hardship_data
from extractors.rules_sanitizer import ingest_and_sanitize_all_rules
from extractors.service_conditions_extractor import extract_service_conditions_data
from extractors.special_compensatory_extractor import extract_special_compensatory_data
from extractors.transport_allowance_extractor import extract_transport_allowance_data
from extractors.travel_tada_extractor import extract_travel_tada_data


OUTPUT_DIR = os.path.join(SCRIPT_DIR, "output")


def save_json_pack(filename: str, data: Any) -> str:
    """Save data structure as formatted JSON in output directory."""
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    out_path = os.path.join(OUTPUT_DIR, filename)
    with open(out_path, "w", encoding="utf-8") as fp:
        json.dump(data, fp, indent=2)
    size_kb = os.path.getsize(out_path) / 1024
    return f"{out_path} ({size_kb:.1f} KB)"


def run_pipeline() -> None:
    """Execute the full PCDA(O) Data Factory extraction and sanitization pipeline."""
    print("=" * 70)
    print("  PCDA(O) Military Financial Intelligence Engine: Data Factory")
    print("=" * 70)
    start_time = time.time()

    tasks = [
        ("1. Pay Matrix 7th CPC", "pay_matrix_7th_cpc.json", extract_pay_matrix_data),
        ("2. Transport Allowance", "transport_allowance_rates.json", extract_transport_allowance_data),
        ("3. Risk & Hardship Matrix", "risk_hardship_rates.json", extract_risk_hardship_data),
        ("4. Allowances & Additions", "allowances_and_additions.json", extract_allowances_data),
        ("5. Travel TA/DA Entitlements", "travel_tada_rates.json", extract_travel_tada_data),
        ("6. Canonical Codex Rules (378)", "canonical_pcdao_rules.json", lambda: {
            "total_rules": len(ingest_and_sanitize_all_rules()),
            "rules": ingest_and_sanitize_all_rules()
        }),
        ("7. Special Compensatory & Operational", "special_compensatory_allowances.json", extract_special_compensatory_data),
        ("8. Service Conditions, Funds & Schemes", "service_conditions_and_funds.json", extract_service_conditions_data),
        ("9. Promotion Pay Fixation & Workflows", "promotion_and_pcdao_workflows.json", extract_promotion_and_workflow_data)
    ]

    for label, fname, func in tasks:
        t0 = time.time()
        print(f"\n[*] Running: {label}...")
        try:
            data = func()
            saved_msg = save_json_pack(fname, data)
            elapsed = time.time() - t0
            print(f"    [+] Saved: {saved_msg} in {elapsed:.2f}s")
        except Exception as err:
            print(f"    [-] ERROR in {label}: {err}", file=sys.stderr)
            raise

    total_time = time.time() - start_time
    print("\n" + "=" * 70)
    print(f"  All 9 Data Packs successfully generated in {total_time:.2f}s")
    print("=" * 70)


if __name__ == "__main__":
    run_pipeline()
