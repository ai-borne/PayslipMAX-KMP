package com.payslipmax.pdfparser.guide.data

private const val ID_MAX = 32

/** Lowercase letters, digits and underscores, starting with a letter, at most [ID_MAX] long: the shape of every id the Guide tools write. */
internal fun isGuideId(value: String): Boolean =
    value.isNotEmpty() && value.length <= ID_MAX && value[0] in 'a'..'z' && value.all { it in 'a'..'z' || it in '0'..'9' || it == '_' }

/** One "duplicate [kind] id X" problem for every id that appears more than once in [ids]. */
internal fun duplicateIdProblems(
    kind: String,
    ids: List<String>,
): List<String> = ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.map { "duplicate $kind id $it" }
