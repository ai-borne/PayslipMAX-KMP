package com.payslipmax.pdfparser.ui.components

import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * WHY: on the Pixel the keyboard covered the field the officer was typing in (the Year of a new casualty): it overlays the window, nothing
 * made room for it, and the field stayed where it was. With a form scrolled to its foot and a keyboard 300 dp tall coming up, the focused
 * field must end up above the keyboard, not behind it. And when the keyboard is already up and Next moves focus to a field below the
 * visible area (the Pixel walk, 2026-11-02), that field must be scrolled up too: Compose does not do it by itself.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h640dp")
class KeyboardAwareTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun showKeyboard(heightPx: Int) {
        composeRule.runOnUiThread {
            val content = composeRule.activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
            val insets = WindowInsetsCompat.Builder().setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, heightPx)).setVisible(WindowInsetsCompat.Type.ime(), true).build()
            ViewCompat.dispatchApplyWindowInsets(content as View, insets)
        }
        composeRule.waitForIdle()
    }

    private fun form() {
        composeRule.setContent {
            Box(Modifier.fillMaxSize()) {
                LazyColumn(Modifier.fillMaxSize().aboveKeyboard().testTag("list")) {
                    items(10, key = { it }) { i ->
                        OutlinedTextField(
                            value = "",
                            onValueChange = {},
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            label = { Text("Field $i") },
                            modifier = Modifier.height(80.dp).testTag("field-$i").scrollToWhenFocused(),
                        )
                    }
                }
            }
        }
    }

    private fun assertAboveKeyboardAfterItComesUp(
        tag: String,
        scrollToIndex: Int? = null,
    ) {
        scrollToIndex?.let { composeRule.onNodeWithTag("list").performScrollToIndex(it) }
        composeRule.onNodeWithTag(tag).requestFocus()
        composeRule.waitForIdle()
        val density = composeRule.activity.resources.displayMetrics.density

        showKeyboard((300 * density).toInt())

        composeRule.onNodeWithTag(tag).assertIsDisplayed()
        val screen = 640.dp
        val bottom = composeRule.onNodeWithTag(tag).getUnclippedBoundsInRoot().bottom
        assertTrue(bottom <= screen - 300.dp, "the field's foot ($bottom) is above the keyboard's top (${screen - 300.dp})")
    }

    @Test
    fun aFocusedFieldAtTheFootOfALongFormEndsAboveTheKeyboard() {
        form()
        assertAboveKeyboardAfterItComesUp("field-9", scrollToIndex = 9)
    }

    @Test
    fun aFieldBelowTheVisibleAreaIsScrolledIntoViewWhenNextMovesFocusToItWithTheKeyboardAlreadyUp() {
        form()
        val density = composeRule.activity.resources.displayMetrics.density
        showKeyboard((300 * density).toInt())

        composeRule.onNodeWithTag("field-3").requestFocus()
        composeRule.waitForIdle()
        repeat(3) {
            composeRule.onNode(androidx.compose.ui.test.isFocused()).performImeAction()
            composeRule.waitForIdle()
        }

        composeRule.onNodeWithTag("field-6").assertIsDisplayed()
        val bottom = composeRule.onNodeWithTag("field-6").getUnclippedBoundsInRoot().bottom
        assertTrue(bottom <= 640.dp - 300.dp, "the field's foot ($bottom) is above the keyboard's top")
    }

    @Test
    fun aFocusedFieldLowOnTheScreenIsScrolledUpWhenTheKeyboardWouldHideIt() {
        form()
        assertAboveKeyboardAfterItComesUp("field-6")
    }
}
