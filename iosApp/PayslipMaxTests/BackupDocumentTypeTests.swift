//
//  BackupDocumentTypeTests.swift
//  PayslipMaxTests
//
//  Covers iOS Uniform Type Identifier (UTI) and CFBundleDocumentTypes declarations
//  for PayslipMax backup files (.pcda). Verifies exported type declarations conform to
//  public.data and public.content so Mail/Gmail and system share extensions can attach
//  and open .pcda backup archives without restriction.
//

import XCTest
import UniformTypeIdentifiers
@testable import PayslipMax

final class BackupDocumentTypeTests: XCTestCase {

    func test_backupUTITypeRegistered_inExportedTypeDeclarations() {
        let infoPlist = Bundle.main.infoDictionary
        let exportedTypes = infoPlist?["UTExportedTypeDeclarations"] as? [[String: Any]]
        XCTAssertNotNil(exportedTypes, "UTExportedTypeDeclarations must be present in Info.plist")

        let backupType = exportedTypes?.first { ($0["UTTypeIdentifier"] as? String) == "com.payslipmax.backup" }
        XCTAssertNotNil(backupType, "com.payslipmax.backup must be declared in UTExportedTypeDeclarations")

        XCTAssertEqual(backupType?["UTTypeDescription"] as? String, "PayslipMax Backup Archive")

        let conformsTo = backupType?["UTTypeConformsTo"] as? [String]
        XCTAssertNotNil(conformsTo, "UTTypeConformsTo must be present")
        XCTAssertTrue(conformsTo?.contains("public.data") == true, "Must conform to public.data")
        XCTAssertTrue(conformsTo?.contains("public.content") == true, "Must conform to public.content")

        let tagSpec = backupType?["UTTypeTagSpecification"] as? [String: Any]
        XCTAssertNotNil(tagSpec, "UTTypeTagSpecification must be present")

        let extensions = tagSpec?["public.filename-extension"] as? [String]
        XCTAssertTrue(extensions?.contains("pcda") == true, "Must specify 'pcda' extension")

        let mimeTypes = tagSpec?["public.mime-type"] as? [String]
        XCTAssertTrue(mimeTypes?.contains("application/octet-stream") == true, "Must specify 'application/octet-stream' MIME type")
    }

    func test_backupDocumentTypeRegistered_inCFBundleDocumentTypes() {
        let infoPlist = Bundle.main.infoDictionary
        let documentTypes = infoPlist?["CFBundleDocumentTypes"] as? [[String: Any]]
        XCTAssertNotNil(documentTypes, "CFBundleDocumentTypes must be present in Info.plist")

        let backupDoc = documentTypes?.first {
            let types = $0["LSItemContentTypes"] as? [String]
            return types?.contains("com.payslipmax.backup") == true
        }
        XCTAssertNotNil(backupDoc, "com.payslipmax.backup must be registered in CFBundleDocumentTypes")
        XCTAssertEqual(backupDoc?["CFBundleTypeName"] as? String, "PayslipMax Backup")
        XCTAssertEqual(backupDoc?["CFBundleTypeRole"] as? String, "Editor")
    }

    func test_uniformTypeIdentifier_resolvesCustomBackupType() {
        let utType = UTType("com.payslipmax.backup")
        XCTAssertNotNil(utType, "UTType should resolve com.payslipmax.backup")
        XCTAssertEqual(utType?.preferredFilenameExtension, "pcda")
        XCTAssertEqual(utType?.preferredMIMEType, "application/octet-stream")
        XCTAssertTrue(utType?.conforms(to: .data) == true, "Must conform to UTType.data")
    }

    func test_openingDocumentsInPlace_isExplicitlyDisabled() {
        let infoPlist = Bundle.main.infoDictionary
        let supportsInPlace = infoPlist?["LSSupportsOpeningDocumentsInPlace"] as? Bool
        XCTAssertEqual(
            supportsInPlace,
            false,
            "LSSupportsOpeningDocumentsInPlace must be explicitly false so iOS stages imported .pcda files into app sandbox"
        )
    }
}
