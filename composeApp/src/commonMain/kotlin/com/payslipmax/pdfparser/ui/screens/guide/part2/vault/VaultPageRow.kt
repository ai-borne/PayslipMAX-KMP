package com.payslipmax.pdfparser.ui.screens.guide.part2.vault

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.Part2DeleteButton
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.Part2VaultStrings

/**
 * One stored page: a button that shows it and hides it again, and one that deletes it after asking ([onlyPage] says the order goes with it). The picture exists only while it is shown (it is dropped on hide and when
 * the screen leaves composition, which includes the app lock), and is built from the decrypted bytes inside [loadPage], which the
 * repository zeroes the moment it returns. It is never written to a file, the gallery or the clipboard.
 */
@Composable
internal fun VaultPageRow(
    page: PageUi,
    onlyPage: Boolean,
    loadPage: suspend (pageId: String) -> ImageBitmap?,
    onDelete: () -> Unit,
) {
    var showing by remember(page.id) { mutableStateOf(false) }
    var image by remember(page.id) { mutableStateOf<ImageBitmap?>(null) }
    var unreadable by remember(page.id) { mutableStateOf(false) }
    LaunchedEffect(page.id, showing) {
        image = null
        unreadable = false
        if (showing) {
            image = loadPage(page.id)
            unreadable = image == null
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        OutlinedButton(onClick = { showing = !showing }, modifier = Modifier.fillMaxWidth()) { Text(if (showing) Part2VaultStrings.hidePage else Part2VaultStrings.viewPage(page.number)) }
        image?.let { Image(bitmap = it, contentDescription = page.label, contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth()) }
        if (unreadable) Text(Part2VaultStrings.pageUnreadable, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        Part2DeleteButton(
            Part2VaultStrings.deletePage,
            Part2VaultStrings.deletePageTitle,
            if (onlyPage) Part2VaultStrings.deleteLastPageBody else Part2VaultStrings.deletePageBody,
            onDelete,
        )
    }
}
