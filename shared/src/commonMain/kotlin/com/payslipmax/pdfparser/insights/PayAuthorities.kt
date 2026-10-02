package com.payslipmax.pdfparser.insights

/**
 * SSOT for the authorities cited on rule-based findings. Each string is taken from the offline
 * authoring reference in `scripts/pcdao_factory/output/` (rules marked high-confidence, source-derived).
 * A finding cites an authority only if it is listed here; never add one that is not verified.
 */
object PayAuthorities {
    const val TRANSPORT_ALLOWANCE =
        "GoI MoD letter No. 12630/Tpt.A/Mov C/246/D(Mov)/17 dated 15-09-2017 (Handbook of Pay and Allowances 2023, p. 122)"

    // The pcdao_factory reference cites "MoD letter No. 1(16)/2017/D(Pay/Services) dated 18-09-2017" for
    // this, copied from the same (wrong) source as the flying-allowance rate. That letter number, verified
    // against public MoD circulars, is actually dated 16-11-2017 and covers Extra Work Allowance / the
    // abolition of Flight Charge Certificate Allowance — unrelated to MSP. Citing the rate's true source
    // (the pay matrix itself) instead, until a correct implementing letter is verified.
    const val MILITARY_SERVICE_PAY =
        "Army Officers Pay Rules 2017 (7th CPC); Handbook of Pay and Allowances 2023, pp. 88-93"
    const val ANNUAL_INCREMENT =
        "Army Officers Pay Rules 2017; SRO 12(E) dated 03-05-2017 (Handbook of Pay and Allowances 2023, pp. 18, 95)"
}
