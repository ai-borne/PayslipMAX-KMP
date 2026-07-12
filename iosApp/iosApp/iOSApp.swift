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

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()

        // Register telemetry delegate
        IosGemmaInstallTelemetry.companion.delegate = SwiftGemmaInstallTelemetry()

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
        
        // Bridge the Firebase ID token provider delegate to KMP.
        // fetchToken: gets ID token for a known user, retrying sign-in once on internal error.
        AuthTokenProvider.companion.tokenProviderDelegate = { completion in
            func fetchToken(user: User) {
                user.getIDToken { token, error in
                    if let error = error as NSError? {
                        print("Error fetching ID token on iOS: domain=\(error.domain) code=\(error.code) \(error.userInfo)")
                        // code 17999 = AuthErrorCodeInternalError (keychain/network transient failure)
                        // Re-sign-in anonymously and retry once.
                        if error.code == 17999 {
                            Auth.auth().signInAnonymously { result, signInError in
                                if let newUser = result?.user {
                                    newUser.getIDToken { retryToken, _ in _ = completion(retryToken) }
                                } else {
                                    print("Re-auth failed: \(signInError?.localizedDescription ?? "unknown")")
                                    _ = completion(nil)
                                }
                            }
                        } else {
                            _ = completion(nil)
                        }
                    } else {
                        _ = completion(token)
                    }
                }
            }

            if let user = Auth.auth().currentUser {
                fetchToken(user: user)
            } else {
                // currentUser is nil — sign-in hasn't completed yet (race condition at startup).
                // Sign in now so we can get a valid token.
                Auth.auth().signInAnonymously { result, error in
                    if let user = result?.user {
                        fetchToken(user: user)
                    } else {
                        print("signInAnonymously failed: \(error?.localizedDescription ?? "unknown")")
                        _ = completion(nil)
                    }
                }
            }
        }

        // Bridge the LiteRT-LM Gemma inference runtime (Tier 6 offline fallback) to KMP. The shared
        // GemmaEngine.ios.kt fails loudly until this delegate is registered.
        GemmaInferenceBridge.register()

        return true
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}