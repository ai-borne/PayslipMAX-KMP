package com.ssbmax.pdfparser.parser.debug

import com.ssbmax.pdfparser.parser.PositionedToken
import kotlin.math.abs

/** Stage 1 Runtime Token Diff Logger comparing real Android and iOS extraction dumps. */
object RuntimeTokenDiffLogger {
    fun logDiffAndStopAtFirstDivergence(
        androidTokens: List<PositionedToken>,
        iosTokens: List<PositionedToken>,
    ) {
        println("=== STAGE 1: TOKEN DIFF REPORT ===")
        val maxLen = maxOf(androidTokens.size, iosTokens.size)
        for (i in 0 until maxLen) {
            val a = androidTokens.getOrNull(i)
            val ios = iosTokens.getOrNull(i)
            if (a == null) {
                println("Android: TOKEN #$i missing\niOS: TOKEN #$i text=${ios?.text}")
                println("FIRST DIVERGENCE FOUND (Extra token on iOS)")
                return
            }
            if (ios == null) {
                println("Android: TOKEN #$i text=${a.text}\niOS: TOKEN #$i missing")
                println("FIRST DIVERGENCE FOUND (Missing token on iOS)")
                return
            }
            if (a.text != ios.text) {
                println("Android:\nTOKEN #$i\ntext=${a.text}\niOS:\nTOKEN #$i\ntext=${ios.text}")
                println("FIRST DIVERGENCE FOUND (Token text / split / merge mismatch)")
                return
            }
            if (abs(a.x - ios.x) > 1f || abs(a.y - ios.y) > 1f) {
                println("Android:\ntext=${a.text}\nx=${a.x}\ny=${a.y}\niOS:\ntext=${ios.text}\nx=${ios.x}\ny=${ios.y}")
                println("FIRST DIVERGENCE FOUND (Coordinate shift > 1pt)")
                return
            }
        }
        println("No Stage 1 divergence found between Android and iOS token lists.")
        println("===================================")
    }
}
