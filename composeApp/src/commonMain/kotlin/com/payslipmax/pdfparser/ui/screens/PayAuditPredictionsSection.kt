package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.insights.DsopRoom
import com.payslipmax.pdfparser.insights.timeline.NextIncrementPrediction
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.PayAuditStrings

/**
 * "What's next" section of [PayAuditScreen] (docs/Plan/09_PayAudit_PhasePlan.md Phase 6): the next
 * increment and DSOP room predictions, both derived from the timeline/history alone with no user input —
 * free, same as the timeline and change-explanation sections. The pay-fixation calculator is a separate
 * section ([payAuditFixationCalculatorItems]) because it is the one Phase 6 deliverable that inherently
 * needs a user input (a promotion that hasn't happened yet).
 */
fun LazyListScope.payAuditPredictionsItems(
    incrementPrediction: NextIncrementPrediction?,
    dsopRoom: DsopRoom?,
) {
    item(key = "pay_audit_predictions_header", contentType = "section_header") {
        Text(text = PayAuditStrings.predictionsSectionTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
    item(key = "pay_audit_next_increment", contentType = "prediction_card") {
        NextIncrementCard(incrementPrediction)
    }
    dsopRoom?.let {
        item(key = "pay_audit_dsop_room", contentType = "prediction_card") {
            DsopRoomCard(it)
        }
    }
}

@Composable
private fun NextIncrementCard(prediction: NextIncrementPrediction?) {
    PredictionCard(title = PayAuditStrings.nextIncrementTitle) {
        if (prediction == null) {
            Text(text = PayAuditStrings.nextIncrementEmptyState, style = MaterialTheme.typography.bodyMedium)
        } else {
            val datePrefix = if (prediction.isOverdue) PayAuditStrings.nextIncrementOverduePrefix else PayAuditStrings.nextIncrementDatePrefix
            Text(
                text = "$datePrefix${formatPayMonth(prediction.date)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (prediction.isOverdue) MaterialTheme.colorScheme.error else Color.Unspecified,
            )
            Text(
                text = "${PayAuditStrings.nextIncrementAmountPrefix}${formatCurrency(prediction.predictedBasicPay)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DsopRoomCard(room: DsopRoom) {
    PredictionCard(title = "${PayAuditStrings.dsopRoomTitle} (${room.financialYearLabel})") {
        Text(
            text = "${PayAuditStrings.dsopRoomSubscribedPrefix}${formatCurrency(room.subscribedYtd)}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "${PayAuditStrings.dsopRoomLeftPrefix}${formatCurrency(room.roomLeft)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun PredictionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadiusMedium),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = BorderStroke(AppDimensions.BorderHairline, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingSmall),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTwo),
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}
