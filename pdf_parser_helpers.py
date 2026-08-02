import re
import pypdf
from pdf_mappings import credit_keys_mapping, debit_keys_mapping, month_map, month_names

def clean_commas_and_whitespace(text):
    cleaned = re.sub(r"(\d),(\d)", r"\1\2", text)
    return re.sub(r"\s+", " ", cleaned)

def split_credit_debit_sections(cleaned_text):
    credit_anchor_match = re.search(r"(\bBPAY\b|\bBasic Pay\b)", cleaned_text, re.IGNORECASE)
    debit_anchor_match = re.search(r"(\bDSOPF Subn\b|\bDSOP\b|\bAGIF\b|\bIncm Tax\b|\bITAX\b)", cleaned_text, re.IGNORECASE)
    
    if credit_anchor_match and debit_anchor_match:
        credit_idx = credit_anchor_match.start()
        debit_idx = debit_anchor_match.start()
        
        if credit_idx < debit_idx:
            credit_section = cleaned_text[credit_idx:debit_idx]
            debit_section = cleaned_text[debit_idx:]
            return credit_section, debit_section, True

    return cleaned_text, cleaned_text, False

def extract_from_column(col_text, credit_mapping, debit_mapping):
    extracted = {}
    combined_mapping = {**credit_mapping, **debit_mapping}
    
    for key in combined_mapping:
        escaped_key = re.escape(key)
        pattern = re.compile(r"(?<![a-zA-Z0-9])" + escaped_key + r"(?![a-zA-Z0-9])\s*[:\-–]?\s*(?:Rs\.?\s*)?(\d+)", re.IGNORECASE)
        match = pattern.search(col_text)
        if match:
            val = float(match.group(1))
            extracted[key] = val
    return extracted

def should_skip_file(reader, filename):
    if "form 16" in filename.lower() or "form16" in filename.lower():
        return True
    first_page_text = reader.pages[0].extract_text() or ""
    if "form no. 16" in first_page_text.lower() or "form no 16" in first_page_text.lower():
        return True
    return False

def find_table_page(reader):
    page_count = len(reader.pages)
    for i in range(page_count):
        txt = reader.pages[i].extract_text() or ""
        if "bpay" in txt.lower() or "basic pay" in txt.lower():
            return i
    return 0

def locate_table_bounds(chars, page_height):
    bpay_y = 250.0
    total_credit_y = 700.0
    for char in chars:
        if "\n" in char['text'] or len(char['text']) > 100:
            continue
        lower_text = char['text'].lower()
        pdfbox_y = page_height - char['y']
        if "bpay" in lower_text or "basic pay" in lower_text:
            bpay_y = pdfbox_y
        if "total credit" in lower_text or "gross pay" in lower_text or "total debit" in lower_text or "total deductions" in lower_text:
            total_credit_y = pdfbox_y
            
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

    y_start = min(180.0, bpay_y - 5.0)
    y_end = total_credit_y - 2.0
    x_split = dsop_x
    return y_start, y_end, x_split, details_x

def extract_columns_text(chars, bounds, page_width, page_height):
    y_start, y_end, x_split, details_x = bounds
    x_right_bound = details_x if details_x > x_split else page_width
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

    left_text = group_chars_by_lines(left_chars)
    middle_text = group_chars_by_lines(middle_chars)
    return left_text, middle_text

def group_chars_by_lines(char_list):
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
                if (cx - prev_x_end) > 3.0:
                    line_text += " "
            line_text += ctext
            prev_x_end = cx + len(ctext) * 5.0
        result_lines.append(line_text)
    return "\n".join(result_lines)

def parse_date_and_officer(full_text, filename):
    month_num = None
    year = None
    date_match = re.search(r"STATEMENT OF ACCOUNT FOR (\d{2})/(\d{4})", full_text, re.IGNORECASE)
    if date_match:
        month_num = int(date_match.group(1))
        year = int(date_match.group(2))
    if not year:
        stmt_month_match = re.search(r"STATEMENT OF ACCOUNT FOR\s+([A-Za-z]+)\s+(\d{4})", full_text, re.IGNORECASE)
        if stmt_month_match:
            month_num = month_map.get(stmt_month_match.group(1).lower())
            year = int(stmt_month_match.group(2))
    if not year:
        standalone_match = re.search(r"\b(0[1-9]|1[0-2])/(\d{4})\b", full_text)
        if standalone_match:
            month_num = int(standalone_match.group(1))
            year = int(standalone_match.group(2))
    if not year:
        file_month_match = re.search(r"(?:^|\d+\s+)([a-zA-Z]+)", filename)
        file_year_match = re.search(r"(\d{4})", filename)
        month_num = month_map.get(file_month_match.group(1).lower()) if file_month_match else 1
        if file_year_match:
            year = int(file_year_match.group(1))
        else:
            year_2d_match = re.search(r"(\d{2})\.pdf$", filename, re.IGNORECASE)
            year = 2000 + int(year_2d_match.group(1)) if year_2d_match else 2024
            
    month_name = month_names[month_num]
    month_abbr = month_name[:3]

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
    return year, month_num, month_name, month_abbr, officer_name, ac_no, pan_no

def parse_totals(full_text):
    totals = {}
    totals_mapping = {
        "Gross Pay": ["kula Aaya Gross Pay", "Gross Pay", "Total Credit"],
        "Total Deductions": ["kula kTaOtI Total Deductions", "Total Deductions", "Total Debit"],
        "Net Remittance": ["Net Remittance", "REMITTANCE", "inavala p`oiYat Qana/Net Remittance"]
    }
    for term, keys in totals_mapping.items():
        for key in keys:
            pattern = re.compile(r"(?<![a-zA-Z0-9])" + re.escape(key) + r"(?![a-zA-Z0-9])\s*[:\-–]?\s*(?:Rs\.?\s*)?(\d+)", re.IGNORECASE)
            match = pattern.search(full_text)
            if match:
                totals[term] = float(match.group(1))
                break
    return totals
