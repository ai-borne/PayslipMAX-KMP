package com.payslipmax.pdfparser.rating

import kotlin.test.Test

class ReviewRequesterIosTest {
    @Test
    fun requestReviewDoesNotThrowOnSimulatorTestHost() {
        requestReview()
    }
}
