package com.payslipmax.pdfparser.parser

object PayslipPatternConfig {
    val monthMap =
        mapOf(
            "january" to 1, "jan" to 1,
            "february" to 2, "feb" to 2,
            "march" to 3, "mar" to 3,
            "april" to 4, "apr" to 4,
            "may" to 5,
            "june" to 6, "jun" to 6,
            "july" to 7, "jul" to 7,
            "august" to 8, "aug" to 8,
            "september" to 9, "sep" to 9, "sept" to 9,
            "october" to 10, "oct" to 10,
            "november" to 11, "nov" to 11,
            "december" to 12, "dec" to 12,
        )

    val hindiTransliterations =
        listOf(
            "kuula", "kula", "uula", "Aaya", "kTaOtI", "laona", "dona", "ivavarNa", "raiSa", "laoKa",
            "inavala", "p`oiYat", "Qana", "rxaa", "p`Qaana", "inayaM~k", "Af,sar", "puNao",
            "ka", "kI", "ivavarNaI", "sqaayaI", "Kata", "saM#yaa", "laoKaI", "Aiga`ma", "?Na",
        )

    val blocklist =
        setOf(
            "gross pay", "total credit", "total debit", "total deductions", "net remittance", "remittance", "remitance", "remitancer",
            "gross salary", "total taxable income", "net taxable income", "standard deduction", "tax payable",
            "tax deducted", "cess deducted", "page", "note", "date", "cda", "pan", "account", "name", "rank",
            "january", "jan", "february", "feb", "march", "mar", "april", "apr", "may", "june", "jun",
            "july", "jul", "august", "aug", "september", "sep", "october", "oct", "november", "nov", "december", "dec",
            "amount", "description", "credit", "debit", "earnings", "deductions",
            "bank code", "bank a/c no", "a/c no", "ifsc", "ifsc code", "bank account",
            "prosperous new year",
        )

    /**
     * Whole-label *prefix* blocklist, for header/statement-title boilerplate whose trailing text varies
     * (the printed month/year) so it can never be captured by [blocklist]'s exact-match check. Checked
     * via `normalized.startsWith(prefix)` in [TokenTableClassifier], same guard, minimum added generality.
     */
    val blocklistPrefixes =
        setOf(
            "statement of account for",
        )

    val strictlyMandatoryCredits = setOf("basicPay", "dearnessAllowance", "militaryServicePay")
    val strictlyMandatoryDebits = setOf("agif", "dsopSubscription")

    val sentenceWords =
        setOf(
            "the", "in", "with", "and", "will", "be", "is", "as", "has", "have", "been",
            "this", "that", "which", "ensuing", "provisions", "conformity", "interim", "budget",
            "recovery", "units", "formations", "connected", "manual", "transmission", "hard",
            "copies", "cease", "effect", "network", "accept", "officers",
        )

    val invalidEntireKeys =
        setOf(
            "to", "from", "for", "of", "on", "at", "by", "or", "no", "amt", "units", "bill",
            "recovery", "inst", "instal", "dated", "order", "pt", "ii", "part", "note", "date", "page",
        )

    val creditKeysMapping: Map<String, String> = AllowancePatternMappings.all

    val debitKeysMapping: Map<String, String> = DeductionPatternMappings.all

    val monthNames =
        listOf(
            "", "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December",
        )

    /**
     * Debit standardized keys that are ledger carry-over balances, not real deductions. When such a
     * key surfaces in the credit column it must be routed to the deductions map (so the ledger
     * resolver can pull it out), never folded into pay adjustments. SSOT for the cross-column routing
     * shared by the legacy string path and the Phase 4 [ReconciliationSolver].
     */
    val ledgerDebitKeys = setOf("openingDebitBalance", "closingCreditBalance")

    /**
     * Debit standardized keys that, when they appear in the *credit* column, are credit reversals of a
     * prior deduction (e.g. a refunded licence fee) and therefore become a pay adjustment.
     */
    val creditReversalDebitKeys =
        setOf("licenseFee", "furnitureRent", "waterCharges", "electricityCharges", "barrackDamage", "ticketRecovery")

    /**
     * Maps a credit standardized key found in the *debit* column (a recovery of a previously paid
     * allowance) to the deductions key that records that recovery. Mirrors the legacy routing in the
     * string path so both pipelines book recoveries identically.
     */
    fun recoveryTargetFor(creditStandardKey: String): String =
        when (creditStandardKey) {
            "fieldAllowance" -> "recFieldAllowance"
            "specialForcesPay" -> "recSpecialForces"
            else -> "recoveryOfDebits"
        }
}
