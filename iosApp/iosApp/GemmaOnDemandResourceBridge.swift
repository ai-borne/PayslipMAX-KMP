import Foundation
import composeApp

/// Bridges Apple's On-Demand Resources (ODR) fetch of the Tier 6 Gemma model to Kotlin
/// `IosGemmaBaseModelInstaller` companion slots.
///
/// Kotlin/Native `IosGemmaBaseModelInstaller.install()` invokes `installTrigger` to start the
/// fetch; this bridge reports progress/completion back via `progressReporter`/`completionReporter`.
/// The resource tag here must match exactly what's assigned to `gemma-active.litertlm` in Xcode's
/// asset catalog (On Demand Resource Tags), set to the "On Demand" download policy — not
/// "Prefetched: Install", which would bundle it into the initial app download instead of fetching
/// it later.
final class GemmaOnDemandResourceBridge {
    static let shared = GemmaOnDemandResourceBridge()

    /// Must be retained for as long as the app wants the fetched resource to stay on disk — ODR
    /// resources can be purged by the OS once no live `NSBundleResourceRequest` holds access to
    /// them. Held for the process lifetime once a fetch succeeds.
    private var activeRequest: NSBundleResourceRequest?
    private var progressObservation: NSKeyValueObservation?

    private static let tag = "GemmaModel"

    private init() {}

    /// Registers the bridge on app startup. Call from AppDelegate.didFinishLaunchingWithOptions.
    static func register() {
        IosGemmaBaseModelInstaller.companion.installTrigger = {
            GemmaOnDemandResourceBridge.shared.beginFetch()
        }
    }

    private func beginFetch() {
        let request = NSBundleResourceRequest(tags: [Self.tag])
        activeRequest = request

        progressObservation = request.progress.observe(\.completedUnitCount) { progress, _ in
            // totalUnitCount can be 0/-1 briefly before the download size is known — skip until real.
            guard progress.totalUnitCount > 0 else { return }
            self.reportProgress(bytesDownloaded: progress.completedUnitCount, totalBytes: progress.totalUnitCount)
        }

        request.beginAccessingResources { [weak self] error in
            DispatchQueue.main.async {
                self?.progressObservation = nil
                if let error {
                    self?.reportCompletion(success: false, errorMessage: error.localizedDescription)
                    self?.activeRequest = nil
                } else {
                    self?.reportCompletion(success: true)
                }
            }
        }
    }

    private func reportProgress(bytesDownloaded: Int64, totalBytes: Int64) {
        DispatchQueue.main.async {
            IosGemmaBaseModelInstaller.companion.progressReporter?(
                KotlinLong(value: bytesDownloaded),
                KotlinLong(value: totalBytes)
            )
        }
    }

    private func reportCompletion(success: Bool, errorMessage: String? = nil) {
        DispatchQueue.main.async {
            IosGemmaBaseModelInstaller.companion.completionReporter?(
                KotlinBoolean(bool: success),
                errorMessage
            )
        }
    }
}
