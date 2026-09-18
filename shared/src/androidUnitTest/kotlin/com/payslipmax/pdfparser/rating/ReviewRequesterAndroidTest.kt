package com.payslipmax.pdfparser.rating

import com.payslipmax.pdfparser.crypto.ContextHolder
import org.junit.After
import org.junit.Test

class ReviewRequesterAndroidTest {
    @After
    fun tearDown() {
        ContextHolder.context = null
        ReviewActivityBridge.activityProvider = null
    }

    @Test
    fun requestReviewDoesNotThrowWhenContextAndActivityAreUnset() {
        ContextHolder.context = null
        ReviewActivityBridge.activityProvider = null

        requestReview()
    }
}
