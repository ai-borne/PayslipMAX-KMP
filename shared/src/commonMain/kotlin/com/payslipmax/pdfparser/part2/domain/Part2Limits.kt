package com.payslipmax.pdfparser.part2.domain

/**
 * The bounds on what an officer can type into a Part II draft and on what the app stores: the one home for them, so the engine, the
 * form, the repository and the backup restore agree. The limits on bundle content ship in the bundle itself (`template_limits`).
 * Storage judges sizes and counts only; whether a typed value makes sense (a real date, a day count in range) stays the engine's call,
 * so a half-filled form can always be saved.
 */
object Part2Limits {
    /** A typed remark, in characters. */
    const val TEXT_MAX = 200

    /** A published order number as typed, in characters. */
    const val ORDER_NO_MAX = 40

    /** A typed whole number (a year, a day count). */
    const val NUMBER_MAX = 9999

    /**
     * A year-by-year list (the break-up of an encashment) as text, in characters, and its rows. An officer with a long service has up to
     * about 40 years of accumulation; 60 rows of `YYYY DDDD` and a newline come to 600 characters. The list is stored as an ordinary text
     * answer, so storage bounds that answer by this limit and not by [TEXT_MAX].
     */
    const val BREAKUP_TEXT_MAX = 600
    const val BREAKUP_ROWS_MAX = 60

    /** Occurrence requests in one event; a leave event is a handful, never more than one per leave year. */
    const val REQUESTS_MAX = 10

    /** Answers per request, and the slot values per request (a template declares at most 10 slots; linked templates share ids). */
    const val VALUES_PER_REQUEST_MAX = 16
    const val ANSWERS_PER_REQUEST_MAX = 16

    /** One yes/no answer id (a rule family's own token), a slot id, a code, a template revision, an id (UUID-sized), in characters. */
    const val ANSWER_MAX = 64
    const val SLOT_ID_MAX = 40
    const val CODE_MAX = 40
    const val REV_MAX = 16
    const val ID_MAX = 64

    /** Reminders one draft can have dismissed: a return, an add-the-order series and a year end for each leave year, with room to spare. Each is a plain token (`ReminderTask`). */
    const val DISMISSED_MAX = 32

    /** Template revisions kept with a draft: the requested codes plus every linked template that was drafted. */
    const val REVS_MAX = 64

    /**
     * The print snapshot a draft keeps (the rows as last handed to the clerk): rows in it (10 requests, each with at most 16 linked orders, and
     * the request itself), characters in one rendered column (a template narrative or certificate is at most 1300 and a typed remark 200), and Data columns a row has.
     */
    const val PRINTED_ROWS_MAX = 170
    const val PRINTED_TEXT_MAX = 4000
    const val PRINTED_DATA_MAX = 4

    /** How long the app waits for a print job to end before it stops watching it (the job itself is the system's), in milliseconds. */
    const val PRINT_WATCH_MS = 30L * 60 * 1000

    /** A draft or order payload before encryption, in bytes. The longest real draft is about 40 KB; this is the ceiling, not a target. */
    const val PAYLOAD_MAX_BYTES = 128 * 1024

    /** One re-encoded scanned page (long edge 1600 px, JPEG), in bytes; far under any row limit. */
    const val PAGE_MAX_BYTES = 500 * 1024

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

    /** Drafts and vault orders the device holds. A restore adds no more than this many, whatever the file claims. */
    const val DRAFTS_MAX = 1000
    const val ORDERS_MAX = 1000

    /** The vault (sum of stored page ciphertext, in bytes): Settings warns above [VAULT_WARN_BYTES]; a write that would pass [VAULT_BYTES_MAX] is refused. */
    const val VAULT_WARN_BYTES = 60L * 1024 * 1024
    const val VAULT_BYTES_MAX = 100L * 1024 * 1024

    /** Pages a restore writes per insert: a handful, so the pages in memory at once stay a few megabytes whatever the vault holds. */
    const val PAGE_INSERT_BATCH = 4

    /** What `CryptoHelper.encrypt` adds to the plaintext: salt, IV, GCM tag and the magic header. Pinned against the real helper by a test on both platforms. */
    const val SEAL_OVERHEAD_BYTES = 48

    /** Bytes in the frame around a page's JPEG: version, two id lengths and the two ids at their longest. */
    const val PAGE_FRAME_MAX_BYTES = 3 + 2 * ID_MAX

    /** An id the app made (a UUID) or a slot, code or answer token: letters, digits, `-` and `_` only, so it can never carry markup or a path. */
    fun isPlainToken(
        value: String,
        max: Int,
    ): Boolean = value.length in 1..max && value.all { it.isLetterOrDigit() && it.code < 128 || it == '-' || it == '_' }

    fun isPlainId(value: String): Boolean = isPlainToken(value, ID_MAX)
}
