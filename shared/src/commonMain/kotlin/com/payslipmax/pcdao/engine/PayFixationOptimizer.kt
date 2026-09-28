package com.payslipmax.pcdao.engine

import com.payslipmax.pcdao.model.PayMatrixData

class PayFixationOptimizer(private val payMatrix: PayMatrixData) {
    fun optimizePromotion(request: PayFixationRequest): PayFixationResult {
        val lFrom =
            payMatrix.regularOfficersPayMatrix[request.fromLevel]
                ?: error("Source pay level '${request.fromLevel}' not found in regular officers pay matrix")
        val lTo =
            payMatrix.regularOfficersPayMatrix[request.toLevel]
                ?: error("Target promotional pay level '${request.toLevel}' not found in pay matrix")

        val fromStageIdx = (request.fromStage - 1).coerceIn(0, lFrom.size - 1)
        val fromBasicPay = lFrom[fromStageIdx]
        val (pYear, pMonth, pDay) = parseDate(request.promotionDate)
        val mspAddition = if (request.toLevel == "14") request.mspMonthly else 0

        val fix = calculateOptionFixations(lFrom, lTo, fromStageIdx, fromBasicPay, mspAddition)
        val months = generate36MonthPeriods(pYear, pMonth)
        val opt1Monthly = computeOption1Trajectory(lTo, fix.opt1CellIdx, pYear, pMonth, months)
        val opt2Monthly = computeOption2Trajectory(lTo, fix.opt2PreDniPay, fix.opt2CellIdx, pYear, pMonth, request.dniMonth, months)
        val trajectory = buildTrajectory(months, opt1Monthly, opt2Monthly)

        val opt1Total = opt1Monthly.fold(0L) { acc, v -> acc + v }
        val opt2Total = opt2Monthly.fold(0L) { acc, v -> acc + v }
        val delta = opt2Total - opt1Total
        val recommended = if (delta > 0) FixationOption.OPTION_2 else FixationOption.OPTION_1

        return PayFixationResult(
            fromLevel = request.fromLevel,
            fromStage = request.fromStage,
            fromBasicPay = fromBasicPay,
            toLevel = request.toLevel,
            promotionDate = request.promotionDate,
            dniMonth = request.dniMonth,
            opt1FixedPay = fix.opt1FixedPay,
            opt1InitialStage = fix.opt1CellIdx + 1,
            opt2PreDniPay = fix.opt2PreDniPay,
            opt2PostDniFixedPay = fix.opt2PostDniFixedPay,
            opt2DniStage = fix.opt2CellIdx + 1,
            opt1Total36Months = opt1Total,
            opt2Total36Months = opt2Total,
            cumulativeDelta = delta,
            recommendedOption = recommended,
            recommendationSummary = buildSummary(recommended, delta),
            statutoryElectionDeadline = calculateElectionDeadline(pYear, pMonth, pDay),
            statutoryWarning = STATUTORY_WARNING,
            monthlyTrajectory = trajectory,
        )
    }

    private fun calculateOptionFixations(
        lFrom: List<Int>,
        lTo: List<Int>,
        fromStageIdx: Int,
        fromBasicPay: Int,
        mspAddition: Int,
    ): InitialFixation {
        val incrInLower = lFrom[(fromStageIdx + 1).coerceAtMost(lFrom.size - 1)] + mspAddition
        val opt1FixedPay = findCellInLevel(lTo, incrInLower)
        val opt1CellIdx = lTo.indexOf(opt1FixedPay).coerceAtLeast(0)

        val opt2PreDniPay = findCellInLevel(lTo, fromBasicPay + mspAddition)
        val dniStageIdx = (fromStageIdx + 2).coerceAtMost(lFrom.size - 1)
        val twoIncrsInLower = lFrom[dniStageIdx] + mspAddition
        val opt2PostDniFixedPay = findCellInLevel(lTo, twoIncrsInLower)
        val opt2CellIdx = lTo.indexOf(opt2PostDniFixedPay).coerceAtLeast(0)

        return InitialFixation(
            opt1FixedPay = opt1FixedPay,
            opt1CellIdx = opt1CellIdx,
            opt2PreDniPay = opt2PreDniPay,
            opt2PostDniFixedPay = opt2PostDniFixedPay,
            opt2CellIdx = opt2CellIdx,
        )
    }

    private data class InitialFixation(
        val opt1FixedPay: Int,
        val opt1CellIdx: Int,
        val opt2PreDniPay: Int,
        val opt2PostDniFixedPay: Int,
        val opt2CellIdx: Int,
    )

    private fun findCellInLevel(
        levelStages: List<Int>,
        targetAmount: Int,
    ): Int {
        for (cell in levelStages) {
            if (cell >= targetAmount) return cell
        }
        return levelStages.last()
    }

    private fun computeOption1Trajectory(
        lTo: List<Int>,
        opt1CellIdx: Int,
        pYear: Int,
        pMonth: Int,
        months: List<Pair<Int, Int>>,
    ): List<Int> {
        val firstIncrYear = pYear + 1
        val firstIncrMonth = if (pMonth <= 6) 1 else 7
        val firstIncrMonthsFromEpoch = firstIncrYear * 12 + firstIncrMonth

        return months.map { (y, m) ->
            val curMonthsFromEpoch = y * 12 + m
            if (curMonthsFromEpoch >= firstIncrMonthsFromEpoch) {
                val monthsSinceFirst = curMonthsFromEpoch - firstIncrMonthsFromEpoch
                val steps = 1 + (monthsSinceFirst / 12)
                val cIdx = (opt1CellIdx + steps).coerceAtMost(lTo.size - 1)
                lTo[cIdx]
            } else {
                lTo[opt1CellIdx]
            }
        }
    }

    private fun computeOption2Trajectory(
        lTo: List<Int>,
        opt2PreDniPay: Int,
        opt2CellIdx: Int,
        pYear: Int,
        pMonth: Int,
        dniMonth: Int,
        months: List<Pair<Int, Int>>,
    ): List<Int> {
        val dniYear = if (dniMonth >= pMonth) pYear else pYear + 1
        val dniMonthsFromEpoch = dniYear * 12 + dniMonth
        val firstPostDniIncrMonthsFromEpoch =
            if (dniMonth == 7) {
                (dniYear + 1) * 12 + 1
            } else {
                dniYear * 12 + 7
            }

        return months.map { (y, m) ->
            val cur = y * 12 + m
            when {
                cur < dniMonthsFromEpoch -> opt2PreDniPay
                cur < firstPostDniIncrMonthsFromEpoch -> lTo[opt2CellIdx]
                else -> {
                    val monthsSinceFirst = cur - firstPostDniIncrMonthsFromEpoch
                    val steps = 1 + (monthsSinceFirst / 12)
                    val cIdx = (opt2CellIdx + steps).coerceAtMost(lTo.size - 1)
                    lTo[cIdx]
                }
            }
        }
    }

    private fun generate36MonthPeriods(
        startYear: Int,
        startMonth: Int,
    ): List<Pair<Int, Int>> {
        val list = ArrayList<Pair<Int, Int>>(36)
        var y = startYear
        var m = startMonth
        repeat(36) {
            list.add(Pair(y, m))
            m++
            if (m > 12) {
                m = 1
                y++
            }
        }
        return list
    }

    private fun parseDate(dateStr: String): Triple<Int, Int, Int> {
        val parts = dateStr.split("-")
        require(parts.size == 3) { "Invalid ISO date format: $dateStr. Expected YYYY-MM-DD" }
        return Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
    }

    private fun formatMonthLabel(
        year: Int,
        month: Int,
    ): String {
        val names = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val mName = if (month in 1..12) names[month - 1] else month.toString()
        return "$mName $year"
    }

    private fun calculateElectionDeadline(
        year: Int,
        month: Int,
        day: Int,
    ): String {
        var dMonth = month + 1
        var dYear = year
        if (dMonth > 12) {
            dMonth = 1
            dYear++
        }
        val mStr = dMonth.toString().padStart(2, '0')
        val dStr = day.toString().padStart(2, '0')
        return "$dYear-$mStr-$dStr"
    }

    private fun buildSummary(
        option: FixationOption,
        delta: Long,
    ): String {
        val formattedDelta = if (delta >= 0) "+₹$delta" else "-₹${-delta}"
        return when (option) {
            FixationOption.OPTION_2 ->
                "Option 2 (Fixation from DNI) delivers a net financial gain of $formattedDelta over 36 months compared to Option 1."
            FixationOption.OPTION_1 ->
                "Option 1 (Fixation from Promotion Date) is financially superior by $formattedDelta over 36 months compared to Option 2."
        }
    }

    private fun buildTrajectory(
        months: List<Pair<Int, Int>>,
        opt1Monthly: List<Int>,
        opt2Monthly: List<Int>,
    ): List<MonthlyPayPoint> =
        months.indices.map { i ->
            val (y, m) = months[i]
            val p1 = opt1Monthly[i]
            val p2 = opt2Monthly[i]
            MonthlyPayPoint(
                monthIndex = i + 1,
                year = y,
                month = m,
                monthLabel = formatMonthLabel(y, m),
                option1BasicPay = p1,
                option2BasicPay = p2,
                delta = p2 - p1,
            )
        }

    companion object {
        const val STATUTORY_WARNING: String =
            "Under Rule 11 of Army Officers Pay Rules 2017 (SRO 12(E)), option must be exercised " +
                "within 1 month (30 days) from the date of publication of the promotion order. " +
                "Option once exercised shall be final and cannot be revised. Default: Option 1."
    }
}
