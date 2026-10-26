//
//  ReminderNotificationTests.swift
//  PayslipMaxTests
//
//  The Swift half of Part II reminders (Phase 7b). A notification is the one thing Part II puts outside the app, so these pin what it may
//  carry (the injected words, a kind and a draft id), that a rebuild replaces rather than doubles, that the planner's instant is only turned
//  into "seconds from now", and that a tap reaches Kotlin once, as text, and never when the user info is not exactly ours.
//

import XCTest
import UserNotifications
import composeApp
@testable import PayslipMax

private final class FakeCenter: ReminderNotificationCenter {
    var removeCalls = 0
    var added: [UNNotificationRequest] = []
    var status: UNAuthorizationStatus = .notDetermined
    var grants = true
    var asked = 0

    func removeAllPendingNotificationRequests() {
        removeCalls += 1
        added.removeAll()
    }

    func add(_ request: UNNotificationRequest) { added.append(request) }
    func authorizationStatus(_ completion: @escaping (UNAuthorizationStatus) -> Void) { completion(status) }

    func requestAuthorization(_ completion: @escaping (Bool) -> Void) {
        asked += 1
        completion(grants)
    }
}

private final class RecordingListener: NSObject, IosPermissionListener {
    var answers: [Bool] = []
    func onAnswer(allowed: Bool) { answers.append(allowed) }
}

final class ReminderNotificationTests: XCTestCase {
    private let now = Date(timeIntervalSince1970: 1_800_000_000)

    private func request(_ key: String, kind: String = "RETURN", draft: String = "d1", inSeconds: Double = 3_600) -> IosReminderRequest {
        IosReminderRequest(
            key: key, kind: kind, draftId: draft,
            fireAtMillis: Int64((now.timeIntervalSince1970 + inSeconds) * 1000),
            title: "TITLE", body: "BODY"
        )
    }

    private func gateway(_ center: FakeCenter) -> ReminderNotificationGateway {
        ReminderNotificationGateway(center: center, now: { self.now })
    }

    func test_replaceAll_clearsWhatWasPendingAndAddsOneRequestPerReminderKeyedByItsKey() {
        let center = FakeCenter()

        gateway(center).replaceAll(requests: [request("return:d1"), request("add-order:d1:0", kind: "ADD_ORDER")])

        XCTAssertEqual(center.removeCalls, 1)
        XCTAssertEqual(center.added.map { $0.identifier }, ["return:d1", "add-order:d1:0"])
    }

    func test_replaceAll_twiceLeavesOnlyTheSecondList() {
        let center = FakeCenter()
        let sut = gateway(center)

        sut.replaceAll(requests: [request("a"), request("b")])
        sut.replaceAll(requests: [request("b")])

        XCTAssertEqual(center.added.map { $0.identifier }, ["b"], "a rebuild replaces; it never doubles")
    }

    func test_replaceAll_withNothingClearsEverything() {
        let center = FakeCenter()
        let sut = gateway(center)
        sut.replaceAll(requests: [request("a")])

        sut.replaceAll(requests: [])

        XCTAssertEqual(center.added, [])
        XCTAssertEqual(center.removeCalls, 2)
    }

    func test_theNotificationCarriesTheInjectedWordsAndOnlyTheKindAndTheDraftId() throws {
        let center = FakeCenter()

        gateway(center).replaceAll(requests: [request("return:d1", kind: "YEAR_END", draft: "draft-9")])

        let content = try XCTUnwrap(center.added.first).content
        XCTAssertEqual(content.title, "TITLE")
        XCTAssertEqual(content.body, "BODY")
        XCTAssertEqual(content.sound, UNNotificationSound.default, "a reminder that falls while the phone is in a pocket must be heard")
        XCTAssertEqual(Set(content.userInfo.keys.compactMap { $0 as? String }), ["kind", "draftId"])
        XCTAssertEqual(content.userInfo["kind"] as? String, "YEAR_END")
        XCTAssertEqual(content.userInfo["draftId"] as? String, "draft-9")
    }

    func test_theTriggerIsThePlannersInstantAsSecondsFromNowAndDoesNotRepeat() throws {
        let center = FakeCenter()

        gateway(center).replaceAll(requests: [request("a", inSeconds: 7_200)])

        let trigger = try XCTUnwrap(center.added.first?.trigger as? UNTimeIntervalNotificationTrigger)
        XCTAssertEqual(trigger.timeInterval, 7_200, accuracy: 0.001)
        XCTAssertFalse(trigger.repeats)
    }

    func test_anInstantThatHasPassedStillFiresOneSecondFromNow() throws {
        let center = FakeCenter()

        gateway(center).replaceAll(requests: [request("a", inSeconds: -500)])

        let trigger = try XCTUnwrap(center.added.first?.trigger as? UNTimeIntervalNotificationTrigger)
        XCTAssertEqual(trigger.timeInterval, 1, accuracy: 0.001, "the system refuses a zero or negative interval")
    }

    func test_canAsk_isTrueOnlyWhileTheAuthorizationIsUndecided() {
        let statuses: [(UNAuthorizationStatus, Bool)] = [
            (.notDetermined, true), (.denied, false), (.authorized, false), (.provisional, false), (.ephemeral, false),
        ]
        for (status, expected) in statuses {
            let center = FakeCenter()
            center.status = status
            let listener = RecordingListener()
            let answered = expectation(description: "answered \(status.rawValue)")
            gateway(center).canAsk(listener: listener)
            DispatchQueue.main.async { answered.fulfill() }
            wait(for: [answered], timeout: 2)
            XCTAssertEqual(listener.answers, [expected], "status \(status.rawValue)")
        }
    }

    func test_request_showsTheSystemDialogOnceAndAnswersWithTheOfficersChoice() {
        for choice in [true, false] {
            let center = FakeCenter()
            center.grants = choice
            let listener = RecordingListener()
            let answered = expectation(description: "answered \(choice)")
            gateway(center).request(listener: listener)
            DispatchQueue.main.async { answered.fulfill() }
            wait(for: [answered], timeout: 2)
            XCTAssertEqual(center.asked, 1)
            XCTAssertEqual(listener.answers, [choice])
        }
    }

    func test_registerPutsTheGatewayWhereKotlinFindsItAndTakesTheDelegate() {
        ReminderNotificationGateway.register()

        XCTAssertNotNil(IosReminderBridge.companion.gateway)
        XCTAssertTrue(UNUserNotificationCenter.current().delegate === ReminderTapRouter.shared)
    }

    // MARK: - the tap

    private func tapInfo(kind: Any? = "RETURN", draft: Any? = "d1") -> [AnyHashable: Any] {
        var info: [AnyHashable: Any] = [:]
        if let kind { info["kind"] = kind }
        if let draft { info["draftId"] = draft }
        return info
    }

    func test_aTapWithOurUserInfoReachesTheSinkOnceAsText() {
        let router = ReminderTapRouter()
        var received: [(String, String)] = []
        router.sink = { received.append(($0, $1)) }

        router.receive(userInfo: tapInfo(kind: "ADD_ORDER", draft: "d7"))

        XCTAssertEqual(received.count, 1)
        XCTAssertEqual(received.first?.0, "ADD_ORDER")
        XCTAssertEqual(received.first?.1, "d7")
    }

    func test_aTapThatIsNotExactlyOursIsDropped() {
        let router = ReminderTapRouter()
        var calls = 0
        router.sink = { _, _ in calls += 1 }

        router.receive(userInfo: [:])
        router.receive(userInfo: tapInfo(kind: nil))
        router.receive(userInfo: tapInfo(draft: nil))
        router.receive(userInfo: tapInfo(draft: 42))
        router.receive(userInfo: tapInfo(kind: ["RETURN"]))

        XCTAssertEqual(calls, 0)
    }

    func test_aTapThatArrivesBeforeKotlinIsReadyIsHeldAndDeliveredOnce() {
        let router = ReminderTapRouter()
        router.receive(userInfo: tapInfo(kind: "RETURN", draft: "first"))
        router.receive(userInfo: tapInfo(kind: "YEAR_END", draft: "second"))
        var received: [(String, String)] = []

        router.sink = { received.append(($0, $1)) }
        router.sink = { received.append(($0, $1)) }

        XCTAssertEqual(received.count, 1, "delivered once, not once per assignment")
        XCTAssertEqual(received.first?.1, "second", "only the newest tap is kept")
    }

    func test_aReminderThatFallsWhileTheAppIsOpenIsABannerWithNoSound() {
        XCTAssertEqual(ReminderTapRouter.foregroundOptions, [.banner, .list])
    }
}
