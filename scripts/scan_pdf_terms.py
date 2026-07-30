import os
import re
import pypdf

pdf_dir = "/Users/test/Desktop/Pay Slip Elements"
password = os.environ.get("PDF_DECRYPTION_PASSWORD", "535d04")

# Load current mappings from regenerate_json
credit_keys_mapping = {
    "Basic Pay": "basic_pay", "BPAY": "basic_pay", "DA": "dearness_allowance", "MSP": "military_service_pay",
    "Tpt Allc": "transport_allowance", "TPTA": "transport_allowance", "TRAN1": "transport_allowance", "TRAN-1": "transport_allowance",
    "TPTADA": "transport_allowance_da", "Tpt DA": "transport_allowance_da", "DRESALW": "dress_allowance",
    "A/o DressAllowance": "dress_allowance", "RSHNA": "ration_money", "RMONEYAllce-RA": "ration_money",
    "RA": "ration_money", "RH12": "ration_money", "SpCmd Pay": "special_forces_pay", "SPCDO": "special_forces_pay",
    "SC": "special_forces_pay", "FD": "field_allowance", "CEA": "children_education_allowance",
    "C E A(NT)": "children_education_allowance", "C E A (T)": "children_education_allowance", "C E A": "children_education_allowance",
    "ARR-CEA": "arrears_cea", "ARR-DA": "arrears_da", "ARR-RSHNA": "arrears_ration", "ARR-RH11": "arrears_ration",
    "ARR-RH12": "arrears_ration", "ARR-SPCDO": "arrears_special_forces", "ARR-TPTA": "arrears_tpta",
    "ARR-TPTADA": "arrears_tpta_da", "ARR-HH32": "arrears_hra", "A/o BPAY-": "adj_basic_pay", "A/o DA-": "adj_da",
    "A/o MSP-": "adj_msp", "A/o TRAN-1": "adj_tpta", "A/o TRAN-2": "adj_tpta", "A/o Pay & Allce": "adj_pay_and_allce",
    "A/o FIELD-R1": "adj_field_allowance", "ETKT-ref": "ticket_recovery", "MEDICAL": "medical_allowance",
    "Op Cr Bal": "opening_credit_balance", "Cl. Dr. Bal.": "closing_debit_balance", "Clos Bal(-)": "closing_debit_balance"
}

debit_keys_mapping = {
    "DSOPF Subn": "dsop_subscription", "DSOP": "dsop_subscription", "AGIF": "agif", "Incm Tax": "income_tax",
    "ITAX": "income_tax", "Educ Cess": "education_cess", "EHCESS": "education_cess", "L Fee": "license_fee",
    "LF": "license_fee", "Fur": "furniture_rent", "FUR": "furniture_rent", "Water": "water_charges",
    "WATER": "water_charges", "Elec": "electricity_charges", "Barrack Damage": "barrack_damage",
    "Dr Barrack Damage": "barrack_damage", "ETKT": "ticket_recovery", "R/o Etkt": "ticket_recovery",
    "Rec CIA-FD": "rec_field_allowance", "Rec PARA-SC": "rec_special_forces", "Op Dr Bal": "opening_debit_balance",
    "Cl. Cr. Bal.": "closing_credit_balance", "Clos Bal(+)": "closing_credit_balance", "OP Bal(-)": "opening_debit_balance",
    "R/o Of /Drs": "recovery_of_debits"
}

known_keys = set(k.lower() for k in list(credit_keys_mapping.keys()) + list(debit_keys_mapping.keys()))

# Simple text cleaning
def clean_text(text):
    cleaned = re.sub(r"(\d),(\d)", r"\1\2", text)
    return re.sub(r"\s+", " ", cleaned)

rh_occurrences = []
potential_unmapped = {}

for root_dir, dirs, files in os.walk(pdf_dir):
    for filename in sorted(files):
        if not filename.endswith(".pdf"):
            continue
        file_path = os.path.join(root_dir, filename)
        
        try:
            reader = pypdf.PdfReader(file_path)
            if reader.is_encrypted:
                reader.decrypt(password)
                
            full_text = ""
            for p in reader.pages:
                full_text += (p.extract_text() or "") + "\n"
                
            cleaned = clean_text(full_text)
            
            # Find RH occurrences
            for line in full_text.split("\n"):
                if "RH" in line or "rh" in line.lower():
                    rh_occurrences.append((filename, line.strip()))
            
            # Find potential unmapped keys. 
            # We look for patterns like: LABEL followed by a number, e.g. "LABEL: 1234" or "LABEL 1234"
            # Label can contain letters, numbers, slash, hyphen, or spaces.
            # Let's extract any word sequence followed by space/colon/dash and a number.
            matches = re.finditer(r"(?<![a-zA-Z0-9])([A-Za-z0-9/-]{2,}(?:\s+[A-Za-z0-9/-]+)?)\s*[:\-–]?\s*(\d+)(?![a-zA-Z0-9])", cleaned)
            for m in matches:
                key = m.group(1).strip()
                val = int(m.group(2))
                
                # Filter out obvious non-keys (like dates, account numbers, totals, common words)
                if len(key) < 2 or key.lower() in known_keys:
                    continue
                if re.match(r"^\d+$", key): # just digits
                    continue
                # Skip common structural words or months
                if key.lower() in [
                    "gross pay", "total deductions", "net remittance", "remittance", 
                    "cda", "pan", "account", "no", "statement", "for", "year", "month",
                    "january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december",
                    "jan", "feb", "mar", "apr", "jun", "jul", "aug", "sep", "oct", "nov", "dec",
                    "name", "email", "mobile", "rank", "cda a/c no", "a/c no", "pan no"
                ]:
                    continue
                
                # Check if it has letters
                if not any(c.isalpha() for c in key):
                    continue
                
                # Store
                if key not in potential_unmapped:
                    potential_unmapped[key] = []
                potential_unmapped[key].append((filename, val))
                
        except Exception as e:
            print(f"Error reading {filename}: {e}")

print("=== RH OCCURRENCES ===")
for fn, line in rh_occurrences[:100]:
    print(f"[{fn}]: {line}")

print("\n=== POTENTIAL UNMAPPED KEYS ===")
# Sort by frequency of appearance
sorted_unmapped = sorted(potential_unmapped.items(), key=lambda x: len(x[1]), reverse=True)
for key, occs in sorted_unmapped:
    # Print key and the first 3 files it appeared in
    files_str = ", ".join(f"{fn} ({val})" for fn, val in occs[:5])
    print(f"Key: '{key}' (Count: {len(occs)}) -> Examples: {files_str}")
