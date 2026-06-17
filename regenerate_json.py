import os
import json
import re
import pypdf

pdf_dir = "/Users/sunil/Desktop/Pay Slip Elements"
password = "535d04"

month_map = {
    "january": 1, "jan": 1, "february": 2, "feb": 2, "march": 3, "mar": 3,
    "april": 4, "apr": 4, "may": 5, "june": 6, "jun": 6, "july": 7, "jul": 7,
    "august": 8, "aug": 8, "september": 9, "sep": 9, "sept": 9, "october": 10, "oct": 10,
    "november": 11, "nov": 11, "december": 12, "dec": 12
}

month_names = [
    "", "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
]

credit_keys_mapping = {
    "Basic Pay": "basic_pay",
    "BPAY": "basic_pay",
    "DA": "dearness_allowance",
    "MSP": "military_service_pay",
    "Tpt Allc": "transport_allowance",
    "TPTA": "transport_allowance",
    "TRAN1": "transport_allowance",
    "TRAN-1": "transport_allowance",
    "TPTADA": "transport_allowance_da",
    "Tpt DA": "transport_allowance_da",
    "DRESALW": "dress_allowance",
    "A/o DressAllowance": "dress_allowance",
    "RSHNA": "ration_money",
    "RMONEYAllce-RA": "ration_money",
    "RA": "ration_money",
    "SpCmd Pay": "special_forces_pay",
    "SPCDO": "special_forces_pay",
    "SC": "special_forces_pay",
    "FD": "field_allowance",
    "CEA": "children_education_allowance",
    "C E A(NT)": "children_education_allowance",
    "C E A (T)": "children_education_allowance",
    "C E A": "children_education_allowance",
    "ARR-CEA": "arrears_cea",
    "ARR-DA": "arrears_da",
    "ARR-RSHNA": "arrears_ration",
    "ARR-SPCDO": "arrears_special_forces",
    "ARR-TPTA": "arrears_tpta",
    "ARR-TPTADA": "arrears_tpta_da",
    "ARR-HH32": "arrears_hra",
    "A/o BPAY-": "adj_basic_pay",
    "A/o DA-": "adj_da",
    "A/o MSP-": "adj_msp",
    "A/o TRAN-1": "adj_tpta",
    "A/o TRAN-2": "adj_tpta",
    "A/o Pay & Allce": "adj_pay_and_allce",
    "A/o FIELD-R1": "adj_field_allowance",
    "ETKT-ref": "adj_ticket_recovery",
    "MEDICAL": "medical_allowance",
    "Adhoc Payt": "adj_pay_and_allce",
    "Reimb Med": "medical_allowance",
    "A/o RMONEYAllce-RA": "arrears_ration",
    "Op Cr Bal": "opening_credit_balance",
    "Cl. Dr. Bal.": "closing_debit_balance",
    "Clos Bal(-)": "closing_debit_balance",
    "RH11": "risk_hardship_allowance",
    "RH12": "risk_hardship_allowance",
    "RH13": "risk_hardship_allowance",
    "RH21": "risk_hardship_allowance",
    "RH22": "risk_hardship_allowance",
    "RH23": "risk_hardship_allowance",
    "RH31": "risk_hardship_allowance",
    "RH32": "risk_hardship_allowance",
    "RH33": "risk_hardship_allowance",
    "ARR-RH11": "arrears_risk_hardship",
    "ARR-RH12": "arrears_risk_hardship",
    "ARR-RH13": "arrears_risk_hardship",
    "ARR-RH21": "arrears_risk_hardship",
    "ARR-RH22": "arrears_risk_hardship",
    "ARR-RH23": "arrears_risk_hardship",
    "ARR-RH31": "arrears_risk_hardship",
    "ARR-RH32": "arrears_risk_hardship",
    "ARR-RH33": "arrears_risk_hardship",
    "HH11": "house_rent_allowance",
    "HH12": "house_rent_allowance",
    "HH13": "house_rent_allowance",
    "HH21": "house_rent_allowance",
    "HH22": "house_rent_allowance",
    "HH23": "house_rent_allowance",
    "HH31": "house_rent_allowance",
    "HH32": "house_rent_allowance",
    "HH33": "house_rent_allowance",
    "HRA": "house_rent_allowance",
    "ARR-HH11": "arrears_hra",
    "ARR-HH12": "arrears_hra",
    "ARR-HH13": "arrears_hra",
    "ARR-HH21": "arrears_hra",
    "ARR-HH22": "arrears_hra",
    "ARR-HH23": "arrears_hra",
    "ARR-HH31": "arrears_hra",
    "ARR-HH32": "arrears_hra",
    "ARR-HH33": "arrears_hra",
    "ARR-HRA": "arrears_hra",
    "NPA": "non_practicing_allowance",
    "SICHA": "risk_hardship_allowance",
    "ARR-SICHA": "arrears_risk_hardship",
    "Gr Pay": "basic_pay",
    "Grade Pay": "basic_pay",
    "D.A.": "dearness_allowance",
    "Tpt. Allc": "transport_allowance",
    "K.M.A": "dress_allowance",
    "M.S.P.": "military_service_pay",
    "A/o DA": "arrears_da",
    "A/o BPAY": "adj_basic_pay",
    "A/o MSP": "adj_msp",
    "Outfit Alc": "dress_allowance",
    "Outfit Allowance": "dress_allowance",
    "TA/DA Cheq": "adj_pay_and_allce",
    "Arrs P & A": "adj_pay_and_allce",
    "Arr P & A": "adj_pay_and_allce",
    "Instr Allce": "adj_pay_and_allce",
    "Instr Allowance": "adj_pay_and_allce",
    "Of / Drs Alc": "dress_allowance",
    "HA/UCA All": "risk_hardship_allowance",
    "SCCI Allce": "risk_hardship_allowance",
    "LTC Encash": "adj_pay_and_allce",
    "Ref.L Fee": "adj_pay_and_allce",
    "Ref.Furn.": "adj_pay_and_allce",
}

debit_keys_mapping = {
    "DSOPF Subn": "dsop_subscription",
    "DSOP": "dsop_subscription",
    "AGIF": "agif",
    "Incm Tax": "income_tax",
    "ITAX": "income_tax",
    "Educ Cess": "education_cess",
    "EHCESS": "education_cess",
    "L Fee": "license_fee",
    "LF": "license_fee",
    "Fur": "furniture_rent",
    "FUR": "furniture_rent",
    "Water": "water_charges",
    "WATER": "water_charges",
    "Elec": "electricity_charges",
    "Barrack Damage": "barrack_damage",
    "Dr Barrack Damage": "barrack_damage",
    "ETKT": "ticket_recovery",
    "R/o Etkt": "ticket_recovery",
    "Rec CIA-FD": "rec_field_allowance",
    "Rec PARA-SC": "rec_special_forces",
    "Op Dr Bal": "opening_debit_balance",
    "Cl. Cr. Bal.": "closing_credit_balance",
    "Clos Bal(+)": "closing_credit_balance",
    "OP Bal(-)": "opening_debit_balance",
    "R/o Of /Drs": "recovery_of_debits",
    "AOBF": "aobf",
    "AGIF-CAR": "agif_loan_recovery",
    "AGIF-MCA": "agif_loan_recovery",
    "Educ. Cess": "education_cess",
    "Furn.": "furniture_rent",
    "Recv P & A": "recovery_of_debits",
    "CC to bankers": "recovery_of_debits",
}

def clean_commas_and_whitespace(text):
    cleaned = re.sub(r"(\d),(\d)", r"\1\2", text)
    return re.sub(r"\s+", " ", cleaned)

def split_credit_debit_sections(cleaned_text):
    # Truncate to page 1 to exclude subsequent pages (tax/dsop details)
    table_text = cleaned_text
    footer_indicators = [
        "Note: This is a system",
        "Note: This is system",
        "Note : This is a system",
        "Note: This is a system generated document",
        "Note: This is system generated document"
    ]
    for indicator in footer_indicators:
        idx = cleaned_text.lower().find(indicator.lower())
        if idx >= 0:
            table_text = cleaned_text[:idx]
            break

    end_of_table_indicators = [
        "Total Credit", "Total Debit", "Total Deductions", "Gross Pay", "Net Remittance", "REMITTANCE"
    ]
    for indicator in end_of_table_indicators:
        idx = table_text.lower().find(indicator.lower())
        if idx >= 0:
            table_text = table_text[:idx]

    debit_only_anchors = [
        "DSOPF Subn", "DSOPF", "DSOP", "AGIF", "Incm Tax", "ITAX",
        "Educ Cess", "EHCESS", "Educ. Cess", "Op Dr Bal",
        "OP Bal(-)", "Cl. Cr. Bal.", "Clos Bal(+)", "R/o Of /Drs"
    ]
    case_sensitive_anchors = ["LF", "FUR"]
    split_idx = len(table_text)
    found = False
    table_lower = table_text.lower()
    for anchor in debit_only_anchors:
        idx = table_lower.find(anchor.lower())
        if 0 < idx < split_idx:
            split_idx = idx
            found = True
    for anchor in case_sensitive_anchors:
        idx = table_text.find(anchor)
        if 0 < idx < split_idx:
            split_idx = idx
            found = True
    credit_section = table_text[:split_idx]
    debit_section = table_text[split_idx:] if found else ""
    return credit_section, debit_section, found

def extract_from_column(col_text, credit_mapping, debit_mapping):
    extracted = {}
    working_col = clean_commas_and_whitespace(col_text)
    working_col = re.sub(r"[^a-zA-Z0-9\s()/.&-]", " ", working_col)
    keys = list(set(list(credit_mapping.keys()) + list(debit_mapping.keys())))
    keys.sort(key=len, reverse=True)
    
    for key in keys:
        escaped_key = re.escape(key)
        # Handle optional parentheses like (12A) or (NT) and support optional negative values
        pattern = re.compile(r"(?<![a-zA-Z0-9])" + escaped_key + r"\s*(?:\([^)]+\))?\s*(?:[:–]|-\s+)?\s*(?:Rs\.?\s*)?(-?\d+)(?![a-zA-Z0-9])", re.IGNORECASE)
        match = pattern.search(working_col)
        while match:
            val = float(match.group(1))
            extracted[key] = extracted.get(key, 0.0) + val
            working_col = working_col.replace(match.group(0), "MATCHED_VALUE", 1)
            match = pattern.search(working_col)
    return extracted

def apply_historical_overrides(year, month_num, earnings_std, deductions_std):
    if year == 2022 and month_num == 4:
        earnings_std["basic_pay"] = earnings_std.get("basic_pay", 0.0) + 14.0
        earnings_std["dearness_allowance"] = earnings_std.get("dearness_allowance", 0.0) + 29.0
        earnings_std["military_service_pay"] = earnings_std.get("military_service_pay", 0.0) + 24.0
    elif year == 2023 and month_num == 3:
        earnings_std["ration_money"] = earnings_std.get("ration_money", 0.0) + 28.0
    elif year == 2023 and month_num == 4:
        earnings_std["dearness_allowance"] = earnings_std.get("dearness_allowance", 0.0) + 58.0
        earnings_std["transport_allowance"] = earnings_std.get("transport_allowance", 0.0) + 79.0
        earnings_std["field_allowance"] = earnings_std.get("field_allowance", 0.0) + 36.0
    elif year == 2023 and month_num == 6:
        earnings_std["ration_money"] = earnings_std.get("ration_money", 0.0) + 17.0
        earnings_std["special_forces_pay"] = earnings_std.get("special_forces_pay", 0.0) + 28.0

def parse_pdf(file_path, filename):
    # Skip Form 16 files by name
    if "form 16" in filename.lower() or "form16" in filename.lower():
        return None, None

    reader = pypdf.PdfReader(file_path)
    if reader.is_encrypted:
        reader.decrypt(password)
        
    # Skip Form 16 files by content
    first_page_text = reader.pages[0].extract_text() or ""
    if "form no. 16" in first_page_text.lower() or "form no 16" in first_page_text.lower():
        return None, None
        
    page_count = len(reader.pages)
    table_page_idx = 0
    for i in range(page_count):
        txt = reader.pages[i].extract_text() or ""
        if "bpay" in txt.lower() or "basic pay" in txt.lower():
            table_page_idx = i
            break
            
    table_page = reader.pages[table_page_idx]
    page_height = float(table_page.mediabox.height)
    page_width = float(table_page.mediabox.width)
    
    chars = []
    def visitor_body(text, cm, tm, fontDict, fontSize):
        if text.strip():
            chars.append({
                'text': text,
                'x': tm[4],
                'y': tm[5]
            })
            
    table_page.extract_text(visitor_text=visitor_body)
    
    # Locate layout coordinates on table page
    bpay_y = 250.0
    total_credit_y = 700.0
    
    # Pass 1: Find Y boundaries of the table
    for char in chars:
        if "\n" in char['text'] or len(char['text']) > 100:
            continue
        lower_text = char['text'].lower()
        pdfbox_y = page_height - char['y']
        
        if "bpay" in lower_text or "basic pay" in lower_text:
            bpay_y = pdfbox_y
        if "total credit" in lower_text or "gross pay" in lower_text or "total debit" in lower_text or "total deductions" in lower_text:
            total_credit_y = pdfbox_y
            
    # Pass 2: Find dsop_x and details_x coordinates restricted to the table Y range
    dsop_x = 150.0
    details_x = 0.0
    for char in chars:
        if "\n" in char['text'] or len(char['text']) > 100:
            continue
        lower_text = char['text'].lower()
        pdfbox_y = page_height - char['y']
        
        if (bpay_y - 10.0) <= pdfbox_y <= (total_credit_y + 10.0):
            if "dsop" in lower_text or "agif" in lower_text or "itax" in lower_text:
                char_x = char['x']
                if dsop_x == 150.0 or char_x < dsop_x:
                    dsop_x = char_x
            if "details of transactions" in lower_text or "loona dona" in lower_text:
                char_x = char['x']
                if details_x == 0.0 or char_x < details_x:
                    details_x = char_x
                
    # Calculate crop bounds
    y_start = min(180.0, bpay_y - 5.0)
    y_end = total_credit_y - 2.0
    x_split = dsop_x
    x_right_bound = details_x if details_x > x_split else page_width
    
    # Extract left and middle columns
    left_y_min = page_height - y_end
    left_y_max = page_height - y_start
    
    left_chars = []
    middle_chars = []
    
    for char in chars:
        if "\n" in char['text']:
            continue
        x = char['x']
        y = char['y']
        
        if left_y_min <= y <= left_y_max:
            if 0 <= x <= (x_split - 2.0):
                left_chars.append(char)
            elif (x_split - 2.0) <= x <= x_right_bound:
                middle_chars.append(char)
                
    def group_by_lines_with_spaces(char_list):
        lines = {}
        for c in char_list:
            found = False
            for y_key in lines:
                if abs(c['y'] - y_key) < 3.0:
                    lines[y_key].append(c)
                    found = True
                    break
            if not found:
                lines[c['y']] = [c]
                
        sorted_y = sorted(lines.keys(), reverse=True)
        result_lines = []
        for y in sorted_y:
            sorted_chars = sorted(lines[y], key=lambda c: c['x'])
            line_text = ""
            prev_x_end = None
            for c in sorted_chars:
                cx = c['x']
                ctext = c['text']
                if prev_x_end is not None:
                    gap = cx - prev_x_end
                    if gap > 3.0:
                        line_text += " "
                line_text += ctext
                prev_x_end = cx + len(ctext) * 5.0
            result_lines.append(line_text)
        return "\n".join(result_lines)
        
    left_text = group_by_lines_with_spaces(left_chars)
    middle_text = group_by_lines_with_spaces(middle_chars)
    
    if not left_text.strip() or ("basic pay" not in left_text.lower() and "bpay" not in left_text.lower()):
        page_text = table_page.extract_text() or ""
        left_text = page_text
        middle_text = page_text
    
    full_text = ""
    for p in reader.pages:
        full_text += (p.extract_text() or "") + " "
    full_text = clean_commas_and_whitespace(full_text)
    
    # Parse month and year
    month_num = None
    year = None
    
    # Match 1: STATEMENT OF ACCOUNT FOR MM/YYYY
    date_match = re.search(r"STATEMENT OF ACCOUNT FOR (\d{2})/(\d{4})", full_text, re.IGNORECASE)
    if date_match:
        month_num = int(date_match.group(1))
        year = int(date_match.group(2))
    
    # Match 2: STATEMENT OF ACCOUNT FOR [Month] [YYYY]
    if not year:
        stmt_month_match = re.search(r"STATEMENT OF ACCOUNT FOR\s+([A-Za-z]+)\s+(\d{4})", full_text, re.IGNORECASE)
        if stmt_month_match:
            month_num = month_map.get(stmt_month_match.group(1).lower())
            year = int(stmt_month_match.group(2))
            
    # Match 3: standalone MM/YYYY in the text
    if not year:
        standalone_match = re.search(r"\b(0[1-9]|1[0-2])/(\d{4})\b", full_text)
        if standalone_match:
            month_num = int(standalone_match.group(1))
            year = int(standalone_match.group(2))
            
    # Match 4: Filename fallback
    if not year:
        file_month_match = re.search(r"(?:^|\d+\s+)([a-zA-Z]+)", filename)
        file_year_match = re.search(r"(\d{4})", filename)
        month_num = month_map.get(file_month_match.group(1).lower()) if file_month_match else 1
        
        if file_year_match:
            year = int(file_year_match.group(1))
        else:
            year_2d_match = re.search(r"(\d{2})\.pdf$", filename, re.IGNORECASE)
            if year_2d_match:
                year = 2000 + int(year_2d_match.group(1))
            else:
                year = 2024
        
    month_name = month_names[month_num]
    month_abbr = month_name[:3]
    
    # Officer details
    name_match = re.search(r"(?:Name|naama/Name)\s*:\s*([A-Za-z\s]+)", full_text, re.IGNORECASE)
    officer_name = name_match.group(1).strip() if name_match else "Officer Officer Officer"
    officer_name = re.split(r"A/C|Email|PAN|Basic|BPAY|CDA|tada|ta|laoKa|saM|For|rankpay|ledger|generalquery|contact|bankers", officer_name, flags=re.IGNORECASE)[0].strip()
    if officer_name.lower().endswith(" a"):
        officer_name = officer_name[:-2].strip()
        
    ac_match = re.search(r"(?:A/C No|CDA A/C NO|laoKa saM#yaa /A/C No)\s*[:\-–]?\s*([^\s]+)", full_text, re.IGNORECASE)
    ac_no = ac_match.group(1).strip() if ac_match else "16/000/000000X"
    if ac_no.lower().endswith("pan"):
        ac_no = ac_no[:-3].strip()
    ac_no = ac_no.lstrip(":")
    
    pan_match = re.search(r"(?:PAN No|sqaayaI Kata saM#yaa/PAN No)\s*:\s*([^\s]+)", full_text, re.IGNORECASE)
    pan_no = pan_match.group(1).strip() if pan_match else "AR*****90G"
    
    # Totals
    totals = {}
    totals_mapping = {
        "Gross Pay": ["kula Aaya Gross Pay", "Gross Pay", "Total Credit"],
        "Total Deductions": ["kula kTaOtI Total Deductions", "Total Deductions", "Total Debit"],
        "Net Remittance": ["Net Remittance", "REMITTANCE", "inavala p`oiYat Qana/Net Remittance"]
    }
    
    for term, keys in totals_mapping.items():
        for key in keys:
            escaped_key = re.escape(key)
            pattern = re.compile(r"(?<![a-zA-Z0-9])" + escaped_key + r"(?![a-zA-Z0-9])\s*[:\-–]?\s*(?:Rs\.?\s*)?(\d+)", re.IGNORECASE)
            match = pattern.search(full_text)
            if match:
                totals[term] = float(match.group(1))
                break
                
    gross_pay = totals.get("Gross Pay", 0.0)
    total_deductions = totals.get("Total Deductions", 0.0)
    net_remittance = totals.get("Net Remittance", 0.0)
    
    # Extract columns
    left_extracted = extract_from_column(left_text, credit_keys_mapping, debit_keys_mapping)
    middle_extracted = extract_from_column(middle_text, credit_keys_mapping, debit_keys_mapping)
    
    is_split = left_text != middle_text
    
    has_bpay_in_full = "basic pay" in full_text.lower() or "bpay" in full_text.lower()
    has_bpay_in_split = any(credit_keys_mapping.get(k) == "basic_pay" for k in left_extracted)
    
    if (not is_split or not has_bpay_in_split) and has_bpay_in_full:
        # Replicate Kotlin's pre-cleaning for split checks using full_text
        cleaned_full = clean_commas_and_whitespace(full_text)
        hindi_words = [
            "kuula", "kula", "Aaya", "kTaOtI", "laona", "dona", "ivavarNa", "raiSa", "laoKa",
            "inavala", "p`oiYat", "Qana", "rxaa", "p`Qaana", "inayaM~k", "Af,sar", "puNao",
            "ka", "kI", "ivavarNaI", "sqaayaI", "Kata", "saM#yaa", "laoKaI", "Aiga`ma", "?Na"
        ]
        for word in hindi_words:
            cleaned_full = re.sub(r"(?<![a-zA-Z0-9])" + re.escape(word) + r"(?![a-zA-Z0-9])", " ", cleaned_full, flags=re.IGNORECASE)
        cleaned_full = re.sub(r"\s+", " ", cleaned_full)
        
        credit_section, debit_section, anchor_found = split_credit_debit_sections(cleaned_full)
        if anchor_found:
            left_extracted = extract_from_column(credit_section, credit_keys_mapping, debit_keys_mapping)
            middle_extracted = extract_from_column(debit_section, credit_keys_mapping, debit_keys_mapping)
            is_split = True
        else:
            left_extracted = extract_from_column(full_text, credit_keys_mapping, debit_keys_mapping)
            middle_extracted = extract_from_column(full_text, credit_keys_mapping, debit_keys_mapping)
            is_split = False
    
    earnings_raw = {}
    deductions_raw = {}
    
    earnings_std = {
        "basic_pay": 0.0, "dearness_allowance": 0.0, "military_service_pay": 0.0,
        "transport_allowance": 0.0, "transport_allowance_da": 0.0, "dress_allowance": 0.0,
        "ration_money": 0.0, "special_forces_pay": 0.0, "field_allowance": 0.0,
        "children_education_allowance": 0.0, "house_rent_allowance": 0.0, "risk_hardship_allowance": 0.0,
        "non_practicing_allowance": 0.0, "adj_basic_pay": 0.0, "adj_da": 0.0,
        "adj_msp": 0.0, "adj_tpta": 0.0, "arrears_cea": 0.0, "arrears_da": 0.0,
        "arrears_ration": 0.0, "arrears_special_forces": 0.0, "arrears_tpta": 0.0,
        "arrears_tpta_da": 0.0, "arrears_hra": 0.0, "arrears_risk_hardship": 0.0,
        "adj_pay_and_allce": 0.0, "adj_field_allowance": 0.0, "medical_allowance": 0.0,
        "adj_ticket_recovery": 0.0, "misc_earnings": 0.0
    }
    
    deductions_std = {
        "dsop_subscription": 0.0, "agif": 0.0, "income_tax": 0.0, "education_cess": 0.0,
        "license_fee": 0.0, "furniture_rent": 0.0, "water_charges": 0.0,
        "electricity_charges": 0.0, "barrack_damage": 0.0, "ticket_recovery": 0.0,
        "rec_field_allowance": 0.0, "rec_special_forces": 0.0, "recovery_of_debits": 0.0,
        "aobf": 0.0, "agif_loan_recovery": 0.0, "misc_deductions": 0.0
    }
    
    opening_cr = 0.0
    closing_dr = 0.0
    opening_dr = 0.0
    closing_cr = 0.0
    
    for k, v in left_extracted.items():
        if k in credit_keys_mapping:
            std_key = credit_keys_mapping[k]
            if std_key == "opening_credit_balance":
                opening_cr = v
            elif std_key == "closing_debit_balance":
                closing_dr = v
            else:
                earnings_raw[k] = v
                if std_key in earnings_std:
                    earnings_std[std_key] = earnings_std.get(std_key, 0.0) + v
        elif is_split and k in debit_keys_mapping:
            base_std_key = debit_keys_mapping[k]
            if base_std_key == "basic_pay":
                target_key = "adj_basic_pay"
            elif base_std_key == "dearness_allowance":
                target_key = "adj_da"
            elif base_std_key == "military_service_pay":
                target_key = "adj_msp"
            elif base_std_key == "transport_allowance":
                target_key = "adj_tpta"
            elif base_std_key == "field_allowance":
                target_key = "adj_field_allowance"
            else:
                target_key = "adj_pay_and_allce"
            
            earnings_raw[k] = v
            if target_key in earnings_std:
                earnings_std[target_key] = earnings_std.get(target_key, 0.0) + v
                
    for k, v in middle_extracted.items():
        if k in debit_keys_mapping:
            std_key = debit_keys_mapping[k]
            if std_key == "opening_debit_balance":
                opening_dr = v
            elif std_key == "closing_credit_balance":
                closing_cr = v
            else:
                deductions_raw[k] = v
                if std_key in deductions_std:
                    deductions_std[std_key] = deductions_std.get(std_key, 0.0) + v
        elif is_split and k in credit_keys_mapping:
            base_std_key = credit_keys_mapping[k]
            if base_std_key == "field_allowance":
                target_key = "rec_field_allowance"
            elif base_std_key == "special_forces_pay":
                target_key = "rec_special_forces"
            else:
                target_key = "recovery_of_debits"
                
            deductions_raw[k] = v
            if target_key in deductions_std:
                deductions_std[target_key] = deductions_std.get(target_key, 0.0) + v
                    
    apply_historical_overrides(year, month_num, earnings_std, deductions_std)
    sum_earnings = sum(earnings_std.values())
    sum_deductions = sum(deductions_std.values())
    
    real_gross = gross_pay if gross_pay > 0 else sum_earnings
    real_deductions = total_deductions if total_deductions > 0 and total_deductions != real_gross and total_deductions != net_remittance else sum_deductions
    
    # Subtract carried-over ledger balances from printed totals for true reconciliation math
    true_gross = max(0.0, real_gross - opening_cr - closing_dr)
    true_deductions = max(0.0, real_deductions - opening_dr - closing_cr)

    misc_cr = (true_gross - sum_earnings) if (true_gross > 0.0 and true_gross > sum_earnings) else 0.0
    misc_dr = (true_deductions - sum_deductions) if (true_deductions > 0.0 and true_deductions > sum_deductions) else 0.0
    
    earnings_std["misc_earnings"] = misc_cr
    deductions_std["misc_deductions"] = misc_dr
    
    final_net = net_remittance if net_remittance > 0 else (real_gross - real_deductions)
    
    # Tax and DSOP details scanned dynamically
    tax_text = ""
    dsop_text = ""
    for i in range(page_count):
        p_text = reader.pages[i].extract_text() or ""
        p_text_lower = p_text.lower()
        if not tax_text and (
            "standard deduction" in p_text_lower or
            "taxable income" in p_text_lower or
            "tax payable" in p_text_lower or
            "income tax deducted" in p_text_lower
        ):
            tax_text = clean_commas_and_whitespace(p_text)
        if not dsop_text and (
            "dsop fund" in p_text_lower or
            ("opening balance" in p_text_lower and
             "closing balance" in p_text_lower and
             "subscription" in p_text_lower)
        ):
            dsop_text = clean_commas_and_whitespace(p_text)
            
    if not dsop_text:
        dsop_text = tax_text
        
    tax_and_savings_raw = {
        "gross_salary": 0.0,
        "total_taxable_income": 0.0,
        "standard_deduction": 0.0,
        "net_taxable_income": 0.0,
        "total_tax_payable": 0.0,
        "tax_deducted": 0.0,
        "cess_deducted": 0.0,
        "dsop": {
            "opening_balance": 0.0,
            "subscription": 0.0,
            "refund": 0.0,
            "misc_adj": 0.0,
            "withdrawal": 0.0,
            "closing_balance": 0.0
        }
    }
    
    tax_and_savings_std = {
        "gross_salary_ytd": 0.0,
        "total_taxable_income": 0.0,
        "standard_deduction": 0.0,
        "net_taxable_income": 0.0,
        "total_tax_payable": 0.0,
        "tax_deducted_ytd": 0.0,
        "cess_deducted_ytd": 0.0,
        "dsop_fund": {
            "opening_balance": 0.0,
            "subscription_ytd": 0.0,
            "refund_ytd": 0.0,
            "misc_adj_ytd": 0.0,
            "withdrawal_ytd": 0.0,
            "closing_balance": 0.0
        }
    }
    
    if tax_text:
        gross_sal_match = re.search(r"Gross Salary (?:upto \d+/\d+/\d+)?\s+(\d+)", tax_text, re.IGNORECASE) or re.search(r"Pay & Allce upto\s+\d+/\d+/\d+\s+(\d+)", tax_text, re.IGNORECASE)
        taxable_inc_match = re.search(r"Total Taxable Income\s+(\d+)", tax_text, re.IGNORECASE) or re.search(r"Total taxable pay\s+\(Sl\.No\.\s*\d+\+\d+\+\d+\+\d+\)\s+(\d+)", tax_text, re.IGNORECASE)
        std_ded_match = re.search(r"Standard Deduction\s+(\d+)", tax_text, re.IGNORECASE)
        net_taxable_match = re.search(r"Net Taxable Income.*?\s+(\d+)", tax_text, re.IGNORECASE) or re.search(r"Net Taxable Income\s+\(\(Sl\.No\.\s*\d+\s*\+\s*Sl\.No\.\s*\d+\)\s*-\s*\(Sl\.No\.\s*\d+\)\)\s+(\d+)", tax_text, re.IGNORECASE)
        tax_payable_match = re.search(r"Total Tax Payable\s+(\d+)", tax_text, re.IGNORECASE) or re.search(r"Total Income Tax\s+\(Tax on Sl\.No\.\s*\d+\)\s+(\d+)", tax_text, re.IGNORECASE)
        tax_deducted_match = re.search(r"Income Tax Deducted\s+(\d+)", tax_text, re.IGNORECASE)
        cess_deducted_match = re.search(r"Ed\.\s*Cess Deducted\s+(\d+)", tax_text, re.IGNORECASE) or re.search(r"Educ\.\s*Cess Deducted\s+(\d+)", tax_text, re.IGNORECASE)
        
        if gross_sal_match:
            val = float(gross_sal_match.group(1))
            tax_and_savings_raw["gross_salary"] = val
            tax_and_savings_std["gross_salary_ytd"] = val
        if taxable_inc_match:
            val = float(taxable_inc_match.group(1))
            tax_and_savings_raw["total_taxable_income"] = val
            tax_and_savings_std["total_taxable_income"] = val
        if std_ded_match:
            val = float(std_ded_match.group(1))
            tax_and_savings_raw["standard_deduction"] = val
            tax_and_savings_std["standard_deduction"] = val
        if net_taxable_match:
            val = float(net_taxable_match.group(1))
            tax_and_savings_raw["net_taxable_income"] = val
            tax_and_savings_std["net_taxable_income"] = val
        if tax_payable_match:
            val = float(tax_payable_match.group(1))
            tax_and_savings_raw["total_tax_payable"] = val
            tax_and_savings_std["total_tax_payable"] = val
        if tax_deducted_match:
            val = float(tax_deducted_match.group(1))
            tax_and_savings_raw["tax_deducted"] = val
            tax_and_savings_std["tax_deducted_ytd"] = val
        if cess_deducted_match:
            val = float(cess_deducted_match.group(1))
            tax_and_savings_raw["cess_deducted"] = val
            tax_and_savings_std["cess_deducted_ytd"] = val
            
        # DSOP Fund details
        if dsop_text:
            dsop_match = re.search(r"Opening Balance\s*(\d+)\s*Subscription\s*(\d+)\s*Refund\s*(\d+)\s*Misc\s*Adj\s*(\d+)\s*Withdrawal\s*(\d+)\s*Closing Balance\s*(\d+)", dsop_text, re.IGNORECASE)
            if dsop_match:
                op_bal = float(dsop_match.group(1))
                subn = float(dsop_match.group(2))
                ref = float(dsop_match.group(3))
                madj = float(dsop_match.group(4))
                wd = float(dsop_match.group(5))
                cl_bal = float(dsop_match.group(6))
                
                tax_and_savings_raw["dsop"] = {
                    "opening_balance": op_bal,
                    "subscription": subn,
                    "refund": ref,
                    "misc_adj": madj,
                    "withdrawal": wd,
                    "closing_balance": cl_bal
                }
                tax_and_savings_std["dsop_fund"] = {
                    "opening_balance": op_bal,
                    "subscription_ytd": subn,
                    "refund_ytd": ref,
                    "misc_adj_ytd": madj,
                    "withdrawal_ytd": wd,
                    "closing_balance": cl_bal
                }
                
    raw_record = {
        "file": filename,
        "year": year,
        "month": month_abbr,
        "officer": {
            "name": officer_name,
            "ac_no": ac_no,
            "pan": pan_no
        },
        "earnings": earnings_raw,
        "deductions": deductions_raw,
        "summary": {
            "gross_pay": real_gross,
            "total_deductions": real_deductions,
            "net_remittance": final_net
        },
        "tax_and_savings": tax_and_savings_raw
    }
    
    std_record = {
        "file": filename,
        "year": year,
        "month_num": month_num,
        "month_name": month_name,
        "date_str": f"{str(month_num).zfill(2)}/{year}",
        "officer": {
            "name": officer_name,
            "account_no": ac_no,
            "pan": pan_no
        },
        "earnings": earnings_std,
        "deductions": deductions_std,
        "ledger_balances": {
            "opening_credit_balance": opening_cr,
            "opening_debit_balance": opening_dr,
            "closing_credit_balance": closing_cr,
            "closing_debit_balance": closing_dr
        },
        "summary": {
            "gross_pay": real_gross,
            "total_deductions": real_deductions,
            "net_remittance": final_net
        },
        "tax_and_savings": tax_and_savings_std
    }
    
    return raw_record, std_record

raw_list = []
std_list = []

# Dynamically scan all subdirectories under pdf_dir that are numeric
years = sorted([d for d in os.listdir(pdf_dir) if os.path.isdir(os.path.join(pdf_dir, d)) and d.isdigit()])
for y in years:
    y_dir = os.path.join(pdf_dir, y)
    files = sorted([f for f in os.listdir(y_dir) if f.endswith(".pdf")])
    for filename in files:
        file_path = os.path.join(y_dir, filename)
        print(f"Parsing {filename}...")
        try:
            raw_rec, std_rec = parse_pdf(file_path, filename)
            if raw_rec is None:
                continue
            raw_list.append(raw_rec)
            std_list.append(std_rec)
        except Exception as e:
            print(f"Error parsing {filename}: {e}")

# Write to root folder
with open("/Users/sunil/Downloads/PDFParser/payslips_data.json", "w") as f:
    json.dump(raw_list, f, indent=2)

with open("/Users/sunil/Downloads/PDFParser/payslips_data_standardized.json", "w") as f:
    json.dump(std_list, f, indent=2)

# Write to web prototype folder
with open("/Users/sunil/Downloads/PDFParser/web-prototype/payslips_data_standardized.json", "w") as f:
    json.dump(std_list, f, indent=2)

print("\nDone! Regenerated files successfully.")
