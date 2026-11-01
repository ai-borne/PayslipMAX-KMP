package com.payslipmax.pdfparser.part2.pages

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.testing.JpegProbe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * WHY: real use is hundreds of orders, so the size of one stored page decides how long the vault lasts and how big a backup gets. This measures
 * the real encoder on a synthetic yellowed text page (ruled lines of dark marks on a cream ground, photographed large, like a scanner's
 * cleaned output) against [Part2Limits.PAGE_BUDGET_BYTES], and ties the budget to the vault arithmetic. A synthetic page compresses better than
 * paper, so this is a floor to watch, not the final figure: the owner's own scan sets that.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PageSizeBudgetTest {
    private fun textPage(
        width: Int,
        height: Int,
    ): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.rgb(250, 240, 200))
            val ink = Paint().apply { color = Color.rgb(30, 30, 30) }
            var y = height / 20
            while (y < height - height / 20) {
                var x = width / 12
                while (x < width - width / 12) {
                    canvas.drawRect(x.toFloat(), y.toFloat(), (x + width / 90).toFloat(), (y + height / 70).toFloat(), ink)
                    x += width / 70
                }
                y += height / 45
            }
        }

    @Test
    fun aFullTextPageStaysUnderTheBudgetAtTheStoredResolution() {
        val stored = assertNotNull(AndroidPageEncoder().encode(PageBitmaps.jpeg(textPage(3000, 4000))))

        assertEquals(1600 to 1200, JpegProbe.size(stored)?.let { maxOf(it.first, it.second) to minOf(it.first, it.second) })
        assertTrue(stored.size <= Part2Limits.PAGE_BUDGET_BYTES, "a text page weighs ${stored.size} bytes, budget ${Part2Limits.PAGE_BUDGET_BYTES}")
    }

    @Test
    fun theBudgetSitsInsideTheRefusalLimitAndTheVaultArithmeticFollowsFromIt() {
        assertTrue(Part2Limits.PAGE_BUDGET_BYTES < Part2Limits.PAGE_MAX_BYTES, "the budget is a target; the maximum is what refuses")
        assertEquals(409L, Part2Limits.VAULT_BYTES_MAX / Part2Limits.PAGE_BUDGET_BYTES, "the cap holds about 409 budget-sized pages")
    }
}
