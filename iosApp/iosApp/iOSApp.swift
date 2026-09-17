import SwiftUI
import FirebaseCore
import FirebaseAuth
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

/// Pure retry/completion-once state machine behind Firebase ID-token fetching, decoupled from
/// `FirebaseAuth.User`/`Auth` (via generic `UserType` + closures) so it's directly testable — see
/// `AuthTokenFetcherTests`. Retries sign-in exactly once, only on Firebase's transient
/// keychain/network error code 17999 (`AuthErrorCodeInternalError`), and calls `completion` exactly
/// once on every path (fresh success, retry success, retry failure, non-retryable error, initial
/// sign-in failure).
enum AuthTokenFetcher {
    static func fetchIdToken<UserType>(
        currentUser: UserType?,
        signInAnonymously: @escaping (@escaping (UserType?) -> Void) -> Void,
        getIDToken: @escaping (UserType, @escaping (String?, Int?) -> Void) -> Void,
        completion: @escaping (String?) -> Void
    ) {
        func fetchToken(user: UserType) {
            getIDToken(user) { token, errorCode in
                guard let errorCode = errorCode else {
                    completion(token)
                    return
                }
                guard errorCode == 17999 else {
                    completion(nil)
                    return
                }
                signInAnonymously { newUser in
                    guard let newUser = newUser else {
                        completion(nil)
                        return
                    }
                    getIDToken(newUser) { retryToken, _ in completion(retryToken) }
                }
            }
        }

        if let user = currentUser {
            fetchToken(user: user)
        } else {
            // currentUser is nil — sign-in hasn't completed yet (race condition at startup).
            signInAnonymously { user in
                guard let user = user else {
                    completion(nil)
                    return
                }
                fetchToken(user: user)
            }
        }
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

        // Ensure user is signed in anonymously to retrieve a valid ID token
        if Auth.auth().currentUser == nil {
            Auth.auth().signInAnonymously { authResult, error in
                if let error = error {
                    print("Error signing in anonymously on iOS: \(error.localizedDescription)")
                } else if let user = authResult?.user {
                    print("Successfully signed in anonymously on iOS: \(user.uid)")
                }
            }
        }
        
        // Bridge the Firebase ID token provider delegate to KMP; see AuthTokenFetcher for the
        // retry/completion-once logic.
        AuthTokenProvider.companion.tokenProviderDelegate = { completion in
            AuthTokenFetcher.fetchIdToken(
                currentUser: Auth.auth().currentUser,
                signInAnonymously: { onUser in
                    Auth.auth().signInAnonymously { result, error in
                        if let error = error {
                            print("signInAnonymously failed on iOS: \(error.localizedDescription)")
                        }
                        onUser(result?.user)
                    }
                },
                getIDToken: { user, onToken in
                    user.getIDToken { token, error in
                        if let error = error as NSError? {
                            print("Error fetching ID token on iOS: domain=\(error.domain) code=\(error.code) \(error.userInfo)")
                            onToken(nil, error.code)
                        } else {
                            onToken(token, nil)
                        }
                    }
                },
                completion: { token in _ = completion(token) }
            )
        }

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