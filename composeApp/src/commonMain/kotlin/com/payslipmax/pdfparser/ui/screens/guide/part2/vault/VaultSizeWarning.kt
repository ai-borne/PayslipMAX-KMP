package com.payslipmax.pdfparser.ui.screens.guide.part2.vault

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.Part2VaultStrings
import org.koin.compose.getKoin

/** The Settings line about the vault's size: nothing until it passes `Part2Limits.VAULT_WARN_BYTES`, then the figures. */
internal object VaultSizeWarning {
    private const val MB = 1024L * 1024

    fun text(bytes: Long): String? =
        if (bytes > Part2Limits.VAULT_WARN_BYTES) Part2VaultStrings.sizeWarning(megabytes(bytes), megabytes(Part2Limits.VAULT_BYTES_MAX)) else null

    /** Whole megabytes, rounded up, so a vault just over 60 MB reads 61 and never "60 MB" beside a warning. */
    private fun megabytes(bytes: Long): Long = (bytes + MB - 1) / MB
}

/** The warning for [repository]'s vault, read when the line is shown; nothing when there is no repository (Part II is off) or the vault is small. */
@Composable
internal fun VaultSizeSettingsLine(repository: Part2Repository?) {
    if (repository == null) return
    val bytes by produceState(0L, repository) { value = repository.vaultBytes() }
    val warning = VaultSizeWarning.text(bytes) ?: return
    Text(
        warning,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.fillMaxWidth().padding(AppDimensions.PaddingMedium),
    )
}

/** The Settings line, with the repository taken from Koin: bound only while Part II is on, so a build with it off shows nothing. */
@Composable
fun VaultSizeSettings() {
    VaultSizeSettingsLine(getKoin().getOrNull<Part2Repository>())
}
