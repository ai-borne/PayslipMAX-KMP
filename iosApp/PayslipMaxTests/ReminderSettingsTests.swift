//
//  ReminderSettingsTests.swift
//  PayslipMaxTests
//
//  The Swift half of the Due screen's "reminders are off" note (Phase 12a, DECISIONS Round 44): the gateway answers whether notifications are
//  allowed right now, and opens this app's page in the Settings app. Whether the note shows is Kotlin's decision (a common test); these pin only
//  what iOS contributes: which authorization states count as allowed and which URL is opened.
//

import XCTest
import UserNotifications
import UIKit
import composeApp
@testable import PayslipMax

private final class StatusCenter: ReminderNotificationCenter {
    var status: UNAuthorizationStatus = .notDetermined
    var asked = 0

    func removeAllPendingNotificationRequests() {}
    func add(_ request: UNNotificationRequest) {}
    func authorizationStatus(_ completion: @escaping (UNAuthorizationStatus) -> Void) { completion(status) }

    func requestAuthorization(_ completion: @escaping (Bool) -> Void) {
        asked += 1
        completion(false)
    }
}

private final class AnswerListener: NSObject, IosPermissionListener {
    var answers: [Bool] = []
    func onAnswer(allowed: Bool) { answers.append(allowed) }
}

final class ReminderSettingsTests: XCTestCase {
    func test_allows_isTrueForEveryStateInWhichAReminderCanBeShownAndFalseOtherwise() {
        let expected: [(UNAuthorizationStatus, Bool)] = [
            (.notDetermined, false), (.denied, false), (.authorized, true), (.provisional, true), (.ephemeral, true),
        ]
        for (status, allowed) in expected {
            XCTAssertEqual(ReminderNotificationGateway.allows(status), allowed, "status \(status.rawValue)")
        }
    }

    func test_isAllowed_answersFromTheCentersStatusOnTheMainThreadWithoutAsking() {
        let statuses: [(UNAuthorizationStatus, Bool)] = [(.denied, false), (.authorized, true)]
        for (status, expected) in statuses {
            let center = StatusCenter()
            center.status = status
            let listener = AnswerListener()
            let answered = expectation(description: "answered \(status.rawValue)")
            ReminderNotificationGateway(center: center).isAllowed(listener: listener)
            DispatchQueue.main.async { answered.fulfill() }
            wait(for: [answered], timeout: 2)
            XCTAssertEqual(listener.answers, [expected], "status \(status.rawValue)")
            XCTAssertEqual(center.asked, 0, "reading the state never shows the system dialog")
        }
    }

    func test_openSettings_opensTheAppsSettingsPageExactlyOnce() {
        var opened: [URL] = []
        let gateway = ReminderNotificationGateway(center: StatusCenter(), openURL: { opened.append($0) })
        let done = expectation(description: "opened")

        gateway.openSettings()
        DispatchQueue.main.async { done.fulfill() }
        wait(for: [done], timeout: 2)

        XCTAssertEqual(opened.map { $0.absoluteString }, [UIApplication.openSettingsURLString])
    }
}
