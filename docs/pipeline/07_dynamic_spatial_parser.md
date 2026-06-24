# 07. Dynamic Spatial Parser Architecture

This document describes the design, coordinate constraints, matching rules, and verification checks of the dynamic layout-driven PDF parsing engine in PayslipMax.

---

## 1. Dynamic Column Splitting

Rather than matching keys from a static, hardcoded dictionary, the parser maps columns dynamically using layout coordinates. This allows automatic capture of any custom allowances, arrears, or recoveries without codebase updates.

### Coordinate Anchors
- **Left Column (Earnings)**: `0 <= x < (x_split - 2.0)` where `x_split` is the X coordinate of the `DSOP` column header.
- **Middle Column (Deductions)**: `(x_split - 2.0) <= x <= x_right_bound`.
- **Right Column (Details / Bounding Limit)**: The details column starts at `x_right_bound` and is matched using spatial key-value anchors: `"details"`, `"trans"`, `"loan"`, `"instal"`, `"bldg"`, `"ior"`, `"adv"`, `"wef"`, `"remark"`.

### Vertical Table Filtering
Footnote sentences and header tags are excluded from anchor coordinate detection by scanning characters only within the vertical table region:
- `t_top = min(180.0, bpay_y - 30.0)`
- `t_bot = total_credit_y + 20.0`

---

## 2. Text Cleansing & Matching Rules

Before matching lines in either the left (earnings) or middle (deductions) columns, text strings undergo cleansing to isolate values from notes:

1. **Tag Removal**: Parenthetical alphanumeric strings (such as `(12A)` or `(NT)`) are stripped using:
   ```regex
   \(\w+\)
   ```
2. **Regex Key-Value Capture**: Cleaned lines are matched against the dynamic regex pattern:
   ```regex
   \b([A-Za-z/().&-]{1,15}(?:\s+[A-Za-z/().&-]{1,15}){0,2}\d*)\s+(-?\d+)\b
   ```
   *Matches a key of up to 3 words, optionally ending with digits, followed by a numeric amount.*

3. **Standalone Key Filtering**:
   Standalone helper/date words (like `"to"`, `"from"`, `"bill"`) are blocked from forming full keys via an `invalid_entire_keys` check to keep compound keys (like `"CC to bankers"`) intact.

---

## 3. Fallback Sequential Flow Parser

For coordinate-free PDFs (older statements, OCR, or layout edge cases), raw page text is parsed line-by-line:
1. **Table Bounds**: Starts scanning on lines containing basic pay or opening balances (`"basic pay"`, `"bpay"`, `"op cr bal"`, `"op dr bal"`, `"opening balance"`, `"op bal"`).
2. **Split Anchors**: Splits the earnings block and deductions block using strong debit anchors (like `DSOPF Subn`, `AGIF`, `Incm Tax`).
3. **Weak Anchor Exclusion**: Weak anchors (like `LF`, `FUR`) are excluded from the split check to avoid splitting early on refund credits (like `L Fee` credit adjustments). Refund credit lines starting with `"ref."` or `"refund"` are explicitly skipped.

---

## 4. Mathematical Self-Validation

To prevent silent ingestion of layout parsing errors, every PDF import runs a mathematical reconciliation check:
$$\text{Calculated Net} = \text{Sum of Dynamic Earnings} - \text{Sum of Dynamic Deductions}$$
If $\lvert \text{Calculated Net} - \text{Printed Net} \rvert \ge \text{₹2.0}$, the parser fails loud and throws an exception, preventing incorrect data from entering the database.
