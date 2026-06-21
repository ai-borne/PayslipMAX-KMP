package com.ssbmax.pdfparser.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals

class InsightsHealthStatusTest {
    @Test
    fun testScore100IsExcellent() {
        assertEquals(HealthStatus.EXCELLENT, statusFor(100))
    }

    @Test
    fun testScore90IsExcellent() {
        assertEquals(HealthStatus.EXCELLENT, statusFor(90))
    }

    @Test
    fun testScore89IsHealthy() {
        assertEquals(HealthStatus.HEALTHY, statusFor(89))
    }

    @Test
    fun testScore75IsHealthy() {
        assertEquals(HealthStatus.HEALTHY, statusFor(75))
    }

    @Test
    fun testScore74IsFair() {
        assertEquals(HealthStatus.FAIR, statusFor(74))
    }

    @Test
    fun testScore60IsFair() {
        assertEquals(HealthStatus.FAIR, statusFor(60))
    }

    @Test
    fun testScore59IsNeedsAttention() {
        assertEquals(HealthStatus.NEEDS_ATTENTION, statusFor(59))
    }

    @Test
    fun testScore40IsNeedsAttention() {
        assertEquals(HealthStatus.NEEDS_ATTENTION, statusFor(40))
    }

    @Test
    fun testScore39IsCritical() {
        assertEquals(HealthStatus.CRITICAL, statusFor(39))
    }

    @Test
    fun testScore0IsCritical() {
        assertEquals(HealthStatus.CRITICAL, statusFor(0))
    }
}
