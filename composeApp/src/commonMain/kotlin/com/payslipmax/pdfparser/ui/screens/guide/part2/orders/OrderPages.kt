package com.payslipmax.pdfparser.ui.screens.guide.part2.orders

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.payslipmax.pdfparser.part2.domain.VaultPageRef
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.OrderStrings

/** Reads one stored page and turns it into a picture; the bytes are zeroed by the repository once this returns. */
internal fun interface PageReader {
    suspend fun read(pageId: String): ImageBitmap?
}

internal fun LazyListScope.pageRows(
    state: OrderState,
    actions: OrderActions,
) {
    items(state.pages.size, key = { state.pages[it].id }) { PageRow(state.pages[it], state.pages.size, actions) }
}

internal fun LazyListScope.pagesFooter(
    state: OrderState,
    actions: OrderActions,
) {
    OrderNotices.of(state.capture)?.let { note -> item(key = "capture") { Text(note, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) } }
    if (actions.canEdit) item(key = "addPages") { Button(onClick = actions.onAddPages, modifier = Modifier.fillMaxWidth()) { Text(OrderStrings.addPages) } }
}

/** One page: shown only while the officer asks to see it, and dropped from memory when hidden. */
@Composable
private fun PageRow(
    ref: VaultPageRef,
    total: Int,
    actions: OrderActions,
) {
    var shown by remember(ref.id) { mutableStateOf(false) }
    var confirming by remember(ref.id) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        Text(OrderStrings.page(ref.position + 1, total), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
            TextButton(onClick = { shown = !shown }) { Text(if (shown) OrderStrings.hidePage else OrderStrings.showPage) }
            // A delete, like Delete order: a lapsed officer may still view, delete and back up.
            TextButton(onClick = { confirming = true }) { Text(OrderStrings.deletePage) }
        }
        if (shown) PageImage(ref, actions)
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(OrderStrings.deletePage) },
            text = { Text(OrderStrings.deletePageBody) },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    actions.onDeletePage(ref.id)
                }) { Text(OrderStrings.confirm) }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text(OrderStrings.cancel) } },
        )
    }
}

@Composable
private fun PageImage(
    ref: VaultPageRef,
    actions: OrderActions,
) {
    val image by produceState<ImageBitmap?>(null, ref.id) { value = actions.readPage.read(ref.id) }
    val bitmap = image
    if (bitmap == null) {
        Text(OrderStrings.pageUnreadable, style = MaterialTheme.typography.bodySmall)
    } else {
        Image(bitmap = bitmap, contentDescription = OrderStrings.pageNumber(ref.position + 1), contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth())
    }
}
