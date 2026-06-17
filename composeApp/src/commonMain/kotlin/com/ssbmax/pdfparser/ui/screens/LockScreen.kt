package com.ssbmax.pdfparser.ui.screens

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
import androidx.compose.ui.unit.sp
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun LockScreen(
    onUnlock: (String) -> Boolean,
    modifier: Modifier = Modifier,
) {
    var enteredPin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

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

        PinIndicatorDots(pinLength = enteredPin.length)

        LockKeyboard(
            onKeyPress = { char ->
                if (enteredPin.length < 4) {
                    showError = false
                    enteredPin += char
                    if (enteredPin.length == 4) {
                        val unlocked = onUnlock(enteredPin)
                        if (!unlocked) {
                            showError = true
                            enteredPin = ""
                        }
                    }
                }
            },
            onBackspace = {
                if (enteredPin.isNotEmpty()) {
                    showError = false
                    enteredPin = enteredPin.dropLast(1)
                }
            },
        )
    }
}

@Composable
private fun LockHeader(
    showError: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("🛡️", fontSize = 64.sp)
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
            modifier = Modifier.padding(horizontal = 24.dp),
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
                        .padding(8.dp)
                        .size(16.dp)
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
        modifier = modifier.width(280.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
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
                horizontalArrangement = Arrangement.spacedBy(16.dp),
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
        tonalElevation = 2.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}
