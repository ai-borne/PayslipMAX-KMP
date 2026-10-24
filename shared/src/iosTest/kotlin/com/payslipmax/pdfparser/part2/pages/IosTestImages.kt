package com.payslipmax.pdfparser.part2.pages

import com.payslipmax.pdfparser.crypto.toByteArray
import com.payslipmax.pdfparser.crypto.toNSData
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextFillRect
import platform.CoreGraphics.CGContextSetRGBFillColor
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import kotlin.random.Random

/** Real JPEG bytes and pixel reads for the iOS encoder tests, made with UIKit and CoreGraphics in the simulator. */
@OptIn(ExperimentalForeignApi::class)
internal object IosTestImages {
    /** Left half red, right half blue, with [noise] small random squares on top (larger [noise], larger JPEG). */
    fun jpeg(
        width: Int,
        height: Int,
        noise: Int = 0,
        quality: Double = 0.9,
    ): ByteArray {
        val format = UIGraphicsImageRendererFormat.defaultFormat().apply { scale = 1.0 }
        val renderer = UIGraphicsImageRenderer(size = CGSizeMake(width.toDouble(), height.toDouble()), format = format)
        val random = Random(7)
        val image =
            renderer.imageWithActions { context ->
                val cg = context!!.CGContext
                CGContextSetRGBFillColor(cg, 1.0, 0.0, 0.0, 1.0)
                CGContextFillRect(cg, CGRectMake(0.0, 0.0, width / 2.0, height.toDouble()))
                CGContextSetRGBFillColor(cg, 0.0, 0.0, 1.0, 1.0)
                CGContextFillRect(cg, CGRectMake(width / 2.0, 0.0, width / 2.0, height.toDouble()))
                repeat(noise) {
                    CGContextSetRGBFillColor(cg, random.nextDouble(), random.nextDouble(), random.nextDouble(), 1.0)
                    CGContextFillRect(cg, CGRectMake(random.nextInt(width).toDouble(), random.nextInt(height).toDouble(), 6.0, 6.0))
                }
            }
        return UIImageJPEGRepresentation(image, quality)!!.toByteArray()
    }

    /** The red, green and blue of the pixel at ([x], [y]) counted from the top left, as the decoder shows it. */
    fun rgbAt(
        jpeg: ByteArray,
        x: Int,
        y: Int,
    ): Triple<Int, Int, Int> {
        val cg = UIImage(data = jpeg.toNSData())!!.CGImage!!
        val width = CGImageGetWidth(cg).toInt()
        val height = CGImageGetHeight(cg).toInt()
        val buffer = ByteArray(width * height * 4)
        buffer.usePinned { pinned ->
            val context = CGBitmapContextCreate(pinned.addressOf(0), width.toULong(), height.toULong(), 8u, (width * 4).toULong(), CGColorSpaceCreateDeviceRGB(), CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value)
            CGContextDrawImage(context, CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()), cg)
        }
        val at = (y * width + x) * 4
        return Triple(buffer[at].toInt() and 0xFF, buffer[at + 1].toInt() and 0xFF, buffer[at + 2].toInt() and 0xFF)
    }

    fun isRed(rgb: Triple<Int, Int, Int>) = rgb.first > 180 && rgb.third < 80

    fun isBlue(rgb: Triple<Int, Int, Int>) = rgb.third > 180 && rgb.first < 80
}
