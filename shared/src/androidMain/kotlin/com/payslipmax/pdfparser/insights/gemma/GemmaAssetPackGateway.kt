package com.payslipmax.pdfparser.insights.gemma

import com.google.android.play.core.assetpacks.AssetPackManager
import com.google.android.play.core.assetpacks.AssetPackState
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import com.google.android.play.core.ktx.requestFetch

/**
 * The Play Asset Delivery operations [AndroidGemmaBaseModelInstaller] needs, translated directly
 * into [BaseModelInstallState] at the boundary so the installer (and its tests) never touch Play
 * Core's `AssetPackManager`/`AssetPackState`/`Task` types directly — mirrors the [GemmaModelFileOps]
 * seam pattern [GemmaModelStorageManager] uses for the same reason.
 */
interface GemmaAssetPackGateway {
    /** Requests the pack fetch and returns its state once the request itself resolves. */
    suspend fun fetch(packName: String): BaseModelInstallState

    /** Registers a listener for state changes to [packName], for the lifetime of the process. */
    fun registerListener(
        packName: String,
        onStateChanged: (BaseModelInstallState) -> Unit,
    )
}

class DefaultGemmaAssetPackGateway(
    private val assetPackManager: AssetPackManager,
) : GemmaAssetPackGateway {
    override suspend fun fetch(packName: String): BaseModelInstallState {
        val states = assetPackManager.requestFetch(listOf(packName))
        return states.packStates()[packName]?.toInstallState() ?: BaseModelInstallState.NotStarted
    }

    override fun registerListener(
        packName: String,
        onStateChanged: (BaseModelInstallState) -> Unit,
    ) {
        assetPackManager.registerListener(
            AssetPackStateUpdateListener { state ->
                if (state.name() == packName) {
                    onStateChanged(state.toInstallState())
                }
            },
        )
    }

    private fun AssetPackState.toInstallState(): BaseModelInstallState =
        when (status()) {
            AssetPackStatus.PENDING, AssetPackStatus.TRANSFERRING, AssetPackStatus.NOT_INSTALLED ->
                BaseModelInstallState.Downloading(progress = 0f)
            AssetPackStatus.DOWNLOADING -> {
                val total = totalBytesToDownload()
                BaseModelInstallState.Downloading(
                    if (total > 0) bytesDownloaded().toFloat() / total.toFloat() else 0f,
                )
            }
            AssetPackStatus.COMPLETED ->
                BaseModelInstallState.Installed(resolveInstalledGemmaModelPath() ?: "")
            AssetPackStatus.FAILED ->
                BaseModelInstallState.Failed("Asset pack download failed (error code ${errorCode()})")
            AssetPackStatus.CANCELED ->
                BaseModelInstallState.Failed("Asset pack download canceled")
            AssetPackStatus.WAITING_FOR_WIFI, AssetPackStatus.REQUIRES_USER_CONFIRMATION -> {
                AndroidGemmaBaseModelInstaller.confirmationHandler?.invoke(assetPackManager)
                BaseModelInstallState.NeedsUserConfirmation
            }
            else -> BaseModelInstallState.NotStarted
        }
}
