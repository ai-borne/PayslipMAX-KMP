package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.guide.domain.PersonalFigure
import com.payslipmax.pdfparser.ui.screens.FlatBorderedCardShape
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.GuideStrings

/**
 * The "Your figure" block, drawn above Key points. It is part of the paid half: it is only ever given a figure that
 * [GuideCardFull] holds, so a locked card cannot reach it. Nothing is drawn when the card has no figure or the payslip
 * could not settle one, and the card is complete without it.
 */
internal fun LazyListScope.guideYourFigure(figure: PersonalFigure?) {
    if (figure == null) return
    item(key = "figure") { GuideYourFigureBlock(figureText(figure)) }
}

@Composable
private fun GuideYourFigureBlock(text: GuideFigureText) {
    FlatBorderedCardShape {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppDimensions.SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny),
        ) {
            Text(
                GuideStrings.sectionYourFigure,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            Text(text.headline, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(text.detail, style = MaterialTheme.typography.bodyMedium)
            Text(text.footnote, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
