package com.ssbmax.pdfparser.subscription

enum class FeatureGate {
    PREMIUM_INTELLIGENCE,
    WEALTH_OPTIMIZATION,
    AI_AUDIT,
    TAX_PLANNER,
    DSOP_SIMULATOR,
    ANOMALY_DETECTION,
    CLAIM_GENERATOR,
}

interface SubscriptionService {
    fun hasAccess(feature: FeatureGate): Boolean
}

class SubscriptionManager(
    private val isPremiumEnabledProvider: () -> Boolean,
) : SubscriptionService {
    var isDeveloperPro = false

    fun enableDeveloperPro() {
        isDeveloperPro = true
    }

    override fun hasAccess(feature: FeatureGate): Boolean {
        if (isDeveloperPro) {
            return true
        }
        return isPremiumEnabledProvider()
    }
}
