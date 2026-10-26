import Foundation
import UserNotifications
import composeApp

/// What the gateway needs of the system's notification center. The real center needs a host app bundle, so XCTest stands in with a fake.
protocol ReminderNotificationCenter: AnyObject {
    func removeAllPendingNotificationRequests()
    func add(_ request: UNNotificationRequest)
    func authorizationStatus(_ completion: @escaping (UNAuthorizationStatus) -> Void)
    func requestAuthorization(_ completion: @escaping (Bool) -> Void)
}

extension UNUserNotificationCenter: ReminderNotificationCenter {
    func add(_ request: UNNotificationRequest) {
        add(request, withCompletionHandler: nil)
    }

    func authorizationStatus(_ completion: @escaping (UNAuthorizationStatus) -> Void) {
        getNotificationSettings { completion($0.authorizationStatus) }
    }

    func requestAuthorization(_ completion: @escaping (Bool) -> Void) {
        requestAuthorization(options: [.alert, .sound]) { granted, _ in completion(granted) }
    }
}

/// The Swift half of Part II reminders: turns the planner's reminders (already timed, already worded, handed over by Kotlin) into local
/// notifications and answers the permission questions. It works out no date: the only arithmetic is "seconds from now" for an instant the
/// planner chose. Each request is keyed by the reminder's key, so a rebuild replaces instead of doubling; its user info is the reminder kind and
/// the draft id and nothing else. Nothing is logged. The system holds at most 64 pending requests; the planner gives at most 20.
final class ReminderNotificationGateway: NSObject, IosNotificationGateway {
    static let kindKey = "kind"
    static let draftKey = "draftId"

    private let center: ReminderNotificationCenter
    private let now: () -> Date

    init(center: ReminderNotificationCenter, now: @escaping () -> Date = Date.init) {
        self.center = center
        self.now = now
    }

    /// Registers the gateway and the tap router on app startup. Call from AppDelegate.didFinishLaunchingWithOptions: the system needs the
    /// delegate set before launch ends, or a tap that launched the app is lost.
    static func register() {
        let center = UNUserNotificationCenter.current()
        IosReminderBridge.companion.gateway = ReminderNotificationGateway(center: center)
        center.delegate = ReminderTapRouter.shared
        ReminderTapRouter.shared.sink = { kind, draftId in ReminderLinksKt.acceptReminderTap(kind: kind, draftId: draftId) }
    }

    func replaceAll(requests: [IosReminderRequest]) {
        center.removeAllPendingNotificationRequests()
        requests.forEach { center.add(notification(for: $0)) }
    }

    func canAsk(listener: IosPermissionListener) {
        center.authorizationStatus { status in
            DispatchQueue.main.async { listener.onAnswer(allowed: status == .notDetermined) }
        }
    }

    func request(listener: IosPermissionListener) {
        center.requestAuthorization { granted in
            DispatchQueue.main.async { listener.onAnswer(allowed: granted) }
        }
    }

    /// A request for the planner's instant. A past instant still fires, a second from now (the system refuses a zero interval).
    func notification(for request: IosReminderRequest) -> UNNotificationRequest {
        let content = UNMutableNotificationContent()
        content.title = request.title
        content.body = request.body
        content.sound = .default
        content.userInfo = [Self.kindKey: request.kind, Self.draftKey: request.draftId]
        let seconds = Double(request.fireAtMillis) / 1000 - now().timeIntervalSince1970
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: max(seconds, 1), repeats: false)
        return UNNotificationRequest(identifier: request.key, content: content, trigger: trigger)
    }
}
