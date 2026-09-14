"""Extractor for Travel and Daily Allowance (TA/DA) rules from TA Handbook 2023.

Extracts:
- Travel Entitlements by Train and Air (Levels 10-11, 12-13B, 14+)
- Daily Allowance on Tour (Hotel ceilings, Food lump-sum, Local travel)
- Absence regulation percentages for food bills (<6h: 0%, 6-12h: 70%, >12h: 100%)
- Road Mileage Allowance (RMA ₹24/km default)
- Composite Transfer Grant (CTG 80% / 100%) and Baggage entitlements (6,000 kg, ₹50/km)
- Advance settlement deadlines and forfeiture rules (30/60/180 days)
"""

import json
import os
from typing import Any, Dict, List


def get_travel_entitlements() -> List[Dict[str, Any]]:
    """Return air and train travel class entitlements by pay level."""
    return [
        {
            "levels": ["14", "15", "16", "17", "18"],
            "rank_group": "Major General and above",
            "air_travel_class": "Business / Club Class",
            "train_travel_class": "AC-I (First Class)",
            "premium_trains_class": "Executive / AC 1st Class",
            "sea_travel_class": "Highest Class / Deluxe"
        },
        {
            "levels": ["12", "12A", "12B", "13", "13A", "13B"],
            "rank_group": "Lieutenant Colonel, Colonel, Brigadier",
            "air_travel_class": "Economy Class",
            "train_travel_class": "AC-I (First Class)",
            "premium_trains_class": "Executive / AC 1st Class",
            "sea_travel_class": "Deluxe Class"
        },
        {
            "levels": ["10", "10A", "10B", "11"],
            "rank_group": "Lieutenant, Captain, Major",
            "air_travel_class": "Economy Class",
            "train_travel_class": "AC-II (Second AC)",
            "premium_trains_class": "AC 2nd Class / Chair Car",
            "sea_travel_class": "First / Deluxe Class"
        }
    ]


def get_daily_allowance_rates() -> List[Dict[str, Any]]:
    """Return Daily Allowance entitlements (Hotel, Food, City Travel)."""
    return [
        {
            "levels": ["14", "15", "16", "17", "18"],
            "hotel_reimbursement_ceiling_daily": 7500,
            "hotel_escalated_at_50_da": 9375,
            "travel_within_city": "Reimbursement of AC Taxi charges as per actual expenditure",
            "food_bills_lump_sum_daily": 1200,
            "food_escalated_at_50_da": 1500
        },
        {
            "levels": ["12", "12A", "12B", "13", "13A", "13B"],
            "hotel_reimbursement_ceiling_daily": 4500,
            "hotel_escalated_at_50_da": 5625,
            "travel_within_city": "Reimbursement of AC Taxi charges up to 50 kms per day",
            "food_bills_lump_sum_daily": 1000,
            "food_escalated_at_50_da": 1250
        },
        {
            "levels": ["10", "10A", "10B", "11"],
            "hotel_reimbursement_ceiling_daily": 2250,
            "hotel_escalated_at_50_da": 2813,
            "travel_within_city": "Reimbursement of Non-AC Taxi charges up to Rs. 338/- per day (self-certified)",
            "food_bills_lump_sum_daily": 900,
            "food_escalated_at_50_da": 1125
        }
    ]


def get_food_absence_regulation() -> Dict[str, Any]:
    """Return absence regulation percentages for daily food allowance."""
    return {
        "concept": "Lump sum grant; no food vouchers required w.e.f. 01.07.2017",
        "absence_brackets": [
            {"absence_duration": "Less than 6 hours", "percentage_admissible": 0, "amount_rate": "Nil"},
            {"absence_duration": "6 hours to 12 hours", "percentage_admissible": 70, "amount_rate": "70% of daily rate"},
            {"absence_duration": "More than 12 hours", "percentage_admissible": 100, "amount_rate": "100% of daily rate"}
        ],
        "escalation_clause": "The lump sum amount will increase by 25% whenever DA increases by 50%."
    }


def get_transfer_and_baggage_entitlements() -> Dict[str, Any]:
    """Return Composite Transfer Grant and Personal Effects transportation limits."""
    return {
        "composite_transfer_grant": {
            "standard_rate_percent_of_basic": 80,
            "island_rate_percent_of_basic": 100,
            "island_territories": ["Andaman & Nicobar Islands", "Lakshadweep"],
            "minimum_distance_km": 20,
            "retirement_concession": "Condition of 20 km done away with w.e.f. 06.01.2022 if change of residence involved",
            "basic_pay_definition": "Excludes NPA and MSP"
        },
        "baggage_entitlements": [
            {
                "levels": ["12", "12A", "12B", "13", "13A", "13B", "14", "15", "16", "17", "18"],
                "by_train": "6,000 kg by goods train / 4 wheeler wagon / 1 double container",
                "by_road_per_km": 50,
                "escalation": "Rates rise by 25% whenever DA increases by 50%"
            },
            {
                "levels": ["10", "10A", "10B", "11"],
                "by_train": "6,000 kg by goods train / 4 wheeler wagon / 1 single container",
                "by_road_per_km": 50,
                "escalation": "Rates rise by 25% whenever DA increases by 50%"
            }
        ],
        "road_mileage_allowance_tour": {
            "standard_rate_per_km": 24,
            "effective_date": "2022-10-01",
            "authority": "DGOL & SM letter No. B/89621/RMA/Mov C dated 15/09/2022",
            "condition": "Admitted at Rs. 24/- per Km where state RTO rates are more than 2 years old"
        }
    }


def get_deadlines_and_documentation() -> Dict[str, Any]:
    """Return claim submission deadlines and mandatory documentary checklists."""
    return {
        "claim_deadlines": {
            "td_and_pdm_without_advance": {
                "deadline_days": 60,
                "penalty": "Forfeited / deemed relinquished under Rule 290 GFR-2017"
            },
            "td_and_pdm_with_advance": {
                "deadline_days": 60,
                "penalty": "Advance recovered summarily with penal interest from second month's pay"
            },
            "ltc_with_advance": {
                "deadline_days": 30,
                "penalty": "Immediate refund of advance with penal interest"
            },
            "retirement_ta": {
                "deadline_days": 180,
                "effective_from": "2021-06-15",
                "notes": "Modified from 60 days to 180 days (6 months) succeeding retirement date"
            }
        },
        "mandatory_documentation": [
            "Movement Order copy duly signed by Competent Authority",
            "Detention Certificate from Station HQ / Unit with guest room availability status",
            "Original Hotel Cash Receipt / Tax Invoice (GST compliant)",
            "Boarding Passes and Air / Rail Tickets (DTS tickets)",
            "Ration Non-Drawal Certificate countersigned by OC Unit",
            "Self-declaration certificate for change of residence (for CTG)"
        ]
    }


def extract_travel_tada_data() -> Dict[str, Any]:
    """Compile and return complete Travelling and Daily Allowance data dictionary."""
    return {
        "allowance_group": "Travelling & Daily Allowance (TA/DA)",
        "governing_rules": "Travel Regulations (TR) & 7th CPC Orders w.e.f. 01.07.2017",
        "source_document": "Handbook_Travelling_Allowances_2023.pdf",
        "source_pages": "38-90 (Handbook Chapters 4, 5, 10)",
        "travel_entitlements": get_travel_entitlements(),
        "daily_allowance_rates": get_daily_allowance_rates(),
        "food_absence_regulation": get_food_absence_regulation(),
        "transfer_and_baggage": get_transfer_and_baggage_entitlements(),
        "deadlines_and_documentation": get_deadlines_and_documentation()
    }


if __name__ == "__main__":
    data = extract_travel_tada_data()
    out_dir = os.path.join(os.path.dirname(__file__), "..", "output")
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, "travel_tada_rates.json")
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
    print(f"Extracted TA/DA rates saved to {out_path}")
