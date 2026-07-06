package com.payslipmax.pdfparser.insights.gemma

import com.google.android.play.core.assetpacks.AssetPackManager
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import com.payslipmax.pdfparser.crypto.ContextHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Play Asset Delivery-backed installer for the Tier 6 base model (on-demand asset pack
 * `gemma_model_pack`, see `gemmaModelPack/build.gradle.kts`). Delegates all Play Core interaction to
 * [GemmaAssetPackGateway] so this class stays unit-testable with a fake gateway instead of the real
 * `AssetPackManager`/`Task` machinery.
 *
 * [gatewayProvider] is invoked lazily, only from inside [install]'s try/catch, rather than as an
 * eagerly-evaluated constructor default — `PayslipViewModel`'s default constructor argument builds
 * one of these on every plain-JVM unit test that doesn't inject a fake, where `ContextHolder.context`
 * is never set; resolving Play Core eagerly would crash every such test at construction time instead
 * of degrading to [BaseModelInstallState.Failed] like any other install failure.
 */
class AndroidGemmaBaseModelInstaller(
    private val gatewayProvider: () -> GemmaAssetPackGateway = {
        DefaultGemmaAssetPackGateway(
            AssetPackManagerFactory.getInstance(
                checkNotNull(ContextHolder.context) {
                    "ContextHolder.context not set — PayslipApplication.onCreate() must run first"
                },
            ),
        )
    },
) : GemmaBaseModelInstaller {
    private val _state = MutableStateFlow<BaseModelInstallState>(BaseModelInstallState.NotStarted)
    override val state: StateFlow<BaseModelInstallState> = _state.asStateFlow()

    private var gateway: GemmaAssetPackGateway? = null

    override suspend fun install() {
        _state.value =
            try {
                val resolvedGateway =
                    gateway ?: gatewayProvider().also { newGateway ->
                        gateway = newGateway
                        newGateway.registerListener(PACK_NAME) { _state.value = it }
                    }
                resolvedGateway.fetch(PACK_NAME)
            } catch (e: Exception) {
                BaseModelInstallState.Failed(e.message ?: "Asset pack fetch failed")
            }
    }

    companion object {
        /** Must match gemmaModelPack/build.gradle.kts's `assetPack { packName.set(...) }`. */
        const val PACK_NAME = "gemma_model_pack"

        /**
         * Registered once from `MainActivity.onCreate()` to
         * `{ manager -> manager.showConfirmationDialog(launcher) }` — Play Asset Delivery's
         * cellular-consent / unrecognized-app confirmation needs an Activity-scoped
         * `ActivityResultLauncher`, which this Activity-agnostic installer cannot itself own.
         */
        var confirmationHandler: ((AssetPackManager) -> Unit)? = null
    }
}

actual fun provideGemmaBaseModelInstaller(): GemmaBaseModelInstaller = AndroidGemmaBaseModelInstaller()
