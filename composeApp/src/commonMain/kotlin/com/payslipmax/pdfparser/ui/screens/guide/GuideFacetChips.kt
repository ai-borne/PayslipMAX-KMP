package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.GuideStrings

/**
 * "All" plus one chip per facet in the feed, each with its count; scrolls sideways on a narrow screen. Drawn only
 * when [GuideFeedContent.facets] is not empty (a long feed with more than one facet). Each chip is read aloud as
 * its label and card count, and the chosen one as selected.
 */
@Composable
internal fun GuideFacetChips(
    feed: GuideFeedContent,
    onSelect: (facet: String?) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSix),
    ) {
        FacetChip(
            text = GuideStrings.facetAll(feed.totalCount),
            description = GuideStrings.facetChipDescription(GuideStrings.facetAllLabel, feed.totalCount),
            selected = feed.selectedFacet == null,
            onClick = { onSelect(null) },
        )
        feed.facets.forEach { facet ->
            FacetChip(
                text = GuideStrings.facetChip(facet.label, facet.count),
                description = GuideStrings.facetChipDescription(facet.label, facet.count),
                selected = feed.selectedFacet == facet.key,
                onClick = { onSelect(facet.key) },
            )
        }
    }
}

@Composable
private fun FacetChip(
    text: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        modifier = Modifier.semantics { contentDescription = description },
    )
}
