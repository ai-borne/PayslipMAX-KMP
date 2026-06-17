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
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(onPickPdf: { onResult in
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
        }, onOpenPdf: { bytes, name in
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
        })
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

private enum AssociatedKeys {
    static var delegate: UInt8 = 0
    static var dataSource: UInt8 = 1
}

private func topViewController() -> UIViewController? {
    guard let scene = UIApplication.shared.connectedScenes
        .first(where: { $0.activationState == .foregroundActive }) as? UIWindowScene,
        let rootVC = scene.windows.first(where: { $0.isKeyWindow })?.rootViewController
    else { return nil }
    var topVC = rootVC
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

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.keyboard) // Compose has its own keyboard handling
            .edgesIgnoringSafeArea(.all)
    }
}
