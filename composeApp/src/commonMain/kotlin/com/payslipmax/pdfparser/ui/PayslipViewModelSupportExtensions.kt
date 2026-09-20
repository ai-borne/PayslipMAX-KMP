package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.platform.PlatformSystemInfo
import com.payslipmax.pdfparser.platform.getPlatformSystemInfo
import com.payslipmax.pdfparser.platform.platformAppVersion
import com.payslipmax.pdfparser.telemetry.DiagnosticReportGenerator
import com.payslipmax.pdfparser.telemetry.InstallationIdManager
import com.payslipmax.pdfparser.telemetry.TelemetrySanitizer
import com.payslipmax.pdfparser.ui.theme.AppStringsSupport

/** Device/app facts embedded in a support report; injectable so tests need no platform APIs. */
data class IssueReportEnvironment(
    val appVersion: String,
    val installationId: String,
    val systemInfo: PlatformSystemInfo,
) {
    companion object {
        fun current() =
            IssueReportEnvironment(
                appVersion = platformAppVersion(),
                installationId = InstallationIdManager().getOrCreateInstallationId(),
                systemInfo = getPlatformSystemInfo(),
            )
    }
}

/** Builds the anonymous diagnostic report, appending the user's PII-sanitized description. */
fun PayslipViewModel.buildDiagnosticReport(
    userDescription: String,
    environment: IssueReportEnvironment = IssueReportEnvironment.current(),
): String {
    val report =
        DiagnosticReportGenerator.generateReport(
            appVersion = environment.appVersion,
            installationId = environment.installationId,
            osVersion = environment.systemInfo.osVersion,
            deviceModel = environment.systemInfo.deviceModel,
            cpuArch = environment.systemInfo.cpuArchitecture,
            lastScreen = AppStringsSupport.reportIssueScreenName,
            lastOperation = AppStringsSupport.reportIssueOperationName,
        )
    val description = TelemetrySanitizer.sanitizeMessage(userDescription).trim()
    if (description.isEmpty()) return report
    return report + AppStringsSupport.reportIssueUserDescriptionHeader + "\n" + description + "\n"
}

/** Sends the report to support; version + installation ID in the subject let triagers filter. */
fun PayslipViewModel.submitIssueReport(
    userDescription: String,
    environment: IssueReportEnvironment = IssueReportEnvironment.current(),
) {
    val subject =
        "${AppStringsSupport.reportIssueEmailSubject} " +
            "(v${environment.appVersion}, ${environment.installationId})"
    issueReportSender(
        AppStringsSupport.supportEmail,
        subject,
        buildDiagnosticReport(userDescription, environment),
    )
}
