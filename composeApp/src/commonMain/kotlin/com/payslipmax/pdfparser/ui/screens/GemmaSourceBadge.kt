package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.theme.AppColors
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

/** Small badge marking a ledger row's value as Tier 6 Gemma-inferred rather than solver-parsed. */
@Composable
internal fun GemmaSourceBadge() {
    Box(
        modifier =
            Modifier
                .padding(start = AppDimensions.SpacingTiny)
                .background(AppColors.AiInferred.copy(alpha = 0.15f), RoundedCornerShape(AppDimensions.CornerRadiusSmall))
                .padding(horizontal = AppDimensions.SpacingTiny),
    ) {
        Text(
            text = AppStrings.correctionAiSourceBadge,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = AppColors.AiInferred,
        )
    }
}
