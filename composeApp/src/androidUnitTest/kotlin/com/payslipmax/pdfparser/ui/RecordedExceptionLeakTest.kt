package com.payslipmax.pdfparser.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P10, "From P10" item 5): Android's crash reporter forwards a recorded exception's message to Crashlytics, and a
 * library's message can quote a path, a file or a row. So the app records only events of its own with a fixed message. A source scan locks
 * it: every `recordException(` call in production passes one of those events, or is one of the three places that must forward a throwable
 * (the two platform reporters' own forwarding, and the uncaught-crash handler, which reports a crash as it is). A new call with a library
 * exception fails here, not in the field. Explicit roots, so a moved source fails loudly instead of making the scan vacuous.
 */
class RecordedExceptionLeakTest {
    private val root =
        run {
            var dir = File(System.getProperty("user.dir")!!).absoluteFile
            while (!File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile ?: error("repo root not found")
            dir
        }

    private val sources =
        listOf("shared", "composeApp")
            .flatMap { module -> listOf("commonMain", "androidMain", "iosMain").map { File(root, "$module/src/$it") } }
            .flatMap { dir -> dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList() }

    /** The first argument of every `recordException(` call (not its declaration) in [text]. */
    private fun firstArguments(text: String): List<String> =
        Regex("""(?<!fun )recordException\(""").findAll(text).map { m ->
            var depth = 0
            var i = m.range.last + 1
            val start = i
            while (i < text.length) {
                val c = text[i]
                if (c == '(') depth++
                if (c == ')') {
                    if (depth == 0) break
                    depth--
                }
                if (c == ',' && depth == 0) break
                i++
            }
            text.substring(start, i).trim()
        }.toList()

    private val ownEvents = listOf("PdfImportFailureEvent()", "GemmaInstallFailureEvent()", "BackupFailureEvent()", "IllegalStateException(\"guide_load_failed_")

    /** The places that forward a throwable on purpose, by file name and first argument. */
    private val forwarders =
        setOf(
            "CrashReporter.android.kt" to "throwable",
            "CrashReporter.ios.kt" to "throwable",
            "PayslipApplication.kt" to "throwable",
        )

    @Test
    fun theScanReadsTheRealSourcesAndFindsTheKnownCalls() {
        val all = sources.flatMap { f -> firstArguments(f.readText()).map { f.name to it } }
        assertTrue(sources.size > 300, "found only ${sources.size} sources")
        assertTrue(all.size >= 6, "the known recordException calls were not found: $all")
    }

    @Test
    fun everyRecordExceptionCallPassesAnEventOfOurOwnOrIsAKnownForwarder() {
        val offenders =
            sources.flatMap { f ->
                firstArguments(f.readText()).filter { arg -> ownEvents.none { arg.startsWith(it) } && (f.name to arg) !in forwarders }.map { "${f.relativeTo(root).path}: $it" }
            }
        assertEquals(emptyList(), offenders, "record a PdfImportFailureEvent-style event with a fixed message, never the exception itself")
    }
}
