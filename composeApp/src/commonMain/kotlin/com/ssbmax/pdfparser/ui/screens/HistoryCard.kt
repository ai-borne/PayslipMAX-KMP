package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryCard(
    payslip: ParsedPayslip,
    onViewReplica: () -> Unit,
    onLongPress: () -> Unit,
    onSwipeDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        onDismiss = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onSwipeDelete()
            }
            scope.launch { dismissState.reset() }
        },
        backgroundContent = { SwipeDismissBackground() },
        modifier = modifier.clip(CardDefaults.shape),
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = onViewReplica,
                        onLongClick = onLongPress,
                    ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            elevation = CardDefaults.cardElevation(defaultElevation = AppDimensions.BorderThin),
        ) {
            content()
        }
    }
}

@Composable
private fun SwipeDismissBackground() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.error)
                .padding(horizontal = AppDimensions.PaddingMedium),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onError,
            modifier = Modifier.size(AppDimensions.IconSizeMedium),
        )
    }
}
