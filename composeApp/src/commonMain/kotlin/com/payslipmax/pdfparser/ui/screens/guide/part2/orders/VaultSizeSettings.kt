package com.payslipmax.pdfparser.ui.screens.guide.part2.orders

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.OrderStrings
import org.koin.compose.getKoin

/** The Settings line about the vault's size: always the figure while Part II is on (the repository is bound only then), the warning tone past the threshold. */
@Composable
internal fun VaultSizeSettingsLine(repository: Part2Repository?) {
    if (repository == null) return
    val bytes by produceState(0L, repository) { value = repository.vaultBytesOrZero() }
    val warning = VaultSize.warning(bytes)
    Text(
        warning ?: VaultSize.usage(bytes),
        style = MaterialTheme.typography.bodySmall,
        color = if (warning != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(AppDimensions.PaddingMedium),
    )
}

/** The Settings line, with the repository taken from Koin: bound only while Part II is on, so a build with it off shows nothing. */
@Composable
fun VaultSizeSettings() {
    VaultSizeSettingsLine(getKoin().getOrNull<Part2Repository>())
}

/** What the backup screen says about where a backup goes: shown while Part II is on, because a backup then carries scanned pages. */
@Composable
fun BackupCloudNote(visible: Boolean) {
    if (!visible) return
    Text(
        OrderStrings.backupCloudWarning,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )
}
