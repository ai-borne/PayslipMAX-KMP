package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
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
 * Wraps on a narrow screen rather than cutting a long area or case title. Each link keeps the 48dp minimum touch
 * target; separators are not read aloud.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GuideBreadcrumb(
    crumbs: List<GuideCrumb>,
    onCrumb: (path: List<GuideDestination>) -> Unit,
) {
    FlowRow(
        modifier = Modifier.padding(horizontal = AppDimensions.PaddingMedium),
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSix),
    ) {
        crumbs.forEachIndexed { position, crumb ->
            if (position > 0) {
                Text(
                    GuideStrings.breadcrumbSeparator,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically).clearAndSetSemantics {},
                )
            }
            val description = GuideStrings.breadcrumbDescription(crumb.label)
            Text(
                crumb.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier =
                    Modifier
                        .align(Alignment.CenterVertically)
                        .minimumInteractiveComponentSize()
                        .clickable(role = Role.Button) { onCrumb(crumb.path) }
                        .semantics { contentDescription = description },
            )
        }
    }
}
