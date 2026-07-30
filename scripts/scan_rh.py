import os
import re
import pypdf

pdf_dir = "/Users/test/Desktop/Pay Slip Elements"
password = os.environ.get("PDF_DECRYPTION_PASSWORD", "535d04")

# Mappings from regenerate_json
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

rh_lines = []
unmapped_terms = {}

for root_dir, dirs, files in os.walk(pdf_dir):
    for filename in sorted(files):
        if not filename.endswith(".pdf"):
            continue
        file_path = os.path.join(root_dir, filename)
        
        try:
            reader = pypdf.PdfReader(file_path)
            if reader.is_encrypted:
                reader.decrypt(password)
                
            for page_num, page in enumerate(reader.pages):
                text = page.extract_text() or ""
                lines = text.split("\n")
                for line in lines:
                    if "RH" in line or "rh" in line.lower():
                        rh_lines.append(f"{filename} (p.{page_num+1}): {line.strip()}")
                    
                    # Look for potential key-value pairs in the line: e.g. "LABEL VALUE"
                    # We match uppercase codes like TPTA, RSHNA, RH12, etc. or words
                    match = re.search(r"\b([A-Z0-9/&_-]+(?:\s+[A-Z0-9/&_-]+)?)\s+([0-9]{3,7})\b", line)
                    if match:
                        key = match.group(1).strip()
                        val = int(match.group(2))
                        
                        # filter
                        if key.lower() not in known_keys and not key.isdigit():
                            if not any(c.isalpha() for c in key):
                                continue
                            if key.lower() in ["gross", "deductions", "remittance", "cda", "pan", "year", "month"]:
                                continue
                            if key not in unmapped_terms:
                                unmapped_terms[key] = []
                            unmapped_terms[key].append((filename, val))
                            
        except Exception as e:
            print(f"Error reading {filename}: {e}")

# Write to output file
output_path = "/Users/test/Downloads/PDFParser/scripts/scan_rh_output.txt"
with open(output_path, "w") as f:
    f.write("=== RH LINES FOUND ===\n")
    for rhl in sorted(rh_lines):
        f.write(rhl + "\n")
        
    f.write("\n=== POTENTIAL UNMAPPED UPPERCASE CODES IN LINES ===\n")
    sorted_terms = sorted(unmapped_terms.items(), key=lambda x: len(x[1]), reverse=True)
    for key, occs in sorted_terms:
        f.write(f"Key: {key} (Count: {len(occs)})\n")
        for fn, val in occs[:10]:
            f.write(f"  - {fn}: {val}\n")

print(f"Done! Scanned {len(rh_lines)} RH lines and found {len(unmapped_terms)} unique unmapped terms.")
print(f"Output written to {output_path}")
