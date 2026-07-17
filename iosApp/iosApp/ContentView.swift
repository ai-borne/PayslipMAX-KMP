import UIKit
import SwiftUI
import composeApp
import UniformTypeIdentifiers
import QuickLook

class PDFPreviewItem: NSObject, QLPreviewItem {
    var previewItemURL: URL?
    var previewItemTitle: String?
    
    init(url: URL, title: String) {
        self.previewItemURL = url
        self.previewItemTitle = title
    }
}

class PDFPreviewDataSource: NSObject, QLPreviewControllerDataSource {
    let item: QLPreviewItem
    
    init(item: QLPreviewItem) {
        self.item = item
    }
    
    func numberOfPreviewItems(in controller: QLPreviewController) -> Int {
        return 1
    }
    
    func previewController(_ controller: QLPreviewController, previewItemAt index: Int) -> QLPreviewItem {
        return item
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeCoordinator() -> NavCoordinator {
        NavCoordinator()
    }

    func makeUIViewController(context: Context) -> UIViewController {
        let coordinator = context.coordinator

        // Kotlin owns the shared nav state; Swift owns the native UINavigationController and
        // executes the push/pop it requests. The coordinator bridges the two directions.
        let navHost = MainViewControllerKt.createNavHost(
            onPickPdf: { onResult in
                DispatchQueue.main.async {
                    let picker = UIDocumentPickerViewController(forOpeningContentTypes: [.pdf])
                    let delegate = DocumentPickerDelegate(onResult: { bytes, name in
                        // Kotlin expects a KotlinUnit return value
                        _ = onResult(bytes, name)
                    })

                    // Keep strong reference to delegate
                    objc_setAssociatedObject(picker, &AssociatedKeys.delegate, delegate, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
                    picker.delegate = delegate
                    topViewController()?.present(picker, animated: true)
                }
            },
            onOpenPdf: { bytes, name in
                DispatchQueue.main.async {
                    let tempURL = FileManager.default.temporaryDirectory.appendingPathComponent(name)
                    var data = Data(count: Int(bytes.size))
                    for i in 0..<Int(bytes.size) {
                        data[i] = UInt8(bitPattern: bytes.get(index: Int32(i)))
                    }
                    do {
                        try data.write(to: tempURL)
                        let previewVC = QLPreviewController()
                        let previewItem = PDFPreviewItem(url: tempURL, title: name)
                        let dataSource = PDFPreviewDataSource(item: previewItem)

                        objc_setAssociatedObject(previewVC, &AssociatedKeys.dataSource, dataSource, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
                        previewVC.dataSource = dataSource
                        previewVC.modalPresentationStyle = .overFullScreen
                        topViewController()?.present(previewVC, animated: true)
                    } catch {
                        print("Error opening PDF: \(error)")
                    }
                }
            },
            onPickBackup: { onResult in
                DispatchQueue.main.async {
                    // Backup archives (.pcda) have no registered UTType, so open on generic data/item.
                    let picker = UIDocumentPickerViewController(forOpeningContentTypes: [.data, .item])
                    let delegate = BackupPickerDelegate(onResult: { bytes in
                        _ = onResult(bytes)
                    })

                    objc_setAssociatedObject(picker, &AssociatedKeys.backupDelegate, delegate, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
                    picker.delegate = delegate
                    topViewController()?.present(picker, animated: true)
                }
            },
            pushScreen: { [weak coordinator] name in coordinator?.push(screenName: name) },
            requestPop: { [weak coordinator] in coordinator?.pop() }
        )

        let root = navHost.rootViewController()
        let navController = UINavigationController(rootViewController: root)
        // The app draws its own back header (ScreenBackHeader), so the native bar would be a
        // redundant second back affordance — hide it.
        navController.setNavigationBarHidden(true, animated: false)
        navController.delegate = coordinator
        // Hiding the native bar disables interactivePopGestureRecognizer on some iOS versions;
        // re-enable edge-swipe-back via the coordinator's UIGestureRecognizerDelegate shim.
        navController.interactivePopGestureRecognizer?.delegate = coordinator

        coordinator.navHost = navHost
        coordinator.navController = navController
        return navController
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

/// Bridges the Kotlin `IosNavHost` to a native `UINavigationController` (Phase 4).
/// - Kotlin → Swift: `push`/`pop` execute the requested native stack change.
/// - Swift → Kotlin: `didShow` syncs `AppNavState` after any native pop (incl. edge-swipe).
final class NavCoordinator: NSObject, UINavigationControllerDelegate, UIGestureRecognizerDelegate {
    var navHost: IosNavHost?
    weak var navController: UINavigationController?

    func push(screenName: String) {
        guard let navHost = navHost, let navController = navController else { return }
        let detailVC = navHost.detailViewController(screenName: screenName)
        navController.pushViewController(detailVC, animated: true)
    }

    func pop() {
        navController?.popViewController(animated: true)
    }

    // The stack is exactly one level deep (a single tab root + at most one pushed detail), so a
    // return to a single-VC stack means a detail was popped/swiped away — sync AppNavState. On a
    // push (count == 2) we skip, avoiding a feedback loop back into the native stack.
    func navigationController(_ navigationController: UINavigationController, didShow viewController: UIViewController, animated: Bool) {
        if navigationController.viewControllers.count == 1 {
            navHost?.onNativePopObserved()
        }
    }

    // Only allow the interactive pop gesture when there is something to pop back to, and never
    // while the pushed detail has an unsaved edit in flight — an edge-swipe bypasses Compose's
    // BackHandler, so without this gate it would silently discard drafts/corrections instead of
    // running the two-step cancel-then-exit path (Phase 5).
    func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
        guard (navController?.viewControllers.count ?? 0) > 1 else { return false }
        return !(navHost?.hasActiveUnsavedSubState() ?? false)
    }
}

private enum AssociatedKeys {
    static var delegate: UInt8 = 0
    static var dataSource: UInt8 = 1
    static var backupDelegate: UInt8 = 2
}

private func topViewController() -> UIViewController? {
    guard let scene = UIApplication.shared.connectedScenes
        .first(where: { $0.activationState == .foregroundActive }) as? UIWindowScene,
        let rootVC = scene.windows.first(where: { $0.isKeyWindow })?.rootViewController
    else { return nil }
    var topVC = rootVC
    // rootVC is now a UINavigationController; resolve the currently pushed screen first so the PDF
    // picker / QuickLook preview present from whatever detail is showing, not just the tab root.
    if let nav = topVC as? UINavigationController, let visible = nav.visibleViewController {
        topVC = visible
    }
    while let presented = topVC.presentedViewController {
        topVC = presented
    }
    return topVC
}

class DocumentPickerDelegate: NSObject, UIDocumentPickerDelegate {
    let onResult: (KotlinByteArray, String) -> Void

    init(onResult: @escaping (KotlinByteArray, String) -> Void) {
        self.onResult = onResult
    }

    func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
        guard let url = urls.first else { return }
        
        // Start accessing security-scoped resource
        guard url.startAccessingSecurityScopedResource() else { return }
        defer { url.stopAccessingSecurityScopedResource() }
        
        do {
            let data = try Data(contentsOf: url)
            let filename = url.lastPathComponent
            
            // Convert Swift Data to KotlinByteArray
            let kotlinArray = KotlinByteArray(size: Int32(data.count))
            for i in 0..<data.count {
                kotlinArray.set(index: Int32(i), value: Int8(bitPattern: data[i]))
            }
            
            onResult(kotlinArray, filename)
        } catch {
            print("Error reading PDF file: \(error)")
        }
    }

    func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
        // Handle cancellation if needed
    }
}

/// Reads a picked encrypted backup archive (.pcda) and hands its raw bytes back to Kotlin.
class BackupPickerDelegate: NSObject, UIDocumentPickerDelegate {
    let onResult: (KotlinByteArray) -> Void

    init(onResult: @escaping (KotlinByteArray) -> Void) {
        self.onResult = onResult
    }

    func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
        guard let url = urls.first else { return }
        guard url.startAccessingSecurityScopedResource() else { return }
        defer { url.stopAccessingSecurityScopedResource() }

        do {
            let data = try Data(contentsOf: url)
            let kotlinArray = KotlinByteArray(size: Int32(data.count))
            for i in 0..<data.count {
                kotlinArray.set(index: Int32(i), value: Int8(bitPattern: data[i]))
            }
            onResult(kotlinArray)
        } catch {
            print("Error reading backup file: \(error)")
        }
    }

    func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
        // Handle cancellation if needed
    }
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.keyboard) // Compose has its own keyboard handling
            .edgesIgnoringSafeArea(.all)
    }
}
