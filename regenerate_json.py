import os
import json
import re
import pypdf

from pdf_mappings import (
    credit_keys_mapping, debit_keys_mapping,
    apply_historical_overrides
)
from pdf_parser_helpers import (
    clean_commas_and_whitespace, split_credit_debit_sections, extract_from_column,
    should_skip_file, find_table_page, locate_table_bounds, extract_columns_text,
    parse_date_and_officer, parse_totals
)

pdf_dir = "/Users/test/Desktop/Pay Slip Elements"
password = os.environ.get("PDF_DECRYPTION_PASSWORD", "535d04")

def extract_pdf_chars(table_page):
    chars = []
    def visitor_body(text, cm, tm, fontDict, fontSize):
        if text.strip():
            chars.append({'text': text, 'x': tm[4], 'y': tm[5]})
    table_page.extract_text(visitor_text=visitor_body)
    return chars

def extract_tax_and_dsop_text(reader, page_count):
    tax_text = ""
    dsop_text = ""
    for i in range(page_count):
        p_text = reader.pages[i].extract_text() or ""
        p_text_lower = p_text.lower()
        if not tax_text and any(term in p_text_lower for term in ["standard deduction", "taxable income", "tax payable", "income tax deducted"]):
            tax_text = clean_commas_and_whitespace(p_text)
        if not dsop_text and ("dsop fund" in p_text_lower or ("opening balance" in p_text_lower and "closing balance" in p_text_lower and "subscription" in p_text_lower)):
            dsop_text = clean_commas_and_whitespace(p_text)
    return tax_text, dsop_text or tax_text

def parse_tax_and_savings(tax_text, dsop_text):
    raw = {
        "gross_salary": 0.0, "total_taxable_income": 0.0, "standard_deduction": 0.0,
        "net_taxable_income": 0.0, "total_tax_payable": 0.0, "tax_deducted": 0.0, "cess_deducted": 0.0,
        "dsop": {"opening_balance": 0.0, "subscription": 0.0, "refund": 0.0, "misc_adj": 0.0, "withdrawal": 0.0, "closing_balance": 0.0}
    }
    std = {
        "gross_salary_ytd": 0.0, "total_taxable_income": 0.0, "standard_deduction": 0.0,
        "net_taxable_income": 0.0, "total_tax_payable": 0.0, "tax_deducted_ytd": 0.0, "cess_deducted_ytd": 0.0,
        "dsop_fund": {"opening_balance": 0.0, "subscription_ytd": 0.0, "refund_ytd": 0.0, "misc_adj_ytd": 0.0, "withdrawal_ytd": 0.0, "closing_balance": 0.0}
    }
    if tax_text:
        gross_sal_match = re.search(r"Gross Salary (?:upto \d+/\d+/\d+)?\s+(\d+)", tax_text, re.IGNORECASE) or re.search(r"Pay & Allce upto\s+\d+/\d+/\d+\s+(\d+)", tax_text, re.IGNORECASE)
        taxable_inc_match = re.search(r"Total Taxable Income\s+(\d+)", tax_text, re.IGNORECASE) or re.search(r"Total taxable pay\s+\(Sl\.No\.\s*\d+\+\d+\+\d+\+\d+\)\s+(\d+)", tax_text, re.IGNORECASE)
        std_ded_match = re.search(r"Standard Deduction\s+(\d+)", tax_text, re.IGNORECASE)
        net_taxable_match = (
            re.search(r"Net Taxable Income\s+\(\(Sl\.No\.\s*\d+\s*\+\s*Sl\.No\.\s*\d+\)\s*-\s*\(Sl\.No\.\s*\d+\)\)\s+(\d+)", tax_text, re.IGNORECASE) or
            re.search(r"Net Taxable Income\s+\(\d+\s*-\s*\d+\s*-\s*\d+\)\s+(\d+)", tax_text, re.IGNORECASE) or
            re.search(r"Net Taxable Income\s+(\d+)", tax_text, re.IGNORECASE)
        )
        tax_payable_match = re.search(r"Total Tax Payable\s+(\d+)", tax_text, re.IGNORECASE) or re.search(r"Total Income Tax\s+\(Tax on Sl\.No\.\s*\d+\)\s+(\d+)", tax_text, re.IGNORECASE)
        tax_deducted_match = re.search(r"Income Tax Deducted\s+(\d+)", tax_text, re.IGNORECASE)
        cess_deducted_match = re.search(r"Ed\.\s*Cess Deducted\s+(\d+)", tax_text, re.IGNORECASE) or re.search(r"Educ\.\s*Cess Deducted\s+(\d+)", tax_text, re.IGNORECASE)
        
        gross_salary_ytd = float(gross_sal_match.group(1)) if gross_sal_match else 0.0
        if gross_sal_match:
            raw["gross_salary"] = std["gross_salary_ytd"] = gross_salary_ytd
        if taxable_inc_match:
            raw["total_taxable_income"] = std["total_taxable_income"] = float(taxable_inc_match.group(1))
        if std_ded_match:
            raw["standard_deduction"] = std["standard_deduction"] = float(std_ded_match.group(1))
        if net_taxable_match:
            raw["net_taxable_income"] = std["net_taxable_income"] = float(net_taxable_match.group(1))
        if tax_payable_match:
            val = float(tax_payable_match.group(1))
            raw["total_tax_payable"] = val
            std["total_tax_payable"] = round(gross_salary_ytd * 0.30) if (gross_salary_ytd > 0.0 and val > gross_salary_ytd) else val
        if tax_deducted_match:
            raw["tax_deducted"] = std["tax_deducted_ytd"] = float(tax_deducted_match.group(1))
        if cess_deducted_match:
            raw["cess_deducted"] = std["cess_deducted_ytd"] = float(cess_deducted_match.group(1))

    if dsop_text:
        dsop_match = re.search(r"Opening Balance\s*(\d+)\s*Subscription\s*(\d+)\s*Refund\s*(\d+)\s*Misc\s*Adj\s*(\d+)\s*Withdrawal\s*(\d+)\s*Closing Balance\s*(\d+)", dsop_text, re.IGNORECASE)
        if dsop_match:
            op_bal, subn, ref, madj, wd, cl_bal = [float(dsop_match.group(i)) for i in range(1, 7)]
            raw["dsop"] = {"opening_balance": op_bal, "subscription": subn, "refund": ref, "misc_adj": madj, "withdrawal": wd, "closing_balance": cl_bal}
            std["dsop_fund"] = {"opening_balance": op_bal, "subscription_ytd": subn, "refund_ytd": ref, "misc_adj_ytd": madj, "withdrawal_ytd": wd, "closing_balance": cl_bal}

    return raw, std

def parse_pdf(file_path, filename):
    reader = pypdf.PdfReader(file_path)
    if reader.is_encrypted:
        reader.decrypt(password)
        
    if should_skip_file(reader, filename):
        return None, None

    page_count = len(reader.pages)
    table_page_idx = find_table_page(reader)
    table_page = reader.pages[table_page_idx]
    page_height = float(table_page.mediabox.height)
    page_width = float(table_page.mediabox.width)
    
    chars = extract_pdf_chars(table_page)
    bounds = locate_table_bounds(chars, page_height)
    left_text, middle_text = extract_columns_text(chars, bounds, page_width, page_height)
    
    if not left_text.strip() or ("basic pay" not in left_text.lower() and "bpay" not in left_text.lower()):
        page_text = table_page.extract_text() or ""
        left_text = middle_text = page_text

    full_text = clean_commas_and_whitespace(" ".join((p.extract_text() or "") for p in reader.pages))
    year, month_num, month_name, month_abbr, officer_name, ac_no, pan_no = parse_date_and_officer(full_text, filename)
    totals = parse_totals(full_text)

    left_extracted = extract_from_column(left_text, credit_keys_mapping, debit_keys_mapping)
    middle_extracted = extract_from_column(middle_text, credit_keys_mapping, debit_keys_mapping)
    is_split = left_text != middle_text

    earnings_raw, deductions_raw = {}, {}
    earnings_std = {k: 0.0 for k in [
        "basic_pay", "dearness_allowance", "military_service_pay", "transport_allowance", "transport_allowance_da",
        "dress_allowance", "ration_money", "special_forces_pay", "field_allowance", "children_education_allowance",
        "house_rent_allowance", "risk_hardship_allowance", "non_practicing_allowance", "adj_basic_pay", "adj_da",
        "adj_msp", "adj_tpta", "arrears_cea", "arrears_da", "arrears_ration", "arrears_special_forces", "arrears_tpta",
        "arrears_tpta_da", "arrears_hra", "arrears_risk_hardship", "adj_pay_and_allce", "adj_field_allowance",
        "medical_allowance", "adj_ticket_recovery", "misc_earnings"
    ]}
    deductions_std = {k: 0.0 for k in [
        "dsop_subscription", "agif", "income_tax", "education_cess", "license_fee", "furniture_rent", "water_charges",
        "electricity_charges", "barrack_damage", "ticket_recovery", "rec_field_allowance", "rec_special_forces",
        "recovery_of_debits", "aobf", "agif_loan_recovery", "misc_deductions"
    ]}
    opening_cr = closing_dr = opening_dr = closing_cr = 0.0

    for k, v in left_extracted.items():
        if k in credit_keys_mapping:
            std_key = credit_keys_mapping[k]
            if std_key == "opening_credit_balance": opening_cr = v
            elif std_key == "closing_debit_balance": closing_dr = v
            else:
                earnings_raw[k] = v
                earnings_std[std_key] = earnings_std.get(std_key, 0.0) + v

    for k, v in middle_extracted.items():
        if k in debit_keys_mapping:
            std_key = debit_keys_mapping[k]
            if std_key == "opening_debit_balance": opening_dr = v
            elif std_key == "closing_credit_balance": closing_cr = v
            else:
                deductions_raw[k] = v
                deductions_std[std_key] = deductions_std.get(std_key, 0.0) + v

    apply_historical_overrides(year, month_num, earnings_std, deductions_std)
    gross_pay = totals.get("Gross Pay", 0.0)
    total_deductions = totals.get("Total Deductions", 0.0)
    net_remittance = totals.get("Net Remittance", 0.0)

    real_gross = gross_pay if gross_pay > 0 else sum(earnings_std.values())
    real_deductions = total_deductions if (total_deductions > 0 and total_deductions != real_gross and total_deductions != net_remittance) else sum(deductions_std.values())
    final_net = net_remittance if net_remittance > 0 else (real_gross - real_deductions)

    tax_text, dsop_text = extract_tax_and_dsop_text(reader, page_count)
    tax_and_savings_raw, tax_and_savings_std = parse_tax_and_savings(tax_text, dsop_text)

    raw_record = {
        "file": filename, "year": year, "month": month_abbr,
        "officer": {"name": officer_name, "ac_no": ac_no, "pan": pan_no},
        "earnings": earnings_raw, "deductions": deductions_raw,
        "summary": {"gross_pay": real_gross, "total_deductions": real_deductions, "net_remittance": final_net},
        "tax_and_savings": tax_and_savings_raw
    }

    std_record = {
        "file": filename, "year": year, "month_num": month_num, "month_name": month_name,
        "date_str": f"{str(month_num).zfill(2)}/{year}",
        "officer": {"name": officer_name, "account_no": ac_no, "pan": pan_no},
        "earnings": earnings_std, "deductions": deductions_std,
        "ledger_balances": {"opening_credit_balance": opening_cr, "opening_debit_balance": opening_dr, "closing_credit_balance": closing_cr, "closing_debit_balance": closing_dr},
        "summary": {"gross_pay": real_gross, "total_deductions": real_deductions, "net_remittance": final_net},
        "tax_and_savings": tax_and_savings_std
    }
    return raw_record, std_record

if __name__ == "__main__":
    if os.path.exists(pdf_dir):
        raw_list, std_list = [], []
        years = sorted([d for d in os.listdir(pdf_dir) if os.path.isdir(os.path.join(pdf_dir, d)) and d.isdigit()])
        for y in years:
            y_dir = os.path.join(pdf_dir, y)
            for filename in sorted([f for f in os.listdir(y_dir) if f.endswith(".pdf")]):
                try:
                    raw_rec, std_rec = parse_pdf(os.path.join(y_dir, filename), filename)
                    if raw_rec:
                        raw_list.append(raw_rec)
                        std_list.append(std_rec)
                except Exception as e:
                    print(f"Error parsing {filename}: {e}")

        with open("/Users/test/Downloads/PDFParser/payslips_data.json", "w") as f:
            json.dump(raw_list, f, indent=2)
        with open("/Users/test/Downloads/PDFParser/payslips_data_standardized.json", "w") as f:
            json.dump(std_list, f, indent=2)
        print("Done! Regenerated files successfully.")
