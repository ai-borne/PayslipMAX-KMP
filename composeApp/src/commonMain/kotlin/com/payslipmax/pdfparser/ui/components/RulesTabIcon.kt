package com.payslipmax.pdfparser.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The Rules tab's book. Drawn here because the app links only the core Material icons, which have no book and
 * whose extended set is far too large to ship for one glyph (this is the Material "Book" outline, 24dp viewport).
 */
internal val RulesTabIcon: ImageVector by lazy {
    ImageVector
        .Builder(name = "RulesTabBook", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .addPath(
            pathData = addPathNodes("M18,2H6C4.9,2 4,2.9 4,4v16c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V4C20,2.9 19.1,2 18,2zM6,4h5v8l-2.5,-1.5L6,12V4z"),
            fill = SolidColor(Color.Black),
        ).build()
}
