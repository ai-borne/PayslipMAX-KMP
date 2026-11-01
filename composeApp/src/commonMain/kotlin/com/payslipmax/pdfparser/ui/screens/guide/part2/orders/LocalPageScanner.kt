package com.payslipmax.pdfparser.ui.screens.guide.part2.orders

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import com.payslipmax.pdfparser.part2.pages.PageScanner
import org.jetbrains.compose.resources.decodeToImageBitmap

/** A scanner a test (or a host with its own) provides in place of the platform's; null means the platform's own, built from the screen. */
internal val LocalPageScanner = compositionLocalOf<PageScanner?> { null }

/**
 * Turns a stored page's JPEG bytes into a picture. It must not keep [bytes]: the repository zeroes them the moment the read returns, so
 * the picture has to be built from a copy (the platform decoders copy). Null when the bytes are not an image.
 */
internal fun interface PageImageDecoder {
    fun decode(bytes: ByteArray): ImageBitmap?
}

/** The decoder a test provides in place of the platform's; null means the platform's own. */
internal val LocalPageImageDecoder = compositionLocalOf<PageImageDecoder?> { null }

/** The platform decoder: a stored page is a plain JPEG, so anything it cannot read is simply not shown. */
internal val PlatformPageImageDecoder =
    PageImageDecoder { bytes ->
        try {
            bytes.decodeToImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
