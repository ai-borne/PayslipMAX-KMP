"""Extractor for 7th CPC Defence Pay Matrix from Pay & Allowances Handbook 2023.

Extracts:
- Regular Officers Pay Matrix (Levels 10 to 18, all stages)
- MNS Pay Matrix (Levels 10 to 13B)
- Rank to Level mappings (Regular, MNS, NCC)
- Military Service Pay (MSP) rules
- Index of Rationalisation (IOR) metadata
"""

import json
import os
import subprocess
from typing import Any, Dict, List


PDF_PATH_DEFAULT = "/Users/sunil/Downloads/PCDAO PDFs/Handbook_Pay_and_Allowances_2023.pdf"


def run_pdftotext(pdf_path: str, start_page: int, end_page: int) -> str:
    """Extract layout-preserved text from PDF using pdftotext CLI."""
    cmd = ["pdftotext", "-layout", "-f", str(start_page), "-l", str(end_page), pdf_path, "-"]
    res = subprocess.run(cmd, capture_output=True, text=True, check=True)
    return res.stdout


def parse_regular_pay_matrix(pdf_path: str) -> Dict[str, List[int]]:
    """Parse Regular Officers Pay Matrix (Levels 10 to 18) across 40 stages."""
    text = run_pdftotext(pdf_path, 88, 89)
    levels = ["10", "10B", "11", "12A", "13", "13A", "14", "15", "16", "17", "18"]
    matrix: Dict[str, List[int]] = {lvl: [] for lvl in levels}

    for line in text.split("\n"):
        parts = line.split()
        if not parts:
            continue
        # Strip header/margin page numbers (e.g. 78, 79) appearing before cell numbers
        if len(parts) > 1 and parts[0] in ["78", "79"] and parts[1].isdigit():
            parts = parts[1:]
        if not parts[0].isdigit():
            continue
        cell_num = int(parts[0])
        if not (1 <= cell_num <= 40):
            continue

        values = [int(p) for p in parts[1:] if p.isdigit() and len(p) >= 5]
        if not values:
            continue

        if cell_num <= 17:
            _assign_cells_1_to_17(cell_num, values, matrix)
        else:
            _assign_cells_18_to_40(cell_num, values, matrix)

    return matrix


def _assign_cells_1_to_17(cell_num: int, values: List[int], matrix: Dict[str, List[int]]) -> None:
    """Assign parsed stage values for cells 1 to 17 across appropriate levels."""
    levels = ["10", "10B", "11", "12A", "13", "13A", "14", "15", "16", "17", "18"]
    # Mapping active levels per cell range
    active_lvls = levels.copy()
    if cell_num == 1:
        active_lvls = levels  # all 11
    elif 2 <= cell_num <= 4:
        active_lvls = levels[:-2]  # up to 16
    elif 5 <= cell_num <= 8:
        active_lvls = levels[:-3]  # up to 15
    elif 9 <= cell_num <= 15:
        active_lvls = levels[:-4]  # up to 14
    elif cell_num == 16:
        active_lvls = levels[:-5]  # up to 13A
    elif cell_num == 17:
        active_lvls = levels[:-6]  # up to 13

    for idx, lvl in enumerate(active_lvls):
        if idx < len(values):
            matrix[lvl].append(values[idx])


def _assign_cells_18_to_40(cell_num: int, values: List[int], matrix: Dict[str, List[int]]) -> None:
    """Assign parsed stage values for cells 18 to 40."""
    if cell_num == 18:
        active_lvls = ["10", "10B", "11", "12A", "13"]
    elif 19 <= cell_num <= 20:
        active_lvls = ["10", "10B", "11", "12A"]
    elif 21 <= cell_num <= 38:
        active_lvls = ["10", "10B", "11"]
    else:  # 39, 40
        active_lvls = ["10", "10B"]

    for idx, lvl in enumerate(active_lvls):
        if idx < len(values):
            matrix[lvl].append(values[idx])


def parse_mns_pay_matrix(pdf_path: str) -> Dict[str, List[int]]:
    """Parse Military Nursing Service (MNS) Pay Matrix from pages 90-91 (cells 1-24)."""
    text = run_pdftotext(pdf_path, 90, 91)
    levels = ["10", "10A", "10B", "11", "12", "12B", "13B"]
    matrix: Dict[str, List[int]] = {lvl: [] for lvl in levels}

    for line in text.split("\n"):
        if "NCC Whole Time Lady" in line:
            break
        parts = line.split()
        if not parts:
            continue
        if len(parts) > 1 and parts[0] in ["80", "81"] and parts[1].isdigit():
            parts = parts[1:]
        if not parts[0].isdigit():
            continue
        cell_num = int(parts[0])
        if not (1 <= cell_num <= 24):
            continue

        values = [int(p) for p in parts[1:] if p.isdigit() and len(p) >= 5]
        # Active levels decrease as stages increase
        if cell_num <= 15:
            active_lvls = levels
        elif cell_num <= 17:
            active_lvls = levels[:-2]  # up to 12
        elif cell_num <= 21:
            active_lvls = levels[:-3]  # up to 11
        else:
            active_lvls = levels[:3]   # 10, 10A, 10B

        for idx, lvl in enumerate(active_lvls):
            if idx < len(values):
                matrix[lvl].append(values[idx])

    return matrix


def parse_ncc_pay_matrix(pdf_path: str) -> Dict[str, List[int]]:
    """Parse NCC Whole Time Lady Officers Pay Matrix from pages 91-92 (cells 1-24)."""
    text = run_pdftotext(pdf_path, 91, 92)
    levels = ["10", "10B", "11", "12"]
    matrix: Dict[str, List[int]] = {lvl: [] for lvl in levels}
    in_ncc_section = False

    for line in text.split("\n"):
        if "NCC Whole Time Lady" in line:
            in_ncc_section = True
            continue
        if not in_ncc_section or "MSP is not admissible" in line:
            if "MSP is not admissible" in line:
                break
            continue

        parts = line.split()
        if not parts or not parts[0].isdigit():
            continue
        cell_num = int(parts[0])
        if not (1 <= cell_num <= 24):
            continue

        values = [int(p) for p in parts[1:] if p.isdigit() and len(p) >= 5]
        for idx, lvl in enumerate(levels):
            if idx < len(values):
                matrix[lvl].append(values[idx])

    return matrix


def get_rank_mappings() -> Dict[str, Any]:
    """Return official Rank to Pay Level mappings from page 93."""
    return {
        "regular_army": {
            "Lieutenant": {"level": "10", "grade_pay_pre_7th": 5400, "pay_band": "PB-3"},
            "Captain": {"level": "10B", "grade_pay_pre_7th": 6100, "pay_band": "PB-3"},
            "Major": {"level": "11", "grade_pay_pre_7th": 6600, "pay_band": "PB-3"},
            "Lieutenant Colonel": {"level": "12A", "grade_pay_pre_7th": 8000, "pay_band": "PB-4"},
            "Colonel": {"level": "13", "grade_pay_pre_7th": 8700, "pay_band": "PB-4"},
            "Brigadier": {"level": "13A", "grade_pay_pre_7th": 8900, "pay_band": "PB-4"},
            "Major General": {"level": "14", "grade_pay_pre_7th": 10000, "pay_band": "PB-4"},
            "Lieutenant General (HAG)": {"level": "15", "scale": "67000-79000", "pay_band": "HAG"},
            "Lieutenant General (HAG+)": {"level": "16", "scale": "75500-80000", "pay_band": "HAG+"},
            "Vice Chief of Army Staff / Army Commander": {"level": "17", "scale": "80000 (fixed)", "pay_band": "Apex"},
            "Chief of Army Staff": {"level": "18", "scale": "90000 (fixed)", "pay_band": "Cabinet Sec/COAS"}
        },
        "military_nursing_service": {
            "Lieutenant": "10", "Captain": "10A", "Major": "10B",
            "Lieutenant Colonel": "11", "Colonel": "12", "Brigadier": "12B", "Major General": "13B"
        },
        "ncc_whole_time_lady_officers": {
            "Lieutenant": "10", "Captain": "10B", "Major": "11", "Lieutenant Colonel": "12"
        }
    }


def get_msp_rules() -> Dict[str, Any]:
    """Return Military Service Pay (MSP) rules under 7th CPC."""
    return {
        "regular_officers": {
            "rate_monthly": 15500,
            "applicable_levels": ["10", "10B", "11", "12A", "13", "13A"],
            "applicable_ranks": ["Lieutenant", "Captain", "Major", "Lieutenant Colonel", "Colonel", "Brigadier"],
            "counts_for_da": True,
            "counts_for_pension": True,
            "counts_for_hra": False,
            "authority": "MoD letter No. 1(16)/2017/D(Pay/Services) dated 18 Sept 2017"
        },
        "mns_officers": {
            "rate_monthly": 10800,
            "applicable_levels": ["10", "10A", "10B", "11", "12", "12B", "13B"],
            "counts_for_da": True,
            "counts_for_pension": True,
            "counts_for_hra": False
        },
        "ineligible": ["Major General and above (Level 14+)", "NCC Officers"]
    }


def extract_pay_matrix_data(pdf_path: str = PDF_PATH_DEFAULT) -> Dict[str, Any]:
    """Generate complete 7th CPC Pay Matrix and metadata dictionary."""
    regular_matrix = parse_regular_pay_matrix(pdf_path)
    mns_matrix = parse_mns_pay_matrix(pdf_path)
    ncc_matrix = parse_ncc_pay_matrix(pdf_path)

    return {
        "commission": "7th Central Pay Commission",
        "effective_date": "2016-01-01",
        "source_document": "Handbook_Pay_and_Allowances_2023.pdf",
        "source_pages": "88-93",
        "index_of_rationalisation": {
            "level_10_to_11": 2.57,
            "level_12A_and_13": 2.67,
            "level_13A_and_above": 2.72,
            "revision_note": "IOR of Level 12A and 13 enhanced from 2.57 to 2.67; extended to 40 stages."
        },
        "regular_officers_pay_matrix": regular_matrix,
        "mns_pay_matrix": mns_matrix,
        "ncc_pay_matrix": ncc_matrix,
        "rank_mappings": get_rank_mappings(),
        "military_service_pay": get_msp_rules()
    }


if __name__ == "__main__":
    data = extract_pay_matrix_data()
    out_dir = os.path.join(os.path.dirname(__file__), "..", "output")
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, "pay_matrix_7th_cpc.json")
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
    print(f"Extracted pay matrix saved to {out_path}")
