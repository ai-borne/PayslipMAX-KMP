package com.payslipmax.pdfparser.part2.data

/**
 * A row that cannot be turned back into a draft, order or page: wrong key, damaged or tampered bytes, JSON that is not a Part II
 * payload, a payload from a newer app, or an item moved to another id. The message is fixed and there is no cause on purpose:
 * serialization errors quote the input, and the input is the officer's private data.
 */
class Part2UnreadableException : Exception("A Part II item could not be read.")

/** An item whose payload passes `Part2Limits.PAYLOAD_MAX_BYTES` or whose page passes `PAGE_MAX_BYTES`. Fixed message, no cause. */
class Part2TooLargeException : Exception("A Part II item is too large to store.")
