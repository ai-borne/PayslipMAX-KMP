package com.payslipmax.pdfparser.integrity

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.payslipmax.pdfparser.domain.AppIntegrityChecker
import com.payslipmax.pdfparser.domain.AppIntegrityStatus
import java.security.MessageDigest

/**
 * Android implementation of [AppIntegrityChecker].
 * Validates installer package identity and optional certificate signature hashes.
 */
class AndroidAppIntegrityChecker(
    private val context: Context,
    private val expectedSha256Signature: String = "",
    private val isDebug: Boolean = false,
    private val installerNameProvider: ((Context) -> String?)? = null,
) : AppIntegrityChecker {
    companion object {
        private val ALLOWED_INSTALLERS =
            listOf(
                "com.android.vending",
                "com.google.android.feedback",
            )
        private val DEBUG_INSTALLERS =
            listOf(
                "com.android.shell",
                "com.google.android.packageinstaller",
                "org.robolectric",
                "com.android.pipeline",
                "com.android.test",
            )
    }

    override suspend fun checkIntegrity(): AppIntegrityStatus {
        try {
            val pm = context.packageManager
            val packageName = context.packageName

            val installerName =
                if (installerNameProvider != null) {
                    installerNameProvider.invoke(context)
                } else {
                    getInstallerPackageName(pm, packageName)
                }

            val isInstallerValid = isInstallerAllowed(installerName)

            if (!isInstallerValid) {
                val reason = "Unauthorized installer: ${installerName ?: "unknown/direct sideload"}"
                return AppIntegrityStatus.Sideloaded(reason)
            }

            if (expectedSha256Signature.isNotBlank()) {
                val isSignatureValid = verifySignature(pm, packageName)
                if (!isSignatureValid) {
                    return AppIntegrityStatus.Tampered("App signing signature mismatch")
                }
            }

            return AppIntegrityStatus.Valid
        } catch (e: Exception) {
            if (isDebug) {
                return AppIntegrityStatus.Valid
            }
            return AppIntegrityStatus.Sideloaded("Integrity check failed: ${e.message}")
        }
    }

    private fun getInstallerPackageName(
        pm: PackageManager,
        packageName: String,
    ): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(packageName)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun isInstallerAllowed(installer: String?): Boolean {
        if (installer in ALLOWED_INSTALLERS) return true
        if (isDebug && (installer == null || installer in DEBUG_INSTALLERS)) return true
        return false
    }

    private fun verifySignature(
        pm: PackageManager,
        packageName: String,
    ): Boolean {
        try {
            @Suppress("DEPRECATION")
            val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)

            @Suppress("DEPRECATION")
            val signatures = packageInfo.signatures ?: return false

            for (sig in signatures) {
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(sig.toByteArray())
                val hexString = digest.joinToString("") { "%02X".format(it) }
                if (hexString.equals(expectedSha256Signature, ignoreCase = true)) {
                    return true
                }
            }
        } catch (_: Exception) {
            return false
        }
        return false
    }
}
