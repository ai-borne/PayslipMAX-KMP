//
//  NavCoordinatorTests.swift
//  PayslipMaxTests
//
//  Covers NavCoordinator.shouldAllowInteractivePop, the edge-swipe-back gate, and pushedDetailCount,
//  which keeps AppNavState in step with the native stack. An edge-swipe
//  bypasses Compose's BackHandler entirely, so if this gate mis-fires it silently discards an
//  in-flight draft/correction instead of running the two-step cancel-then-exit path (Phase 5).
//

import XCTest
@testable import PayslipMax

final class NavCoordinatorTests: XCTestCase {

    func test_deniesPop_whenStackHasOnlyRoot() {
        XCTAssertFalse(
            NavCoordinator.shouldAllowInteractivePop(stackDepth: 1, hasActiveUnsavedSubState: false),
            "nothing to pop back to at the root — the gesture must not begin"
        )
    }

    func test_deniesPop_whenStackIsEmpty() {
        XCTAssertFalse(
            NavCoordinator.shouldAllowInteractivePop(stackDepth: 0, hasActiveUnsavedSubState: false)
        )
    }

    func test_allowsPop_whenDetailPushedAndNoUnsavedEdit() {
        XCTAssertTrue(
            NavCoordinator.shouldAllowInteractivePop(stackDepth: 2, hasActiveUnsavedSubState: false)
        )
    }

    func test_deniesPop_whenDetailPushedButUnsavedEditInFlight() {
        XCTAssertFalse(
            NavCoordinator.shouldAllowInteractivePop(stackDepth: 2, hasActiveUnsavedSubState: true),
            "an unsaved edit must block edge-swipe so it can't silently discard the draft"
        )
    }

    // MARK: - pushedDetailCount

    func test_pushedDetailCount_isZeroAtTheTabRoot() {
        XCTAssertEqual(
            NavCoordinator.pushedDetailCount(stackDepth: 1), 0,
            "index 0 is always the tab root, so a lone root means no detail is pushed"
        )
    }

    func test_pushedDetailCount_reportsANestedPopToTheScreenBelow() {
        // Item 5: root → payslip → Retirement Calculators, edge-swiped back to the payslip. The old
        // rule only reported depth 1, so this pop never reached AppNavState.
        XCTAssertEqual(NavCoordinator.pushedDetailCount(stackDepth: 2), 1)
        XCTAssertEqual(NavCoordinator.pushedDetailCount(stackDepth: 3), 2)
    }

    func test_pushedDetailCount_neverGoesNegative() {
        XCTAssertEqual(
            NavCoordinator.pushedDetailCount(stackDepth: 0), 0,
            "an empty stack never happens in practice, but Kotlin must never be sent a negative count"
        )
    }
}
