"""Extractor for Military Allowances, Additions to Pay, HRA, and CEA.

Extracts:
- Gallantry Awards (Param Vir Chakra to Sena Medal, tax exemption, bars)
- Flying Allowance & Special Forces Allowance (₹25,000 + 25% escalation)
- Qualification Allowance & Technical Allowance (Tier I/II)
- Ration Money Allowance (RMA) peace/field rules
- House Rent Allowance (HRA) slabs (24/16/8 -> 27/18/9 -> 30/20/10), floors, SPR
- Children Education Allowance (CEA) & Hostel Subsidy (₹33,750 / ₹1,01,250)
"""

import json
import os
from typing import Any, Dict, List


def get_gallantry_awards() -> Dict[str, Any]:
    """Return Gallantry Awards monetary allowances and conditions."""
    return {
        "authority": "MoD letter No. 7(62)/2014-D(AG) dated 04 Dec 2017",
        "tax_exemption": "100% exempted from Income Tax under AO 46/79",
        "awards": [
            {"decoration": "Param Vir Chakra (PVC)", "monthly_rate": 20000, "rank_independent": True},
            {"decoration": "Ashoka Chakra (AC)", "monthly_rate": 12000, "rank_independent": True},
            {"decoration": "MahaVir Chakra (MVC)", "monthly_rate": 10000, "rank_independent": True},
            {"decoration": "Kirti Chakra (KC)", "monthly_rate": 9000, "rank_independent": True},
            {"decoration": "Vir Chakra (VrC)", "monthly_rate": 7000, "rank_independent": True},
            {"decoration": "Shaurya Chakra (SC)", "monthly_rate": 6000, "rank_independent": True},
            {"decoration": "Sena Medal (Gallantry only)", "monthly_rate": 2000, "rank_independent": True},
            {"decoration": "Asadharan Suraksha Seva Praman Patra (ASSPP)", "monthly_rate": 6000, "effective_from": "2019-08-27"}
        ],
        "bar_rule": "Each bar to the decoration carries the same amount of monetary allowance as admissible to original award.",
        "survivorship_rules": "On recipient's death, allowance continues to widow lawfully married until her death. If bachelor, paid to father/mother."
    }


def get_specialized_service_allowances() -> Dict[str, Any]:
    """Return Flying, Special Forces, Qualification, and Technical Allowances."""
    return {
        "flying_allowance": {
            "rate_monthly": 25000,
            "escalated_rate_at_50_da": 31250,
            "eligible_personnel": "Officers of Army Aviation Corps and qualified pilots",
            "escalation_trigger": "Increases by 25% each time DA rises by 50%",
            "authority": "MoD letter No. 1(16)/2017/D(Pay/Services) dated 18 Sept 2017"
        },
        "special_forces_allowance": {
            "rate_monthly": 25000,
            "escalated_rate_at_50_da": 31250,
            "eligible_personnel": "Serving in Para (Special Forces) Battalions",
            "escalation_trigger": "Increases by 25% each time DA rises by 50%",
            "authority": "MoD letter No. B/36389/AG/PS3(b)/82/S/D(Pay/Services) dated 18 Sept 2017"
        },
        "qualification_allowance_aviation": {
            "master_aviation_instructor": 1125,
            "senior_aviation_instructor_class_1": 900,
            "senior_aviation_instructor_class_2": 630,
            "aviators_master_green_card": 900,
            "aviators_green_card": 630,
            "escalation_trigger": "Increases by 25% each time DA rises by 50%"
        },
        "technical_allowance": {
            "tier_1_monthly": 3000,
            "tier_2_monthly": 4500,
            "description": "Technically qualified officers in accordance with SAI 5/S/76"
        },
        "ration_money_allowance": {
            "description": "RMA in lieu of free rations in peace or field concessional areas",
            "regulation": "Rule 174(B) DSR Pay and Allowances (Officers)",
            "own_arrangements": "Admissible in exceptional circumstances with prior approval of Local Station Commander"
        }
    }


def get_house_rent_allowance_rules() -> Dict[str, Any]:
    """Return 7th CPC House Rent Allowance (HRA) slabs, transitions, and rules."""
    return {
        "definition_of_basic_pay": "Pay drawn in prescribed Level in Pay Matrix only; excludes NPA, MSP, and special pay.",
        "slabs": {
            "base_rates_at_launch": {"X": 24, "Y": 16, "Z": 8},
            "revised_rates_when_da_crosses_25": {"X": 27, "Y": 18, "Z": 9},
            "revised_rates_when_da_crosses_50": {"X": 30, "Y": 20, "Z": 10}
        },
        "minimum_floor_amounts": {
            "X_class_city_min": 5400,
            "Y_class_city_min": 3600,
            "Z_class_city_min": 1800
        },
        "selected_place_of_residence": {
            "rule": "Admissible at SPR or Home Town rate when posted to concessional/field/afloat areas",
            "license_fee": "Charged at married accommodation rate for reserved accommodation occupied at duty station"
        },
        "nac_dispensation": "Dispensation of condition of furnishing No Accommodation Certificate (NAC) for admissibility of HRA"
    }


def get_cea_and_hostel_subsidy() -> Dict[str, Any]:
    """Return Children Education Allowance & Hostel Subsidy rules from FAQ Dec 2025."""
    return {
        "cea_annual_rate": 33750,
        "cea_pre_50_da_rate": 27000,
        "hostel_subsidy_annual_rate": 101250,
        "hostel_subsidy_pre_50_da_rate": 81000,
        "effective_date": "2024-04-01",
        "divyang_child_multiplier": 2.0,
        "divyang_cea_annual_rate": 67500,
        "max_children": 2,
        "children_exception": "Two eldest surviving children; exception if second childbirth results in twins/multiples",
        "classes_prior_to_class_1": 3,
        "classes_prior_effective_year": "2023-2024",
        "occurrence_codes": {
            "normal_cea": "RIMBCEA",
            "twin_children": "RIMBCEAT",
            "divyang_child": "RIMBCEAD"
        },
        "mandatory_evidence": [
            "School Bonafide Certificate / Self-Declaration for CEA",
            "Hostel fee receipt showing lodging and boarding for Hostel Subsidy",
            "Disability certificate (>= 40%) for Divyang CEA"
        ]
    }


def extract_allowances_data() -> Dict[str, Any]:
    """Compile and return complete military allowances and additions data dictionary."""
    return {
        "category": "Additions to Pay & Educational Allowances",
        "commission": "7th Central Pay Commission",
        "source_documents": [
            "Handbook_Pay_and_Allowances_2023.pdf (pp. 68-73, 105-129, 225-235)",
            "PCDA O Pune All Frequently Asked Questons Dec 2025.pdf (pp. 15-20)"
        ],
        "gallantry_awards": get_gallantry_awards(),
        "specialized_allowances": get_specialized_service_allowances(),
        "house_rent_allowance": get_house_rent_allowance_rules(),
        "children_education_allowance": get_cea_and_hostel_subsidy()
    }


if __name__ == "__main__":
    data = extract_allowances_data()
    out_dir = os.path.join(os.path.dirname(__file__), "..", "output")
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, "allowances_and_additions.json")
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
    print(f"Extracted Allowances data saved to {out_path}")
