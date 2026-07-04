package com.ssbmax.pdfparser.insights.gemma

import java.io.File
import java.io.FileOutputStream

actual fun gemmaModelStorageDir(): String {
    return try {
        val context =
            Class.forName("android.app.ActivityThread")
                .getMethod("currentApplication")
                .invoke(null) as? android.content.Context
        context?.filesDir?.absolutePath ?: ""
    } catch (e: Throwable) {
        ""
    }
}

actual fun fileExistsAt(path: String): Boolean = path.isNotEmpty() && File(path).exists()

actual fun moveFileAt(
    fromPath: String,
    toPath: String,
): Boolean {
    return try {
        val dest = File(toPath)
        dest.parentFile?.mkdirs()
        // File.renameTo is not guaranteed atomic across filesystems, but active and staging slots
        // live in the same directory here, so a plain rename is an atomic same-volume move.
        if (File(fromPath).renameTo(dest)) return true
        // Fallback for the rare cross-volume case: copy then delete the source.
        File(fromPath).copyTo(dest, overwrite = true)
        File(fromPath).delete()
        dest.exists()
    } catch (e: Throwable) {
        false
    }
}

actual fun deleteFileAt(path: String): Boolean {
    return try {
        val file = File(path)
        if (!file.exists()) true else file.delete()
    } catch (e: Throwable) {
        false
    }
}

actual fun readTextFileAt(path: String): String? {
    return try {
        val file = File(path)
        if (file.exists()) file.readText() else null
    } catch (e: Throwable) {
        null
    }
}

actual fun writeTextFileAt(
    path: String,
    content: String,
): Boolean {
    return try {
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeText(content)
        true
    } catch (e: Throwable) {
        false
    }
}

actual fun readFileInChunks(
    path: String,
    onChunk: (chunk: ByteArray, length: Int) -> Unit,
): Boolean {
    return try {
        val file = File(path)
        if (!file.exists()) return false
        file.inputStream().use { stream ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                if (read > 0) onChunk(buffer, read)
            }
        }
        true
    } catch (e: Throwable) {
        false
    }
}

actual class ModelFileSink actual constructor(path: String) : ModelSink {
    // Truncate on open: every downloadModel() call is a full, non-resumable download from byte
    // zero, so a stale partial file from a previous failed attempt must not be appended onto.
    private val stream: FileOutputStream =
        File(path).let { file ->
            file.parentFile?.mkdirs()
            FileOutputStream(file, false)
        }

    actual override fun append(bytes: ByteArray) {
        stream.write(bytes)
    }

    actual override fun close() {
        stream.close()
    }
}
