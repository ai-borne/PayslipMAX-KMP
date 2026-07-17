package com.payslipmax.pdfparser.insights.gemma

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * In-memory [GemmaAssetPackGateway] so [AndroidGemmaBaseModelInstaller] can be exercised without
 * touching Play Core's real `AssetPackManager`/`Task` machinery.
 */
private class FakeGemmaAssetPackGateway : GemmaAssetPackGateway {
    var fetchResult: BaseModelInstallState = BaseModelInstallState.Installed("/fake/path")
    var fetchException: Exception? = null
    var registeredPackName: String? = null
    private var listener: ((BaseModelInstallState) -> Unit)? = null

    override suspend fun fetch(packName: String): BaseModelInstallState {
        fetchException?.let { throw it }
        return fetchResult
    }

    override fun registerListener(
        packName: String,
        onStateChanged: (BaseModelInstallState) -> Unit,
    ) {
        registeredPackName = packName
        listener = onStateChanged
    }

    fun emit(state: BaseModelInstallState) {
        listener?.invoke(state)
    }
}

class AndroidGemmaBaseModelInstallerTest {
    @Test
    fun initialStateIsNotStartedAndTheGatewayProviderIsNotInvokedEagerly() {
        // PayslipViewModel's default constructor argument builds one of these on every plain-JVM
        // unit test that doesn't inject a fake; if the real Play Core gateway were resolved eagerly
        // here (as it is not — only from install()), every such test would crash on construction
        // since ContextHolder.context is never set outside a real Android runtime.
        var providerCalls = 0
        val installer =
            AndroidGemmaBaseModelInstaller(
                gatewayProvider = {
                    providerCalls++
                    FakeGemmaAssetPackGateway()
                },
            )

        assertEquals(BaseModelInstallState.NotStarted, installer.state.value)
        assertEquals(0, providerCalls)
    }

    @Test
    fun installResolvesTheGatewayLazilyAndRegistersItsListener() =
        runTest {
            val gateway = FakeGemmaAssetPackGateway()
            val installer = AndroidGemmaBaseModelInstaller(gatewayProvider = { gateway })

            installer.install()

            assertEquals(AndroidGemmaBaseModelInstaller.PACK_NAME, gateway.registeredPackName)
        }

    @Test
    fun installUpdatesStateToTheGatewaysFetchResult() =
        runTest {
            val gateway = FakeGemmaAssetPackGateway().apply { fetchResult = BaseModelInstallState.Downloading(0.5f) }
            val installer = AndroidGemmaBaseModelInstaller(gatewayProvider = { gateway })

            installer.install()

            assertEquals(BaseModelInstallState.Downloading(0.5f), installer.state.value)
        }

    @Test
    fun installSurfacesAGatewayExceptionAsFailedRatherThanCrashing() =
        runTest {
            val gateway = FakeGemmaAssetPackGateway().apply { fetchException = RuntimeException("network down") }
            val installer = AndroidGemmaBaseModelInstaller(gatewayProvider = { gateway })

            installer.install()

            val state = installer.state.value
            assertTrue(state is BaseModelInstallState.Failed)
            assertEquals("network down", state.message)
        }

    @Test
    fun installSurfacesAGatewayProviderFailureAsFailedRatherThanCrashing() =
        runTest {
            // Mirrors the real Android actual: resolving Play Core without a Context (or any other
            // provider-time failure) must degrade to Failed, not propagate out of install().
            val installer =
                AndroidGemmaBaseModelInstaller(
                    gatewayProvider = { error("ContextHolder.context not set") },
                )

            installer.install()

            assertTrue(installer.state.value is BaseModelInstallState.Failed)
        }

    @Test
    fun listenerEmissionsUpdateStateOutsideOfInstall() =
        runTest {
            // Play Core pushes progress/completion asynchronously via the registered listener, not
            // just as install()'s direct return value — the installer must reflect those too.
            val gateway = FakeGemmaAssetPackGateway()
            val installer = AndroidGemmaBaseModelInstaller(gatewayProvider = { gateway })
            installer.install()

            gateway.emit(BaseModelInstallState.Downloading(0.75f))
            assertEquals(BaseModelInstallState.Downloading(0.75f), installer.state.value)

            gateway.emit(BaseModelInstallState.Installed("/pack/gemma-active.litertlm"))
            assertEquals(BaseModelInstallState.Installed("/pack/gemma-active.litertlm"), installer.state.value)
        }
}
