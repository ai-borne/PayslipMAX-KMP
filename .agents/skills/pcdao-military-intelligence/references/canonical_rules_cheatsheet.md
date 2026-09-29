# Canonical PCDA(O) Rules & Financial Intelligence Cheatsheet

This cheatsheet provides the quick reference for the 378 canonical rules and 9 canonical data packs
powering the PayslipMax military financial intelligence moat.

---

## 1. The 9 Canonical Data Packs

| JSON File Name | Scope & Authority | Key Constants / Rates |
|---|---|---|
| `pay_matrix_7th_cpc.json` | Army Officers Pay Rules 2017 | Levels 10–18, 40 stages, IOR 2.67 (12A/13), MSP ₹15,500 |
| `transport_allowance_rates.json` | MoD 1(25)/2017/D(Pay/Services) | Higher UA ₹7,200+DA, Other ₹3,600+DA, Divyang 2.0x |
| `risk_hardship_rates.json` | MoD 1(16)/2017/D(Pay/Services) | Siachen RH-MAX ₹42,500, HAFAA ₹16,900, CFAA ₹10,500 |
| `allowances_and_additions.json` | Special allowances & gallantry | Gallantry ₹20k/mo, Flying ₹25k/mo, Technical ₹3k–4.5k |
| `travel_tada_rates.json` | MoD travel & transfer orders | Daily allowance, Hotel limits, CTG (80% Basic Pay) |
| `canonical_pcdao_rules.json` | 378 core statutory rules | Checklist mapping, dispute authorities, recovery risks |
| `special_compensatory_allowances.json` | Remote & border location rules | SDA (10% BP), Tough Location Allowance (TLA I/II/III) |
| `service_conditions_and_funds.json` | Statutory funds & welfare | DSOPF ₹5L tax cap (Sec 10(11)), Rule 14 stoppage, AGIF |
| `promotion_and_pcdao_workflows.json` | Army Rules 10 & 11 | DNI cycles (1 Jan / 1 July), 30-day statutory election |

---

## 2. 5 Standard Rule Action Types

1. **`ENTITLEMENT_UNDERPAID`**:
   - Statutory allowance omitted or credited below MoD rate (e.g. Higher UA TPTA underpaid).
   - Mathematical diff: `[Entitled] - [Credited] = [Net Due]`.
2. **`RECOVERY_HAZARD`**:
   - Mutually exclusive allowances credited concurrently.
   - PCDA audits debit principal + **18% penal interest**.
3. **`FORFEITURE_RISK`**:
   - Time-barred statutory claims (SPR 60d, LTC 30d, TD 60d, CTG 180d).
   - Prompts Condonation of Delay application once time-barred.
4. **`TAX_EXPOSURE`**:
   - Annual DSOPF subscription exceeding ₹5,00,000 threshold under Section 10(11).
   - Computes taxable interest on excess.
5. **`PROMOTION_OPTION`**:
   - Pay Fixation Rule 10/11 optimization: Option 1 (Promotion Date) vs Option 2 (DNI).
   - Generates 36-month net cumulative delta and statutory 30-day deadline alert.

---

## 3. Mutual Exclusion Collision Pairs

| Claim 1 | Claim 2 | Collision Reason & Statutory Citation | Penalty |
|---|---|---|---|
| **TPTA** | **Field Area (HAFAA / CFAA)** | Government conveyance provided in field (TR-230(B)) | Principal + 18% penal interest |
| **SDA (10%)** | **Tough Location (TLA)** | SCA(PFE) cannot be drawn with SDA concurrently | Excess recovery |
| **HRA** | **Married Quarters (MQ)** | Cannot draw HRA while allotted govt accommodation | License fee + HRA recovery |
| **Flying Pay** | **Special Forces Pay** | Mutually exclusive specialized duty allowances | Full debit of lower allowance |

---

## 4. Central DA Escalation Triggers (DA $\ge$ 50%)

When Central Dearness Allowance reaches or exceeds **50%**, 7th CPC statutory triggers apply:
- **Children Education Allowance (CEA)**: ₹2,250/mo (₹27,000/yr) $\rightarrow$ **₹2,812.50/mo (₹33,750/yr)** (+25%).
- **Hostel Subsidy**: ₹6,750/mo (₹81,000/yr) $\rightarrow$ **₹8,437.50/mo (₹1,01,250/yr)** (+25%).
- **Divyang Child CEA**: ₹54,000/yr $\rightarrow$ **₹67,500/yr** (2.0x normal).
- **Dress Allowance**: ₹20,000/yr $\rightarrow$ **₹25,000/yr** (+25%).
- **House Rent Allowance (HRA)**: X/Y/Z rates escalate to **30% / 20% / 10%**.

---

## 5. PCDA(O) Pune Task Desks & Ledger Routing

- **Mailing Address**: The Principal Controller of Defence Accounts (Officers), Golibar Maidan, Pune – 411001.
- **Section R**: Regimental Officers (Infantry, Armoured, Artillery, Mech Inf).
- **Section L-1**: Logistics, ASC, AOC, EME officers.
- **Section T**: Travel & TA/DA claims, CTG, Conveyance claims.
- **Section F**: DSOPF, Fund advances, final withdrawals, and interest reconciliations.
