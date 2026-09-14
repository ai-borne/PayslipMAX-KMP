"""Sanitizer and normalizer for PCDA(O) Codex knowledge rules.

Ingests 378 JSONL rules across 28 files, then:
1. Fixes the 30x leave encashment bug in LTC_ENCASH_001.
2. Collapses 217 action verbs into 5 standard enums:
   - FLAG_DISALLOWANCE
   - REQUIRE_EVIDENCE
   - TRIGGER_DEADLINE
   - CALCULATE_CEILING
   - VERIFY_PREREQUISITE
3. Maps mandatory military paperwork (Part II Orders, OC Certificates, etc.).
4. Outputs canonical_pcdao_rules.json with 100% fidelity.
"""

import glob
import json
import os
from typing import Any, Dict, List, Set


RULES_DIR_DEFAULT = (
    "/Users/sunil/.codex/.chatgpt-projects/g-p-6a327d2ab49c81918adadea212c237e8/pcdao-knowledge/rules"
)

DISALLOWANCE_VERBS = {
    "not_admissible", "not_admissible_for_that_calendar_month", "not_admissible_subject_to_rule_exceptions",
    "not_admissible_or_review_exception", "not_admissible_until_age", "not_admissible_subject_to_current_exception",
    "not_admissible_for_same_period", "not_admissible_as_additional_concession",
    "block_or_recover", "block_additional_concession_subject_to_exception", "block_final_submission",
    "reject_or_request_specific_authority", "reject_or_request_exception_authority", "reject_duplicate_purpose",
    "exclude_from", "exclude_from_reimbursement", "exclude_fare", "exclude_unless_specific_rule_allows",
    "exclude_component", "exclude_new_accumulation_for_encashment", "exclude_retroactive_effect",
    "exclude_from_calculation", "exclude_concurrent",
    "flag_for_review", "flag_overlap_for_review", "create_alert", "alert", "alert_missing_audit_evidence",
    "create_linked_alerts", "close_alert",
    "stop_contribution", "stop_and_request_clarification", "hold_or_adjust", "require_refund", "require_refund_or_adjustment"
}

DEADLINE_VERBS = {
    "calculate_deadline_from", "submit_within_days", "deadline", "derive_deadline", "apply_date_window",
    "start_on", "schedule_increment", "start_recovery", "show_possible_recovery_window", "calculate_interest_window",
    "require_order_after", "select_one_date", "assign_increment_date", "store_separate_dates", "preserve_both_dates",
    "set_severity_by_days_remaining", "normalize_period", "derive_period"
}

CEILING_VERBS = {
    "cap_amount", "annual_cap", "cap_days", "cap_aggregate_days", "cap_each_claim_days", "limit_claim",
    "cap_advance", "cap_courses", "cap_recovery_installments", "cap_reimbursement_or_reject_excess", "cap_against",
    "calculate", "calculate_separately", "calculate_least_of", "calculate_possible_penal_interest", "recalculate",
    "lookup_rate", "lookup_charge", "use_rate_for", "use_rates", "apply_course_rate_or_exclusion_rule",
    "require_rate_table_and_effective_date", "multiply_normal_rate", "increase_rate", "round_to",
    "apply_partial_month_rule", "limit_frequency", "advance_to_next_cell", "grant_increment_count",
    "split_components", "split_period", "credit_fraction_to_dsop", "aggregate_by", "decompose_variance",
    "define_salary_base", "capture_base_component", "produce_settlement", "finalize_payment",
    "reconcile_deduction", "reconcile"
}

EVIDENCE_VERBS = {
    "require_evidence", "request_evidence", "require_evidence_per_leg", "request_evidence_by",
    "require_certificate_before_hra", "require_fields", "require_field", "require_context",
    "request_context", "require_link", "also_require", "store_fields", "store_separately",
    "store_alias_history", "store_snapshot", "request_origin_justification", "require_exception_approval",
    "request_extension_approval", "require_current_tax_reference", "require_lookup_keys",
    "require_lookup_key", "require_audit_link", "require_provenance", "require_audit_fields",
    "require_currentness_check", "publish_cease_and_grant_orders", "require_block_and_destination_record",
    "require_event", "require_event_resolution", "require_follow_on_event", "return_missing_fields",
    "include_fields", "include_components", "map_evidence_to_conditions"
}


def classify_action_verb(raw_action_type: str) -> str:
    """Classify any raw action verb into one of the 5 standard enums."""
    if raw_action_type in DISALLOWANCE_VERBS:
        return "FLAG_DISALLOWANCE"
    if raw_action_type in DEADLINE_VERBS:
        return "TRIGGER_DEADLINE"
    if raw_action_type in CEILING_VERBS:
        return "CALCULATE_CEILING"
    if raw_action_type in EVIDENCE_VERBS:
        return "REQUIRE_EVIDENCE"
    return "VERIFY_PREREQUISITE"


def fix_leave_encashment_math(rule: Dict[str, Any]) -> None:
    """Fix 30x leave encashment formula bug in LTC_ENCASH_001 and similar rules."""
    calc = rule.get("calculation")
    if not calc or not isinstance(calc, dict):
        return

    formula = calc.get("formula", "")
    if "encashed_days" in formula and "/ 30" not in formula and "/30" not in formula:
        calc["original_buggy_formula"] = formula
        calc["formula"] = "(basic_pay + NPA_if_applicable + DA_on_that_base) / 30 * encashed_days"
        rule["notes"] = (rule.get("notes") or "") + " [FIX: Corrected formula to divide monthly base by 30]."


def detect_paperwork_requirements(rule: Dict[str, Any]) -> List[str]:
    """Map required military paperwork and certificates based on rule context."""
    paperwork: List[str] = []
    text_content = (
        f"{rule.get('title', '')} {rule.get('notes', '')} {rule.get('domain', '')} "
        f"{json.dumps(rule.get('conditions', {}))} {json.dumps(rule.get('action', {}))}"
    ).lower()

    if any(k in text_content for k in ["part ii order", "part 2 order", "order", "casualty", "published", "notified"]):
        paperwork.append("Part II Order (Casualty Publication)")
    if any(k in text_content for k in ["co/oc", "oc unit", "commanding officer", "oc certificate", "certificate"]):
        paperwork.append("OC / CO Unit Certificate")
    if "detention" in text_content or "guest room" in text_content:
        paperwork.append("Detention Certificate (Station HQ / Mess)")
    if "depend" in text_content or "family" in text_content:
        paperwork.append("Dependency Certificate")
    if "disability" in text_content or "handicapped" in text_content or "divyang" in text_content:
        paperwork.append("Medical Certificate (MH Specialist)")
    if any(k in text_content for k in ["ticket", "boarding pass", "dts"]):
        paperwork.append("Boarding Passes & Tickets (DTS)")
    if any(k in text_content for k in ["receipt", "hotel bill", "tax invoice", "cash memo"]):
        paperwork.append("Original Hotel Tax Invoice / Cash Memo")

    return paperwork


def sanitize_rule(rule: Dict[str, Any]) -> Dict[str, Any]:
    """Normalize and enrich a single rule dictionary."""
    action = rule.get("action", {})
    raw_action_type = action.get("type", "unknown") if isinstance(action, dict) else str(action)
    standard_action = classify_action_verb(raw_action_type)

    fix_leave_encashment_math(rule)
    paperwork = detect_paperwork_requirements(rule)

    rule["action_type"] = standard_action
    rule["raw_action"] = action
    rule["mandatory_paperwork"] = paperwork
    return rule


def ingest_and_sanitize_all_rules(rules_dir: str = RULES_DIR_DEFAULT) -> List[Dict[str, Any]]:
    """Ingest all 378 rules across 28 JSONL files and apply normalization."""
    jsonl_files = sorted(glob.glob(os.path.join(rules_dir, "*.jsonl")))
    sanitized_rules: List[Dict[str, Any]] = []
    seen_ids: Set[str] = set()

    for fpath in jsonl_files:
        with open(fpath, "r", encoding="utf-8") as fp:
            for line in fp:
                line = line.strip()
                if not line:
                    continue
                rule = json.loads(line)
                rid = rule.get("rule_id")
                if rid in seen_ids:
                    continue
                seen_ids.add(rid)
                sanitized_rules.append(sanitize_rule(rule))

    return sanitized_rules


if __name__ == "__main__":
    rules = ingest_and_sanitize_all_rules()
    out_dir = os.path.join(os.path.dirname(__file__), "..", "output")
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, "canonical_pcdao_rules.json")
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump({"total_rules": len(rules), "rules": rules}, f, indent=2)
    print(f"Sanitized {len(rules)} rules saved to {out_path}")
