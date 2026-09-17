//
//  PayslipMaxProtectedOverlayTests.swift
//  PayslipMaxTests
//
//  Verifies the iOS privacy overlay gating, typography, and SSOT alignment
//  with the multiplatform AppStrings definitions.
//

import XCTest
import SwiftUI
import composeApp
@testable import PayslipMax

final class PayslipMaxProtectedOverlayTests: XCTestCase {

    func test_ssotString_appProtectedTitle_matchesKMPDefinition() {
        XCTAssertEqual(
            AppStrings.shared.appProtectedTitle,
            "PayslipMax Protected",
            "iOS overlay title must strictly match AppStrings.appProtectedTitle SSOT"
        )
    }

    func test_ssotString_appProtectedShieldDesc_matchesKMPDefinition() {
        XCTAssertEqual(
            AppStrings.shared.appProtectedShieldDesc,
            "Security Shield",
            "iOS shield accessibility description must strictly match AppStrings.appProtectedShieldDesc SSOT"
        )
    }

    func test_gate_whenActive_overlayIsNotDisplayed() {
        XCTAssertFalse(
            ProtectedOverlayGate.shouldDisplay(scenePhase: .active),
            "When the app is active, overlay must be hidden to allow screenshots and normal user interaction"
        )
    }

    func test_gate_whenInactive_overlayIsDisplayed() {
        XCTAssertTrue(
            ProtectedOverlayGate.shouldDisplay(scenePhase: .inactive),
            "When moving to app switcher or transitionary state, overlay must be displayed"
        )
    }

    func test_gate_whenBackground_overlayIsDisplayed() {
        XCTAssertTrue(
            ProtectedOverlayGate.shouldDisplay(scenePhase: .background),
            "When backgrounded, overlay must be displayed to protect snapshot"
        )
    }

    func test_overlayView_initializesSuccessfully() {
        let view = PayslipMaxProtectedOverlayView()
        XCTAssertNotNil(view.body, "Overlay view body should initialize cleanly")
    }
}
