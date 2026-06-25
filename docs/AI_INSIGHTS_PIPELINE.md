# AI Insights Architecture & Pipeline Documentation

This document serves as the master architectural reference for PayslipMax's **AI Insights Pipeline**. 

PayslipMax is an offline-first military payslip intelligence platform designed for Indian Army officers. It extracts structured salary data from monthly PCDA(O) PDF payslips and turns it into wealth optimization suggestions (tax planning, DSOP optimization) and error detection alerts (missing allowances, deduction spikes). AI-driven auditing is offered as a premium subscription tier.

Due to strict modularity guidelines, this documentation is partitioned into specialized, self-contained modules.

---

## Technical Index

1. **[01. User Journey & Sequence Diagrams](file:///Users/test/Downloads/PDFParser/docs/pipeline/01_user_journey.md)**
   - High-level user flow from PDF upload to local database storage, premium gating, and insight delivery.
2. **[02. Data Pipeline & Schema Design](file:///Users/test/Downloads/PDFParser/docs/pipeline/02_data_pipeline.md)**
   - Validation, duplicate check, OCR fallbacks, normalization, and database schemas.
3. **[03. AI Pipeline & Context Strategy](file:///Users/test/Downloads/PDFParser/docs/pipeline/03_ai_pipeline.md)**
   - Gemini proxy cloud flow, prompt assembly, and a cost/value evaluation of historical payslip context.
4. **[04. AI Provider Abstraction Layer](file:///Users/test/Downloads/PDFParser/docs/pipeline/04_ai_abstraction.md)**
   - Contract design for local on-device migrations (Core AI, Android Edge SDK, Gemma, Mistral, Llama).
5. **[05. Premium Feature Architecture](file:///Users/test/Downloads/PDFParser/docs/pipeline/05_premium_architecture.md)**
   - Monetization model, free vs. premium feature matrix, gating decorators, and secure verification flows.
6. **[06. Accrued Intelligence, Moats & Roadmap](file:///Users/test/Downloads/PDFParser/docs/pipeline/06_moat_and_scalability.md)**
   - Five-year intelligence compounding timeline, defensive moats, and the phased cloud-to-local scalability roadmap.

---

## Recommended Final Architecture

The combined current-state (Cloud Proxy) and future-state (Local Provider) architecture operates on a decoupled provider-agnostic design:

```mermaid
graph TD
    User([User App UI]) -->|1. Uploads PDF| UploadL[Upload & Validation Layer]
    UploadL -->|2. Extracts Text| ParseL[Parsing & Extraction Layer]
    ParseL -->|3. Local SQLite Snapshot| DB[(Salary Database & Ledger)]
    
    DB -->|4. Trigger Audit| Engine[Insight Engine / ViewModel]
    Engine -->|5. Verify Subscription| PremiumGate{Premium Gate}
    
    PremiumGate -->|Free Tier| RenderFree[Render Basic Charts & Anomalies]
    PremiumGate -->|Premium Tier| Abstraction[AI Abstraction Layer]
    
    Abstraction -->|Active: Cloud Provider| GeminiProxy[Cloud Gemini API Proxy]
    Abstraction -->|Active: Local Provider| LocalAI[Local AI Engine: CoreAI / Android Edge SDK / Gemma]
    
    GeminiProxy -->|JSON Response| SaveRepo[Intelligence Repository]
    LocalAI -->|JSON Response| SaveRepo
    
    SaveRepo -->|Cache JSON| DB
    SaveRepo -->|6. Render Native UI Views| User
```

This master architecture ensures that the presentation layer (UI) and core business logic (Insight Engine) are completely shielded from the underlying AI execution environment.

---

## Implementation Status (Current Standing)

As of the latest sprint completion, the AI Insights Pipeline has transitioned from Phase 1 (Cloud-Only) to **Phase 2 (Hybrid AI)**:

1. **Deterministic Rules Engine Partitioning**:
   - Refactored [DeterministicIntelligenceEngine](file:///Users/test/Downloads/PDFParser/shared/src/commonMain/kotlin/com/ssbmax/pdfparser/insights/DeterministicIntelligenceEngine.kt) to comply with the 300-line limit by creating specialized, single-purpose rule auditor classes under the `RuleAuditor` interface:
     - [DaArrearsAuditor](file:///Users/test/Downloads/PDFParser/shared/src/commonMain/kotlin/com/ssbmax/pdfparser/insights/DaArrearsAuditor.kt): Mathematical progression checks on Dearness Allowance adjustments.
     - [MarriedQuartersRiskAuditor](file:///Users/test/Downloads/PDFParser/shared/src/commonMain/kotlin/com/ssbmax/pdfparser/insights/MarriedQuartersRiskAuditor.kt): Retroactive rent recovery risk detection.
     - [UnexpectedDebitAuditor](file:///Users/test/Downloads/PDFParser/shared/src/commonMain/kotlin/com/ssbmax/pdfparser/insights/UnexpectedDebitAuditor.kt): Identification of abnormal pay ledger recovery recoveries.
     - [DsopComplianceAuditor](file:///Users/test/Downloads/PDFParser/shared/src/commonMain/kotlin/com/ssbmax/pdfparser/insights/DsopComplianceAuditor.kt): Audits statutory 6% minimum savings targets and static savings periods.
     - [TaxProjectionAuditor](file:///Users/test/Downloads/PDFParser/shared/src/commonMain/kotlin/com/ssbmax/pdfparser/insights/TaxProjectionAuditor.kt): Forecasting annual income tax trajectories based on YTD values.
     - Migrated existing rules ([SalaryLossAuditor](file:///Users/test/Downloads/PDFParser/shared/src/commonMain/kotlin/com/ssbmax/pdfparser/insights/SalaryLossAuditor.kt), [MissingAllowanceAuditor](file:///Users/test/Downloads/PDFParser/shared/src/commonMain/kotlin/com/ssbmax/pdfparser/insights/MissingAllowanceAuditor.kt), and [TptaEntitlementAuditor](file:///Users/test/Downloads/PDFParser/shared/src/commonMain/kotlin/com/ssbmax/pdfparser/insights/TptaEntitlementAuditor.kt)) to this modular layout.

2. **Prioritization & Dashboard Capping**:
   - Implemented [InsightPrioritizationEngine](file:///Users/test/Downloads/PDFParser/shared/src/commonMain/kotlin/com/ssbmax/pdfparser/insights/InsightPrioritizationEngine.kt) to score generated alerts using a 5-dimension weighted formula, filtering out alerts scoring below 7.0.
   - Implemented display capping to limit the dashboard alerts to the top 4 active items.
   - Standardized financial value (₹) as the descending tie-breaker.

3. **Decoupled AI Provider Architecture**:
   - Introduced [AIInsightProvider](file:///Users/test/Downloads/PDFParser/shared/src/commonMain/kotlin/com/ssbmax/pdfparser/insights/AIInsightProvider.kt) interface to isolate business logic from model execution.
   - Implemented [GeminiCloudProvider](file:///Users/test/Downloads/PDFParser/shared/src/commonMain/kotlin/com/ssbmax/pdfparser/insights/GeminiCloudProvider.kt) for cloud-based inference and [LocalGemmaProvider](file:///Users/test/Downloads/PDFParser/shared/src/commonMain/kotlin/com/ssbmax/pdfparser/insights/LocalGemmaProvider.kt) as a placeholder for offline on-device execution.
   - Implemented [AIProviderManager](file:///Users/test/Downloads/PDFParser/shared/src/commonMain/kotlin/com/ssbmax/pdfparser/insights/AIProviderManager.kt) to coordinate provider selection.

4. **Preferences & Settings Toggle**:
   - Migrated local Room Database to Schema Version 7 to add `useLocalAi` preference to [AppSettingsEntity](file:///Users/test/Downloads/PDFParser/shared/src/commonMain/kotlin/com/ssbmax/pdfparser/database/AppSettingsEntity.kt).
   - Implemented a togglable preference switch in `SettingsScreen.kt` permitting the user to choose between the Cloud model and the Local Gemma model.

5. **Hardware-Backed Device Keys & Secure Backups (TDD)**:
   - Transitioned local database encryption from hardcoded string fallbacks to unique hardware-backed keys utilizing `Android Keystore` and `iOS Keychain Services` via `CryptoHelper.getDatabaseSecretKey()`.
   - Implemented a secure cross-device porting flow: universal backups (.json) decrypt database records using the source device key, encrypt with the transit password, and re-encrypt with the target device's key on import.
   - Built a robust TDD test suite to verify encryption uniqueness across different devices and compatibility of the data-porting migration.
   - **Auto-Backup & Keystore Gotcha Fixed**: Disabled Android Auto Backup (`allowBackup="false"`) in the Android Manifest to prevent restoring database files without their corresponding hardware-bound Keystore keys (which causes permanent `BAD_DECRYPT` errors on reinstall).
   - **Self-Healing Decryption Recovery**: Implemented automatic fallback decryption using the legacy key `"PCDAPayslipOfflineSecret2026!"` for backward compatibility, and a self-healing database recovery routine that automatically wipes corrupt/undecryptable data on permanent key loss to prevent app bricking.

6. **Dynamic Offline AI Provider & Core Launch Blockers (TDD)**:
   - **Dynamic Offline AI Provider:** Refactored [LocalGemmaProvider](file:///Users/test/Downloads/PDFParser/shared/src/commonMain/kotlin/com/ssbmax/pdfparser/insights/LocalGemmaProvider.kt) to dynamically construct `AiInsightReport` JSON containing extracted anomalies and wealth optimization opportunities instead of a static stub. Added test coverage in [LocalGemmaProviderTest.kt](file:///Users/test/Downloads/PDFParser/shared/src/commonTest/kotlin/com/ssbmax/pdfparser/insights/LocalGemmaProviderTest.kt).
   - **Structured KMP Logger:** Added multiplatform [Logger](file:///Users/test/Downloads/PDFParser/shared/src/commonMain/kotlin/com/ssbmax/pdfparser/logging/Logger.kt) in `commonMain` to replace all `println()` calls in payslip text parsers with tag-structured debug logging, automatically disabled in production/release configurations.
   - **Interactive DSOP Simulator:** Upgraded [DsopSimulatorSection](file:///Users/test/Downloads/PDFParser/composeApp/src/commonMain/kotlin/com/ssbmax/pdfparser/ui/screens/DsopSimulatorSection.kt) to accept user-specific Initial Balance and Monthly Subscription inputs, dynamically invoking the shared math projection engine ([ProjectionMath](file:///Users/test/Downloads/PDFParser/shared/src/commonMain/kotlin/com/ssbmax/pdfparser/insights/ProjectionMath.kt)).
   - **Native Sharing Framework:** Implemented native common-share logic via `expect`/`actual` function [shareText](file:///Users/test/Downloads/PDFParser/shared/src/commonMain/kotlin/com/ssbmax/pdfparser/utils/ShareUtils.kt) utilizing `Intent.ACTION_SEND` on Android and `UIActivityViewController` on iOS. Integrated a native "Share" button in [RepresentationScreen](file:///Users/test/Downloads/PDFParser/composeApp/src/commonMain/kotlin/com/ssbmax/pdfparser/ui/screens/RepresentationScreen.kt).
   - **Collapsible Historical Ledger UI:** Created the collapsible [HistoricalLedgerCard](file:///Users/test/Downloads/PDFParser/composeApp/src/commonMain/kotlin/com/ssbmax/pdfparser/ui/screens/HistoricalLedgerCard.kt) component to format and present all historical parsed pay ledger details (Gross, Net, Basic, DSOP, and Tax) in a sorted tabular view under the STATEMENTS tab in [HistoryScreen](file:///Users/test/Downloads/PDFParser/composeApp/src/commonMain/kotlin/com/ssbmax/pdfparser/ui/screens/HistoryScreen.kt), resolving UI/data visualization limitations.

7. **Passcode Recovery & Lock Screen Flow (TDD)**:
   - Implemented passcode reset mechanism (`resetPinWithPdf`) in [PayslipViewModelExtensions.kt](file:///Users/test/Downloads/PDFParser/composeApp/src/commonMain/kotlin/com/ssbmax/pdfparser/ui/PayslipViewModelExtensions.kt) to decrypt and verify ownership using statement PDF decryption and PAN matching before disabling/clearing passcode locks.
   - Added wrong-passcode shake animation (using Jetpack Compose keyframe animations) to the passcode dot indicators in [LockScreen.kt](file:///Users/test/Downloads/PDFParser/composeApp/src/commonMain/kotlin/com/ssbmax/pdfparser/ui/screens/LockScreen.kt).
   - Extracted passcode recovery dialog into [ResetPinDialog.kt](file:///Users/test/Downloads/PDFParser/composeApp/src/commonMain/kotlin/com/ssbmax/pdfparser/ui/screens/ResetPinDialog.kt) to strictly preserve the 300-lines-per-file and 50-lines-per-composable constraints.
   - Fixed locking/unlocking unit tests in [PayslipViewModelSettingsTest.kt](file:///Users/test/Downloads/PDFParser/composeApp/src/commonTest/kotlin/com/ssbmax/pdfparser/ui/PayslipViewModelSettingsTest.kt).

8. **Navigation & Highlights Polish (TDD)**:
   - Modified `calculateAiHighlights` in [FreeInsightsComponents.kt](file:///Users/test/Downloads/PDFParser/composeApp/src/commonMain/kotlin/com/ssbmax/pdfparser/ui/screens/FreeInsightsComponents.kt) to de-duplicate Dearness Allowance and House Rent Allowance highlights when they are also highlighted as the biggest component change.
   - Added a "Go to Insights Screen" button inside the Empty AI Reports state in [HistoryAiComponents.kt](file:///Users/test/Downloads/PDFParser/composeApp/src/commonMain/kotlin/com/ssbmax/pdfparser/ui/screens/HistoryAiComponents.kt) to guide users when no reports are saved, and passed navigation callbacks down from [App.kt](file:///Users/test/Downloads/PDFParser/composeApp/src/commonMain/kotlin/com/ssbmax/pdfparser/App.kt) through [HistoryScreen.kt](file:///Users/test/Downloads/PDFParser/composeApp/src/commonMain/kotlin/com/ssbmax/pdfparser/ui/screens/HistoryScreen.kt).
   - Added unit test cases for the highlight de-duplication in [FreeInsightsLogicTest.kt](file:///Users/test/Downloads/PDFParser/composeApp/src/commonTest/kotlin/com/ssbmax/pdfparser/ui/screens/FreeInsightsLogicTest.kt).

9. **Robust Token Preservation & Regex Parser Fix (TDD)**:
   - **Newline-Preserving Cleaner**: Implemented `cleanPreservingNewlines` in [ParserUtils.kt](file:///Users/test/Downloads/PDFParser/shared/src/commonMain/kotlin/com/ssbmax/pdfparser/parser/ParserUtils.kt) to clean Hindi transliterations and format-specific artifacts while maintaining newlines, preventing header keys from prepending and polluting parsed items.
   - **Multi-Line Token Reconstruction**: Refactored [DynamicSpatialParser.kt](file:///Users/test/Downloads/PDFParser/shared/src/commonMain/kotlin/com/ssbmax/pdfparser/parser/DynamicSpatialParser.kt) to parse line-by-line first, filtering out header/sentence lines, and then joining the remainder with space separators. This correctly reconstructs key-value pairs (like `ARR-DA`, `ARR-TPTADA`, and `ETKT`) even when PDFBox extracts keys and their numeric values on separate lines.
   - **Strict Regression Suite**: Added comprehensive unit test coverage in [PayslipTextParser2026Test.kt](file:///Users/test/Downloads/PDFParser/shared/src/commonTest/kotlin/com/ssbmax/pdfparser/parser/PayslipTextParser2026Test.kt) and verified full compatibility with all 46 historical payslips in [PlatformPdfParserTest.kt](file:///Users/test/Downloads/PDFParser/shared/src/androidUnitTest/kotlin/com/ssbmax/pdfparser/parser/PlatformPdfParserTest.kt).
