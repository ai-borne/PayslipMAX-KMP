package com.payslipmax.pdfparser.insights.timeline

/** Regular-Army officer pay levels of the 7th CPC pay matrix, lowest rank first ([ordinal] is the rank order). */
enum class PayLevel(val label: String) {
    L10("10"),
    L10B("10B"),
    L11("11"),
    L12A("12A"),
    L13("13"),
    L13A("13A"),
    L14("14"),
    L15("15"),
    L16("16"),
    L17("17"),
    L18("18"),
}

/**
 * SSOT for the 7th CPC pay matrix (Handbook of Pay and Allowances 2023, pp. 88-93) for regular Army
 * officers under PCDA(O). Ported from `scripts/pcdao_factory/output/pay_matrix_7th_cpc.json`, which stays
 * the authoring reference; `PayMatrixTest` fails if the two drift apart. The matrix changes once per
 * Pay Commission, so it is data, not logic. Stage 1 is the first cell of a level.
 */
object PayMatrix {
    private val cells: Map<PayLevel, IntArray> =
        mapOf(
            PayLevel.L10 to
                intArrayOf(
                    56100, 57800, 59500, 61300, 63100, 65000, 67000, 69000, 71100, 73200,
                    75400, 77700, 80000, 82400, 84900, 87400, 90000, 92700, 95500, 98400,
                    101400, 104400, 107500, 110700, 114000, 117400, 120900, 124500, 128200, 132000,
                    136000, 140100, 144300, 148600, 153100, 157700, 162400, 167300, 172300, 177500,
                ),
            PayLevel.L10B to
                intArrayOf(
                    61300, 63100, 65000, 67000, 69000, 71100, 73200, 75400, 77700, 80000,
                    82400, 84900, 87400, 90000, 92700, 95500, 98400, 101400, 104400, 107500,
                    110700, 114000, 117400, 120900, 124500, 128200, 132000, 136000, 140100, 144300,
                    148600, 153100, 157700, 162400, 167300, 172300, 177500, 182800, 188300, 193900,
                ),
            PayLevel.L11 to
                intArrayOf(
                    69400, 71500, 73600, 75800, 78100, 80400, 82800, 85300, 87900, 90500,
                    93200, 96000, 98900, 101900, 105000, 108200, 111400, 114700, 118100, 121600,
                    125200, 129000, 132900, 136900, 141000, 145200, 149600, 154100, 158700, 163500,
                    168400, 173500, 178700, 184100, 189600, 195300, 201200, 207200,
                ),
            PayLevel.L12A to
                intArrayOf(
                    121200, 124800, 128500, 132400, 136400, 140500, 144700, 149000, 153500, 158100,
                    162800, 167700, 172700, 177900, 183200, 188700, 194400, 200200, 206200, 212400,
                ),
            PayLevel.L13 to
                intArrayOf(
                    130600, 134500, 138500, 142700, 147000, 151400, 155900, 160600, 165400, 170400,
                    175500, 180800, 186200, 191800, 197600, 203500, 209600, 215900,
                ),
            PayLevel.L13A to
                intArrayOf(
                    139600, 143800, 148100, 152500, 157100, 161800, 166700, 171700, 176900, 182200,
                    187700, 193300, 199100, 205100, 211300, 217600,
                ),
            PayLevel.L14 to
                intArrayOf(
                    144200, 148500, 153000, 157600, 162300, 167200, 172200, 177400, 182700, 188200,
                    193800, 199600, 205600, 211800, 218200,
                ),
            PayLevel.L15 to
                intArrayOf(
                    182200, 187700, 193300, 199100, 205100, 211300, 217600, 224100,
                ),
            PayLevel.L16 to
                intArrayOf(
                    205400, 211600, 217900, 224400,
                ),
            PayLevel.L17 to
                intArrayOf(
                    225000,
                ),
            PayLevel.L18 to
                intArrayOf(
                    250000,
                ),
        )

    /** Levels whose pay column contains [basicPay], lowest rank first. Empty when it is not a matrix cell. */
    fun levelsContaining(basicPay: Double): List<PayLevel> = PayLevel.entries.filter { stageOf(it, basicPay) != null }

    /** 1-based stage of [basicPay] within [level], or null when the level has no such cell. */
    fun stageOf(
        level: PayLevel,
        basicPay: Double,
    ): Int? {
        val index = cells.getValue(level).indexOf(basicPay.toInt())
        return if (index >= 0 && basicPay == basicPay.toInt().toDouble()) index + 1 else null
    }

    /** The pay in [level] at 1-based [stage], or null when the level has fewer stages. */
    fun payAt(
        level: PayLevel,
        stage: Int,
    ): Int? = cells.getValue(level).getOrNull(stage - 1)

    internal fun levelCells(level: PayLevel): List<Int> = cells.getValue(level).toList()
}
