import Foundation
import UserNotifications

/// Receives the officer's tap on a reminder notification and passes its kind and draft id on, as text, to Kotlin (which shape-checks them,
/// holds them while the app is locked and checks the database before opening anything). A tap can launch the app before the Compose side is
/// ready, so a tap that arrives before the sink is set is held and delivered when it is. Only the newest is kept, as Kotlin keeps only one.
/// Nothing is logged.
final class ReminderTapRouter: NSObject, UNUserNotificationCenterDelegate {
    static let shared = ReminderTapRouter()

    private var held: (kind: String, draftId: String)?

    var sink: ((String, String) -> Void)? {
        didSet { deliverHeld() }
    }

    /// The tap's two strings, or nil when the user info is not exactly what we put there (a forged or foreign notification).
    static func link(from userInfo: [AnyHashable: Any]) -> (kind: String, draftId: String)? {
        guard let kind = userInfo[ReminderNotificationGateway.kindKey] as? String,
              let draftId = userInfo[ReminderNotificationGateway.draftKey] as? String
        else { return nil }
        return (kind, draftId)
    }

    func receive(userInfo: [AnyHashable: Any]) {
        guard let link = Self.link(from: userInfo) else { return }
        held = link
        deliverHeld()
    }

    private func deliverHeld() {
        guard let sink, let link = held else { return }
        held = nil
        sink(link.kind, link.draftId)
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
        receive(userInfo: response.notification.request.content.userInfo)
        completionHandler()
    }

    /// A reminder that falls while the app is open is shown as a banner, with no sound.
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler(Self.foregroundOptions)
    }

    static let foregroundOptions: UNNotificationPresentationOptions = [.banner, .list]
}
