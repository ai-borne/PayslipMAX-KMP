@file:OptIn(ExperimentalTestApi::class)

package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Modifier
import java.util.IdentityHashMap
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P10, the field spike): a `TextFieldState` keeps what was typed in its buffer, in its current value and in its undo
 * history. Deleting the text leaves it in the history (and `undo` brings it back), so [TypedPassword.wipe] deletes and then clears the
 * history. Nothing public shows this, so the test walks the state's object graph with reflection and looks for the password's
 * text. It first proves the walk can see the password before wiping (otherwise a Compose upgrade that hides its internals would make the
 * test pass for the wrong reason). The `String`s Compose made of each keystroke are unreachable garbage after the wipe and cannot be zeroed:
 * this test shows nothing still points at them, not that they are gone from memory.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TypedPasswordReachabilityTest {
    @AfterTest
    fun tearDown() {
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    /** Where under [root] a string, builder or char array that contains [needle] can still be reached (Compose classes and collections only). */
    private fun holders(
        root: Any,
        needle: String,
    ): List<String> {
        val seen = IdentityHashMap<Any, Boolean>()
        val hits = mutableListOf<String>()

        fun walk(
            o: Any?,
            path: String,
            depth: Int,
        ) {
            if (o == null || depth > 9 || seen.put(o, true) != null) return
            when (o) {
                is CharArray -> if (String(o).contains(needle)) hits += "$path [char[]]"
                is String -> if (o.contains(needle)) hits += "$path [String]"
                is StringBuilder -> if (o.toString().contains(needle)) hits += "$path [StringBuilder]"
                is Array<*> -> o.forEachIndexed { i, e -> walk(e, "$path[$i]", depth + 1) }
                is Collection<*> -> o.forEachIndexed { i, e -> walk(e, "$path[$i]", depth + 1) }
                is Map<*, *> -> o.entries.forEachIndexed { i, e -> walk(e.value, "$path{$i}", depth + 1) }
                else -> {
                    val name = o.javaClass.name
                    if (!(name.startsWith("androidx.compose") || name.startsWith("kotlin.collections") || name.startsWith("kotlinx"))) return
                    var c: Class<*>? = o.javaClass
                    while (c != null && c != Any::class.java) {
                        for (f in c.declaredFields) {
                            if (Modifier.isStatic(f.modifiers) || f.type.isPrimitive) continue
                            try {
                                f.isAccessible = true
                                walk(f.get(o), "$path.${f.name}", depth + 1)
                            } catch (_: Throwable) {
                            }
                        }
                        c = c.superclass
                    }
                }
            }
        }
        walk(root, "state", 0)
        return hits
    }

    @Test
    fun afterTheWipeNothingReachableFromTheFieldsStateHoldsThePasswordsText() =
        runComposeUiTest {
            val typed = TypedPassword()
            setContent { BackupRestorePasswordField(password = typed.field) }
            onNodeWithTag(BackupRestorePasswordFieldTestTag).performTextInput("hunterTwoSecret")
            waitForIdle()
            assertTrue(holders(typed.field, "hunterTwoSecret").isNotEmpty(), "the setup: Compose keeps the text, so the walk has something to find")

            typed.wipe()
            waitForIdle()

            assertEquals(emptyList(), holders(typed.field, "hunterTwoSecret"), "deleting alone leaves it in the undo history; the wipe deletes and clears it")
        }
}
