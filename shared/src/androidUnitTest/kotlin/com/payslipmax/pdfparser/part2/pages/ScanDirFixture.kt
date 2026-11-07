package com.payslipmax.pdfparser.part2.pages

import org.junit.After
import org.junit.Before
import java.io.File
import java.nio.file.Files

/** What the [AndroidPageScanner] tests share: a temporary app cache with the scanner's folder, and helpers that write files as the scanner does. */
abstract class ScanDirFixture {
    protected lateinit var root: File
    protected lateinit var scanDir: File

    @Before
    fun setUp() {
        root = Files.createTempDirectory("MARKER_cache").toFile()
        scanDir = File(root, "mlkit_docscan_ui_client")
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    protected fun scanned(
        name: String,
        vararg bytes: Byte,
    ): File = File(scanDir.also { it.mkdirs() }, name).also { it.writeBytes(bytes) }

    /** A source that writes [files] when the scan runs (after the scanner's own sweep, as the real scanner does) and reports them. */
    protected fun writing(vararg files: Pair<String, ByteArray>) = ScanSource { SourceResult.Files(files.map { (name, bytes) -> scanned(name, *bytes).path }) }

    protected fun sized(
        name: String,
        length: Long,
    ) = ScanSource { SourceResult.Files(listOf(scanned(name).also { java.io.RandomAccessFile(it, "rw").use { f -> f.setLength(length) } }.path)) }

    protected fun file(name: String) = File(scanDir, name)

    protected fun scanner(source: ScanSource) = AndroidPageScanner(scanDir, source)

    protected fun left(): List<String> = scanDir.listFiles().orEmpty().map { it.name }.sorted()
}
