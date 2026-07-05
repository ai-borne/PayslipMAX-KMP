package com.payslipmax.pdfparser.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

@Composable
fun LockScreen(
    onUnlock: (String) -> Boolean,
    onPickPdf: (onResult: (ByteArray, String) -> Unit) -> Unit,
    onResetPin: (ByteArray, String, String, (Result<Unit>) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var enteredPin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(showError) {
        if (showError) triggerShake(shakeOffset)
    }

    LockScreenContent(
        showError = showError,
        pinLength = enteredPin.length,
        shakeOffset = shakeOffset.value,
        onKeyPress = { char ->
            if (enteredPin.length < 4) {
                showError = false
                enteredPin += char
                if (enteredPin.length == 4 && !onUnlock(enteredPin)) {
                    showError = true
                    enteredPin = ""
                }
            }
        },
        onBackspace = {
            if (enteredPin.isNotEmpty()) {
                showError = false
                enteredPin = enteredPin.dropLast(1)
            }
        },
        onForgotPin = { showResetDialog = true },
        modifier = modifier,
    )

    if (showResetDialog) {
        ResetPinDialog(
            onDismiss = { showResetDialog = false },
            onPickPdf = onPickPdf,
            onResetPin = onResetPin,
        )
    }
}

@Composable
private fun LockScreenContent(
    showError: Boolean,
    pinLength: Int,
    shakeOffset: Float,
    onKeyPress: (Char) -> Unit,
    onBackspace: () -> Unit,
    onForgotPin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(AppDimensions.PaddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        LockHeader(showError = showError)

        PinIndicatorDots(
            pinLength = pinLength,
            modifier = Modifier.offset(x = shakeOffset.dp),
        )

        LockKeyboard(
            onKeyPress = onKeyPress,
            onBackspace = onBackspace,
        )

        TextButton(onClick = onForgotPin) {
            Text(AppStrings.lockScreenForgotPin, fontWeight = FontWeight.SemiBold)
        }
    }
}

private suspend fun triggerShake(animatable: Animatable<Float, *>) {
    animatable.animateTo(
        targetValue = 0f,
        animationSpec =
            keyframes {
                durationMillis = 300
                0f at 0
                15f at 50
                -15f at 100
                15f at 150
                -15f at 200
                10f at 250
                0f at 300
            },
    )
}

@Composable
private fun LockHeader(
    showError: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
    ) {
        Text("🛡️", fontSize = AppDimensions.FontSizeEmoji)
        Text(
            text = AppStrings.lockScreenTitle,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = if (showError) AppStrings.lockScreenError else AppStrings.lockScreenSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = if (showError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = AppDimensions.SpacingHuge),
        )
    }
}

@Composable
private fun PinIndicatorDots(
    pinLength: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(4) { index ->
            val isFilled = index < pinLength
            Box(
                modifier =
                    Modifier
                        .padding(AppDimensions.SpacingSmall)
                        .size(AppDimensions.SpacingLarge)
                        .clip(CircleShape)
                        .background(
                            if (isFilled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
            )
        }
    }
}

@Composable
private fun LockKeyboard(
    onKeyPress: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(AppDimensions.LockKeyboardWidth),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingLarge),
    ) {
        val keys =
            listOf(
                listOf('1', '2', '3'),
                listOf('4', '5', '6'),
                listOf('7', '8', '9'),
                listOf(null, '0', '⌫'),
            )
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingLarge),
            ) {
                row.forEach { char ->
                    Box(modifier = Modifier.weight(1f)) {
                        if (char != null) {
                            LockKeyButton(
                                label = char.toString(),
                                onClick = {
                                    if (char == '⌫') {
                                        onBackspace()
                                    } else {
                                        onKeyPress(char)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LockKeyButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier =
            modifier
                .aspectRatio(1f)
                .fillMaxWidth(),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = AppDimensions.SpacingTwo,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = AppDimensions.TextSizeHuge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}
