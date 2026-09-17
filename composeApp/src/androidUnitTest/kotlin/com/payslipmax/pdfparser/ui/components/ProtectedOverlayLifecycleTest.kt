package com.payslipmax.pdfparser.ui.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.payslipmax.pdfparser.ui.theme.AppStrings
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProtectedOverlayLifecycleTest {
    @AfterTest
    fun tearDown() {
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    private class TestLifecycleOwner(
        initialState: Lifecycle.State = Lifecycle.State.INITIALIZED,
    ) : LifecycleOwner {
        private val registry = LifecycleRegistry(this).apply { currentState = initialState }
        override val lifecycle: Lifecycle get() = registry

        fun handleEvent(event: Lifecycle.Event) {
            registry.handleLifecycleEvent(event)
        }

        fun setState(state: Lifecycle.State) {
            registry.currentState = state
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun whenStateIsResumed_overlayIsNotDisplayed() =
        runComposeUiTest {
            val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)
            setContent {
                PayslipMaxProtectedHost(lifecycleOwner = lifecycleOwner) {
                    Text("Confidential Salary Details")
                }
            }

            onNodeWithText("Confidential Salary Details").assertIsDisplayed()
            onNodeWithText(AppStrings.appProtectedTitle).assertDoesNotExist()
            onNodeWithContentDescription(AppStrings.appProtectedShieldDesc).assertDoesNotExist()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun whenStateIsPaused_overlayIsDisplayed() =
        runComposeUiTest {
            val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)
            setContent {
                PayslipMaxProtectedHost(lifecycleOwner = lifecycleOwner) {
                    Text("Confidential Salary Details")
                }
            }

            lifecycleOwner.handleEvent(Lifecycle.Event.ON_PAUSE)

            onNodeWithText(AppStrings.appProtectedTitle).assertIsDisplayed()
            onNodeWithContentDescription(AppStrings.appProtectedShieldDesc).assertIsDisplayed()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun whenStateIsStopped_overlayIsDisplayed() =
        runComposeUiTest {
            val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)
            setContent {
                PayslipMaxProtectedHost(lifecycleOwner = lifecycleOwner) {
                    Text("Confidential Salary Details")
                }
            }

            lifecycleOwner.handleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleOwner.handleEvent(Lifecycle.Event.ON_STOP)

            onNodeWithText(AppStrings.appProtectedTitle).assertIsDisplayed()
            onNodeWithContentDescription(AppStrings.appProtectedShieldDesc).assertIsDisplayed()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun whenStateReturnsToResumed_overlayIsDismissed() =
        runComposeUiTest {
            val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)
            setContent {
                PayslipMaxProtectedHost(lifecycleOwner = lifecycleOwner) {
                    Text("Confidential Salary Details")
                }
            }

            lifecycleOwner.handleEvent(Lifecycle.Event.ON_PAUSE)
            onNodeWithText(AppStrings.appProtectedTitle).assertIsDisplayed()

            lifecycleOwner.handleEvent(Lifecycle.Event.ON_RESUME)
            onNodeWithText(AppStrings.appProtectedTitle).assertDoesNotExist()
            onNodeWithText("Confidential Salary Details").assertIsDisplayed()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun whenStateIsCreatedOrPreResume_overlayIsDisplayed() =
        runComposeUiTest {
            val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.CREATED)
            setContent {
                PayslipMaxProtectedHost(lifecycleOwner = lifecycleOwner) {
                    Text("Confidential Salary Details")
                }
            }

            onNodeWithText(AppStrings.appProtectedTitle).assertIsDisplayed()

            lifecycleOwner.handleEvent(Lifecycle.Event.ON_START)
            lifecycleOwner.handleEvent(Lifecycle.Event.ON_RESUME)

            onNodeWithText(AppStrings.appProtectedTitle).assertDoesNotExist()
            onNodeWithText("Confidential Salary Details").assertIsDisplayed()
        }
}
