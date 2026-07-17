# 04. AI Provider Abstraction Layer

This document outlines the architecture designed to decouple PayslipMax's core logic from specific AI models, enabling seamless transitions from cloud APIs (Gemini) to local, on-device AI engines (Apple Core AI, Android AI Edge, Gemma).

---

## 1. Provider Abstraction Contract

By wrapping AI logic in a standard contract interface, the presentation and repository layers are insulated from changes in API SDKs or local runtimes.

```mermaid
classDiagram
    class AIInsightProvider {
        <<interface>>
        +generateInsights(promptInput: PromptPayload) Result~String~
    }

    class GeminiCloudProvider {
        -proxyUrl: String
        -client: HttpClient
        +generateInsights(promptInput: PromptPayload) Result~String~
    }

    class AppleCoreAIProvider {
        -localModel: CoreMLModel
        +generateInsights(promptInput: PromptPayload) Result~String~
    }

    class AndroidEdgeAIProvider {
        -llmInference: LlmInference
        +generateInsights(promptInput: PromptPayload) Result~String~
    }

    class LocalGemmaProvider {
        -onnxRuntime: OrtSession
        +generateInsights(promptInput: PromptPayload) Result~String~
    }

    AIInsightProvider <|.. GeminiCloudProvider
    AIInsightProvider <|.. AppleCoreAIProvider
    AIInsightProvider <|.. AndroidEdgeAIProvider
    AIInsightProvider <|.. LocalGemmaProvider

    class AIProviderManager {
        -activeProvider: AIInsightProvider
        +getInsights(payload: PromptPayload) Result~AiInsightReport~
    }
    
    AIProviderManager --> AIInsightProvider
```

---

## 2. Kotlin Interface Definition

```kotlin
package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.database.LedgerRecordEntity
import com.payslipmax.pdfparser.domain.ParsedPayslip
import kotlinx.serialization.Serializable

/**
 * Common payload structure independent of target model API.
 */
@Serializable
data class PromptPayload(
    val currentMonthRawText: String,
    val sanitizedJsonData: String,
    val historicalSummaryText: String,
    val anomaliesCount: Int,
    val sanitizedPayslip: ParsedPayslip,
    val engineResult: EngineResult,
    val history: List<LedgerRecordEntity> = emptyList(),
    val authToken: String? = null
)

/**
 * Interface contract defining the AI execution engine.
 */
interface AIInsightProvider {
    /**
     * Executes the text generation request.
     * Returns a Result containing the raw JSON response string or an execution error.
     */
    suspend fun generateInsights(payload: PromptPayload): Result<String>
}
```

---

## 3. Execution & Selection Flow

The `AIProviderManager` evaluates network conditions, platform capabilities (iOS vs. Android), and user preferences to select and delegate work to the correct provider:

```mermaid
sequenceDiagram
    participant App as App ViewModel
    participant Manager as AIProviderManager
    participant CoreAI as AppleCoreAIProvider
    participant Cloud as GeminiCloudProvider

    App->>Manager: getInsights(payload)
    Manager->>Manager: Determine best provider
    alt Platform is iOS AND Local Model Available
        Manager->>CoreAI: generateInsights(payload)
        CoreAI->>CoreAI: Invoke local Core AI / Swift LLM
        CoreAI-->>Manager: Return raw JSON
    else Platform is Android OR Local Model Not Configured
        Manager->>Cloud: generateInsights(payload)
        Cloud->>Cloud: POST to Firebase Proxy
        Cloud-->>Manager: Return raw JSON
    end
    Manager->>Manager: Deserialize JSON string
    Manager-->>App: Return Result<AiInsightReport>
```

---

## 4. How Migrations Occur Without Rewriting Business Logic
1. **Separation of Concerns**: The ViewModel and UI only interact with `AIProviderManager` and the structured `AiInsightReport` KMP data class.
2. **Provider Swapping**: To add a new local model (e.g., Llama-3-8B running via ONNX on Android), developers create a new class implementing `AIInsightProvider` and register it inside `AIProviderManager`'s dependency resolver.
3. **No UI Changes**: The layout code doesn't care whether the JSON was generated in Google Cloud, in iOS Core AI, or by Gemma on an Android SoC. The output JSON schema is strictly verified, making the integration fully plug-and-play.
