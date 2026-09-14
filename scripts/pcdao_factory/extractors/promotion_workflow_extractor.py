"""Extractor for Promotion Pay Fixation, PCDA(O) Ledger System, and Occurrence Codes.

Extracts:
- Promotion Pay Fixation (Rule 10 & 11: Option 1 vs Option 2 algorithms)
- The 1-Month Election Deadline Window
- PCDA(O) Pune IRLA & CDA Account structure (Wing / Task / Account)
- Ledger Section mapping (R-Sec, M-Sec, G-Sec, T-Sec, L-Sec)
- Official Military Occurrence Codes for Part II Orders
"""

import json
import os
from typing import Any, Dict, List


def get_promotion_fixation_rules() -> Dict[str, Any]:
    """Return Option 1 vs Option 2 promotion fixation algorithms and deadlines."""
    return {
        "statutory_rules": "Rule 10 & 11 Army Officers Pay Rules 2017; SRO 12(E) dated 03 May 2017",
        "election_window_months": 1,
        "deadline_description": "Option must be exercised within 1 month from date of publication of promotion Part II Order / Gazette",
        "finality_rule": "Option once exercised shall be final and cannot be revised",
        "default_if_not_exercised": "Option 1 (Fixation from date of promotion) is applied automatically by default",
        "options": {
            "option_1": {
                "name": "Fixation from Date of Promotion",
                "steps": [
                    "1. On date of promotion, grant one increment in the lower level from which promoted.",
                    "2. Place at a cell equal to that figure in the promotional level.",
                    "3. If no identical cell exists, place at the next higher cell in the promotional level.",
                    "4. Date of Next Increment (DNI) in promotional level is after 6 months (following 01 Jan or 01 July)."
                ]
            },
            "option_2": {
                "name": "Fixation from Date of Next Increment (DNI) in Lower Post",
                "steps": [
                    "1. From date of promotion until DNI, pay is fixed at the next higher stage in promotional level without promotional increment.",
                    "2. On DNI (01 Jan or 01 July), two increments are granted in the lower level (1st for annual increment, 2nd for promotion).",
                    "3. Place at a cell equal to that figure in the promotional level, or next higher cell if no identical figure exists.",
                    "4. First increment in the promotional grade is granted after 6 months on following 01 Jan or 01 July; thereafter every 12 months."
                ],
                "financial_advantage": "Typically advantageous when promotion occurs 3-6 months before the officer's scheduled annual increment date."
            }
        },
        "promotion_to_major_general_rule": {
            "applicable_level": "14",
            "rule": "One increment in Level 13A plus Military Service Pay (MSP ₹15,500) element determines placement cell in Level 14."
        }
    }


def get_pcdao_ledger_system() -> Dict[str, Any]:
    """Return PCDA(O) Pune IRLA and CDA Account number anatomy."""
    return {
        "cda_account_structure": {
            "format": "WW/TTT/AAAAAA",
            "components": {
                "WW": "Wing Number (2 digits representing ledger wing)",
                "TTT": "Task Number (3 digits representing the specific auditor desk)",
                "AAAAAA": "Personal Account Number (6 digits unique to the officer)"
            }
        },
        "sections": {
            "R_Section": "Regimental / Regular Officers Ledger Section (Basic pay, DA, MSP, increments)",
            "M_Section": "Medical Corps Section (AMC, ADC, RVC, MNS pay and NPA)",
            "G_Section": "Gallantry Awards & Honours Section (PVC, AC, VrC, Sena Medal)",
            "T_Section": "Travelling Allowance Section (TD, PDM, LTC adjustment claims)",
            "L_Section": "Leave & Encashment Section (Annual leave, encashment on retirement/LTC)",
            "D_Section": "Disbursement & Recoveries Section (DSOP, AGIF, Income Tax, License Fee)"
        },
        "irla_concept": "Individual Running Ledger Account maintains continuous monthly credit/debit balance with running forward carry."
    }


def get_official_occurrence_codes() -> List[Dict[str, str]]:
    """Return official military Occurrence Codes for Part II Order publications."""
    return [
        {"code": "PROMM", "event": "Substantive Promotion", "section": "R-Section"},
        {"code": "TPROMM", "event": "Acting / Temporary Promotion", "section": "R-Section"},
        {"code": "INCR", "event": "Annual Pay Increment", "section": "R-Section"},
        {"code": "SOSCRS", "event": "Struck Off Strength for Course / Training", "section": "T-Section"},
        {"code": "TOSCRS", "event": "Taken On Strength on Return from Course", "section": "T-Section"},
        {"code": "RIMBCEA", "event": "Reimbursement of Children Education Allowance", "section": "R-Section"},
        {"code": "RIMBCEAT", "event": "Reimbursement of CEA for Twin Children", "section": "R-Section"},
        {"code": "RIMBCEAD", "event": "Reimbursement of CEA for Divyang Child", "section": "R-Section"},
        {"code": "CASLEAVE", "event": "Grant of Casual Leave", "section": "L-Section"},
        {"code": "ANNLEAVE", "event": "Grant of Annual Leave", "section": "L-Section"},
        {"code": "LVENCASH", "event": "Encashment of Leave (LTC / Retirement)", "section": "L-Section"},
        {"code": "FLDALL", "event": "Arrival in / Departure from Field Area (HAFAA/CFAA/CMFAA)", "section": "R-Section"},
        {"code": "HAUCA", "event": "Arrival in High Altitude / Uncongenial Climate Area", "section": "R-Section"},
        {"code": "TPTASTN", "event": "Grant / Cessation of Transport Allowance", "section": "R-Section"},
        {"code": "HRASTN", "event": "Grant / Cessation of House Rent Allowance", "section": "R-Section"}
    ]


def extract_promotion_and_workflow_data() -> Dict[str, Any]:
    """Compile and return complete Promotion Fixation & PCDA(O) Workflow data dictionary."""
    return {
        "category": "Promotion Pay Fixation & PCDA(O) Administrative Workflows",
        "source_documents": [
            "Handbook_Pay_and_Allowances_2023.pdf (pp. 96-104)",
            "PCDA O Pune All Frequently Asked Questons Dec 2025.pdf"
        ],
        "promotion_pay_fixation": get_promotion_fixation_rules(),
        "pcdao_ledger_system": get_pcdao_ledger_system(),
        "occurrence_codes": get_official_occurrence_codes()
    }


if __name__ == "__main__":
    data = extract_promotion_and_workflow_data()
    out_dir = os.path.join(os.path.dirname(__file__), "..", "output")
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, "promotion_and_pcdao_workflows.json")
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
    print(f"Extracted Promotion & PCDA(O) Workflows saved to {out_path}")
