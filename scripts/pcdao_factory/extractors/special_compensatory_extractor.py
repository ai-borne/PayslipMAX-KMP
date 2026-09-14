"""Extractor for Special Compensatory & Operational Allowances from Pay Handbook 2023.

Extracts:
- Annual Dress / Outfit Allowance (₹20,000 p.a. in July payslip + recovery table)
- Tough Location Allowance (TLA I/II/III: ₹5,300 / ₹3,400 / ₹1,200)
- Special Duty Allowance (SDA 10% of Basic Pay for North East & Ladakh)
- Island Special Duty Allowance (ISDA 10%, 16%, 20% for A&N and Lakshadweep)
- Deputation (Duty) Allowance (2.5%, 5%, 10% with ₹2,250 / ₹4,500 / ₹9,000 caps)
- Training / Instructional Allowance (24% at CTIs, 12% at other establishments)
- Parachute Allowance (₹10,500 p.m.) & Para Jump Instructor (₹6,000 p.m.)
- Language Allowance (Cat I ₹2,025, Cat II ₹1,689, Cat III ₹1,350)
- Constant Attendance Allowance (CAA ₹6,750 p.m. for 100% disabled retirees)
"""

import json
import os
from typing import Any, Dict, List


def get_dress_allowance() -> Dict[str, Any]:
    """Return 7th CPC Dress Allowance rules and recovery schedule."""
    return {
        "annual_rate_army_officers": 20000,
        "annual_rate_mns_officers": 15000,
        "credit_month": "July",
        "credit_frequency": "Annually in July payslip",
        "effective_date": "2017-07-01",
        "authority": "MoD letter No. PC-1(16)/2017/D(Pay/Services) dated 16/11/2017",
        "escalation": "Increases by 25% each time DA rises by 50%",
        "subsumed_allowances": ["Uniform Outfit Allowance", "Kit Maintenance Allowance (KMA)"],
        "recovery_on_early_exit": {
            "July_Sept": {"superannuation_percent": 60, "vrs_resignation_percent": 80},
            "Oct_Dec": {"superannuation_percent": 40, "vrs_resignation_percent": 55},
            "Jan_March": {"superannuation_percent": 25, "vrs_resignation_percent": 33},
            "April_June": {"superannuation_percent": 0, "vrs_resignation_percent": 0}
        }
    }


def get_tough_location_allowance() -> Dict[str, Any]:
    """Return Tough Location Allowance (TLA) slabs subsuming SC(RL)A."""
    return {
        "categories": {
            "TLA_I": {"monthly_rate": 5300, "places_covered": "Part A and Part B remote/difficult areas", "escalated_at_50_da": 6625},
            "TLA_II": {"monthly_rate": 3400, "places_covered": "Part C remote areas", "escalated_at_50_da": 4250},
            "TLA_III": {"monthly_rate": 1200, "places_covered": "Part D remote areas", "escalated_at_50_da": 1500}
        },
        "escalation": "Increases by 25% each time DA rises by 50%",
        "mutual_exclusion_rule": "If field concessions are admissible, officer has option of higher of field or TLA."
    }


def get_regional_duty_allowances() -> Dict[str, Any]:
    """Return Special Duty Allowance (SDA) and Island Special Duty Allowance (ISDA)."""
    return {
        "special_duty_allowance_sda": {
            "rate_percent_of_basic_pay": 10,
            "applicable_regions": "North Eastern Region and Ladakh",
            "condition": "Serving in specified difficult areas in civil/military employ",
            "basic_pay_definition": "Pay drawn in prescribed Level in Pay Matrix only"
        },
        "island_special_duty_allowance_isda": {
            "areas_around_capital_towns": {"rate_percent_of_basic": 10, "towns": "Port Blair, Kavaratti, Agatti"},
            "difficult_areas": {"rate_percent_of_basic": 16, "areas": "North & Middle Andaman, South Andaman ex-Port Blair"},
            "more_difficult_areas": {"rate_percent_of_basic": 20, "areas": "Little Andaman, Nicobar group, Narcondam, Minicoy"},
            "leave_limit": "Not admissible during leave/training beyond 15 days at a time and 30 days in a year"
        }
    }


def get_deputation_and_training_allowances() -> Dict[str, Any]:
    """Return Deputation (Duty) Allowance and Training / Instructional Allowance."""
    return {
        "deputation_duty_allowance": {
            "same_station_with_concessions": {"rate_percent": 2.5, "ceiling_monthly": 2250},
            "same_station_without_concessions": {"rate_percent": 5.0, "ceiling_monthly": 4500},
            "outstation_with_concessions": {"rate_percent": 5.0, "ceiling_monthly": 4500},
            "outstation_without_concessions": {"rate_percent": 10.0, "ceiling_monthly": 9000},
            "escalation": "Ceilings increase by 25% each time DA rises by 50%"
        },
        "training_allowance": {
            "national_central_academies": {
                "rate_percent_of_basic_pay": 24,
                "institutions": ["IMA Dehradun", "NDA Khadakwasla", "OTA Chennai", "OTA Gaya", "DIQA Bengaluru", "ITM Mussoorie", "CAATS Nasik Road"]
            },
            "other_training_establishments": {
                "rate_percent_of_basic_pay": 12,
                "institutions": ["AFMC Pune", "Armoured Corps School", "School of Artillery", "AATS Agra", "AIPT Pune", "ASC Centre", "Infantry School Mhow", "HAWS", "CIJW"]
            }
        }
    }


def get_special_corps_allowances() -> Dict[str, Any]:
    """Return Parachute, Language, and Constant Attendance allowances."""
    return {
        "parachute_allowance": {
            "rate_monthly": 10500,
            "escalated_at_50_da": 13125,
            "para_jump_instructor": 6000,
            "escalation": "Increases by 25% each time DA rises by 50%",
            "probation": "Admissible after 1 month probation upon qualifying basic parachute course"
        },
        "language_allowance": {
            "Category_I": {"rate_monthly": 2025, "escalated_at_50_da": 2531},
            "Category_II": {"rate_monthly": 1689, "escalated_at_50_da": 2111},
            "Category_III": {"rate_monthly": 1350, "escalated_at_50_da": 1688},
            "condition": "Actual performance of foreign language translator/interpreter duties + annual test pass"
        },
        "constant_attendance_allowance": {
            "rate_monthly": 6750,
            "eligibility": "100% disabled retirees needing constant personal attendance",
            "escalation": "Increases by 25% each time DA rises by 50%"
        }
    }


def extract_special_compensatory_data() -> Dict[str, Any]:
    """Compile and return complete Special Compensatory Allowances data pack."""
    return {
        "allowance_group": "Special Compensatory & Operational Allowances",
        "source_document": "Handbook_Pay_and_Allowances_2023.pdf",
        "source_chapters": "Chapters 14, 15, 17, 18 (pp. 118-121, 144-150, 160-175)",
        "dress_allowance": get_dress_allowance(),
        "tough_location_allowance": get_tough_location_allowance(),
        "regional_duty_allowances": get_regional_duty_allowances(),
        "deputation_and_training": get_deputation_and_training_allowances(),
        "special_corps_allowances": get_special_corps_allowances()
    }


if __name__ == "__main__":
    data = extract_special_compensatory_data()
    out_dir = os.path.join(os.path.dirname(__file__), "..", "output")
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, "special_compensatory_allowances.json")
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
    print(f"Extracted Special Compensatory Allowances saved to {out_path}")
