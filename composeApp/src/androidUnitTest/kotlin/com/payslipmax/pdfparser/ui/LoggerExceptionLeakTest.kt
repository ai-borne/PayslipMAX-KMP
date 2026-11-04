package com.payslipmax.pdfparser.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P10, "From P6" item 7): `Logger.e(tag, message, throwable)` printed the exception's message and sent the exception to
 * Crashlytics, and an exception's message can quote a path, a file or a row. P6 fixed the sites in the backup code; P10 removed the
 * exception parameter, so no caller in the app can send one. A source scan locks it: the parameter is gone, and no call passes a third
 * argument, so someone adding it back (or a call built on another logger) fails here instead of in the field. Explicit file roots, so a
 * moved source fails loudly instead of making the scan vacuous.
 */
class LoggerExceptionLeakTest {
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

    /** The top-level argument count of every `Logger.e(...)` call in [text]. */
    private fun argumentCounts(text: String): List<Int> =
        Regex("""Logger\.e\(""").findAll(text).map { match ->
            var depth = 1
            var args = 1
            var i = match.range.last + 1
            var inString = false
            while (i < text.length && depth > 0) {
                val c = text[i]
                when {
                    inString ->
                        if (c == '\\') {
                            i++
                        } else if (c == '"') {
                            inString = false
                        }
                    c == '"' -> inString = true
                    c == '(' -> depth++
                    c == ')' -> depth--
                    c == ',' && depth == 1 -> args++
                }
                i++
            }
            args
        }.toList()

    @Test
    fun theScanReadsTheRealSourcesSoItIsNotVacuous() {
        assertTrue(sources.size > 300, "found only ${sources.size} sources")
        assertTrue(sources.sumOf { argumentCounts(it.readText()).size } >= 3, "the app still logs errors with Logger.e")
    }

    @Test
    fun noLoggerECallPassesAnException() {
        val offenders = sources.filter { file -> argumentCounts(file.readText()).any { it > 2 && !file.readText().contains("Logger.e(\"\"\"") } }
        assertEquals(emptyList(), offenders.map { it.relativeTo(root).path }, "Logger.e takes a tag and a fixed message only")
    }

    @Test
    fun loggerEHasNoExceptionParameterSoACallerCannotEvenTry() {
        val logger = File(root, "shared/src/commonMain/kotlin/com/payslipmax/pdfparser/logging/Logger.kt").readText()
        val declaration = logger.substringAfter("fun e(").substringBefore(")")

        assertTrue("Throwable" !in declaration, "Logger.e must not accept an exception")
        assertTrue("recordException" !in logger, "Logger sends no exception to Crashlytics")
    }
}
