package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.guide.domain.GuideProfile
import com.payslipmax.pdfparser.guide.domain.GuideProfileBuilder
import com.payslipmax.pdfparser.guide.domain.GuideProfileProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * The profile of the officer's stored payslips ([history] is the repository's payslip flow, taken lazily so nothing is
 * opened until a screen subscribes). Follows the history, so an imported payslip updates an open card. Keeps nothing itself.
 */
class PayslipGuideProfileProvider(
    private val history: () -> Flow<List<ParsedPayslip>>,
) : GuideProfileProvider {
    override fun profile(): Flow<GuideProfile?> = flow { emitAll(history().map(GuideProfileBuilder::from)) }
}
