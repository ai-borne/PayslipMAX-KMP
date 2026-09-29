# PCDA(O) Authority Handbooks & Extraction Pipeline

This document explains the provenance of the authority handbooks and the extraction pipeline
governing the 378 canonical rules and 9 data packs.

---

## 1. Primary Authority Documents

All canonical rules, rates, and workflows originate from official Ministry of Defence (MoD)
and PCDA(O) Pune statutory publications:

| Document Title | Key Coverage | Used By Extractor |
|---|---|---|
| **Handbook of Pay and Allowances for Officers (2023)** | 7th CPC Pay Matrix, MSP, TPTA, Risk & Hardship, Gallantry, Field Allowances, DSOPF | `pay_matrix_extractor.py`, `risk_hardship_extractor.py`, `transport_allowance_extractor.py`, `allowances_extractor.py` |
| **Handbook of Travelling Allowances for Officers (2023)** | Permanent Posting (CTG), Temporary Duty (TD/DA), Baggage Entitlements, LTC, SPR claims | `travel_tada_extractor.py` |
| **Army Officers Pay Rules 2017 (Rule 10 & 11)** | Promotion Pay Fixation (Option 1 vs Option 2), DNI cycles, Bunching provisions | `promotion_workflow_extractor.py`, `PayFixationOptimizer.kt` |
| **MoD Compendium of Entitlements & MoD Circulars** | Siachen orders, Central DA escalation ($\ge 50\%$), Special Duty Allowance (SDA) | `special_compensatory_extractor.py`, `rules_sanitizer.py` |

---

## 2. Who Updates the Rules and When?

- **Who**: You and the engineering team, assisted by the AI agent via the `pcdao-military-intelligence` skill.
- **When**:
  1. **Biannual Central DA Revisions** (January & July) triggering statutory 25% escalations.
  2. **Annual PCDA(O) Circular Releases** (new FAQs, documentation checklists, or procedural advisories).
  3. **New MoD Entitlement Letters** (e.g., revised travel rules, updated Siachen/RH rates).
  4. **8th Central Pay Commission (8th CPC)** major matrix overhaul.

---

## 3. How the Update Pipeline Runs

To ingest new rules or handbook editions:

```bash
# 1. Update the extractor logic or page references in:
#    scripts/pcdao_factory/extractors/<relevant_extractor>.py

# 2. Run the master data factory to recompile all 9 canonical packs:
python3 scripts/pcdao_factory/run_factory.py

# 3. Audit all cells and rules for 100% compliance:
python3 scripts/pcdao_factory/validate_factory_output.py

# 4. Sync the updated JSON packs into Compose resources:
cp scripts/pcdao_factory/output/*.json composeApp/src/commonMain/composeResources/files/pcdao/

# 5. Run the complete regression audit:
bash .agents/skills/pcdao-military-intelligence/scripts/run_pcdao_audit_suite.sh
```

---

## 4. How the Skill Uses Authority Handbooks

1. **Exact Statutory Citations**: Every discrepancy emitted in the app includes the exact handbook name, chapter, and MoD letter date from `canonical_pcdao_rules.json`.
2. **Official Representation Letters**: The 1-Tap PCDA(O) Redressal Kit formats formal letters citing the specific paragraph and handbook authority to ensure legally binding representation.
3. **Auditing & Zero-Hallucination**: `validate_factory_output.py` prevents any manual data entry error or AI hallucination from making it into shipped app binaries.
