//
//  GemmaOnDemandResourceBridgeProgressTests.swift
//  PayslipMaxTests
//
//  Regression test for the v1.2.1 Gemma ODR download-banner bug: commit b518cb4 switched from
//  observing Progress.fractionCompleted to observing Progress.completedUnitCount guarded on
//  totalUnitCount > 0, checked once at observation setup time. For the ~584MB GemmaModel ODR
//  fetch, NSBundleResourceRequest's Progress starts indeterminate (totalUnitCount == -1) because
//  the resource size isn't known yet, and only becomes determinate once ODR reports it — by which
//  point the one-shot guard had already permanently skipped installing any observation. The fix is
//  to always observe fractionCompleted (which Foundation only ever reports meaningfully once the
//  progress is determinate — see below), so that whenever totalUnitCount does become known, the
//  in-flight observation picks up subsequent child updates instead of having missed its one chance.
//
//  Verified independently outside Xcode (plain `swift` scripts against Foundation.Progress on this
//  SDK): a Progress composed via addChild(_:withPendingUnitCount:) NEVER propagates child updates
//  to the parent's fractionCompleted/completedUnitCount while the parent itself is indeterminate
//  (totalUnitCount <= 0) — not via KVO, not even on a direct read. That only starts working once
//  the parent's own totalUnitCount is set to a positive value. So the fix below (and these tests)
//  models the realistic ODR sequence: indeterminate at first, then determinate once the size is
//  known, then child completedUnitCount updates driving fractionCompleted as normal.
//

import XCTest
@testable import PayslipMax

final class GemmaOnDemandResourceBridgeProgressTests: XCTestCase {

    /// Reproduces NSBundleResourceRequest's real On-Demand Resources Progress shape: it starts
    /// indeterminate (size not yet known), composed as a parent/child pair via
    /// addChild(_:withPendingUnitCount:).
    private func makeOdrShapedProgress() -> (parent: Progress, child: Progress) {
        let parent = Progress(totalUnitCount: -1)
        let child = Progress(totalUnitCount: 100)
        parent.addChild(child, withPendingUnitCount: 100)
        return (parent, child)
    }

    func test_observeDownloadProgress_reportsProgress_onceTotalUnitCountBecomesKnown() {
        let (parent, child) = makeOdrShapedProgress()
        XCTAssertLessThanOrEqual(parent.totalUnitCount, 0, "precondition: ODR size not yet known")

        var reportedBytesDownloaded: Int64?
        var reportedTotalBytes: Int64?
        let expectation = expectation(description: "progress reported")

        let observation = GemmaOnDemandResourceBridge.observeDownloadProgress(parent) { bytesDownloaded, totalBytes in
            reportedBytesDownloaded = bytesDownloaded
            reportedTotalBytes = totalBytes
            expectation.fulfill()
        }

        // ODR now reports the resource size, matching the child's pendingUnitCount weight ->
        // parent becomes determinate. (Foundation's Progress only tracks contributions accrued
        // after this transition — any completedUnitCount set on the child while still
        // indeterminate is not retroactively counted once totalUnitCount becomes known, which is
        // fine: the UI only needs correct reporting from the point progress becomes observable.)
        parent.totalUnitCount = 100

        // 60% of the child's 100 units -> parent.fractionCompleted becomes 0.6, now that the
        // parent is determinate.
        child.completedUnitCount = 60

        wait(for: [expectation], timeout: 1.0)
        observation.invalidate()

        XCTAssertNotNil(
            reportedBytesDownloaded,
            "GemmaOnDemandResourceBridge must report progress once totalUnitCount becomes known " +
                "mid-download — this is the exact v1.2.1 regression (commit b518cb4), where a " +
                "one-shot guard at observation setup time permanently missed this transition"
        )
        XCTAssertEqual(reportedTotalBytes, 1000)
        XCTAssertEqual(Double(reportedBytesDownloaded ?? -1), 600, accuracy: 1.0)
    }

    func test_observeDownloadProgress_reportsFullCompletion() {
        let (parent, child) = makeOdrShapedProgress()

        var reportedBytesDownloaded: Int64?
        let expectation = expectation(description: "progress reported at completion")

        let observation = GemmaOnDemandResourceBridge.observeDownloadProgress(parent) { bytesDownloaded, _ in
            reportedBytesDownloaded = bytesDownloaded
            if bytesDownloaded >= 1000 {
                expectation.fulfill()
            }
        }

        parent.totalUnitCount = 100 // ODR reports the resource size
        child.completedUnitCount = 100 // fully complete the child -> parent.fractionCompleted = 1.0

        wait(for: [expectation], timeout: 1.0)
        observation.invalidate()

        XCTAssertEqual(reportedBytesDownloaded, 1000)
    }
}
