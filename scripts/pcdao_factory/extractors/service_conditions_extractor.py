"""Extractor for Military Service Conditions, Provident Funds, and Statutory Schemes.

Extracts:
- DSOP Fund rules (6% to 100%, ₹5 Lakh tax-free interest cap, advances, withdrawals)
- AGIF Insurance (₹10,000 monthly subscription, ₹1 Crore cover, disability slabs)
- Standard License Fee slabs and married accommodation retention rules
- Rank-wise Ages of Retirement (Combat Arms, Services, AMC, ADC, MNS)
- Leave entitlements (60d Annual, 20d Casual, 300d career encashment ceiling)
- Joining Time distance slabs (10 / 12 / 15 days)
- Full LTC Scheme (4-year blocks, authorized agents, 10d encashment career cap 60d)
"""

import json
import os
from typing import Any, Dict, List


def get_dsop_fund_rules() -> Dict[str, Any]:
    """Return DSOP Fund statutory subscription, advance, and withdrawal rules."""
    return {
        "eligibility": "Army officers after continuous service of 1 year (optional for re-employed)",
        "minimum_subscription_percent": 6.0,
        "maximum_subscription_percent": 100.0,
        "emoluments_base": "Basic Pay in prescribed Level + NPA (excludes DA and MSP)",
        "annual_tax_exempt_subscription_limit": 500000,
        "subscription_stoppage_before_retirement": "Compulsorily stopped 3 months prior to superannuation / release",
        "variation_rules": "Subscription may be increased twice and/or reduced once in a financial year",
        "advances": {
            "eligible_purposes": ["Illness of self/dependents", "Higher education", "Marriage / ceremonies", "Consumer durables"],
            "maximum_advance": "12 months pay or 75% of credit balance, whichever is less",
            "recovery_installments_max": 60,
            "sanction_time_limit_days": 15
        },
        "non_refundable_withdrawals": {
            "service_eligibility": "After 15 years of service or within 10 years of retirement",
            "permissible_quota": "Up to 75% to 90% of credit balance for house construction or education"
        }
    }


def get_agif_rules() -> Dict[str, Any]:
    """Return Army Group Insurance Fund subscription, cover, and disability benefits."""
    return {
        "monthly_subscription_regular_officers": 10000,
        "life_insurance_cover": 10000000,
        "effective_cover_formatted": "₹1 Crore",
        "authority": "AO 23/2002 read with SAO 5/S/78",
        "recovery_method": "Compulsory advance monthly deduction from pay by PCDA(O)",
        "disability_benefits": {
            "disability_100_percent": 5000000,
            "disability_proportional_range": "Proportionately scaled from 20% to 90% disability"
        },
        "terminal_benefit": "Lump-sum maturity/saving element paid at the time of retirement or release"
    }


def get_ages_of_retirement() -> Dict[str, Any]:
    """Return rank-wise statutory retirement ages across Army branches."""
    return {
        "combat_arms": {
            "description": "Armoured, Infantry, Artillery, Engineers, Signals, Intelligence",
            "Major_and_below": 52, "Lieutenant_Colonel": 54, "Colonel": 54,
            "Brigadier": 56, "Major_General": 58, "Lieutenant_General": 60, "COAS": 62
        },
        "services": {
            "description": "ASC, AOC, EME, Pioneer Corps",
            "Major_and_below": 54, "Lieutenant_Colonel": 54, "Colonel": 54,
            "Brigadier": 56, "Major_General": 58, "Lieutenant_General": 60
        },
        "amc_and_adc": {
            "description": "Army Medical Corps & Army Dental Corps",
            "Major_and_below": 56, "Lieutenant_Colonel": 57, "Colonel": 58,
            "Brigadier": 59, "Major_General": 60, "Lieutenant_General": 61, "DGAFMS": 62
        },
        "mns": {
            "description": "Military Nursing Service",
            "Major_and_below": 55, "Lieutenant_Colonel": 56, "Colonel": 57,
            "Brigadier": 58, "Major_General": 59
        }
    }


def get_leave_entitlement_rules() -> Dict[str, Any]:
    """Return annual leave, casual leave, accumulation, and encashment rules."""
    return {
        "annual_leave_days": 60,
        "casual_leave_days": 20,
        "annual_accumulation_limit_days": 30,
        "career_encashment_ceiling_days": 300,
        "encashment_formula": "(basic_pay + NPA + DA) / 30 * encashed_days",
        "retirement_year_encashment": {
            "retiring_january_31": 15,
            "retiring_february_onwards": 30
        },
        "short_service_commission_rule": "Annual leave of the year of termination of engagement is not encashable"
    }


def get_joining_time_and_ltc() -> Dict[str, Any]:
    """Return Joining Time distance slabs and full LTC scheme rules."""
    return {
        "joining_time_on_transfer": {
            "preparatory_period_days": 6,
            "distance_slabs": [
                {"distance_km": "Up to 1000 km", "joining_time_days": 10},
                {"distance_km": "1001 km to 2000 km", "joining_time_days": 12},
                {"distance_km": "More than 2000 km", "joining_time_days": 15}
            ],
            "air_travel_rule": "Actual time occupied in air journey + 7 days"
        },
        "leave_travel_concession_ltc": {
            "block_period_years": 4,
            "home_town_frequency_years": 2,
            "authorized_travel_agents": ["M/s Balmer Lawrie & Co", "M/s Ashok Travels & Tours", "IRCTC"],
            "advance_drawal_limit_percent": 90,
            "adjustment_claim_deadline_days": 30,
            "leave_encashment_on_ltc": {
                "days_per_occasion": 10,
                "career_maximum_days": 60,
                "encashment_formula": "(basic_pay + NPA + DA) / 30 * 10",
                "condition": "Excludes MSP; tied to bona fide LTC journey"
            }
        },
        "standard_license_fee": {
            "type_iv": {"living_area_sqm": "upto 106", "slab_rate_monthly": 680},
            "type_v": {"living_area_sqm": "106 to 159.5", "slab_rate_monthly": 1400},
            "type_vi": {"living_area_sqm": "159.5 to 189.5", "slab_rate_monthly": 1840},
            "type_vii": {"living_area_sqm": "above 189.5", "slab_rate_monthly": 2370}
        }
    }


def extract_service_conditions_data() -> Dict[str, Any]:
    """Compile and return complete military service conditions data dictionary."""
    return {
        "category": "Service Conditions, Statutory Funds & Leave Schemes",
        "source_document": "Handbook_Pay_and_Allowances_2023.pdf",
        "source_chapters": "Chapters 7, 8, 9, 21, 22, 25",
        "dsop_fund": get_dsop_fund_rules(),
        "agif_insurance": get_agif_rules(),
        "retirement_ages": get_ages_of_retirement(),
        "leave_entitlements": get_leave_entitlement_rules(),
        "joining_time_and_ltc": get_joining_time_and_ltc()
    }


if __name__ == "__main__":
    data = extract_service_conditions_data()
    out_dir = os.path.join(os.path.dirname(__file__), "..", "output")
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, "service_conditions_and_funds.json")
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
    print(f"Extracted Service Conditions & Funds saved to {out_path}")
