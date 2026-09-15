//
//  NavCoordinatorTests.swift
//  PayslipMaxTests
//
//  Covers NavCoordinator.shouldAllowInteractivePop, the edge-swipe-back gate. An edge-swipe
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

    // MARK: - shouldNotifyNativePop

    func test_notifiesPop_whenStackReturnsToRootOnly() {
        XCTAssertTrue(
            NavCoordinator.shouldNotifyNativePop(stackDepth: 1),
            "a detail was popped/swiped away — AppNavState must be synced"
        )
    }

    func test_doesNotNotifyPop_whenDetailIsPushed() {
        XCTAssertFalse(
            NavCoordinator.shouldNotifyNativePop(stackDepth: 2),
            "a push must not notify onNativePopObserved(), or it would feed back into the stack " +
                "Kotlin just asked us to push onto"
        )
    }

    func test_doesNotNotifyPop_whenStackIsEmpty() {
        XCTAssertFalse(
            NavCoordinator.shouldNotifyNativePop(stackDepth: 0),
            "an empty stack should never happen in practice (root is always present), but the " +
                "notification must still only fire at exactly depth 1"
        )
    }
}
