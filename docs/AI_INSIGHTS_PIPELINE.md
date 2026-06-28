# PayslipMax — Parser & AI Insights Architecture

**One-stop reference for how the PCDA(O) payslip parser works, how AI Insights are generated, and how all the pieces connect. Updated to reflect the full Token-IR re-architecture (Phases 0–6) plus post-plan bugfixes.**

PayslipMax is an offline-first military payslip intelligence platform for Indian Army officers. It extracts structured salary data from monthly PCDA(O) PDF payslips and turns it into wealth-optimization suggestions and error-detection alerts. Everything described in this document runs on-device with no data leaving the device except for optional cloud AI inference.

---

## Table of Contents

0. [End-to-End User Interaction Lifecycle](#0-end-to-end-user-interaction-lifecycle)
1. [PDF → ParsedPayslip: 7-Tier Token-IR Pipeline](#1-pdf--parsedpayslip-7-tier-token-ir-pipeline)
2. [Stage-by-Stage: Engine Reference](#2-stage-by-stage-engine-reference)
3. [Display Layer: Raw Path vs Structured Path](#3-display-layer-raw-path-vs-structured-path)
4. [Confidence Scoring & Per-Field Correction UI](#4-confidence-scoring--per-field-correction-ui)
5. [Corpus Regression Safety Net](#5-corpus-regression-safety-net)
6. [Legacy String Path (Tests / Secondary)](#6-legacy-string-path-tests--secondary)
7. [Security & PII Controls](#7-security--pii-controls)
8. [AI Insights Pipeline](#8-ai-insights-pipeline)
9. [Key File Reference](#9-key-file-reference)

---

## 0. End-to-End User Interaction Lifecycle

This document serves as the primary technical guide detailing how PayslipMax operates from the moment an officer interacts with the UI to the final local database storage and interactive review loop.

### Complete Interaction Sequence

```mermaid
sequenceDiagram
    autonumber
    actor User as Officer / User
    participant UI as Compose UI / ReplicaScreen
    participant VM as PayslipViewModel
    participant Repo as FinancialIntelligenceRepository
    participant Engine as 7-Tier Token-IR Parser
    participant DB as Room Database (Encrypted)

    User->>UI: 1. Selects PDF payslip & enters password
    UI->>VM: 2. Triggers onUploadPayslip(file, password)
    VM->>Repo: 3. Calls parseAndSavePayslip(pdfBytes, password)
    Repo->>Engine: 4. Delegates to PlatformPdfParser.decryptAndParse()
    Note over Engine: Runs Tiers 1-7 (Native Extractor → Grid → Classifier → Solver → Gemma Fallback → Validator)
    Engine-->>Repo: 5. Returns Result<ParsedPayslip>
    Repo->>DB: 6. Encrypts (AES-256) & persists EncryptedPayslipEntity
    DB-->>VM: 7. StateFlow emits updated payslips list
    VM-->>UI: 8. Renders Replica view & Ledger Section
    Note over UI: Highlights fields with fieldConfidence < 0.7f with warning icons
    User->>UI: 9. Taps low-confidence item & enters correction
    UI->>VM: 10. Calls applyCorrection(dateStr, fieldKey, newValue)
    VM->>Repo: 11. Saves encrypted PayslipCorrectionEntity (v9)
    Repo-->>UI: 12. UI updates dynamically on read via combine flow
```

---

## 1. PDF → ParsedPayslip: 7-Tier Token-IR Pipeline

### High-Level Data Flow

```mermaid
flowchart TD
    PDF([PCDA-O PDF]) -->|Tier 1: AES decrypt + page scan| TokAndroid[Android: TokenScanner / PDFBox]
    PDF -->|Tier 1: AES decrypt + PDFKit| TokIOS[iOS: IosTokenExtractor / PDFKit]

    TokAndroid -->|List<PositionedToken> top-down Y\nfontSize & isBold| PageCls[Tier 2: PageClassifier & IR\nPositionedToken]
    TokIOS -->|List<PositionedToken> top-down Y\nfontSize & isBold| PageCls

    PageCls -->|tableTokens\ntaxTokens / dsopTokens| GR[Tier 3: GridReconstructor & RowPairing\ncluster y→rows, x→cells]
    GR -->|ReconstructedGrid| TTC[Tier 4: TokenTableClassifier\ncontent-driven classification]

    TTC -->|ClassifiedTable| RS[Tier 5: ReconciliationSolver\ncross-column routing\nGross / Deductions / Net invariants]

    RS -->|needsReview || rawTokens| GFE[Tier 6: Offline Gemma Fallback\nGemmaFallbackExtractor & GemmaEngine]
    RS -->|High Confidence| SV[Tier 7: SchemaValidator\nMathematical Invariants Check]
    GFE -->|Proposed Extractions| SV

    SV -->|Validated ReconciledTotals| ASM[PayslipAssembler\nbuilds Earnings + Deductions domain objects]
    
    PageCls -->|fullText| Meta[parseDate / parseOfficer\nparseTotals - ParserUtils]
    Meta -->|Officer, year/month\ngrossPay, totalDeductions| ASM

    ASM -->|ParsedPayslip| DB[(Room DB\nEncryptedPayslipEntity)]
    DB -->|merge corrections| CorrDB[(PayslipCorrectionEntity\nAES-256 encrypted)]
    DB -->|ParsedPayslip| VM[PayslipViewModel]
    VM --> UI[LedgerSection\nReplicaUtils]
```

### What Changed from the Old Parser

| Old (string path)                               | New (7-Tier Token-IR + Gemma Architecture)                         |
|-------------------------------------------------|--------------------------------------------------------------------|
| Platform crops PDF into `leftColumnText` / `middleColumnText` using guessed `xSplit` geometry | Both platforms emit `List<PositionedToken>` with font metadata (`fontSize`, `isBold`) — top-down Y |
| `splitCreditDebitSections()` tries to find the column boundary in text | `GridReconstructor` **learns** credit/debit x-bands per document from matched keywords |
| `DynamicSpatialParser.applyHistoricalOverrides(year, month, …)` hardcodes per-month fudge factors | Deleted. Any residual is booked to `miscEarnings`/`miscDeductions` and recorded as confidence signals |
| `reconciliation throws away the whole parse` on mismatch ≥ ₹2 | Ambiguous fields route to **Tier 6 Offline Gemma Fallback** (`GemmaFallbackExtractor`) for structured resolution |
| Final output unvalidated after extractions | **Tier 7 Schema Validator** validates exact mathematical accounting invariants (`grossPay`, `totalDeductions`, `netRemittance`) |

---

## 2. Stage-by-Stage: Engine Reference

### Stage 0 — User Ingestion & ViewModel Handshake

1. **User Action**: Officer selects a PDF file and inputs password in `UploadWidget` or `PayslipReplicaScreen`.
2. **ViewModel Dispatch**: `PayslipViewModel.onUploadPayslip()` handles asynchronous state transitions (loading/error states).
3. **Repository Execution**: Calls `FinancialIntelligenceRepository.parseAndSavePayslip(pdfBytes, password)`.
4. **Platform Binding**: Delegates directly to platform implementations (`PlatformPdfParser.decryptAndParse()`).

### Stage 1 — Platform Token Extraction

| Platform | Entry point | Library | Notes |
|----------|-------------|---------|-------|
| Android | `AndroidTokenExtractor.extractTokenized()` | PDFBox `PDFTextStripper` via `TokenScanner` | PDFBox Y is already top-down — no inversion needed |
| iOS | `IosTokenExtractor.extractTokenized()` | PDFKit `PDFPage.string(for:)` | `IosTokenCoordinates.topDownY(y, h, pageHeight)` inverts PDFKit's bottom-up Y |

Both produce `TokenizedPayslip(tableTokens, taxTokens, dsopTokens, fullText)`. `PageClassifier` (commonMain SSOT) classifies each page by keyword into table / tax / DSOP buckets — no per-platform divergence.

`PositionedToken` fields: `text`, `x`, `y` (top-down), `width`, `height`, `fontSize`, `isBold`. Derived: `centerX`, `centerY`, `right`, `bottom`.

### Stage 2 — Grid Reconstruction (`GridReconstructor`)

Clusters tokens into a 2D grid per-document:

- **Row tolerance** = `max(3, medianHeight × 0.5)` — tokens within this vertical distance share a row.
- **Cell gap** = `max(3, medianHeight × 1.2)` — tokens separated by more than this horizontal distance are distinct cells; merged otherwise.

No hardcoded pixel values. Translation-invariant: shifting the whole table 120 px produces identical rows.

**Key pathology** (Bug 1 — Feb 2025): When the credit-total amount token sits < cellGap from an adjacent Hindi label token, they merge into one cell. E.g. `"271739 kuula kTaOtI"` gets paired with amount `109310`. Fixed in `TokenTableClassifier.toCandidate()` — see Stage 3.

### Stage 3 — Row Pairing (`RowPairing`)

Within each reconstructed row, pairs the leftmost text cell (label) to the rightmost numeric cell (amount) using an overwrite-pending heuristic. `parseAmount()` rejects dates (`01/2024`), trailing dots (`1.`), and currency prefixes (`Rs.1,39,604`).

### Stage 4 — Token Table Classification (`TokenTableClassifier`)

Content-driven classifier — never geometry-hardcoded:

1. **Match** each label against `PayslipPatternConfig.creditKeysMapping` / `debitKeysMapping` (SSOT — reuse, never copy).
2. **Learn** this document's credit-label and debit-label x-bands as the **median** x of cleanly-matched labels.
3. **Assign** ambiguous / unmatched labels by proximity to learned bands; labels farther than `acceptRadius = |debitBand − creditBand| / 2` are dropped (catches the "Details of Transactions" column).

`toCandidate()` guards:
- `normalized.isBlank()` → null (pure whitespace after Hindi negation)
- `normalized.none { it.isLetter() }` → null (**Bug 1 fix**: drops pseudo-labels like `"271739"` that are digit-only after Hindi words are removed)
- `normalized in normalizedBlocklist` → null

`negateHindiTransliterations()` (word-boundary regex, `IGNORE_CASE`) strips Hindi transliteration tokens (`kuula`, `Aaya`, `kTaOtI`, etc.) before classification.

Output: `ClassifiedTable` → `credits: List<ClassifiedEntry>` + `debits: List<ClassifiedEntry>` + `rawCredits()` / `rawDeductions()`.

### Stage 5 — Confidence & Reconciliation Solver (`ReconciliationSolver`)

Routes each `ClassifiedEntry` into the right map using cross-column routing rules, calculates confidence scores (`fieldConfidence`), performs mandatory field integrity auditing, and flags ambiguous parses (`needsReview = true` or `rawEarnings.isNotEmpty()`).

| Entry | Route |
|-------|-------|
| Matched key, correct column | `earningsMap[standardKey]` / `deductionsMap[standardKey]` |
| Debit key stranded in credit column (ledger carry key) | `deductionsMap[matchedKey]` (ledger entry) |
| Debit key stranded in credit column (credit reversal key) | `earningsMap["adjPayAndAllce"]` |
| Credit key stranded in debit column | `deductionsMap[recoveryTargetFor(matchedKey)]` (recovery) |
| Unmatched | `rawEarnings[rawLabel]` / `rawDeductions[rawLabel]` |

#### Mandatory Domain Field Integrity Audit
`ReconciliationSolver.solve(...)` verifies the presence of strictly mandatory domain fields defined in `PayslipPatternConfig`:
* **Strictly Mandatory Credits**: `basicPay` (`BPAY`), `dearnessAllowance` (`DA`), `militaryServicePay` (`MSP`).
* **Strictly Mandatory Debits**: `agif` (`AGIF`), `dsopSubscription` (`DSOP`).

If any mandatory field is missing from the extracted maps, a **50% confidence penalty** is applied to overall confidence, and `needsReview = true` is flagged to trigger Tier 6 Gemma fallback for structured recovery.

### Stage 6 — Tier 6 Offline Gemma Fallback (`GemmaFallbackExtractor` & `GemmaEngine`)

When spatial confidence rules encounter ambiguity (`solved.needsReview == true` or raw unresolved tokens exist):
1. **On-Demand Storage & Download Lifecycle**: The quantized Gemma model binary (`gemma-3-1b-it-int4.task`, ~650MB) is managed locally via `GemmaModelStorageManager`. Users trigger on-demand background streaming via `GemmaModelDownloadManager` directly from app settings.
2. **Hardware Capability Gate**: `DeviceCapabilityManager` (`expect/actual` in KMP) inspects host hardware before model load, verifying RAM ≥ 3.5GB and free disk space ≥ 1.5GB.
3. **Platform Binding & Runtime**: `PlatformPdfParser` (Android/iOS) verifies binary readiness in app storage (`context.filesDir`) and instantiates `GemmaEngine` wrapping the native Google MediaPipe LLM Inference SDK / native Apple Metal runtime.
4. **Prompt & Response Contract**: `GemmaPromptBuilder` formats unresolved token labels into a deterministic prompt. `GemmaEngine` executes local asynchronous inference, and `GemmaResponseParser` safely parses JSON extractions into standard ledger keys (`BPAY`, `DA`, `ITAX`, `DSOP`, etc.).
5. **Standby Optimization**: If Tiers 1–5 achieve 100% mathematical reconciliation (`needsReview == false`), Tier 6 inference stays on standby to preserve device battery and thermals.

### Stage 7 — Tier 7 Schema Validator (`SchemaValidator`)

Acts as the final gatekeeper verifying mathematical accounting invariants across extracted figures:
- `grossMismatch = |grossPay - creditsSum| <= 2.0`
- `deductionsMismatch = |totalDeductions - debitsSum| <= 2.0`
- `netResidual = |(grossPay - totalDeductions) - netRemittance| <= 2.0`

If `isValid == false`, the parse is preserved but flagged with `needsReview = true` for Phase 5 user correction UI.

### Stage 8 — Domain Assembly (`PayslipAssembler`)

Maps validated `earningsMap` → `Earnings` struct + `miscEarnings`, and `deductionsMap` → `Deductions` struct + `miscDeductions`. Serializes and commits `ParsedPayslip` to local Room DB via `EncryptedPayslipEntity` (AES-256).

### Stage 9 — Interactive Review & Encrypted Persistence

1. **Reactive Render**: `PayslipViewModel` emits updated `ParsedPayslip` to `PayslipReplicaScreen` and `LedgerSection`.
2. **Low-Confidence Alerting**: Fields with `fieldConfidence < 0.7f` render with `AppColors.Warning` warning icons.
3. **User Correction Dialog**: Tapping an item opens `LedgerCorrectionDialog` allowing manual value entry.
4. **Encrypted Overlay Persistence**: `repository.saveCorrection(dateStr, fieldKey, newValue)` stores an encrypted `PayslipCorrectionEntity` (v9). Applied seamlessly on read via `combine` flow without mutating raw parsed ground truth.

---

## 3. Display Layer: Raw Path vs Structured Path

`ReplicaUtils.getCreditsList(payslip)` and `getDebitsList(payslip)` select one of two display paths:

```
rawEarnings.isEmpty() → Structured Path
rawEarnings.nonEmpty() → Raw Path
```

### Structured Path (rawEarnings empty — all credits matched)

Returns explicit `LedgerLine` rows for every standardized `Earnings` / `Deductions` field with `amount ≠ 0.0`. Maps all 30+ domain fields including pay adjustments and arrears (`adjPayAndAllce`, `arrearsDa`, `adjDa`, `recFieldAllowance`, `recoveryOfDebits`, etc.).

### Universal MISC Residual Balancing (Both Paths)

Whether rendering via Structured Path or Raw Path, `ReplicaUtils` applies **Universal MISC Residual Balancing**:

```kotlin
val totalItemSum = items.sumOf { it.amount }
val residual = payslip.summary.grossPay - totalItemSum
if (residual > ConfidenceThresholds.ITEM_SUM_TOLERANCE && items.none { it.code == "MISC" }) {
    items + LedgerLine("MISC", residual, getCreditDesc("miscEarnings"), "miscEarnings")
}
```

This guarantees that for any unlisted custom pay code or residual gap across thousands of users, the displayed table lines always reconcile to `grossPay` and `totalDeductions` with 100% mathematical precision. The MISC row uses `"miscEarnings"` or `"miscDeductions"` as `fieldKey` for SSOT correction flow compatibility.

### Mismatch Banner (phantom over-count)

```kotlin
internal fun creditsMismatch(payslip: ParsedPayslip): Double =
    getCreditsList(payslip).sumOf { it.amount } - payslip.summary.grossPay

internal fun debitsMismatch(payslip: ParsedPayslip): Double =
    getDebitsList(payslip).sumOf { it.amount } - payslip.summary.totalDeductions
```

After MISC absorb, `mismatch > 0` only when items **over-count** the printed total (phantom entry). `LedgerSection` passes both values to `LedgerTableFooter`. When either exceeds `ConfidenceThresholds.ITEM_SUM_TOLERANCE (= 2.0)`, `LedgerMismatchBanner` renders an `AppColors.Warning` chip showing the over-count amount.

**Tolerance SSOT**: `ConfidenceThresholds.ITEM_SUM_TOLERANCE` (shared domain object) is the single constant used by both the MISC absorb decision and the banner threshold.

### `isValidRawKey` — Display Guard (Bug 1 defense-in-depth)

```kotlin
private fun isValidRawKey(key: String): Boolean {
    val afterHindi = key.lowercase().split(Regex("\\s+"))
        .filterNot { it in hindiWordSet }.joinToString(" ").trim()
    return afterHindi.any { it.isLetter() }
}
```

Filters raw map entries whose keys are digit-only or Hindi-word-only after stripping. Defends against any stale DB records that bypassed the parser-layer fix.

---

## 4. Confidence Scoring & Per-Field Correction UI

### Confidence Flow

```
ReconciliationSolver.scoreConfidence()
    → ParsedPayslip.fieldConfidence: Map<String, Float>
    → ParsedPayslip.needsReview: Boolean

ConfidenceThresholds.REVIEW_THRESHOLD = 0.7f  (SSOT)
ParsedPayslip.isFieldLowConfidence(fieldKey)   (extension, ConfidenceThresholds.kt)
```

### Correction Persistence

`PayslipCorrectionEntity(dateStr PK, ciphertext)` stores AES-256-encrypted `Map<String, Double>` via `CryptoHelper`. DB schema v9 (auto-migration from v8). Corrections are:
- **Applied on read** via `combine` in `getAllPayslips()` / `getPayslipByDate()` → `applyCorrections(Map<fieldKey,Double>)`.
- **Never mutate the original parse** — stored separately so re-parsing (e.g. on engine improvement) can overwrite only the parsed side.

### UI Wiring

```
LedgerRowItem         → shows AppColors.Warning Info icon when isFieldLowConfidence(fieldKey)
LedgerCorrectionDialog → inline edit dialog (per-field)
PayslipReplicaScreen  → onCorrectField → PayslipViewModel.applyCorrection()
PayslipViewModel      → repository.saveCorrection(dateStr, fieldKey, newValue) + updates selectedPayslip
```

`LedgerLine.fieldKey` is the SSOT bridge between display and correction:
- Structured path: domain field name (e.g. `"basicPay"`, `"incomeTax"`)
- Raw path: raw PDF label (e.g. `"BONUS X"`, `"RH12"`)

---

## 5. Corpus Regression Safety Net

**Purpose**: makes "fix one month, break another" structurally impossible.

### Fixture Layout

```
shared/src/androidUnitTest/resources/corpus/
    <id>.json          ← scrubbed full-text / column texts (input)
    <id>.tokens.json   ← scrubbed positioned tokens (input)
    ground_truth.json  ← human-verified ParsedPayslip JSON (expected output)
```

52 fixtures covering 2022–2026. De-identified by `CorpusScrubber`:
- Name → `Officer Officer Officer`
- Account → `16/000/000000X`
- PAN → `AR*****90G`
- Email → scrubbed
- Numbers untouched

### Always-On Tests

| Test | Scope | What it checks |
|------|-------|----------------|
| `PayslipCorpusRegressionTest` | androidUnitTest | Text-path `PayslipTextParser.parse` vs ground truth for all 52 fixtures |
| `TokenParseCorpusRegressionTest` | androidUnitTest | Token-path `PayslipTokenParser.parse` vs ground truth (primary path) |
| `TokenCorpusRegressionTest` | androidUnitTest | Token fixture well-formedness (52 `.tokens.json` files) |
| `TokenTableEngineTest` | commonTest | Synthetic token layout tests, incl. translation-invariance + Bug 1 regression |
| `ReplicaUtilsMismatchTest` | commonTest | MISC row appearance, creditsMismatch/debitsMismatch helpers |

### Opt-In Local Tests (never in CI)

`-Dpayslip.localCorpus=<path>` → `CorpusCaptureTest` runs the real pipeline over PDFs at `~/Desktop/Pay Slip Elements`, writes scrubbed fixtures. Gated; no PII ever committed.

---

## 6. Legacy String Path (Tests / Secondary)

`PayslipTextParser` + `DynamicSpatialParser` + `ParserUtils.splitCreditDebitSections` remain in the codebase as the **secondary** path. They are:
- Off the production parse path (both Android and iOS now call `PayslipTokenParser` via the token path).
- Backed by the `PayslipCorpusRegressionTest` text-fixture regression suite (ensures the old path remains stable for comparison).
- Used by the opt-in `CorpusCaptureTest` capture utility.

Removing the string path entirely requires migrating those tests to the token path — deferred as a clean-up task.

**Deleted in Phase 4:**
- `DynamicSpatialParser.applyHistoricalOverrides()` — per-month fudge factors gone.
- iOS string crop (`IosLayoutScanner.kt`, `extractTextSpatially`) — 134 lines deleted.
- Hard-fail `Result.failure` reconciliation — replaced by `netResidual` + `needsReview`.

---

## 7. Security & PII Controls

| Control | Implementation |
|---------|----------------|
| Payslip data at rest | AES-256 via `CryptoHelper` (Android Keystore / iOS Keychain) → `EncryptedPayslipEntity` |
| Corrections at rest | Same `CryptoHelper` → `PayslipCorrectionEntity` (DB v9) |
| Auto-Backup disabled | `allowBackup="false"` in AndroidManifest — prevents restoring DB without hardware-bound key |
| Self-healing decryption | Falls back to legacy key on `BAD_DECRYPT`; clears corrupt data rather than bricking |
| Committed fixtures | `CorpusScrubber` strips all PII before commit; only `AR*****90G` / `16/000/000000X` placeholders remain |
| PDF password | Removed hardcoded default from UI (`UploadWidget`, `SettingsScreen`) — field starts empty |
| Opt-in capture | System-property-gated (`-Dpayslip.localCorpus`); never writes real PII to committed resources |
| PII in git history | Real name/account remained in history pre-Phase 6. Destructive `filter-repo`/BFG rewrite deferred (user decision required) |

---

## 8. AI Insights Pipeline

AI Insights layer sits above `ParsedPayslip` — it consumes already-parsed, already-stored payslip data.

```mermaid
graph TD
    DB[(Salary Database)] -->|4. Trigger Audit| Engine[Insight Engine / ViewModel]
    Engine -->|5. Verify Subscription| PremiumGate{Premium Gate}
    
    PremiumGate -->|Free Tier| RenderFree[Render Basic Charts & Anomalies]
    PremiumGate -->|Premium Tier| Abstraction[AI Abstraction Layer]
    
    Abstraction -->|Cloud Provider| GeminiProxy[Cloud Gemini API Proxy]
    Abstraction -->|Local Provider| LocalAI[Local AI Engine\nGemma / Core AI / Android Edge SDK]
    
    GeminiProxy -->|JSON Response| SaveRepo[Intelligence Repository]
    LocalAI -->|JSON Response| SaveRepo
    
    SaveRepo -->|Cache JSON| DB
    SaveRepo -->|6. Render Native UI| User([User App UI])
```

### Deterministic Rules Engine

`DeterministicIntelligenceEngine` orchestrates specialized `RuleAuditor` implementations:

| Auditor | What it checks |
|---------|---------------|
| `DaArrearsAuditor` | Mathematical DA adjustment progression |
| `MarriedQuartersRiskAuditor` | Retroactive rent recovery risk |
| `UnexpectedDebitAuditor` | Abnormal ledger recovery spikes |
| `DsopComplianceAuditor` | 6% statutory minimum savings; ₹41,666/mo tax threshold |
| `TaxProjectionAuditor` | Annual ITAX trajectory from YTD |
| `SalaryLossAuditor` | Unexplained net-pay drops |
| `MissingAllowanceAuditor` | Allowances present in past but absent this month |
| `TptaEntitlementAuditor` | Transport allowance entitlement vs received |

`InsightPrioritizationEngine` scores alerts on 5 dimensions, filters below 7.0, caps dashboard at top 4, uses ₹ value as tie-breaker.

### AI Provider Abstraction

`AIInsightProvider` interface isolates business logic from model execution:
- `GeminiCloudProvider` — cloud inference (active).
- `LocalGemmaProvider` — on-device (placeholder → upgraded to dynamic `AiInsightReport` JSON construction).
- `AIProviderManager` — selects provider; toggleable via Settings (`useLocalAi` preference, DB schema v7).

---

## 9. Key File Reference

### Parser Core (shared/commonMain)

| File | Role |
|------|------|
| `parser/PositionedToken.kt` | Token IR data class (text, x, y, width, height + derived helpers) |
| `parser/PageClassifier.kt` | SSOT page-type detection (table / tax / DSOP) by keyword |
| `parser/GridReconstructor.kt` | Clusters tokens → 2D grid; tolerances derived from median height |
| `parser/RowPairing.kt` | Row-local label→amount pairing; `parseAmount()` rejection rules |
| `parser/TokenTableClassifier.kt` | Content-driven credit/debit classification; learns x-bands per document |
| `parser/ReconciliationSolver.kt` | Cross-column routing; confidence scoring; `needsReview` flag |
| `parser/GemmaFallbackExtractor.kt` | Tier 6 fallback extraction service wrapping `GemmaEngine` runtime |
| `parser/GemmaPromptBuilder.kt` | Formats deterministic extraction prompts mapping unresolved tokens to standard keys |
| `parser/GemmaResponseParser.kt` | Safely parses structured JSON responses from Gemma offline fallback |
| `parser/SchemaValidator.kt` | Tier 7 final gatekeeper verifying exact mathematical accounting invariants |
| `parser/ReconciliationEngine.kt` | Ledger carry-over extraction; `miscEarnings`/`miscDeductions` computation |
| `parser/PayslipAssembler.kt` | `earningsMap` + `miscEarnings` → `Earnings` domain object; final `ParsedPayslip` construction |
| `parser/PayslipTokenParser.kt` | Primary 7-tier parse entry point: tokens → `ParsedPayslip` |
| `parser/PayslipPatternConfig.kt` | SSOT: `creditKeysMapping`, `debitKeysMapping`, `blocklist`, `hindiTransliterations`, cross-column routing sets |
| `parser/ParserUtils.kt` | `negateHindiTransliterations()`, `parseTotals()`, `parseOfficer()`, `splitCreditDebitSections()` (legacy) |
| `domain/ConfidenceThresholds.kt` | SSOT: `REVIEW_THRESHOLD = 0.7f`, `ITEM_SUM_TOLERANCE = 2.0` |
| `domain/Model.kt` | `ParsedPayslip`, `Earnings`, `Deductions`, `PayslipSummary`, `LedgerBalances` |

### Platform Adapters

| File | Role |
|------|------|
| `androidMain/.../parser/TokenScanner.kt` | PDFBox `PDFTextStripper` → word-level `PositionedToken` list |
| `androidMain/.../parser/AndroidTokenExtractor.kt` | Page classification + scan via `TokenScanner` |
| `iosMain/.../parser/IosTokenExtractor.kt` | PDFKit page scan; delegates Y-inversion to `IosTokenCoordinates` |
| `iosMain/.../parser/SpatialTextExtractor.kt` | `IosTokenCoordinates.topDownY(y, h, pageHeight)` Y normalization |

### Display Layer (composeApp/commonMain)

| File | Role |
|------|------|
| `ui/screens/ReplicaUtils.kt` | `getCreditsList`, `getDebitsList` (raw/structured path selection); MISC absorb row; `creditsMismatch`, `debitsMismatch` helpers; `isValidRawKey` display guard |
| `ui/screens/LedgerSection.kt` | `LedgerSection` composable; `LedgerTableFooter`; `LedgerMismatchBanner` (phantom over-count warning) |
| `ui/screens/LedgerCorrectionDialog.kt` | Per-field inline correction dialog |
| `ui/theme/ConfidenceThresholds.kt` | (see shared domain above — imported by composeApp) |
| `ui/theme/AppStrings.kt` | All UI strings (no hardcoded strings anywhere in the UI) |
| `ui/theme/Theme.kt` | `AppColors`, `AppDimensions` |

### Corpus & Tests

| File | Role |
|------|------|
| `androidUnitTest/.../PayslipCorpusRegressionTest.kt` | Always-on text-path regression (52 fixtures) |
| `androidUnitTest/.../TokenParseCorpusRegressionTest.kt` | Always-on token-path regression (52 fixtures, primary path) |
| `androidUnitTest/.../TokenCorpusRegressionTest.kt` | Token fixture well-formedness check |
| `androidUnitTest/.../CorpusCaptureTest.kt` | Opt-in capture utility (`-Dpayslip.localCorpus`) |
| `commonTest/.../TokenTableEngineTest.kt` | Synthetic token layout tests + translation-invariance + Bug 1 regression |
| `commonTest/.../ReplicaUtilsMismatchTest.kt` | MISC row + mismatch helper unit tests |

---

## Post-Plan Bugfixes

### Bug 1 — Phantom "uula" Deduction (Feb 2025)

**Root cause**: In Feb 2025's PDF layout, the credit-total amount token (`"271739"`) sits 4 px from the adjacent Hindi label (`"kuula kTaOtI"`). Since 4 px < `cellGap` (7.2 px), `GridReconstructor` merges them into one cell: `"271739 kuula kTaOtI"`. After `negateHindiTransliterations`, the label becomes `"271739"` — not blank, not in blocklist → passes toCandidate → gets booked as a debit raw entry with amount 109310 (the actual total deductions, displayed as a phantom line item).

**Fix (two-layer)**:
1. Parser (`TokenTableClassifier.toCandidate`): `if (normalized.none { it.isLetter() }) return null` — drops digit-only pseudo-labels.
2. Display (`ReplicaUtils.isValidRawKey`): filters any stale DB keys that are digit-only/Hindi-word-only after negation.

Commit: `4468b5b`. Regression test: `TokenTableEngineTest.hindiTotalsMergedWithAmountDropped`.

### Bug 3 — Item Sum vs Footer Total Verification

**Root cause**: The display layer showed raw items without accounting for the gap between item sums and printed footer totals. Under-extraction meant some credits/debits were invisible; phantom entries couldn't be flagged.

**Fix (Option C)**:
1. **MISC absorb row**: raw path appends a MISC row for `(printedTotal − itemSum)` when the difference exceeds `ITEM_SUM_TOLERANCE`. Turns invisible residuals into a visible, labeled row.
2. **Mismatch banner**: after MISC absorb, any remaining positive mismatch (items > printed total = phantom entry) fires an `AppColors.Warning` banner in `LedgerTableFooter` with the over-count amount.

Commit: `b33f034`. Tests: `ReplicaUtilsMismatchTest` (9 cases).
