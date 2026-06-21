package com.ssbmax.pdfparser.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals

class InsightsHealthGradeTest {
    @Test
    fun testScore100IsExcellent() {
        assertEquals(HealthGrade.EXCELLENT, gradeFor(100))
    }

    @Test
    fun testScore90IsExcellent() {
        assertEquals(HealthGrade.EXCELLENT, gradeFor(90))
    }

    @Test
    fun testScore89IsGood() {
        assertEquals(HealthGrade.GOOD, gradeFor(89))
    }

    @Test
    fun testScore75IsGood() {
        assertEquals(HealthGrade.GOOD, gradeFor(75))
    }

    @Test
    fun testScore74IsFair() {
        assertEquals(HealthGrade.FAIR, gradeFor(74))
    }

    @Test
    fun testScore60IsFair() {
        assertEquals(HealthGrade.FAIR, gradeFor(60))
    }

    @Test
    fun testScore59IsNeedsAttention() {
        assertEquals(HealthGrade.NEEDS_ATTENTION, gradeFor(59))
    }

    @Test
    fun testScore0IsNeedsAttention() {
        assertEquals(HealthGrade.NEEDS_ATTENTION, gradeFor(0))
    }
}
