package com.payslipmax.pdfparser.tax.rules

import com.payslipmax.pdfparser.tax.Section10Rule

/**
 * Section 10(14)/Rule 2BB monthly exemption caps for defence allowances. Relocated out of
 * TaxRuleKnowledgeBase (Phase 1 file split); wired into [com.payslipmax.pdfparser.insights.Section10CapPolicy]
 * (D8) -- this file only holds the data.
 */
object Section10ExemptionTable {
    val DEFAULT_DEFENCE_SECTION_10 =
        listOf(
            Section10Rule("HIGHLY_ACTIVE_FIELD", "Highly Active Field Area Allowance", 4200.0),
            Section10Rule("FIELD_AREA", "Field Area Allowance", 2700.0),
            Section10Rule("MODIFIED_FIELD", "Modified Field Area Allowance", 1000.0),
            Section10Rule("HIGH_ALTITUDE_L1", "High Altitude Allowance (Tier 1)", 1060.0),
            Section10Rule("HIGH_ALTITUDE_L2", "High Altitude Allowance (Tier 2)", 1600.0),
            Section10Rule("COUNTER_INSURGENCY", "Counter Insurgency Allowance", 3900.0),
            Section10Rule("ISLAND_DUTY", "Island Duty Allowance", 3250.0),
            Section10Rule("CHILDREN_EDU", "Children Education Allowance", 100.0),
            Section10Rule("HOSTEL_SUBSIDY", "Hostel Subsidy Allowance", 300.0),
        )
}
