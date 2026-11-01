package com.payslipmax.pdfparser.part2.domain

/**
 * The bounds on what an officer can type into the Orders Bank and on what the app stores: the one home for them, so the form, the
 * repository and the backup restore agree. Storage judges sizes and counts only; whether a typed date makes sense is the form's call.
 */
object Part2Limits {
    /** A typed column value or the one-line note, in characters. */
    const val TEXT_MAX = 200

    /** A published order number as typed, in characters. */
    const val ORDER_NO_MAX = 40

    /** The "No. of Occurrences" an order prints: the casualties it publishes. */
    const val EXPECTED_COUNT_MAX = 999

    /** A casualty code, an id (UUID-sized), in characters. */
    const val CODE_MAX = 40
    const val ID_MAX = 64

    /** The Data columns of an order (Data 1 to 4); From and To are their own fields. */
    const val DATA_COLUMNS = 4

    /** An entry or order payload before encryption, in bytes: far above the largest entry the form can make (six texts of 200 characters). */
    const val PAYLOAD_MAX_BYTES = 16 * 1024

    /** One re-encoded scanned page (long edge 1600 px, JPEG), in bytes. */
    const val PAGE_MAX_BYTES = 500 * 1024

    /**
     * What one readable text page should weigh once re-encoded (a design target, not a refusal limit: [PAGE_MAX_BYTES] still refuses). The
     * vault cap is planned from it: at this size [VAULT_BYTES_MAX] holds about 409 pages. The encoders aim for it (`JpegFit.fitToBudget`) and fall back to [PAGE_MAX_BYTES]. A test measures the encoder against it; the
     * final figure is set from a scan of the owner's own old, yellowed order, not guessed.
     */
    const val PAGE_BUDGET_BYTES = 250 * 1024

    /**
     * How a scanned page is re-encoded before it is stored (Phase 6a): the long edge in pixels, then JPEG quality stepped down from the
     * start by the step until the page fits [PAGE_MAX_BYTES], and never below the floor (a page that still does not fit is refused, not
     * degraded further). A scanner's own file or image is read only up to [PAGE_RAW_MAX_BYTES] and [PAGE_PIXELS_MAX] pixels, so a crafted
     * or runaway input cannot exhaust memory.
     */
    const val PAGE_LONG_EDGE_PX = 1600
    const val PAGE_JPEG_QUALITY_START = 85
    const val PAGE_JPEG_QUALITY_STEP = 5
    const val PAGE_JPEG_QUALITY_FLOOR = 40
    const val PAGE_RAW_MAX_BYTES = 25 * 1024 * 1024
    const val PAGE_PIXELS_MAX = 100_000_000L

    /** Pages one published order can hold. */
    const val PAGES_PER_ORDER_MAX = 10

    /** Entries and orders the device holds. A restore adds no more than this many, whatever the file claims. */
    const val ENTRIES_MAX = 2000
    const val ORDERS_MAX = 1000

    /** The vault (sum of stored page ciphertext, in bytes): a write that would pass [VAULT_BYTES_MAX] is refused. */
    const val VAULT_BYTES_MAX = 100L * 1024 * 1024

    /** Past this the Bank warns, so the officer hears it before a scan is refused (80% of [VAULT_BYTES_MAX]). */
    const val VAULT_WARN_BYTES = VAULT_BYTES_MAX / 5 * 4

    /** Pages a restore writes per insert: a handful, so the pages in memory at once stay a few megabytes whatever the vault holds. */
    const val PAGE_INSERT_BATCH = 4

    /** What `CryptoHelper.encrypt` adds to the plaintext: salt, IV, GCM tag and the magic header. Pinned against the real helper by a test on both platforms. */
    const val SEAL_OVERHEAD_BYTES = 48

    /** An id the app made (a UUID) or a code: letters, digits, `-` and `_` only, so it can never carry markup or a path. */
    fun isPlainToken(
        value: String,
        max: Int,
    ): Boolean = value.length in 1..max && value.all { it.isLetterOrDigit() && it.code < 128 || it == '-' || it == '_' }

    fun isPlainId(value: String): Boolean = isPlainToken(value, ID_MAX)
}
