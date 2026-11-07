package com.payslipmax.pdfparser.guide

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Security and resource evidence for the printed orders index (plan 13, Phase 5), read from the sources because the property is "this
 * code does not exist": nothing about a casualty, an order or a date reaches a log, telemetry or an exception message; nothing goes
 * through a share sheet, the clipboard, the network or a file the app creates (the pages go to the system print dialog from memory); the
 * note and the Data columns are never read; the job name is one fixed string; and no print source holds a literal of copy or a colour.
 * The file lists are explicit so a moved source fails loudly instead of making a scan vacuous.
 */
class IndexPrintSecurityContractTest {
    private val sharedMain = File("../shared/src")
    private val print = "kotlin/com/payslipmax/pdfparser/part2/print"
    private val app = File("src")
    private val bank = "kotlin/com/payslipmax/pdfparser/ui/screens/guide/part2/bank"
    private val platform = "kotlin/com/payslipmax/pdfparser/ui/platform"

    private val commonPrint = listOf("IndexDocument.kt", "IndexLayout.kt", "IndexPrinter.kt")
    private val androidPrint = listOf("AndroidIndexPrinter.kt", "IndexPdf.kt", "IndexPrintAdapter.kt", "PrintJobWatch.kt")
    private val iosPrint = listOf("IosIndexPdf.kt", "IosIndexPrinter.kt")

    private fun withoutComments(file: File): String = file.readText().replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "").lines().joinToString("\n") { it.substringBefore("//") }

    private fun read(
        base: File,
        dir: String,
        names: List<String>,
    ) = names.associate { "$dir/$it" to withoutComments(File(base, "$dir/$it")) }

    private val printSources by lazy {
        read(File(sharedMain, "commonMain"), print, commonPrint) + read(File(sharedMain, "androidMain"), print, androidPrint) + read(File(sharedMain, "iosMain"), print, iosPrint)
    }
    private val appSources by lazy {
        read(File(app, "commonMain"), bank, listOf("IndexDocumentBuilder.kt", "IndexPrinting.kt")) +
            read(File(app, "androidMain"), platform, listOf("IndexPrinterProvider.kt")) + read(File(app, "iosMain"), platform, listOf("IndexPrinterProvider.kt")) +
            read(File(app, "commonMain"), platform, listOf("IndexPrinterProvider.kt"))
    }
    private val all by lazy { printSources + appSources }

    @Test
    fun theScansReadTheRealFilesSoTheyAreNotVacuous() {
        assertTrue(all.values.all { it.length > 100 }, "a print source is empty or missing: ${all.filterValues { it.length <= 100 }.keys}")
        for ((module, names) in listOf("commonMain" to commonPrint, "androidMain" to androidPrint, "iosMain" to iosPrint)) {
            val onDisk = File(sharedMain, "$module/$print").listFiles { f -> f.extension == "kt" }.orEmpty().map { it.name }.toSet()
            assertEquals(names.toSet(), onDisk, "a $module print source was added or moved; list it here so the scans cover it")
        }
    }

    @Test
    fun noPrintCodeLogsReportsOrBuildsAnExceptionMessageFromAValue() {
        val leaky = Regex("""Logger|println|(?<![.\w])(?<!fun )print\(|\blog\(|Log\.[dievw]\(|Napier|Timber|Firebase|Analytics|CrashReporter|crashReporter|TelemetrySanitizer|setCustomKey|recordException|printStackTrace|NSLog""")
        assertEquals(emptyList(), all.filterValues { leaky.containsMatchIn(it) }.keys.toList(), "a print file logs or reports")
        val templated = Regex("""(Exception|error|require|check|checkNotNull)\([^)]*\$""")
        assertEquals(emptyList(), all.filterValues { templated.containsMatchIn(it) }.keys.toList(), "an exception message interpolates a value")
    }

    @Test
    fun nothingGoesThroughAShareSheetTheClipboardTheNetworkOrAFileTheAppCreates() {
        val outward =
            Regex(
                """https?://|ktor|HttpClient|okhttp|URL\(|openURL|UriHandler|WebView|setPrimaryClip|ClipData|ClipboardManager|UIPasteboard|ACTION_SEND|createChooser|startActivity|android\.content\.Intent|\bIntent\(|FileProvider|UIActivityViewController|""" +
                    """File\(|FileOutputStream|createTempFile|cacheDir|filesDir|externalCacheDir|NSTemporaryDirectory|NSFileManager|writeToFile|writeToURL|MediaStore|GuidePlatform""",
            )
        assertEquals(emptyList(), all.filterValues { outward.containsMatchIn(it) }.keys.toList())
    }

    @Test
    fun eachPlatformPrintsOnlyThroughItsSystemPrintDialog() {
        assertTrue("PrintManager" in printSources.getValue("$print/AndroidIndexPrinter.kt") && "manager.print(jobName, adapter, attributes)" in printSources.getValue("$print/AndroidIndexPrinter.kt"))
        val ios = printSources.getValue("$print/IosIndexPrinter.kt")
        assertTrue("UIPrintInteractionController" in ios && "printingItem = pdf" in ios, "the PDF goes to the print sheet as data")
    }

    @Test
    fun theNoteAndTheDataColumnsAreNeverReadAndNothingIsStoredByAPrint() {
        val builder = appSources.getValue("$bank/IndexDocumentBuilder.kt")
        assertEquals(emptyList(), Regex("""\.note\b|\.data\b|\bnote\b|\bdata\b""").findAll(builder).map { it.value }.toList(), "the builder must not touch the private text")
        val printing = appSources.getValue("$bank/IndexPrinting.kt")
        assertEquals(emptyList(), Regex("""\.save\w*\(|\.delete\w*\(|\.add\w*\(|\.write\w*\(|lastBackup""").findAll(printing).map { it.value }.toList(), "a print writes nothing: no record of it is kept")
    }

    @Test
    fun theJobNameIsOneFixedStringWithNoValueInIt() {
        val printing = appSources.getValue("$bank/IndexPrinting.kt")
        assertTrue("printer.print(IndexStrings.jobName, document)" in printing)
        val strings = withoutComments(File(app, "commonMain/kotlin/com/payslipmax/pdfparser/ui/theme/IndexStrings.kt"))
        assertTrue(Regex("""const val jobName = "[^"$]*"""").containsMatchIn(strings), "the job name is a constant with no interpolation")
    }

    @Test
    fun noPrintSourceHoldsUserFacingCopyOrAColourOrASlowRegex() {
        val literal = Regex(""""((?:[^"\\]|\\.)*)"""")
        val interpolation = Regex("""\$\{[^}]*}|\$[A-Za-z_][A-Za-z0-9_]*""")
        val copy =
            all.mapValues { (_, source) ->
                source.lines().filterNot { it.trimStart().startsWith("import ") || it.trimStart().startsWith("package ") || "override fun toString" in it }
                    .flatMap { line -> literal.findAll(line).map { it.groupValues[1] }.toList() }
                    .filter { it != "{page}" && it != "{pages}" && interpolation.replace(it, "").any(Char::isLetter) }
            }.filterValues { it.isNotEmpty() }
        assertEquals(emptyMap(), copy, "copy belongs in IndexStrings and the Bank's strings (rule 4)")
        assertEquals(emptyList(), all.filterValues { Regex("""(?<![A-Za-z])Color\(|0x[0-9A-Fa-f]{6,8}|Color\.[A-Z]""").containsMatchIn(it) }.keys.toList(), "colours come from IndexLayoutSpec (rule 4)")
        assertEquals(emptyList(), printSources.filterValues { Regex("""Regex\(|\.toRegex\(|java\.util\.regex""").containsMatchIn(it) }.keys.toList(), "no regex on the Kotlin/Native print path")
    }
}
