package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStringsOnboarding

@Composable
private fun OnboardingSlideBody(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(AppDimensions.PaddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(AppDimensions.SpacingMedium))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun OnboardingSlideOne(modifier: Modifier = Modifier) {
    OnboardingSlideBody(
        title = AppStringsOnboarding.onboardingSlide1Title,
        body = AppStringsOnboarding.onboardingSlide1Body,
        modifier = modifier,
    )
}

@Composable
fun OnboardingSlideTwo(modifier: Modifier = Modifier) {
    OnboardingSlideBody(
        title = AppStringsOnboarding.onboardingSlide2Title,
        body = AppStringsOnboarding.onboardingSlide2Body,
        modifier = modifier,
    )
}

@Composable
fun OnboardingSlideThree(
    onGetStarted: () -> Unit,
    onNavigateToFaq: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(AppDimensions.PaddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = AppStringsOnboarding.onboardingSlide3Title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(AppDimensions.SpacingLarge))
        Button(
            onClick = onGetStarted,
            modifier = Modifier.fillMaxWidth(),
            colors =
                androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                ),
        ) {
            Text(text = AppStringsOnboarding.onboardingCtaGetStarted)
        }
        TextButton(onClick = onNavigateToFaq) {
            Text(text = AppStringsOnboarding.onboardingFaqLinkText)
        }
    }
}
