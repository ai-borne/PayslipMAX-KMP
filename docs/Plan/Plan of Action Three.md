
Based on both plans, and your current ground reality as of **29 Jun 2026**, I would slightly change the priorities.

Current status:

- ✅ Android parser: ~90% accurate.
- ✅ iOS parser: Fully aligned and verified against the corpus, matching Android output.
- ✅ Parser architecture is largely complete (grammar detection, structural reconstruction, shared pipeline, IR, etc.).  

This means **your bottleneck is no longer architecture—it is coverage and extraction quality.**

If I were managing PayslipMax, this would become the master roadmap.

---

# **Phase 0 — Finish iOS Coverage (Highest Priority)**

**Goal:** Reach Android parity before adding any new features.

### **0.1 Fix Legacy Grammar (≤ Oct 2023)**

- Grammar detection
- Structural reconstruction
- Earnings extraction
- Deductions extraction
- Validation

**Exit Criteria**

Legacy corpus parses correctly.

---

### **0.2 Fix Modern Grammar (Mar 2025 onwards)**

Support:

- DO2 section
- Next Increment Date
- New layout
- New tables
- Any shifted columns

**Exit Criteria**

Mar 2025–latest parses successfully.

---

### **0.3 Achieve Android ↔ iOS parity**

Every sample PDF should produce identical JSON.

No platform-specific behaviour.

---

## **Milestone**

**iOS parser = Android parser**

Only then move ahead.


---

Actionable now (no device needed):

1. Update TokenParseCorpusRegressionTest to use GrammarAwareParser instead of PayslipTokenParser — iOS now routes through GrammarAwareParser (RC3), but the Android regression suite still uses the old path. They should test the same engine.
2. Fix grammar detection gap for Feb/Mar/Apr 2022 — matchTransitional7thCpc rejects any month containing "BPAY" (case-insensitive), but these months have "A/o BPAY-" in arrears text, not a table BPAY label. Fix: require BPAY as a standalone word/token, not a substring. Currently harmless (pipeline falls back correctly) but the UNKNOWN detection is a diagnostic gap.
3. Fix stale iOS test assertion — PlatformPdfParserIosTest line 210 asserts rawEarnings.isNotEmpty() but the current engine routes BPAY into earningsMap["basicPay"], not rawEarnings. This must be fixed before the test can ever be un-ignored.

Requires iOS device:

4. Run PlatformPdfParserIosTest.verifyRealPayslipsOnIos against real PDFs across all eras to confirm on-device parity.

Held pending evidence:

5. RC2 (85f → 40f merge threshold) — on hold until A/B corpus testing with iOS device output.
6. RC4 (iOS token height normalization) — deferred.

The highest-value unblocked item is #1 (corpus test engine alignment), since it closes the gap between what the regression suite tests and what ships on iOS.

---

# **Phase 1 — Accuracy Sprint**

This is where the biggest gains will come from.

Instead of one generic parser…

Create dedicated extractors.

Checklist:

- ☐ Basic Pay Extractor
- ☐ Allowance Extractor
- ☐ Deduction Extractor
- ☐ Tax Extractor
- ☐ DSOP Extractor
- ☐ Arrears Extractor
- ☐ DO2 Extractor
- ☐ Metadata Extractor

Each extractor should deeply understand one section instead of relying on global regex.  

---

# **Phase 2 — Validation Engine**

Move from “parsed” to “verified”.

Implement:

- ☐ Gross Pay = Sum(Earnings)
- ☐ Total Deductions = Sum(Deductions)
- ☐ Net = Gross − Deductions
- ☐ DSOP reconciliation
- ☐ Tax reconciliation
- ☐ Cross-page validation
- ☐ Confidence score
- ☐ Explain failed validations

---

# **Phase 3 — Golden Corpus**

Probably the highest ROI investment.

```
golden-corpus/

2014/
2015/
...
2026/
```

Every PDF should include:

- PDF
- expected.json
- expected_ui
- diagnostics

Then every commit automatically runs against the corpus.  

---

# **Phase 4 — Differential Testing**

Every build should compare:

```
Android JSON

vs

iOS JSON
```

If different:while android parsing is 

❌ Build fails.

This permanently prevents platform drift.  

---

# **Phase 5 — Parser Inspector**

Build a developer tool.

Display:

- Grammar
- Table reconstruction
- Rows
- Columns
- Ledger
- IR
- Diagnostics
- Validation results

Clicking BPAY should highlight the source tokens.

This will dramatically reduce debugging time.  

---

# **Phase 6 — Parser Health Dashboard**

Track:

- Grammar detected
- Parse success
- Confidence
- Unknown fields
- Validation failures
- Parse duration
- Platform parity

Now you can measure parser quality over time instead of relying on intuition.  

---

# **Phase 7 — Unknown Field Discovery**

Whenever a new allowance appears:

```
XYZ_ALLOWANCE
```

Automatically log:

- field name
- grammar
- parser version
- occurrence count

This makes future PCDA changes much easier to absorb.  

---

# **Phase 8 — Robustness Testing**

Test:

- rotated PDFs
- OCR PDFs
- damaged PDFs
- reordered pages
- missing pages
- duplicate pages
- tiny fonts
- huge fonts
- password-protected PDFs

Goal:

Parser should never crash.

---

# **Phase 9 — Performance**

Benchmark:

- Parse time
- Memory
- CPU
- Battery
- IR generation

Across:

- Android
- iPhone
- Older devices

---

# **Phase 10 — AI Assistance (Not AI Parsing)**

Only after the deterministic parser is mature.

Pipeline:

```
PDF

↓

Deterministic parser

↓

Validation

↓

Needs Review?

↓

YES

↓

Gemma

↓

Suggest interpretation

↓

User confirms
```

The LLM should assist, not replace, the parser.  

---

# **Phase 11 — Production Telemetry**

Collect anonymous metrics only:

- parser version
- grammar
- parse success
- parse time
- unknown fields
- validation failures

Never upload the user’s payslip.

---

# **Phase 12 — SDK Vision**

Convert the parser into a reusable document engine.

Future modules:

- Army Payslip
- Form 16
- Bank Statements
- Pension
- Income Tax
- DSOP

---

# **What I would work on immediately**

Given your **actual current state**, I would ignore everything else until these five items are complete:

|**Priority**|**Task**|**Status**|
|---|---|---|
|🔴 P1|Fix iOS parsing for ≤ Oct 2023|Completed|
|🔴 P2|Fix iOS parsing for ≥ Mar 2025|Completed|
|🔴 P3|Achieve Android ↔ iOS identical output|Completed|
|🟠 P4|Build dedicated field extractors|Pending|
|🟠 P5|Build validation engine|Pending|

Everything after that (Golden Corpus, Inspector, Differential Testing, Telemetry, AI) is about making the parser **production-grade** rather than simply “working.”

I think this gives you a clear progression:

- **Stage 1:** Reach **functional parity** (Android = iOS).
- **Stage 2:** Reach **99%+ extraction accuracy** through dedicated extractors and validation.
- **Stage 3:** Reach **production reliability** through corpus testing, differential testing, observability, and tooling.
- **Stage 4:** Add **AI-assisted review** only for the small percentage of edge cases that deterministic parsing cannot confidently resolve.

This sequence minimizes rework and ensures every new investment builds on a stable parser foundation.

---


