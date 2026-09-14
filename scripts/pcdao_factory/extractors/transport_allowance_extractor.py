"""Extractor for Transport Allowance (TPTA) rules and rates from Pay Handbook 2023.

Extracts:
- Slab rates for Pay Levels 10-13A and Level 14+
- 19 Higher Rate Cities list (+ Gandhinagar agglomeration)
- Official car option for Level 14+
- Divyang (physically disabled) 2x rates and exclusion criteria
- Absence and disallowance rules (full calendar month leave/tour/suspension)
- Mandatory Part II Order certificate text and OC signing authority
"""

import json
import os
from typing import Any, Dict, List


def get_tpta_rates() -> Dict[str, Any]:
    """Return Transport Allowance base rates by pay level slab."""
    return {
        "slabs": [
            {
                "levels": ["10", "10B", "11", "12A", "13", "13A"],
                "rank_range": "Lieutenant to Brigadier",
                "higher_rate_cities_monthly": 7200,
                "other_places_monthly": 3600,
                "da_applicable": True,
                "notes": "Subject to DA thereon at prevailing rates."
            },
            {
                "levels": ["14", "15", "16", "17", "18"],
                "rank_range": "Major General and above",
                "higher_rate_cities_monthly": 15750,
                "other_places_monthly": 7200,
                "official_car_option_rate": 15750,
                "official_car_option_irrespective_of_city": True,
                "da_applicable": True,
                "notes": "Entitled to official car or TPTA @ Rs. 15,750/- p.m. + DA thereon irrespective of city."
            }
        ]
    }


def get_higher_rate_cities() -> List[Dict[str, str]]:
    """Return official list of 19 Higher Rate Urban Agglomerations + Gandhinagar."""
    cities = [
        {"city": "Hyderabad", "type": "Urban Agglomeration (UA)", "state": "Telangana"},
        {"city": "Patna", "type": "Urban Agglomeration (UA)", "state": "Bihar"},
        {"city": "Delhi", "type": "Urban Agglomeration (UA)", "state": "Delhi"},
        {"city": "Ahmedabad", "type": "Urban Agglomeration (UA)", "state": "Gujarat"},
        {"city": "Surat", "type": "Urban Agglomeration (UA)", "state": "Gujarat"},
        {"city": "Gandhinagar", "type": "Census Agglomeration (Ahmedabad UA)", "state": "Gujarat"},
        {"city": "Bengaluru", "type": "Urban Agglomeration (UA)", "state": "Karnataka"},
        {"city": "Kochi", "type": "Urban Agglomeration (UA)", "state": "Kerala"},
        {"city": "Kozhikode", "type": "Urban Agglomeration (UA)", "state": "Kerala"},
        {"city": "Indore", "type": "Urban Agglomeration (UA)", "state": "Madhya Pradesh"},
        {"city": "Greater Mumbai", "type": "Urban Agglomeration (UA)", "state": "Maharashtra"},
        {"city": "Nagpur", "type": "Urban Agglomeration (UA)", "state": "Maharashtra"},
        {"city": "Pune", "type": "Urban Agglomeration (UA)", "state": "Maharashtra"},
        {"city": "Jaipur", "type": "Urban Agglomeration (UA)", "state": "Rajasthan"},
        {"city": "Chennai", "type": "Urban Agglomeration (UA)", "state": "Tamil Nadu"},
        {"city": "Coimbatore", "type": "Urban Agglomeration (UA)", "state": "Tamil Nadu"},
        {"city": "Ghaziabad", "type": "Urban Agglomeration (UA)", "state": "Uttar Pradesh"},
        {"city": "Kanpur", "type": "Urban Agglomeration (UA)", "state": "Uttar Pradesh"},
        {"city": "Lucknow", "type": "Urban Agglomeration (UA)", "state": "Uttar Pradesh"},
        {"city": "Kolkata", "type": "Urban Agglomeration (UA)", "state": "West Bengal"}
    ]
    return cities


def get_divyang_rules() -> Dict[str, Any]:
    """Return rules for Divyang (physically disabled) officers."""
    return {
        "rate_multiplier": 2.0,
        "eligible_categories": [
            "visually impaired",
            "orthopaedically handicapped",
            "deaf and dumb / hearing impaired",
            "spinal deformity"
        ],
        "ineligible_categories": [
            "partially blind service personnel (CGDA letter No. AT/IV/4548/Tpt.Allcs.SER dated 15/10/2001)"
        ],
        "mandatory_evidence": [
            "Medical Certificate from Head of Orthopaedic Dept / Relevant Specialist of Military Hospital",
            "Sanction of Competent Authority as specified in Appendix I of Travel Regulations Rule 230A",
            "Part II Order publication with clause (iv) certified by CO/OC"
        ],
        "campus_restriction_exempt": True,
        "notes": "Admissible at double normal rates irrespective of whether residing within campus or within 1 km."
    }


def get_disallowance_conditions() -> List[Dict[str, Any]]:
    """Return disallowance conditions for Transport Allowance."""
    return [
        {
            "scenario": "government_transport_provided",
            "condition": "Officer provided with facility of Govt transport for commuting between residence and duty",
            "entitlement": "Nil"
        },
        {
            "scenario": "full_calendar_month_leave",
            "condition": "Absence covering the calendar month wholly by any kind of leave",
            "entitlement": "Nil for that calendar month"
        },
        {
            "scenario": "full_calendar_month_tour",
            "condition": "Absent from HQ/posting for full calendar month due to tour/TD",
            "entitlement": "Nil for that calendar month (if partial month, full TPTA is admissible)"
        },
        {
            "scenario": "temporary_duty_exceeding_180_days",
            "condition": "TD exceeding 180 days",
            "entitlement": "Treated as permanent posting; TPTA admissible as per class of city where posted"
        },
        {
            "scenario": "deputation_abroad",
            "condition": "Period of deputation abroad covering full month",
            "entitlement": "Nil"
        },
        {
            "scenario": "full_calendar_month_suspension",
            "condition": "Suspension covering full calendar month",
            "entitlement": "Nil (if partial month, reduced proportionately)"
        }
    ]


def get_part2_order_certificate_requirements() -> Dict[str, Any]:
    """Return required Part II Order certification text from Pay Handbook page 126."""
    return {
        "publishing_authority": "CO / OC Unit",
        "reference_orders": [
            "MoD letter No. 12630/Tpt/A/Q/Mov-C/208/D(Mov)/98 dated 20 Feb 1998",
            "MoD letter No. 12630/Tpt.A/Mov C/246/D(Mov)/17 dated 15 Sept 2017",
            "TR-230(B)"
        ],
        "mandatory_clauses": [
            "Certified conditions of MoD letter dated 15 Sept 2017 fulfilled",
            "Officer stationed at (city) on Permanent Posting / Course up to 180 days without DA claim",
            "Officer is NOT provided with Govt transport for commuting between residence and duty point",
            "If Divyang: Medical Certificate from MH Specialist and TR Rule 230A sanction enclosed",
            "If Level 14+: Official staff car facility withdrawn w.e.f. date"
        ]
    }


def extract_transport_allowance_data() -> Dict[str, Any]:
    """Compile and return comprehensive Transport Allowance data dictionary."""
    return {
        "allowance_name": "Transport Allowance (TPTA)",
        "effective_date": "2017-07-01",
        "source_document": "Handbook_Pay_and_Allowances_2023.pdf",
        "source_pages": "132-136 (Handbook pp. 122-126)",
        "authority": "GoI MoD letter No. 12630/Tpt.A/Mov C/246/D(Mov)/17 dated 15 Sept 2017",
        "rates": get_tpta_rates(),
        "higher_rate_cities": get_higher_rate_cities(),
        "divyang_rules": get_divyang_rules(),
        "disallowance_conditions": get_disallowance_conditions(),
        "part_ii_order_certification": get_part2_order_certificate_requirements()
    }


if __name__ == "__main__":
    data = extract_transport_allowance_data()
    out_dir = os.path.join(os.path.dirname(__file__), "..", "output")
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, "transport_allowance_rates.json")
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
    print(f"Extracted Transport Allowance rates saved to {out_path}")
