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
