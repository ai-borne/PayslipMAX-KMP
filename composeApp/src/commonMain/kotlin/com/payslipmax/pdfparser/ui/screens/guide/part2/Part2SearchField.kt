package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import com.payslipmax.pdfparser.part2.domain.Part2SearchResult
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.GuideStrings
import com.payslipmax.pdfparser.ui.theme.Part2Strings

/** The search box. The keyboard opens only on a fresh, empty search; coming back from a code page keeps the query and leaves it shut. */
@Composable
internal fun Part2SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) { if (query.isEmpty()) focus.requestFocus() }
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(Part2Strings.searchPlaceholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Close, contentDescription = Part2Strings.searchClear) }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        modifier = Modifier.fillMaxWidth().padding(horizontal = AppDimensions.PaddingMedium).focusRequester(focus),
    )
}

/** The rows under the box for everything but "nothing typed", which shows the ribbons instead. */
internal fun LazyListScope.part2SearchItems(
    result: Part2SearchResult,
    onOpenCode: (String) -> Unit,
) {
    when (result) {
        Part2SearchResult.Idle -> Unit
        Part2SearchResult.TooShort -> item(key = "message") { Part2Message(Part2Strings.searchTooShort) }
        is Part2SearchResult.Results ->
            if (result.rows.isEmpty()) {
                item(key = "message") { Part2Message(Part2Strings.searchNone) }
            } else {
                item(key = "count") { Part2Count(GuideStrings.resultCount(result.rows.size)) }
                items(result.rows.size, key = { result.rows[it].serial }) { Part2CodeRowView(result.rows[it]) { onOpenCode(result.rows[it].openCode) } }
            }
    }
}

@Composable
internal fun Part2Message(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** The match count, read out when it changes so a screen-reader user hears the effect of typing. */
@Composable
private fun Part2Count(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

internal val Part2ListPadding = PaddingValues(AppDimensions.PaddingMedium)
internal val Part2ListSpacing = Arrangement.spacedBy(AppDimensions.SpacingMedium)
