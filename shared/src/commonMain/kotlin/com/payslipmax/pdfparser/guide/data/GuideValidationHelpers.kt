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

/** True when [value] holds an angle bracket: officer and bundle text is drawn and printed as plain text, so markup is refused, not escaped. */
internal fun hasAngleBrackets(value: String): Boolean = '<' in value || '>' in value

/** Empty, over-long or markup-bearing text as problems: the plain-text rule every shipped Part II text obeys. */
internal fun textProblems(
    where: String,
    field: String,
    value: String,
    limit: Int,
): List<String> =
    when {
        value.isBlank() -> listOf("$where: $field is empty")
        value.length > limit -> listOf("$where: $field is over $limit characters")
        hasAngleBrackets(value) -> listOf("$where: $field has angle brackets (plain text only)")
        else -> emptyList()
    }
