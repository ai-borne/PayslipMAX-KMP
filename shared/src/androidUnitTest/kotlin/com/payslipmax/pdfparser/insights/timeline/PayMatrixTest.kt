package com.payslipmax.pdfparser.insights.timeline

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PayMatrixTest {
    @Test
    fun kotlinMatrixMatchesTheAuthoringReferenceJson() {
        val json = Json.parseToJsonElement(File("../scripts/pcdao_factory/output/pay_matrix_7th_cpc.json").readText())
        val reference = json.jsonObject.getValue("regular_officers_pay_matrix").jsonObject
        assertEquals(PayLevel.entries.map { it.label }.toSet(), reference.keys)
        for (level in PayLevel.entries) {
            val expected = reference.getValue(level.label).jsonArray.map { it.jsonPrimitive.int }
            assertEquals(expected, PayMatrix.levelCells(level), "Level ${level.label} drifted from the JSON reference")
        }
    }

    @Test
    fun everyLevelRisesStrictlyStageByStage() {
        for (level in PayLevel.entries) {
            val cells = PayMatrix.levelCells(level)
            assertTrue(cells.zipWithNext().all { (a, b) -> a < b }, "Level ${level.label} is not strictly increasing")
        }
    }

    @Test
    fun stageLookupIsOneBasedAndRejectsForeignCells() {
        assertEquals(1, PayMatrix.stageOf(PayLevel.L12A, 121200.0))
        assertEquals(4, PayMatrix.stageOf(PayLevel.L10, 61300.0))
        assertNull(PayMatrix.stageOf(PayLevel.L11, 121201.0))
        assertNull(PayMatrix.stageOf(PayLevel.L11, 85300.5))
        assertEquals(listOf(PayLevel.L10, PayLevel.L10B), PayMatrix.levelsContaining(61300.0).take(2))
        assertEquals(85300, PayMatrix.payAt(PayLevel.L11, 8))
    }
}
