import SwiftUI
import FirebaseCore
import FirebaseAuth
import composeApp

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
        
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
        
        // Bridge the Firebase ID token provider delegate to KMP
        AuthTokenProvider.companion.tokenProviderDelegate = { completion in
            guard let user = Auth.auth().currentUser else {
                _ = completion(nil)
                return
            }
            user.getIDToken { token, error in
                if let error = error {
                    print("Error fetching ID token on iOS: \(error.localizedDescription)")
                    _ = completion(nil)
                } else {
                    _ = completion(token)
                }
            }
        }
        
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