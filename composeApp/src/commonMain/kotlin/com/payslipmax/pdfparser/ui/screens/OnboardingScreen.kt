package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStringsOnboarding

private const val ONBOARDING_PAGE_COUNT = 3
private const val ONBOARDING_CARD_WIDTH_FRACTION = 0.9f

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onNavigateToFaq: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { ONBOARDING_PAGE_COUNT })

    Box(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(ONBOARDING_CARD_WIDTH_FRACTION).testTag("onboarding_card"),
            shape = RoundedCornerShape(AppDimensions.CornerRadius),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                    TextButton(onClick = onFinished, modifier = Modifier.testTag("onboarding_skip")) {
                        Text(text = AppStringsOnboarding.onboardingSkip)
                    }
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().height(AppDimensions.OnboardingCardPagerHeight).testTag("onboarding_pager"),
                ) { page ->
                    when (page) {
                        0 -> OnboardingSlideOne()
                        1 -> OnboardingSlideTwo()
                        else -> OnboardingSlideThree(onGetStarted = onFinished, onNavigateToFaq = onNavigateToFaq)
                    }
                }
                OnboardingPagerIndicator(
                    pageCount = ONBOARDING_PAGE_COUNT,
                    currentPage = pagerState.currentPage,
                    modifier = Modifier.padding(bottom = AppDimensions.PaddingLarge),
                )
            }
        }
    }
}

@Composable
private fun OnboardingPagerIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics { contentDescription = AppStringsOnboarding.onboardingPagerIndicatorDescription },
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        repeat(pageCount) { index ->
            val isActive = index == currentPage
            val color =
                if (isActive) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                }
            Box(
                modifier =
                    Modifier
                        .padding(horizontal = AppDimensions.SpacingTwo)
                        .size(AppDimensions.SpacingSmall)
                        .background(color = color, shape = CircleShape),
            )
        }
    }
}
