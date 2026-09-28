package com.payslipmax.pcdao.repository

import com.payslipmax.pcdao.model.AllowancesAndAdditionsData
import com.payslipmax.pcdao.model.CanonicalRulesData
import com.payslipmax.pcdao.model.PayMatrixData
import com.payslipmax.pcdao.model.PromotionWorkflowData
import com.payslipmax.pcdao.model.RiskHardshipData
import com.payslipmax.pcdao.model.ServiceConditionsData
import com.payslipmax.pcdao.model.SpecialCompensatoryData
import com.payslipmax.pcdao.model.TransportAllowanceData
import com.payslipmax.pcdao.model.TravelTadaData
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * Thread-safe in-memory caching repository for canonical PCDA(O) datasets.
 */
class PcdaoRulesRepository(
    private val assetProvider: PcdaoAssetProvider,
    private val json: Json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        },
) {
    private val mutex = Mutex()

    private var cachedPayMatrix: PayMatrixData? = null
    private var cachedTransport: TransportAllowanceData? = null
    private var cachedRiskHardship: RiskHardshipData? = null
    private var cachedSpecialized: AllowancesAndAdditionsData? = null
    private var cachedSpecialComp: SpecialCompensatoryData? = null
    private var cachedTravelTada: TravelTadaData? = null
    private var cachedServiceConditions: ServiceConditionsData? = null
    private var cachedPromotion: PromotionWorkflowData? = null
    private var cachedRules: CanonicalRulesData? = null

    suspend fun getPayMatrix(): PayMatrixData =
        mutex.withLock {
            cachedPayMatrix ?: json.decodeFromString<PayMatrixData>(
                assetProvider.loadAsset("pay_matrix_7th_cpc.json"),
            ).also { cachedPayMatrix = it }
        }

    suspend fun getTransportAllowance(): TransportAllowanceData =
        mutex.withLock {
            cachedTransport ?: json.decodeFromString<TransportAllowanceData>(
                assetProvider.loadAsset("transport_allowance_rates.json"),
            ).also { cachedTransport = it }
        }

    suspend fun getRiskHardship(): RiskHardshipData =
        mutex.withLock {
            cachedRiskHardship ?: json.decodeFromString<RiskHardshipData>(
                assetProvider.loadAsset("risk_hardship_rates.json"),
            ).also { cachedRiskHardship = it }
        }

    suspend fun getSpecializedAllowances(): AllowancesAndAdditionsData =
        mutex.withLock {
            cachedSpecialized ?: json.decodeFromString<AllowancesAndAdditionsData>(
                assetProvider.loadAsset("allowances_and_additions.json"),
            ).also { cachedSpecialized = it }
        }

    suspend fun getSpecialCompensatory(): SpecialCompensatoryData =
        mutex.withLock {
            cachedSpecialComp ?: json.decodeFromString<SpecialCompensatoryData>(
                assetProvider.loadAsset("special_compensatory_allowances.json"),
            ).also { cachedSpecialComp = it }
        }

    suspend fun getTravelTada(): TravelTadaData =
        mutex.withLock {
            cachedTravelTada ?: json.decodeFromString<TravelTadaData>(
                assetProvider.loadAsset("travel_tada_rates.json"),
            ).also { cachedTravelTada = it }
        }

    suspend fun getServiceConditions(): ServiceConditionsData =
        mutex.withLock {
            cachedServiceConditions ?: json.decodeFromString<ServiceConditionsData>(
                assetProvider.loadAsset("service_conditions_and_funds.json"),
            ).also { cachedServiceConditions = it }
        }

    suspend fun getPromotionWorkflows(): PromotionWorkflowData =
        mutex.withLock {
            cachedPromotion ?: json.decodeFromString<PromotionWorkflowData>(
                assetProvider.loadAsset("promotion_and_pcdao_workflows.json"),
            ).also { cachedPromotion = it }
        }

    suspend fun getCanonicalRules(): CanonicalRulesData =
        mutex.withLock {
            cachedRules ?: json.decodeFromString<CanonicalRulesData>(
                assetProvider.loadAsset("canonical_pcdao_rules.json"),
            ).also { cachedRules = it }
        }

    suspend fun clearCache() =
        mutex.withLock {
            cachedPayMatrix = null
            cachedTransport = null
            cachedRiskHardship = null
            cachedSpecialized = null
            cachedSpecialComp = null
            cachedTravelTada = null
            cachedServiceConditions = null
            cachedPromotion = null
            cachedRules = null
        }
}
