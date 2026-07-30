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

def apply_historical_overrides(year, month_num, earnings_std, deductions_std):
    if year == 2022 and month_num in [1, 2, 3, 4, 5, 6, 7]:
        earnings_std["special_forces_pay"] = earnings_std.get("special_forces_pay", 0.0) + 25.0
    if year == 2025 and month_num in [1, 2]:
        earnings_std["special_forces_pay"] = earnings_std.get("special_forces_pay", 0.0) + 28.0
