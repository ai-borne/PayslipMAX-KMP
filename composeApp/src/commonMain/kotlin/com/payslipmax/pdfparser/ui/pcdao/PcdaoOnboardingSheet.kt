package com.payslipmax.pdfparser.ui.pcdao

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val ONBOARDING_SLIDE_COUNT = 3

@OptIn(ExperimentalComposeUiApi::class)
@Suppress("DEPRECATION")
@Composable
fun PcdaoOnboardingSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { ONBOARDING_SLIDE_COUNT })
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = pagerState.currentPage > 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(pagerState.currentPage - 1)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = modifier.fillMaxWidth(0.92f).testTag("pcdao_onboarding_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        ) {
            OnboardingCardBody(
                pagerState = pagerState,
                coroutineScope = coroutineScope,
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun OnboardingCardBody(
    pagerState: PagerState,
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("pcdao_onboarding_skip")) {
                Text(text = AppStringsPcdao.onboardingBtnSkip)
            }
        }

        OnboardingPagerContent(pagerState = pagerState)

        PcdaoOnboardingPagerIndicator(
            pageCount = ONBOARDING_SLIDE_COUNT,
            currentPage = pagerState.currentPage,
            modifier = Modifier.padding(vertical = 8.dp),
        )

        OnboardingBottomActionsRow(
            pagerState = pagerState,
            onDismiss = onDismiss,
            onNext = { coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
            onBack = { coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
        )
    }
}

@Composable
private fun OnboardingPagerContent(pagerState: PagerState) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth().height(250.dp).testTag("pcdao_onboarding_pager"),
    ) { page ->
        when (page) {
            0 ->
                OnboardingSlideContent(
                    icon = AppStringsPcdao.onboardingSlide1Icon,
                    title = AppStringsPcdao.onboardingSlide1Title,
                    body = AppStringsPcdao.onboardingSlide1Body,
                    testTag = "pcdao_onboarding_slide_0",
                )
            1 ->
                OnboardingSlideContent(
                    icon = AppStringsPcdao.onboardingSlide2Icon,
                    title = AppStringsPcdao.onboardingSlide2Title,
                    body = AppStringsPcdao.onboardingSlide2Body,
                    testTag = "pcdao_onboarding_slide_1",
                )
            else ->
                OnboardingSlideContent(
                    icon = AppStringsPcdao.onboardingSlide3Icon,
                    title = AppStringsPcdao.onboardingSlide3Title,
                    body = AppStringsPcdao.onboardingSlide3Body,
                    testTag = "pcdao_onboarding_slide_2",
                )
        }
    }
}

@Composable
private fun OnboardingSlideContent(
    icon: String,
    title: String,
    body: String,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp)
                .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(60.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        shape = CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = icon, style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PcdaoOnboardingPagerIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics { contentDescription = AppStringsPcdao.onboardingPagerIndicatorDesc }
                .testTag("pcdao_onboarding_indicator"),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val isActive = index == currentPage
            val width = if (isActive) 24.dp else 8.dp
            val color =
                if (isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                }
            Box(
                modifier =
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(width = width, height = 8.dp)
                        .background(color = color, shape = CircleShape),
            )
        }
    }
}

@Composable
private fun OnboardingBottomActionsRow(
    pagerState: PagerState,
    onDismiss: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (pagerState.currentPage > 0) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.testTag("pcdao_onboarding_back"),
            ) {
                Text(text = AppStringsPcdao.onboardingBtnBack)
            }
        } else {
            Spacer(modifier = Modifier.size(1.dp))
        }

        if (pagerState.currentPage < ONBOARDING_SLIDE_COUNT - 1) {
            Button(
                onClick = onNext,
                modifier = Modifier.testTag("pcdao_onboarding_next"),
            ) {
                Text(text = AppStringsPcdao.onboardingBtnNext)
            }
        } else {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("pcdao_onboarding_enter"),
            ) {
                Text(text = AppStringsPcdao.onboardingBtnEnter)
            }
        }
    }
}
