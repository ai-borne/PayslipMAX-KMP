package com.payslipmax.pcdao.reconciliation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Single Source of Truth (SSOT) repository for managing month-keyed Situational Contexts.
 * Maintains sticky overrides pinned to specific payslip months (e.g., "2024-05").
 */
interface SituationalContextRepository {
    fun getContextForMonth(monthKey: String): ActiveSituationalContext?

    fun setContextForMonth(
        monthKey: String,
        context: ActiveSituationalContext,
    )

    fun removeContextForMonth(monthKey: String)

    fun hasManualOverride(monthKey: String): Boolean

    fun getAllContexts(): Map<String, ActiveSituationalContext>

    fun clearAll()
}

/**
 * Thread-safe in-memory implementation of [SituationalContextRepository] using atomic StateFlow updates.
 */
class InMemorySituationalContextRepository : SituationalContextRepository {
    private val _contexts = MutableStateFlow<Map<String, ActiveSituationalContext>>(emptyMap())
    val contexts: StateFlow<Map<String, ActiveSituationalContext>> = _contexts.asStateFlow()

    override fun getContextForMonth(monthKey: String): ActiveSituationalContext? {
        return _contexts.value[monthKey]
    }

    override fun setContextForMonth(
        monthKey: String,
        context: ActiveSituationalContext,
    ) {
        _contexts.update { current ->
            current + (monthKey to context)
        }
    }

    override fun removeContextForMonth(monthKey: String) {
        _contexts.update { current ->
            current - monthKey
        }
    }

    override fun hasManualOverride(monthKey: String): Boolean {
        return _contexts.value.containsKey(monthKey)
    }

    override fun getAllContexts(): Map<String, ActiveSituationalContext> {
        return _contexts.value
    }

    override fun clearAll() {
        _contexts.value = emptyMap()
    }
}
