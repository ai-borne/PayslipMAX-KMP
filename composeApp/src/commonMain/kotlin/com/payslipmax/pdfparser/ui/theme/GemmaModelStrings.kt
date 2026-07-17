package com.payslipmax.pdfparser.ui.theme

/** Offline AI model download (Tier 6 Gemma fallback). */
object GemmaModelStrings {
    const val gemmaLicenseNoticeTitle = "Model License"
    const val gemmaTermsOfUseNotice = "Gemma is provided under and subject to the Gemma Terms of Use found at ai.google.dev/gemma/terms"
    const val gemmaAiSettingRowTitle = "Use Local Gemma AI Model"
    const val gemmaAiSettingRowSubtitleSupported = "Runs 100% offline on-device to protect privacy and battery."
    const val gemmaAiSettingRowSubtitleUnsupported = "Requires device with 4GB RAM and 1.5GB free storage."
    const val gemmaModelDownloadBannerMessage =
        "Offline AI Model (~529MB) downloading in background. Devices with >3.5GB RAM get the most accurate parsing."
}
