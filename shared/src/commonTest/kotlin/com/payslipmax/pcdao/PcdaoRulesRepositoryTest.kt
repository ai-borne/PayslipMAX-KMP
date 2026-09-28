package com.payslipmax.pcdao

import com.payslipmax.pcdao.repository.PcdaoAssetProvider
import com.payslipmax.pcdao.repository.PcdaoRulesRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PcdaoRulesRepositoryTest {
    private class MockAssetProvider(
        private val responses: Map<String, String>,
    ) : PcdaoAssetProvider {
        var callCount: Int = 0

        override suspend fun loadAsset(fileName: String): String {
            callCount++
            return responses[fileName] ?: error("Mock payload not found for $fileName")
        }
    }

    @Test
    fun testRepositoryCachingAvoidsRedundantLoads() =
        runTest {
            val sampleJson =
                """
                {
                  "commission": "7th Central Pay Commission",
                  "effective_date": "2016-01-01",
                  "index_of_rationalisation": {
                    "level_10_to_11": 2.57,
                    "level_12A_and_13": 2.67,
                    "level_13A_and_above": 2.72
                  },
                  "regular_officers_pay_matrix": {
                    "10": [56100, 57800]
                  }
                }
                """.trimIndent()

            val mockProvider = MockAssetProvider(mapOf("pay_matrix_7th_cpc.json" to sampleJson))
            val repo = PcdaoRulesRepository(mockProvider)

            val first = repo.getPayMatrix()
            assertEquals("7th Central Pay Commission", first.commission)
            assertEquals(1, mockProvider.callCount)

            val second = repo.getPayMatrix()
            assertEquals(1, mockProvider.callCount, "Second call must return cached instance")
            assertTrue(first === second)

            repo.clearCache()
            val third = repo.getPayMatrix()
            assertEquals(2, mockProvider.callCount, "Call after clearCache must re-load")
            assertTrue(first !== third)
        }
}
