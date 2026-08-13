package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.billing.FakeBillingManager
import com.payslipmax.pdfparser.billing.PurchaseResult
import com.payslipmax.pdfparser.billing.SubscriptionState
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.subscription.FeatureGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PayslipViewModelBillingTest {
    private val testDispatcher = StandardTestDispatcher()
    private val fakeBillingManager = FakeBillingManager(SubscriptionState.Inactive)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun launchPurchaseFlow_triggers_billingManager_and_grants_entitlement_on_success() =
        runTest {
            val repository =
                PayslipRepository(
                    com.payslipmax.pdfparser.testing.FakePayslipDao(),
                    com.payslipmax.pdfparser.testing.FakePdfParser(),
                    Dispatchers.Unconfined,
                )

            val viewModel =
                PayslipViewModel(
                    repository = repository,
                    billingManager = fakeBillingManager,
                )

            fakeBillingManager.shouldPurchaseSucceed = true

            var purchaseResult: PurchaseResult? = null
            viewModel.launchPurchaseFlow { result ->
                purchaseResult = result
            }

            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(purchaseResult is PurchaseResult.Success)
            assertTrue(viewModel.subscriptionManager.hasAccess(FeatureGate.PREMIUM_INTELLIGENCE))
        }

    @Test
    fun launchPurchaseFlow_handles_cancellation() =
        runTest {
            val repository =
                PayslipRepository(
                    com.payslipmax.pdfparser.testing.FakePayslipDao(),
                    com.payslipmax.pdfparser.testing.FakePdfParser(),
                    Dispatchers.Unconfined,
                )

            val viewModel =
                PayslipViewModel(
                    repository = repository,
                    billingManager = fakeBillingManager,
                )

            fakeBillingManager.shouldPurchaseSucceed = false
            fakeBillingManager.mockErrorMessage = "USER_CANCELLED"

            var purchaseResult: PurchaseResult? = null
            viewModel.launchPurchaseFlow { result ->
                purchaseResult = result
            }

            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(purchaseResult is PurchaseResult.Error)
        }

    @Test
    fun restorePurchases_triggers_billingManager_and_grants_entitlement_on_success() =
        runTest {
            val repository =
                PayslipRepository(
                    com.payslipmax.pdfparser.testing.FakePayslipDao(),
                    com.payslipmax.pdfparser.testing.FakePdfParser(),
                    Dispatchers.Unconfined,
                )

            val viewModel =
                PayslipViewModel(
                    repository = repository,
                    billingManager = fakeBillingManager,
                )

            fakeBillingManager.shouldRestoreSucceed = true

            var restoreResult: PurchaseResult? = null
            viewModel.restorePurchases { result ->
                restoreResult = result
            }

            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(restoreResult is PurchaseResult.Success)
            assertTrue(viewModel.subscriptionManager.hasAccess(FeatureGate.PREMIUM_INTELLIGENCE))
        }

    @Test
    fun restorePurchases_surfaces_error_without_granting_entitlement() =
        runTest {
            val repository =
                PayslipRepository(
                    com.payslipmax.pdfparser.testing.FakePayslipDao(),
                    com.payslipmax.pdfparser.testing.FakePdfParser(),
                    Dispatchers.Unconfined,
                )

            val viewModel =
                PayslipViewModel(
                    repository = repository,
                    billingManager = fakeBillingManager,
                )

            fakeBillingManager.shouldRestoreSucceed = false
            fakeBillingManager.mockErrorMessage = "No active verified subscription found"

            var restoreResult: PurchaseResult? = null
            viewModel.restorePurchases { result ->
                restoreResult = result
            }

            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(restoreResult is PurchaseResult.Error)
        }

    @Test
    fun premiumPriceState_updates_from_live_billingManager_price() =
        runTest {
            fakeBillingManager.fakeFormattedPrice = "₹199 / year"
            val repository =
                PayslipRepository(
                    com.payslipmax.pdfparser.testing.FakePayslipDao(),
                    com.payslipmax.pdfparser.testing.FakePdfParser(),
                    Dispatchers.Unconfined,
                )

            val viewModel =
                PayslipViewModel(
                    repository = repository,
                    billingManager = fakeBillingManager,
                )

            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals("₹199 / year", viewModel.premiumPriceState.value)
        }

    @Test
    fun premiumPriceState_keeps_fallback_when_billingManager_price_unavailable() =
        runTest {
            fakeBillingManager.fakeFormattedPrice = null
            val repository =
                PayslipRepository(
                    com.payslipmax.pdfparser.testing.FakePayslipDao(),
                    com.payslipmax.pdfparser.testing.FakePdfParser(),
                    Dispatchers.Unconfined,
                )

            val viewModel =
                PayslipViewModel(
                    repository = repository,
                    billingManager = fakeBillingManager,
                )

            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(com.payslipmax.pdfparser.ui.theme.AppStrings.settingsPremiumPlanPrice, viewModel.premiumPriceState.value)
        }
}
