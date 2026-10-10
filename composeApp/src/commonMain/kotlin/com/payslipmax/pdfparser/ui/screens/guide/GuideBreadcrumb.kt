package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.components.ScreenBackHeaderTitleInset
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.GuideStrings

/**
 * The top of a feed or card: the breadcrumb, then the app's back header. iOS has no edge-swipe inside a tab, so
 * the back arrow returns to the previous level and each crumb goes straight up to its level.
 */
@Composable
internal fun GuideLevelHeader(
    crumbs: List<GuideCrumb>,
    onCrumb: (path: List<GuideDestination>) -> Unit,
    title: String,
    subtitle: String?,
    onBack: () -> Unit,
) {
    Column {
        GuideBreadcrumb(crumbs, onCrumb)
        ScreenBackHeader(title = title, subtitle = subtitle?.takeIf(String::isNotBlank), onBack = onBack)
    }
}

/**
 * One line, so a long area or case title never wraps and strands a separator: crumbs share the width and a long one
 * is ellipsised (screen readers still hear its full label). Each link is padded to the 48dp touch target; separators
 * are not read aloud.
 */
@Composable
private fun GuideBreadcrumb(
    crumbs: List<GuideCrumb>,
    onCrumb: (path: List<GuideDestination>) -> Unit,
) {
    Row(
        // The first crumb's text starts where the header title starts (its padding is taken off the inset).
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = ScreenBackHeaderTitleInset - CrumbPadding, end = AppDimensions.PaddingMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        crumbs.forEachIndexed { position, crumb ->
            if (position > 0) {
                Text(
                    GuideStrings.breadcrumbSeparator,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }
            val description = GuideStrings.breadcrumbDescription(crumb.label)
            Text(
                crumb.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        // Crumbs share the line, so a long title is shortened rather than wrapped or pushed off screen.
                        .weight(1f, fill = false)
                        .clickable(role = Role.Button) { onCrumb(crumb.path) }
                        .semantics { contentDescription = description }
                        .defaultMinSize(minWidth = AppDimensions.IconSizeDouble, minHeight = AppDimensions.IconSizeDouble)
                        .padding(horizontal = CrumbPadding)
                        .wrapContentHeight(),
            )
        }
    }
}

private val CrumbPadding = AppDimensions.SpacingSmall
