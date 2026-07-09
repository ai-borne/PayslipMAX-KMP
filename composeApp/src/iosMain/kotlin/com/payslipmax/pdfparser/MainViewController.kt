package com.payslipmax.pdfparser

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.payslipmax.pdfparser.auth.AuthTokenProvider
import com.payslipmax.pdfparser.di.appModule
import com.payslipmax.pdfparser.di.sharedModule
import com.payslipmax.pdfparser.nav.AppNavState
import com.payslipmax.pdfparser.nav.NavBridge
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.screens.HelpLegalScreen
import com.payslipmax.pdfparser.ui.screens.RepresentationScreen
import com.payslipmax.pdfparser.ui.screens.RetirementPlanningScreen
import com.payslipmax.pdfparser.ui.screens.TaxPlanningScreen
import com.payslipmax.pdfparser.ui.theme.PDFParserTheme
import com.payslipmax.pdfparser.ui.theme.resolveDarkTheme
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatformTools
import platform.UIKit.UIViewController

fun getAuthTokenProvider(): AuthTokenProvider = AuthTokenProvider()

private fun ensureKoin() {
    if (KoinPlatformTools.defaultContext().getOrNull() == null) {
        startKoin { modules(sharedModule, appModule) }
    }
}

/**
 * Kotlin side of the iOS native back-navigation bridge (Phase 4). Owns the shared
 * [PayslipViewModel], [AppNavState] and [NavBridge], and hands Swift:
 *  - [rootViewController]: the tab-root Compose tree (Scaffold + bottom bar + current tab),
 *    with detail navigation routed to the native stack via [NavBridge.navigateToDetail].
 *  - [detailViewController]: a thin `ComposeUIViewController` per pushed detail screen, reusing
 *    the existing screen composables unchanged (they already take a plain `onBack`).
 *  - [onNativePopObserved]: called by the Swift `UINavigationControllerDelegate` after any native
 *    pop (including an edge-swipe) so [AppNavState] stays in sync without re-triggering a pop.
 *
 * @param pushScreen Swift closure that pushes the native VC for a [Screen] name.
 * @param requestPop Swift closure that pops the top native VC.
 */
class IosNavHost(
    private val onPickPdf: (onResult: (ByteArray, String) -> Unit) -> Unit,
    private val onOpenPdf: (ByteArray, String) -> Unit,
    pushScreen: (screenName: String) -> Unit,
    requestPop: () -> Unit,
) {
    private val viewModel: PayslipViewModel =
        KoinPlatformTools.defaultContext().get().get()
    private val navState = AppNavState()
    private val bridge =
        NavBridge(
            navState = navState,
            isLocked = { viewModel.uiState.value.isAppLocked },
            pushToNative = { screen -> pushScreen(screen.name) },
            popFromNative = requestPop,
        )

    fun rootViewController(): UIViewController =
        // App() themes itself internally (required for Android, which calls App() directly), so the
        // wrapper's theme is redundant-but-harmless here — the point is that ALL iOS VCs go through
        // one themed factory, leaving no un-themed path to create by accident.
        themedViewController {
            App(
                viewModel = viewModel,
                onPickPdf = onPickPdf,
                onOpenPdf = onOpenPdf,
                navState = navState,
                nativeDetailNavigator = { screen -> bridge.navigateToDetail(screen) },
            )
        }

    fun detailViewController(screenName: String): UIViewController {
        val onBack = { bridge.requestPop() }
        return themedViewController {
            when (Screen.valueOf(screenName)) {
                Screen.Representation -> RepresentationScreen(viewModel = viewModel, onBack = onBack)
                Screen.TaxPlanning -> TaxPlanningScreen(viewModel = viewModel, onBack = onBack)
                Screen.RetirementPlanning -> RetirementPlanningScreen(viewModel = viewModel, onBack = onBack)
                else -> HelpLegalScreen(onBack = onBack)
            }
        }
    }

    /**
     * The single approved way to host Compose in a `UIViewController` on iOS: every VC is its own
     * independent Compose tree that does NOT inherit any provider (theme, etc.) from another VC, so
     * this factory applies [PDFParserTheme] centrally. Routing all VC creation through here makes an
     * un-themed screen — the Phase 4 regression where pushed detail screens rendered in the default
     * light theme — impossible to introduce by forgetting a wrapper. Enforced by the structural
     * guard in `check_tech_debt_limits.py` (no bare `ComposeUIViewController` elsewhere in iosMain).
     */
    private fun themedViewController(content: @Composable () -> Unit): UIViewController =
        ComposeUIViewController {
            val uiState by viewModel.uiState.collectAsState()
            PDFParserTheme(darkTheme = resolveDarkTheme(uiState.appTheme)) {
                // Mirror Android's App Scaffold host: a Surface establishes both the themed background
                // and the LocalContentColor baseline. Without it, content that inherits the content
                // color (e.g. the ScreenBackHeader back arrow) falls back to Compose's default
                // near-black and disappears in dark mode — a standalone VC has no Scaffold ancestor.
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    content()
                }
            }
        }

    /** Called by the Swift nav delegate after the stack returns to the root (native pop/swipe). */
    fun onNativePopObserved() {
        bridge.onNativePopObserved()
    }
}

/** Factory Swift calls to build the nav host, initializing Koin on first use. */
fun createNavHost(
    onPickPdf: (onResult: (ByteArray, String) -> Unit) -> Unit,
    onOpenPdf: (ByteArray, String) -> Unit,
    pushScreen: (screenName: String) -> Unit,
    requestPop: () -> Unit,
): IosNavHost {
    ensureKoin()
    return IosNavHost(onPickPdf, onOpenPdf, pushScreen, requestPop)
}
