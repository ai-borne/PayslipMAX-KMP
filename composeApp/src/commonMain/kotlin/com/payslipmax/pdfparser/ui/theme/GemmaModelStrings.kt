package com.payslipmax.pdfparser.ui.theme

/** Offline smart features & local AI model (Tier 6 Gemma fallback). */
object GemmaModelStrings {
    const val gemmaLicenseNoticeTitle = "Model License"
    const val gemmaTermsOfUseNotice =
        "Gemma is provided under and subject to the Gemma Terms of Use found at ai.google.dev/gemma/terms"
    const val gemmaAiSettingRowTitle = "Use Local Gemma AI Model"
    const val gemmaAiSettingRowSubtitleSupported = "Runs 100% offline on-device to protect privacy and battery."
    const val gemmaAiSettingRowSubtitleUnsupported = "Requires device with 4GB RAM and 1.5GB free storage."
    const val gemmaModelDownloadingTitle = "Setting up Smart Features"
    const val gemmaModelDownloadBannerMessage =
        "Downloading offline helper in background for enhanced privacy."
    const val gemmaModelWaitingForWifiTitle = "Smart Features Paused"
    const val gemmaModelWaitingForWifiSubtitle =
        "Waiting for Wi-Fi connection. You can wait or proceed on mobile data."
    const val gemmaModelPausedTitle = "Smart Features Setup Paused"
    const val gemmaModelPausedSubtitle =
        "Will resume automatically on Wi-Fi, or tap to retry."
    const val gemmaModelDownloadErrorTitle = "Smart Features Setup Paused"
    const val gemmaModelDownloadCellularAction = "Resume"
    const val gemmaModelRetryAction = "Retry"
    const val gemmaModelDismissAction = "Dismiss banner"
}
