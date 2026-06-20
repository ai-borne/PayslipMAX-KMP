# 01. UX Analysis & Insight Inventory

This document details Phase 1 (UX Analysis) and Phase 2 (Insight Inventory) for the PayslipMax AI Insights system.

---

## Phase 1 — UX Analysis

We reverse-engineer the interactive preview page to identify why each section is valuable, how it drives engagement, and its priority rating for production.

### 1. Premium Header & Month Selector
* **User Problem Solved**: Eliminates context-switching friction. Military ledger records are typically locked in static PDFs; a standard file view causes user fatigue.
* **Why it is Valuable**: Instantly changes the app from a "single upload tool" to a dynamic financial archive. Seeing months lined up creates a strong retention trigger to upload the next statement.
* **Premium Conversion**: High. Free users can switch months but see a blurred or locked teaser; premium users get instant audits upon switching.
* **User Value Score**: **Must Have**. Mandatory for historical context retrieval.

### 2. Overview Metrics Grid (Net, Gross, DSOP, Tax)
* **User Problem Solved**: Reduces information overload. PCDA(O) statements contain up to 50 small ledger rows. This highlights the only four numbers that determine monthly cash flow.
* **Why it is Valuable**: Offers an immediate health check. A user reads it within 2 seconds of statement upload to confirm their net take-home pay and savings progress.
* **Premium Conversion**: Low. Free users can view basic metrics; premium gating is reserved for AI CA audits.
* **User Value Score**: **Must Have**. Serves as the visual anchor.

### 3. Prioritized Audit Cards
* **User Problem Solved**: Reduces uncertainty and detects missed pay. Army officers rarely double-check PCDA calculations because tax codes and allowances (like TPTA and risk allowances) are too complex.
* **Why it is Valuable**: Resolves anxiety. The system does the auditing automatically and flags errors down to the rupee.
* **Premium Conversion**: Extremely High. This is the primary feature hook. Seeing "Arrears Audited & Verified" or "Missing Allowance Warning" prompts instant upgrades.
* **User Value Score**: **Must Have**. This is the core engine of PayslipMax.

### 4. Historical Intelligence Timeline
* **User Problem Solved**: Highlights financial progression and alerts about trailing risks. Showcases career milestones (increments, interest credits) and debit history.
* **Why it is Valuable**: The visual timeline is a powerful retention driver. Deleting the app means deleting years of accumulated career salary history.
* **Premium Conversion**: Moderate. Visualizing past events builds trust in the intelligence engine's accuracy.
* **User Value Score**: **Must Have**. Drives long-term lock-in.

### 5. Action Layer Modals (PCDA Letter & DSOP Simulator)
* **User Problem Solved**: Solves the inertia of taking action. Drafting official military representation letters to PCDA(O) is tedious and follows rigid service protocols.
* **Why it is Valuable**: Offers instant action. A user can copy a pre-filled, compliant audit letter to clipboard in one tap. The DSOP simulator provides immediate financial feedback.
* **Premium Conversion**: Extremely High. Interactive wealth simulation and pre-drafted legal representation letters represent high-value utility.
* **User Value Score**: **Must Have**. Answers the "So what?" and turns information into direct utility.

---

## Phase 2 — Insight Inventory

We inventory the five flagship insights from our simulated six-month workflow.

### 1. Dearness Allowance (DA) Arrears Audit
* **Required Data**: Current Month `arrears_da`, Current Month `arrears_tpta_da`, Previous 3 Months `basic_pay`, `military_service_pay`, `transport_allowance`, and historical DA Rate (e.g., 58% -> 60%).
* **Historical Window**: 6 months.
* **Business Value**: Catches underpayment/delayed pay. PCDA arrears adjustments are opaque; verifying that a credit is exact down to the rupee prevents financial leakage.
* **Confidence Rules**: Only appear in the month where `arrears_da > 0` or `arrears_tpta_da > 0`. Perform background math: `(Basic + MSP) * rate_difference * months_delayed = arrears_da`. If it matches, flag `success`. If it does not match, flag `danger` (underpaid).

### 2. Government Married Quarters Allotment Risk
* **Required Data**: `house_rent_allowance`, `license_fee`, `furniture_rent`.
* **Historical Window**: 6 months.
* **Business Value**: Protects cash flow. When quarters are occupied, HRA stops immediately, but License Fee / Furniture Rent deductions are often not processed by PCDA for months, leading to sudden ₹30,000+ debt recoveries.
* **Confidence Rules**: Trigger when `house_rent_allowance === 0` AND `license_fee === 0` AND `furniture_rent === 0` continuously for 3+ months. Do NOT show if the user draws HRA.

### 3. Unexpected Debit Recovery
* **Required Data**: `recovery_of_debits` or any abnormal deduction row.
* **Historical Window**: 3 months.
* **Business Value**: Explains sudden drops in net pay. Flagging the variance prevents cash flow confusion.
* **Confidence Rules**: Trigger if `recovery_of_debits > 0`. Calculate `debit_recovery / gross_pay * 100` to determine the severity score.

### 4. Provident Fund (DSOP) Interest Credit Milestone
* **Required Data**: `closing_balance`, `subscription_ytd`, `misc_adj_ytd`.
* **Historical Window**: 12 months.
* **Business Value**: Shows compounding wealth. Highlighting the tax-free annual interest credit encourages DSOP savings and highlights the platform's wealth-tracking utility.
* **Confidence Rules**: Trigger only in the month where `misc_adj_ytd > 0` AND the transaction is recognized as DSOP Interest (typically March payslip).

### 5. New Financial Year Tax Projection
* **Required Data**: Current `gross_pay`, YTD Tax, and PCDA projected tax variables (`total_taxable_income`, `total_tax_payable`).
* **Historical Window**: 6 months.
* **Business Value**: Prevents tax-adjustment shocks. Income tax is adjusted by PCDA in Jan/Feb, causing net pay to crash. Projecting tax early allows the officer to plan savings.
* **Confidence Rules**: Trigger in April (first full month of the PCDA tax year) or if the monthly IT deduction increases by more than 20% MoM.
