import Foundation
import composeApp

/// Notification keys for Background Assets extension event forwarding
extension Notification.Name {
    static let gemmaBackgroundAssetProgress = Notification.Name("GemmaBackgroundAssetDownloadProgress")
    static let gemmaBackgroundAssetCompleted = Notification.Name("GemmaBackgroundAssetDownloadCompleted")
}

/// Bridges iOS Background Assets extension events to Kotlin `IosGemmaBaseModelInstaller` companion slots.
///
/// Kotlin/Native `IosGemmaBaseModelInstaller` expects `progressReporter` and `completionReporter`
/// companion closures to be invoked when Background Assets downloads update.
/// This bridge connects NotificationCenter events (and direct method calls) to those Kotlin slots.
final class GemmaBackgroundAssetsBridge {
    static let shared = GemmaBackgroundAssetsBridge()

    private init() {}

    /// Registers the bridge on app startup. Call from AppDelegate.didFinishLaunchingWithOptions.
    static func register() {
        shared.setupNotificationListeners()
    }

    /// Sets up NotificationCenter observers for background download events.
    private func setupNotificationListeners() {
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleProgressNotification(_:)),
            name: .gemmaBackgroundAssetProgress,
            object: nil
        )

        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleCompletedNotification(_:)),
            name: .gemmaBackgroundAssetCompleted,
            object: nil
        )
    }

    /// Reports download progress (bytesDownloaded, totalBytes) to KMP.
    func reportProgress(bytesDownloaded: Int64, totalBytes: Int64) {
        DispatchQueue.main.async {
            IosGemmaBaseModelInstaller.companion.progressReporter?(bytesDownloaded, totalBytes)
        }
    }

    /// Reports download completion status to KMP.
    func reportCompletion(success: Bool, errorMessage: String? = nil) {
        DispatchQueue.main.async {
            IosGemmaBaseModelInstaller.companion.completionReporter?(success, errorMessage)
        }
    }

    @objc private func handleProgressNotification(_ notification: Notification) {
        guard let userInfo = notification.userInfo,
              let bytes = userInfo["bytesDownloaded"] as? Int64,
              let total = userInfo["totalBytes"] as? Int64 else {
            return
        }
        reportProgress(bytesDownloaded: bytes, totalBytes: total)
    }

    @objc private func handleCompletedNotification(_ notification: Notification) {
        guard let userInfo = notification.userInfo else {
            reportCompletion(success: false, errorMessage: "Missing notification userInfo")
            return
        }
        let success = (userInfo["success"] as? Bool) ?? false
        let errorMessage = userInfo["errorMessage"] as? String
        reportCompletion(success: success, errorMessage: errorMessage)
    }
}
