package com.payslipmax.pdfparser.guide.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Where a Guide screen gets the officer's [GuideProfile]. The flow is cold: nothing is read from the stored payslips until
 * a screen collects it, and the profile is held only while it does. [None] is the default for tests and for screens that
 * have no payslips.
 */
fun interface GuideProfileProvider {
    fun profile(): Flow<GuideProfile?>

    companion object {
        val None = GuideProfileProvider { flowOf(null) }
    }
}
