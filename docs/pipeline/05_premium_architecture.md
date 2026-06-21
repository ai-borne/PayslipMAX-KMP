# 05. Premium Feature Architecture

This document describes the subscription gating architecture, monetization rationale, and the technical boundaries separating free and paid features in PayslipMax.

---

## 1. Rationale for Gated AI Features

AI-powered features are placed behind a premium subscription tier for three reasons:
1. **AI Inference Costs**: Cloud LLM API transactions (tokens generated, thinking overhead) incur direct per-call costs. Monetizing these features prevents a denial-of-wallet vulnerability and guarantees unit-level profitability.
2. **Infrastructure Costs**: Maintaining secure serverless Firebase functions, GCP Secret Manager instances, and credential validation infrastructure adds ongoing server overhead.
3. **Product Differentiation**: Keeping basic charts free lowers user acquisition barriers, while advanced CA audits act as the high-value "hook" driving premium subscription conversions.

---

## 2. Feature Separation Matrix

The platform splits features to provide a conversion hook while protecting operational margin:

```mermaid
graph TD
    subgraph Free Tier [Free Tier: Acquisition Hook]
        A1[Standard PDF Parsing]
        A2[Basic Salary History Grid]
        A3[Monthly Ledger Totals Chart]
    end

    subgraph Premium Tier [Premium Tier: Revenue Moat]
        B1[Gemini AI CA Audits]
        B2[Missing Allowance Warnings]
        B3[PCDA PCDA(O) Claim Drafts]
        B4[Tax Optimization Planner]
        B5[Retirement DSOP Simulator]
    end
    
    User([User Request]) -->|Read Ledger| FreeTier
    User -->|Audit payslip| CheckSubscription{Active Subscription?}
    CheckSubscription -->|No| Teaser[Upgrade Bottom Sheet]
    CheckSubscription -->|Yes| PremiumTier
```

---

## 3. Subscription Gating Architecture

Subscription validation occurs on the client and is checked by the Cloud Functions gateway.

### Client-Side Gating Check (Kotlin Compose / Swift UI)
A repository state property is injected into the view model to conditionally direct navigation or show locks.

```kotlin
class InsightsViewModel(
    private val subscriptionManager: SubscriptionManager,
    private val aiRepository: FinancialIntelligenceRepository
) : ViewModel() {
    
    // UI state exposing subscription details
    val isPremiumUser: StateFlow<Boolean> = subscriptionManager.isPremium
    
    fun onTriggerAudit(monthKey: String) {
        if (!subscriptionManager.isPremium.value) {
            // Trigger UI event to display Premium Upgrade Bottom Sheet
            emitUiEvent(UiEvent.ShowUpgradePromo)
            return
        }
        
        // Proceed to invoke AI Abstraction Layer
        executeAudit(monthKey)
    }
}
```

### Server-Side Proxy Gate (Cloud Functions)
If a user bypasses client-side checks and calls the API directly, the proxy function checks the claims on the ID token:

```javascript
// Verification within functions/index.js
async function verifyPremiumStatus(uid) {
  const userRecord = await admin.auth().getUser(uid);
  // Custom Claim set via payment webhooks (Stripe / Apple / Google Billing)
  if (userRecord.customClaims && userRecord.customClaims.premium === true) {
    return true;
  }
  return false;
}
```

This dual-gated architecture ensures that cloud costs are protected at the server boundary while ensuring a smooth, instant-lock feedback loop in the client UI.
