package com.payslipmax.pcdao.redressal

internal object RedressalRankInferer {
    private const val DEFAULT_FALLBACK_RANK = "Serving Officer"

    fun inferRank(
        level: String?,
        basicPay: Double = 0.0,
    ): String {
        val normalized = level?.trim()?.uppercase()
        val effectiveLevel =
            if (normalized.isNullOrBlank() && basicPay > 0.0) {
                inferLevelFromBasicPay(basicPay)
            } else {
                normalized
            }

        return when (effectiveLevel) {
            "10" -> if (basicPay > 0.0 && basicPay < 61300.0) "Lieutenant" else "Captain"
            "10B" -> "Captain"
            "11" -> "Major"
            "12", "12A" -> "Lt Colonel"
            "13" -> "Colonel"
            "13A" -> "Brigadier"
            "14" -> "Major General"
            else -> DEFAULT_FALLBACK_RANK
        }
    }

    fun inferLevelFromBasicPay(basicPay: Double): String? {
        val payInt = basicPay.toInt()
        val level12ACells = setOf(121200, 124800, 128500, 132400, 136400, 140400, 140500, 144700, 149000, 153500)
        val level11Cells = setOf(69400, 71500, 73600, 75800, 78100, 80400, 82800, 85300, 87900, 90500)
        val level10Cells = setOf(56100, 57800, 59500, 61300, 63100, 65000, 67000, 69000, 71100, 73200)

        return when {
            payInt in level12ACells -> "12A"
            payInt in level11Cells -> "11"
            payInt in level10Cells -> "10"
            payInt in 56100..69000 -> "10"
            payInt in 69400..90500 -> "11"
            payInt in 121200..167700 -> "12A"
            payInt in 167800..215900 -> "13"
            payInt >= 216000 -> "14"
            else -> null
        }
    }
}
