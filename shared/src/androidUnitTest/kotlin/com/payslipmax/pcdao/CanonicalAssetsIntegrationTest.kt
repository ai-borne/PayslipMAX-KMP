package com.payslipmax.pcdao

import com.payslipmax.pcdao.repository.PcdaoAssetProvider
import com.payslipmax.pcdao.repository.PcdaoRulesRepository
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CanonicalAssetsIntegrationTest {
    private val localAssetProvider =
        object : PcdaoAssetProvider {
            override suspend fun loadAsset(fileName: String): String {
                val possiblePaths =
                    listOf(
                        "../scripts/pcdao_factory/output/$fileName",
                        "scripts/pcdao_factory/output/$fileName",
                        "../composeApp/src/commonMain/composeResources/files/pcdao/$fileName",
                        "composeApp/src/commonMain/composeResources/files/pcdao/$fileName",
                        "../../scripts/pcdao_factory/output/$fileName",
                    )
                for (path in possiblePaths) {
                    val file = File(path)
                    if (file.exists()) {
                        return file.readText()
                    }
                }
                val userDir = System.getProperty("user.dir")
                throw IllegalArgumentException("Asset not found for $fileName. Current dir: $userDir")
            }
        }

    private val repository = PcdaoRulesRepository(localAssetProvider)

    @Test
    fun testPayMatrixSerialization() =
        runBlocking {
            val matrix = repository.getPayMatrix()
            assertEquals("7th Central Pay Commission", matrix.commission)
            assertEquals(2.67, matrix.indexOfRationalisation.level12AAnd13)
            assertEquals(15500, matrix.militaryServicePay.regularOfficers.rateMonthly)
            assertEquals(10800, matrix.militaryServicePay.mnsOfficers.rateMonthly)

            val officerLevels = matrix.regularOfficersPayMatrix
            assertTrue(officerLevels.containsKey("10"))
            assertTrue(officerLevels.containsKey("12A"))
            assertTrue(officerLevels.containsKey("18"))

            val level10Stages = officerLevels["10"] ?: error("Level 10 missing")
            assertEquals(40, level10Stages.size)
            assertEquals(56100, level10Stages.first())
            assertEquals(177500, level10Stages.last())

            val level12AStages = officerLevels["12A"] ?: error("Level 12A missing")
            assertEquals(20, level12AStages.size)
            assertEquals(121200, level12AStages.first())
        }

    @Test
    fun testTransportAllowanceSerialization() =
        runBlocking {
            val tpta = repository.getTransportAllowance()
            val slabs = tpta.rates.slabs
            assertEquals(2, slabs.size)
            assertEquals(7200, slabs[0].higherRateCitiesMonthly)
            assertEquals(3600, slabs[0].otherPlacesMonthly)
            assertEquals(15750, slabs[1].higherRateCitiesMonthly)
            assertEquals(7200, slabs[1].otherPlacesMonthly)

            assertTrue(tpta.higherRateCities.isNotEmpty())
            assertEquals(2.0, tpta.divyangRules?.rateMultiplier)
            assertTrue(tpta.disallowanceConditions.isNotEmpty())
        }

    @Test
    fun testRiskHardshipSerialization() =
        runBlocking {
            val rh = repository.getRiskHardship()
            assertEquals(42500, rh.rhMatrix.rhMax.monthlyRate)
            assertEquals(53125, rh.rhMatrix.rhMax.escalatedRateAt50Da)

            val hafa = rh.fieldAllowances["HAFAA"] ?: error("HAFAA missing")
            assertEquals(16900, hafa.rate)
            assertEquals(21125, hafa.escalatedRateAt50Da)

            val cat3 = rh.highAltitudeAllowance["Category_III"] ?: error("Cat III missing")
            assertEquals(25000, cat3.rate)
            assertEquals(31250, cat3.escalatedRateAt50Da)

            assertEquals(25, rh.escalationAndExclusions?.escalationClause?.percentageIncrease)
            assertEquals(50, rh.escalationAndExclusions?.escalationClause?.daThresholdPercent)
        }

    @Test
    fun testAllowancesAndAdditionsSerialization() =
        runBlocking {
            val additions = repository.getSpecializedAllowances()
            val awards = additions.gallantryAwards.awards
            assertTrue(awards.any { it.decoration.contains("Param Vir Chakra") && it.monthlyRate == 20000 })
            assertTrue(awards.any { it.decoration.contains("Sena Medal") && it.monthlyRate == 2000 })

            val flying = additions.specializedAllowances.flyingAllowance
            assertNotNull(flying)
            assertEquals(25000, flying.rateMonthly)
            assertEquals(31250, flying.escalatedRateAt50Da)

            val cea = additions.childrenEducationAllowance
            assertEquals(33750, cea.ceaAnnualRate)
            assertEquals(101250, cea.hostelSubsidyAnnualRate)
            assertEquals(2.0, cea.divyangChildMultiplier)
        }

    @Test
    fun testSpecialCompensatorySerialization() =
        runBlocking {
            val comp = repository.getSpecialCompensatory()
            val dress = comp.dressAllowance ?: error("Dress allowance missing")
            assertEquals(20000, dress.annualRateArmyOfficers)
            assertEquals("July", dress.creditMonth)

            val sda = comp.regionalDutyAllowances?.specialDutyAllowanceSda
            assertNotNull(sda)
            assertEquals(10, sda.ratePercentOfBasicPay)

            val tla = comp.toughLocationAllowance
            assertNotNull(tla)
            assertEquals(5300, tla.categories["TLA_I"]?.monthlyRate)
        }

    @Test
    fun testTravelTadaSerialization() =
        runBlocking {
            val tada = repository.getTravelTada()
            assertTrue(tada.travelEntitlements.isNotEmpty())
            val ctg = tada.transferAndBaggage?.compositeTransferGrant
            assertNotNull(ctg)
            assertEquals(80, ctg.standardRatePercentOfBasic)

            val deadlines = tada.deadlinesAndDocumentation?.claimDeadlines
            assertNotNull(deadlines)
            assertEquals(60, deadlines["td_and_pdm_without_advance"]?.deadlineDays)
            assertEquals(30, deadlines["ltc_with_advance"]?.deadlineDays)
        }

    @Test
    fun testServiceConditionsSerialization() =
        runBlocking {
            val sc = repository.getServiceConditions()
            assertEquals(6.0, sc.dsopFund.minimumSubscriptionPercent)
            assertEquals(100.0, sc.dsopFund.maximumSubscriptionPercent)
            assertEquals(500000L, sc.dsopFund.annualTaxExemptSubscriptionLimit)

            assertEquals(10000, sc.agifInsurance.monthlySubscriptionRegularOfficers)
            assertEquals(10000000L, sc.agifInsurance.lifeInsuranceCover)
        }

    @Test
    fun testPromotionWorkflowsSerialization() =
        runBlocking {
            val promo = repository.getPromotionWorkflows()
            val fixation = promo.promotionPayFixation ?: error("Fixation missing")
            assertEquals(1, fixation.electionWindowMonths)
            assertTrue(fixation.options.containsKey("option_1"))
            assertTrue(fixation.options.containsKey("option_2"))

            val sections = promo.pcdaoLedgerSystem?.sections
            assertNotNull(sections)
            assertTrue(sections.containsKey("R_Section"))
        }

    @Test
    fun testCanonicalRulesSerialization() =
        runBlocking {
            val canonical = repository.getCanonicalRules()
            assertEquals(378, canonical.totalRules)
            assertEquals(378, canonical.rules.size)
            assertTrue(canonical.rules.all { it.ruleId.isNotBlank() && it.domain.isNotBlank() && it.title.isNotBlank() })
        }

    @Test
    fun testRepositoryCaching() =
        runBlocking {
            val firstLoad = repository.getPayMatrix()
            val secondLoad = repository.getPayMatrix()
            assertTrue(firstLoad === secondLoad, "Repository should return cached reference")

            repository.clearCache()
            val thirdLoad = repository.getPayMatrix()
            assertTrue(firstLoad !== thirdLoad, "Repository should reload after clearing cache")
        }

    @Test
    fun testPayFixationOptimizerWithCanonicalPayMatrix() =
        runBlocking {
            val matrix = repository.getPayMatrix()
            val optimizer = com.payslipmax.pcdao.engine.PayFixationOptimizer(matrix)

            val request =
                com.payslipmax.pcdao.engine.PayFixationRequest(
                    fromLevel = "10",
                    fromStage = 8,
                    toLevel = "11",
                    promotionDate = "2026-03-15",
                    dniMonth = 7,
                )

            val result = optimizer.optimizePromotion(request)
            assertEquals(69000, result.fromBasicPay)
            assertEquals(71500, result.opt1FixedPay)
            assertEquals(73600, result.opt2PostDniFixedPay)
            assertEquals(2664000L, result.opt1Total36Months)
            assertEquals(2726800L, result.opt2Total36Months)
            assertEquals(62800L, result.cumulativeDelta)
            assertEquals(com.payslipmax.pcdao.engine.FixationOption.OPTION_2, result.recommendedOption)
        }
}
