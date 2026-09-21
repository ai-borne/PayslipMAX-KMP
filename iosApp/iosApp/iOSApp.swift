import SwiftUI
import FirebaseCore
import FirebaseCrashlytics
import FirebaseAnalytics
import composeApp

class SwiftGemmaInstallTelemetry: NSObject, IosTelemetryDelegate {
    func onTelemetryEnabledChanged(enabled: Bool) {
        Analytics.setAnalyticsCollectionEnabled(enabled)
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(enabled)
    }

    func logEvent(name: String, params: [String : String]?) {
        Analytics.logEvent(name, parameters: params)
    }
}

class SwiftCrashReporterDelegate: NSObject, IosCrashReporterDelegate {
    func log(message: String) {
        Crashlytics.crashlytics().log(message)
    }

    func setCustomKey(key: String, value: String) {
        Crashlytics.crashlytics().setCustomValue(value, forKey: key)
    }

    func recordException(throwable: KotlinThrowable, metadata_: [String : String]) {
        for (k, v) in metadata_ {
            Crashlytics.crashlytics().setCustomValue(v, forKey: k)
        }
        let userInfo: [String: Any] = [
            NSLocalizedDescriptionKey: throwable.message ?? "Kotlin Exception",
            "KotlinStackTrace": throwable.description
        ]
        let nsError = NSError(domain: "in.aiborne.payslipmax.kmp", code: -1, userInfo: userInfo)
        Crashlytics.crashlytics().record(error: nsError)
    }

    func setUserId(userId: String) {
        Crashlytics.crashlytics().setUserID(userId)
    }
}

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()

        // Register telemetry delegate
        IosGemmaInstallTelemetry.companion.delegate = SwiftGemmaInstallTelemetry()

        // Register Crashlytics delegate
        IosCrashReporter.companion.delegate = SwiftCrashReporterDelegate()

        // Bridge the LiteRT-LM Gemma inference runtime (Tier 6 offline fallback) to KMP. The shared
        // GemmaEngine.ios.kt fails loudly until this delegate is registered.
        GemmaInferenceBridge.register()

        // Bridge iOS Background Assets model delivery progress/completion to KMP.
        GemmaOnDemandResourceBridge.register()

        return true
    }
}

enum ProtectedOverlayGate {
    static func shouldDisplay(scenePhase: ScenePhase) -> Bool {
        scenePhase != .active
    }
}

struct PayslipMaxProtectedOverlayView: View {
    var body: some View {
        ZStack {
            Color(UIColor.systemBackground)
                .ignoresSafeArea()
            VStack(spacing: 16) {
                Image(systemName: "lock.shield.fill")
                    .font(.system(size: 48))
                    .foregroundColor(.accentColor)
                    .accessibilityLabel(AppStrings.shared.appProtectedShieldDesc)
                Text(AppStrings.shared.appProtectedTitle)
                    .font(.headline)
                    .fontWeight(.semibold)
                    .foregroundColor(.primary)
            }
        }
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    @Environment(\.scenePhase) var scenePhase

    var body: some Scene {
        WindowGroup {
            ContentView()
                .overlay {
                    if ProtectedOverlayGate.shouldDisplay(scenePhase: scenePhase) {
                        PayslipMaxProtectedOverlayView()
                    }
                }
        }
    }
}