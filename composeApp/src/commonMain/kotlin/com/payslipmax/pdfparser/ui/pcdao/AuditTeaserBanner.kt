package com.payslipmax.pdfparser.ui.pcdao

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.payslipmax.pdfparser.ui.screens.CardTint
import com.payslipmax.pdfparser.ui.screens.FlatBorderedCard
import com.payslipmax.pdfparser.ui.screens.formatCurrency

/**
 * Audit teaser banner displayed on free tier to showcase detected claims and prompt upgrade.
 */
@Composable
fun AuditTeaserBanner(
    claimsCount: Int,
    unclaimedTotal: Double,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlatBorderedCard(
        modifier = modifier.fillMaxWidth().clickable(onClick = onUpgradeClick),
        tint = CardTint.Accent,
        contentSpacing = 6.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TeaserTextContent(
                claimsCount = claimsCount,
                unclaimedTotal = unclaimedTotal,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onUpgradeClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(
                    text = AppStringsPcdao.teaserUpgradeButton,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun TeaserTextContent(
    claimsCount: Int,
    unclaimedTotal: Double,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "✨ $claimsCount ${AppStringsPcdao.teaserHeadlineClaimsPrefix}${formatCurrency(unclaimedTotal)} ${AppStringsPcdao.teaserHeadlineClaimsSuffix}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = AppStringsPcdao.teaserHeadlineDesc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
