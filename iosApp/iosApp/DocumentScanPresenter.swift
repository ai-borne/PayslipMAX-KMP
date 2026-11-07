import UIKit
import VisionKit
import composeApp

/// The pages of a document scan, as the presenter reads them. `VNDocumentCameraScan` has no public initializer, so XCTest fakes this.
protocol DocumentScanSource {
    var pageCount: Int { get }
    func imageOfPage(at index: Int) -> UIImage
}

extension VNDocumentCameraScan: DocumentScanSource {}

enum DocumentScanPages {
    /// JPEG quality of the hand-over to Kotlin. The shared `PageEncoder` re-encodes every page again (long edge 1600 px, at most 500 KB),
    /// so this only needs to keep text sharp; a higher value would just move more bytes through memory.
    static let jpegQuality: CGFloat = 0.9

    /// In-memory JPEG data for each page, in page order. A page that cannot be encoded is skipped. Nothing is written to disk.
    static func jpegData(from scan: DocumentScanSource) -> [Data] {
        (0..<scan.pageCount).compactMap { scan.imageOfPage(at: $0).jpegData(compressionQuality: jpegQuality) }
    }
}

/// The Swift half of the Part II page scanner: shows VisionKit's document camera and hands the pages to Kotlin (`IosPageScanner`) in
/// memory. The camera permission prompt is the system's, driven by `NSCameraUsageDescription`. VisionKit has no page limit, so the
/// Kotlin side drops pages beyond the room left in the order.
final class DocumentScanPresenter: NSObject, IosDocumentScannerDelegate, VNDocumentCameraViewControllerDelegate {
    private let isSupported: () -> Bool
    // Internal so XCTest can stand in for the camera's answer.
    var listener: IosScanListener?
    private weak var controller: VNDocumentCameraViewController?

    init(isSupported: @escaping () -> Bool = { VNDocumentCameraViewController.isSupported }) {
        self.isSupported = isSupported
    }

    /// Registers the presenter on app startup. Call from AppDelegate.didFinishLaunchingWithOptions.
    static func register() {
        IosPageScanner.companion.delegate = DocumentScanPresenter()
    }

    func present(maxPages: Int32, listener: IosScanListener) {
        guard isSupported() else {
            listener.onUnavailable()
            return
        }
        guard let top = Self.topViewController() else {
            listener.onFailed()
            return
        }
        self.listener = listener
        let camera = VNDocumentCameraViewController()
        camera.delegate = self
        controller = camera
        top.present(camera, animated: true)
    }

    func dismiss() {
        DispatchQueue.main.async { [weak self] in
            self?.controller?.dismiss(animated: true)
            self?.listener = nil
        }
    }

    /// Answers Kotlin once and lets go of the listener, so a second delegate callback cannot answer twice.
    func finish(with source: DocumentScanSource) {
        let pages = DocumentScanPages.jpegData(from: source)
        answer { $0.onPages(pages: pages) }
    }

    private func answer(_ reply: (IosScanListener) -> Void) {
        guard let listener else { return }
        self.listener = nil
        reply(listener)
    }

    func documentCameraViewController(_ controller: VNDocumentCameraViewController, didFinishWith scan: VNDocumentCameraScan) {
        controller.dismiss(animated: true)
        finish(with: scan)
    }

    func documentCameraViewControllerDidCancel(_ controller: VNDocumentCameraViewController) {
        controller.dismiss(animated: true)
        answer { $0.onCancelled() }
    }

    func documentCameraViewController(_ controller: VNDocumentCameraViewController, didFailWithError error: Error) {
        controller.dismiss(animated: true)
        answer { $0.onFailed() }
    }

    private static func topViewController() -> UIViewController? {
        let scene = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.first { $0.activationState == .foregroundActive }
        var top = scene?.windows.first(where: \.isKeyWindow)?.rootViewController
        while let presented = top?.presentedViewController { top = presented }
        return top
    }
}
