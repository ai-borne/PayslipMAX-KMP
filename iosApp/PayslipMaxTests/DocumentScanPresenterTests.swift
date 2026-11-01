//
//  DocumentScanPresenterTests.swift
//  PayslipMaxTests
//
//  The Swift half of the Part II page scanner (Phase 6a). A page is a photo of a RESTRICTED order, so the hand-over to Kotlin must be
//  in memory only, in page order, and answer exactly once. The camera itself cannot run in the simulator; the unsupported path can.
//

import XCTest
import UIKit
import VisionKit
import composeApp
@testable import PayslipMax

private struct FakeScan: DocumentScanSource {
    let images: [UIImage]
    var pageCount: Int { images.count }
    func imageOfPage(at index: Int) -> UIImage { images[index] }
}

private final class RecordingListener: NSObject, IosScanListener {
    var pages: [[Data]] = []
    var cancelled = 0
    var unavailable = 0
    var failed = 0

    func onPages(pages: [Data]) { self.pages.append(pages) }
    func onCancelled() { cancelled += 1 }
    func onUnavailable() { unavailable += 1 }
    func onFailed() { failed += 1 }
}

final class DocumentScanPresenterTests: XCTestCase {

    private func solidImage(_ color: UIColor, size: CGSize = CGSize(width: 40, height: 30)) -> UIImage {
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1  // pixels, so a page's size in the JPEG is the size drawn
        return UIGraphicsImageRenderer(size: size, format: format).image { context in
            color.setFill()
            context.fill(CGRect(origin: .zero, size: size))
        }
    }

    private func isJpeg(_ data: Data) -> Bool {
        data.count > 3 && data[0] == 0xFF && data[1] == 0xD8
    }

    func test_everyPageBecomesJpegDataInPageOrder() {
        let scan = FakeScan(images: [solidImage(.red), solidImage(.blue, size: CGSize(width: 20, height: 50))])

        let data = DocumentScanPages.jpegData(from: scan)

        XCTAssertEqual(data.count, 2)
        XCTAssertTrue(data.allSatisfy(isJpeg), "each page is JPEG data")
        XCTAssertEqual(UIImage(data: data[0])?.size.width, 40, "the first page stays first")
        XCTAssertEqual(UIImage(data: data[1])?.size.height, 50, "the second page stays second")
    }

    func test_theHandOverQualityKeepsTextSharp() {
        XCTAssertGreaterThanOrEqual(DocumentScanPages.jpegQuality, 0.8, "the shared encoder re-encodes once more, so the first pass must not blur the text")
        XCTAssertLessThanOrEqual(DocumentScanPages.jpegQuality, 1.0)
    }

    func test_aScanWithNoPagesGivesNoData() {
        XCTAssertEqual(DocumentScanPages.jpegData(from: FakeScan(images: [])).count, 0)
    }

    func test_aPageThatCannotBeEncodedIsSkippedNotFatal() {
        let scan = FakeScan(images: [solidImage(.red), UIImage(), solidImage(.blue)])

        XCTAssertEqual(DocumentScanPages.jpegData(from: scan).count, 2)
    }

    func test_finishingAnswersKotlinOnceWithTheJpegPages() {
        let presenter = DocumentScanPresenter()
        let listener = RecordingListener()
        presenter.listener = listener

        presenter.finish(with: FakeScan(images: [solidImage(.red), solidImage(.blue)]))
        presenter.finish(with: FakeScan(images: [solidImage(.green)]))

        XCTAssertEqual(listener.pages.count, 1, "a second callback must not answer again")
        XCTAssertEqual(listener.pages.first?.count, 2)
        XCTAssertTrue(listener.pages.first?.allSatisfy { isJpeg($0) } ?? false)
        XCTAssertEqual(listener.cancelled + listener.failed + listener.unavailable, 0)
    }

    func test_finishingWithNoListenerDoesNothing() {
        DocumentScanPresenter().finish(with: FakeScan(images: [solidImage(.red)]))
    }

    func test_whereTheDocumentCameraIsNotSupportedKotlinIsToldUnavailable() {
        let listener = RecordingListener()

        DocumentScanPresenter(isSupported: { false }).present(maxPages: 10, listener: listener)

        XCTAssertEqual(listener.unavailable, 1)
        XCTAssertEqual(listener.pages.count + listener.cancelled + listener.failed, 0)
    }

    func test_aClosedCameraAnswersCancelledOnceAndAFailedOneAnswersFailedOnce() {
        let camera = VNDocumentCameraViewController()
        let cancelled = RecordingListener()
        let presenter = DocumentScanPresenter()
        presenter.listener = cancelled
        presenter.documentCameraViewControllerDidCancel(camera)
        presenter.documentCameraViewControllerDidCancel(camera)

        XCTAssertEqual(cancelled.cancelled, 1)
        XCTAssertEqual(cancelled.pages.count + cancelled.failed + cancelled.unavailable, 0)

        let failed = RecordingListener()
        presenter.listener = failed
        presenter.documentCameraViewController(camera, didFailWithError: NSError(domain: "MARKER_test", code: 1))
        presenter.documentCameraViewController(camera, didFailWithError: NSError(domain: "MARKER_test", code: 2))

        XCTAssertEqual(failed.failed, 1)
        XCTAssertEqual(failed.pages.count + failed.cancelled + failed.unavailable, 0)
    }

    func test_dismissWithNothingOnScreenIsHarmless() {
        DocumentScanPresenter().dismiss()
    }
}
