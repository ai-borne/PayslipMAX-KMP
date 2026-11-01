package com.payslipmax.pdfparser.part2.pages

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.io.ByteArrayOutputStream
import kotlin.random.Random

/** Real JPEG and PNG bytes for the encoder tests, made with the platform codec under Robolectric's native graphics. */
object PageBitmaps {
    /** Left half red, right half blue: a rotation or flip shows in where the colours end up. */
    fun twoColour(
        width: Int,
        height: Int,
    ): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = Canvas(bitmap)
            canvas.drawRect(0f, 0f, width / 2f, height.toFloat(), Paint().apply { color = Color.RED })
            canvas.drawRect(width / 2f, 0f, width.toFloat(), height.toFloat(), Paint().apply { color = Color.BLUE })
        }

    /** Pixels with [amplitude] of random noise on a gradient: larger amplitude, larger JPEG. */
    fun noisy(
        width: Int,
        height: Int,
        amplitude: Int,
    ): Bitmap {
        val random = Random(7)
        val pixels = IntArray(width * height) { i -> Color.rgb(((i % width) * 255 / width + random.nextInt(amplitude + 1)).coerceAtMost(255), random.nextInt(amplitude + 1), (i / width) * 255 / height) }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    fun jpeg(
        bitmap: Bitmap,
        quality: Int = 90,
    ): ByteArray = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }.toByteArray()

    fun png(bitmap: Bitmap): ByteArray = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()

    fun decode(bytes: ByteArray): Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}
