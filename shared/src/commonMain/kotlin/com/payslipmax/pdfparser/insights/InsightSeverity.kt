package com.payslipmax.pdfparser.insights

/**
 * Canonical severity SSOT for anomalies and opportunities surfaced across the Insights screen.
 *
 * In-memory only — persisted `FinancialInsightEntity.severity` strings stay on the legacy
 * "INFO"/"SUCCESS"/"WARNING"/"CRITICAL" vocabulary (see [AnomalySeverityMapper.toPersistedString])
 * so no Room migration is needed to introduce this enum.
 */
enum class InsightSeverity { INFO, WARNING, IMPORTANT, OPPORTUNITY }
