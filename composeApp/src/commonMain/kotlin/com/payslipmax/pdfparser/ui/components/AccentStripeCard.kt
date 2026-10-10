package com.payslipmax.pdfparser.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.payslipmax.pdfparser.ui.theme.AppDimensions

private const val ACCENT_BORDER_ALPHA = 0.3f

/** The shape of [AccentStripeCard], for a caller that clips a click ripple to it. */
val AccentStripeCardShape = RoundedCornerShape(AppDimensions.CornerRadius)

/**
 * A surface card with a coloured stripe down its left edge and a matching faint border: the Smart Insights card
 * look, shared so every accent-striped card in the app is drawn the same way. [content] sits to the right of the
 * stripe and owns its own padding.
 */
@Composable
fun AccentStripeCard(
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AccentStripeCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, accent.copy(alpha = ACCENT_BORDER_ALPHA)),
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.fillMaxHeight().width(AppDimensions.SpacingTiny).background(accent))
            content()
        }
    }
}
