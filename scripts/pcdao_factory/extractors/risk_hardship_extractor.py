"""Extractor for Risk & Hardship Matrix rules and rates from Pay Handbook 2023.

Extracts:
- RH Matrix cells (R1H1 to R3H3, RH-MAX Siachen ₹42,500)
- Field allowances (HAFAA ₹16,900, CFAA ₹10,500, CMFAA ₹6,300)
- High Altitude Allowance (Cat I ₹3,400, Cat II ₹5,300, Cat III ₹25,000)
- 25% escalation formula triggered when DA >= 50%
- One Level Down deployment rules (static units, Div HQ, enclosed garrisons)
- Mutual exclusion and Part II Order certificate requirements
"""

import json
import os
from typing import Any, Dict, List


def get_rh_matrix_base_rates() -> Dict[str, Any]:
    """Return the 9-cell Risk & Hardship Matrix base rates w.e.f. 01.07.2017 / 22.02.2019."""
    return {
        "RH_MAX": {
            "name": "Siachen Allowance",
            "code": "RH_MAX",
            "monthly_rate": 42500,
            "escalated_rate_at_50_da": 53125,
            "description": "Admissible for troops deployed in Siachen glacier area."
        },
        "cells": {
            "R1H1": {"risk": "HIGH", "hardship": "HIGH", "rate": 25000, "code": "HH11", "desc": "High Alt Cat III / Extreme Risk"},
            "R1H2": {"risk": "HIGH", "hardship": "MEDIUM", "rate": 16900, "code": "HAFAA", "desc": "Highly Active Field Area Allowance"},
            "R1H2_77pct": {"risk": "HIGH", "hardship": "MEDIUM_PARTIAL", "rate": 13013, "code": "77% R1H2", "desc": "77% of R1H2"},
            "R1H3": {"risk": "HIGH", "hardship": "LOW", "rate": 5300, "code": "R1H3", "desc": "High Risk / Low Hardship"},
            "R2H1": {"risk": "MEDIUM", "hardship": "HIGH", "rate": 16900, "code": "R2H1", "desc": "Medium Risk / High Hardship"},
            "R2H2": {"risk": "MEDIUM", "hardship": "MEDIUM", "rate": 10500, "code": "CFAA", "desc": "Compensatory Field Area Allowance"},
            "R2H2_60pct": {"risk": "MEDIUM", "hardship": "MODIFIED", "rate": 6300, "code": "CMFAA", "desc": "Compensatory Modified Field Area Allowance (60% R2H2)"},
            "R2H3": {"risk": "MEDIUM", "hardship": "LOW", "rate": 3400, "code": "R2H3", "desc": "Medium Risk / Low Hardship"},
            "R3H1": {"risk": "LOW", "hardship": "HIGH", "rate": 5300, "code": "HH31", "desc": "High Alt Cat II / Low Risk High Hardship"},
            "R3H2": {"risk": "LOW", "hardship": "MEDIUM", "rate": 3400, "code": "HH32", "desc": "High Alt Cat I / Low Risk Medium Hardship"},
            "R3H3": {"risk": "LOW", "hardship": "LOW", "rate": 1200, "code": "R3H3", "desc": "Low Risk / Low Hardship"}
        }
    }


def get_field_and_high_altitude_mappings() -> Dict[str, Any]:
    """Return mapped field allowances and High Altitude Cat I/II/III rates."""
    return {
        "field_allowances": {
            "HAFAA": {
                "full_name": "Highly Active Field Area Allowance",
                "rate": 16900,
                "escalated_rate_at_50_da": 21125,
                "rh_cell": "R1H2",
                "authority": "MoD letter No. 8(3)/2000/D(Pay/Services) dated 24 May 2001"
            },
            "CFAA": {
                "full_name": "Compensatory Field Area Allowance",
                "rate": 10500,
                "escalated_rate_at_50_da": 13125,
                "rh_cell": "R2H2",
                "authority": "MoD letter No. 37269/AG/PS3(a)/90/D(Pay/Services) dated 13 Jan 1994"
            },
            "CMFAA": {
                "full_name": "Compensatory Modified Field Area Allowance",
                "rate": 6300,
                "escalated_rate_at_50_da": 7875,
                "rh_cell": "60% of R2H2",
                "authority": "MoD letter No. 37269/AG/PS3(a)/90/D(Pay/Services) dated 13 Jan 1994"
            }
        },
        "high_altitude_allowance": {
            "Category_I": {
                "description": "Lower rate (9,000 to 14,000 feet)",
                "rate": 3400,
                "escalated_rate_at_50_da": 4250,
                "rh_cell": "R3H2"
            },
            "Category_II": {
                "description": "Higher rate / Uncongenial climate",
                "rate": 5300,
                "escalated_rate_at_50_da": 6625,
                "rh_cell": "R3H1"
            },
            "Category_III": {
                "description": "Enhanced rate (14,000 feet and above sea level)",
                "rate": 25000,
                "escalated_rate_at_50_da": 31250,
                "rh_cell": "R1H1"
            }
        }
    }


def get_one_level_down_rules() -> Dict[str, Any]:
    """Return 'One Level Down' rules for static and non-frontline deployments."""
    return {
        "applicable_establishments": [
            "Formation HQrs up to Division level and attached Troops",
            "Troops employed in enclosed Garrison",
            "Static Formation/Units",
            "MES Units",
            "Military Farms",
            "Recruiting Offices",
            "Training Centre and Establishments",
            "NCC Dte and Units",
            "TA Units unless embodied",
            "Record Offices and similar Establishments"
        ],
        "downgrade_transitions": {
            "R1H1": "R2H1",
            "R2H1": "R3H1",
            "R1H2": "R2H2",
            "R2H2": "R3H2",
            "R1H3": "R2H3",
            "R2H3": "R3H3",
            "77% of R1H2": "R2H2",
            "60% of R2H2": "R3H2"
        },
        "grandfathering_protection": "If one level down results in reduction below existing field allowance, existing allowance continues."
    }


def get_escalation_and_exclusions() -> Dict[str, Any]:
    """Return escalation rules and mutual exclusion combinations."""
    return {
        "escalation_clause": {
            "percentage_increase": 25,
            "da_threshold_percent": 50,
            "formula": "rate_current = rate_base * 1.25 when DA >= 50%",
            "notes": "Applies to Siachen, HAFAA, CFAA, CMFAA, High Altitude, and all RH matrix rates."
        },
        "mutual_exclusions": [
            "Cannot draw Field Allowance (CFAA/CMFAA/HAFA) concurrently with RH Allowance at same station",
            "Cannot draw CI Allowances (CIAPC/CIAMF/CIAFD) concurrently with RH Allowance",
            "Cannot draw Tough Location Allowance (TLA) concurrently with RH Allowance",
            "High Altitude Allowance is location-based and admissible along with SCCIA where authorised"
        ],
        "mandatory_paperwork": {
            "rh_part_ii_order": [
                "Reference to MoD letter No. 37269/AG/PS 3(a) 90/D(Pay/Services) dt 13 Jan 1994",
                "Station Code and effective date of arrival in area",
                "Corps Notification Serial Number and Date"
            ],
            "high_altitude_part_ii_order": [
                "Reference to MoD letter No. F.69/3175/D(Pay/Services) dt 28/2/76",
                "Station name, Div/Bde formation, effective date",
                "Height of area in feet above sea level and Category (I, II, or III)",
                "Corps Notification Serial Number and Date"
            ]
        }
    }


def extract_risk_hardship_data() -> Dict[str, Any]:
    """Compile and return complete Risk and Hardship data dictionary."""
    matrix_base = get_rh_matrix_base_rates()
    field_ha = get_field_and_high_altitude_mappings()
    one_level = get_one_level_down_rules()
    escalation = get_escalation_and_exclusions()

    return {
        "allowance_group": "Risk & Hardship Allowance",
        "effective_date": "2019-02-22",
        "7th_cpc_base_date": "2017-07-01",
        "source_document": "Handbook_Pay_and_Allowances_2023.pdf",
        "source_pages": "140-153 (Handbook pp. 130-143)",
        "authorities": [
            "GoI MoD letter No. 1(16)/2017/D(Pay/Services) dated 18 Sept 2017",
            "GoI MoD, DMA letter No. 8(3)/2017/D(Pay/Services) dated 21st April 2022",
            "ADG PS letter No. C/7021/PAY/SAPCS/2022 dated 24th April 2022"
        ],
        "rh_matrix": matrix_base,
        "field_allowances": field_ha["field_allowances"],
        "high_altitude_allowance": field_ha["high_altitude_allowance"],
        "one_level_down_rules": one_level,
        "escalation_and_exclusions": escalation
    }


if __name__ == "__main__":
    data = extract_risk_hardship_data()
    out_dir = os.path.join(os.path.dirname(__file__), "..", "output")
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, "risk_hardship_rates.json")
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
    print(f"Extracted Risk & Hardship rates saved to {out_path}")
