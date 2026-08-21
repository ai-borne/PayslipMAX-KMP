package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.billing.FakeBillingManager
import com.payslipmax.pdfparser.billing.SubscriptionState
import com.payslipmax.pdfparser.database.AppSettingsEntity
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.subscription.DevOverride
import com.payslipmax.pdfparser.subscription.FeatureGate
import com.payslipmax.pdfparser.subscription.SubscriptionManager
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionLifecycleSyncTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun createRepository(initialPremium: Boolean = false): PayslipRepository {
        val dao = FakePayslipDao()
        dao.insertSettings(AppSettingsEntity(isPremiumEnabled = initialPremium))
        return PayslipRepository(dao, FakePdfParser(), Dispatchers.Unconfined)
    }

    @Test
    fun activeSubscriptionState_syncsTrueToRoomDb() =
        runTest {
            val fakeBillingManager = FakeBillingManager(SubscriptionState.Active())
            val repository = createRepository(initialPremium = false)

            val viewModel =
                PayslipViewModel(
                    repository = repository,
                    billingManager = fakeBillingManager,
                )

            testDispatcher.scheduler.advanceUntilIdle()

            val settings = repository.getSettings()
            assertTrue(settings?.isPremiumEnabled == true, "Active subscription state must sync isPremiumEnabled=true to DB")
            assertTrue(viewModel.uiState.value.isPremiumEnabled, "UI state must reflect true")
        }

    @Test
    fun inactiveSubscriptionState_syncsFalseToRoomDb() =
        runTest {
            val fakeBillingManager = FakeBillingManager(SubscriptionState.Inactive)
            val repository = createRepository(initialPremium = true)

            val viewModel =
                PayslipViewModel(
                    repository = repository,
                    billingManager = fakeBillingManager,
                )

            testDispatcher.scheduler.advanceUntilIdle()

            val settings = repository.getSettings()
            assertFalse(settings?.isPremiumEnabled == true, "Inactive subscription state must sync isPremiumEnabled=false to DB")
            assertFalse(viewModel.uiState.value.isPremiumEnabled, "UI state must reflect false")
        }

    @Test
    fun unknownOfflineState_preservesExistingRoomDbEntitlement() =
        runTest {
            val fakeBillingManager = FakeBillingManager(SubscriptionState.Unknown)
            val repository = createRepository(initialPremium = true)

            val viewModel =
                PayslipViewModel(
                    repository = repository,
                    billingManager = fakeBillingManager,
                )

            testDispatcher.scheduler.advanceUntilIdle()

            val settings = repository.getSettings()
            assertTrue(settings?.isPremiumEnabled == true, "Unknown offline state must preserve existing DB access")
            assertTrue(viewModel.uiState.value.isPremiumEnabled, "UI state must remain true")
        }

    @Test
    fun stateTransitionFromActiveToInactive_dynamicallyRevokesAccess() =
        runTest {
            val fakeBillingManager = FakeBillingManager(SubscriptionState.Active())
            val repository = createRepository(initialPremium = false)

            val viewModel =
                PayslipViewModel(
                    repository = repository,
                    billingManager = fakeBillingManager,
                )
            viewModel.subscriptionManager.setDevOverride(DevOverride.FOLLOW_FLAG)

            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(viewModel.subscriptionManager.hasAccess(FeatureGate.PREMIUM_INTELLIGENCE))

            // Subscription expires or is cancelled/refunded
            fakeBillingManager.setSubscriptionState(SubscriptionState.Inactive)
            testDispatcher.scheduler.advanceUntilIdle()

            assertFalse(viewModel.subscriptionManager.hasAccess(FeatureGate.PREMIUM_INTELLIGENCE), "Revocation must deny access")
            assertFalse(repository.getSettings()?.isPremiumEnabled == true, "DB must be synced to false")
        }

    @Test
    fun hierarchicalSSOT_whenBillingInactiveAndDbTrue_deniesAccess() {
        val fakeBillingManager = FakeBillingManager(SubscriptionState.Inactive)
        val subscriptionManager =
            SubscriptionManager(
                isPremiumEnabledProvider = { true },
                isDebugBuildProvider = { false },
                billingManager = fakeBillingManager,
            )

        assertFalse(
            subscriptionManager.hasAccess(FeatureGate.PREMIUM_INTELLIGENCE),
            "Authoritative online Inactive state must take precedence over stale local DB true",
        )
    }

    @Test
    fun hierarchicalSSOT_whenBillingUnknownAndDbTrue_grantsAccess() {
        val fakeBillingManager = FakeBillingManager(SubscriptionState.Unknown)
        val subscriptionManager =
            SubscriptionManager(
                isPremiumEnabledProvider = { true },
                isDebugBuildProvider = { false },
                billingManager = fakeBillingManager,
            )

        assertTrue(
            subscriptionManager.hasAccess(FeatureGate.PREMIUM_INTELLIGENCE),
            "Unknown offline state must fall back to local DB true",
        )
    }
}
