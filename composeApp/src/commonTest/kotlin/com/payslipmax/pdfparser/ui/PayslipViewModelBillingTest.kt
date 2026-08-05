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
}
