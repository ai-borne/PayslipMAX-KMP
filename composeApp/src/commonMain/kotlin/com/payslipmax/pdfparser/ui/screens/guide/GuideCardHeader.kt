package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.theme.AppDimensions

private val AnswerShape = RoundedCornerShape(AppDimensions.CornerRadius)

/**
 * The top of a card under its title: the facet and trust chips, the notices for an unverified point or old rates, then
 * the one-line answer, set apart so it reads first (the approved preview: "answer, then proof"). Everything here is
 * free; the "your figure" line (E6) joins it later. The paid half is drawn by [guideCardSections].
 */
@Composable
internal fun GuideCardHeader(card: GuideCardContent) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium)) {
        GuideLabelPill(card.facetLabel)
        GuideTrustChips(card.trust)
        GuideTrustNotices(card.trust, card.ratesStale)
        Text(
            card.answer,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer, AnswerShape)
                    .padding(AppDimensions.SpacingMedium),
        )
    }
}
