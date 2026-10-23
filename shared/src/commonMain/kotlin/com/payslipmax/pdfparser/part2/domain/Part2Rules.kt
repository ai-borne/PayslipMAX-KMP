package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.guide.model.Part2RuleDecl
import com.payslipmax.pdfparser.guide.model.Part2Template

/** An occurrence as drafted: its [template], where it falls ([from], [to]) and the values the engine worked out (day counts, computed dates), by slot id. */
class DraftedOccurrence(
    val template: Part2Template,
    val from: CalendarDate?,
    val to: CalendarDate?,
    val computed: Map<String, SlotValue>,
) {
    /** The day count the engine worked out for a `days` slot, or null when it could not be worked out. */
    fun days(slot: String): Int? = (computed[slot] as? SlotValue.NumberValue)?.number

    /** A date the engine worked out for a computed date slot, or null. */
    fun date(slot: String): CalendarDate? = (computed[slot] as? SlotValue.DateValue)?.date
}

/** What a rule can read: the officer's [request], the [occurrence] it is judging, and every occurrence of the [event] so far. */
class RuleContext(
    val request: OccurrenceRequest,
    val occurrence: DraftedOccurrence,
    val event: List<DraftedOccurrence>,
)

/** A rule the app implements. Its severity and citation come from the bundle's declaration, never from here. */
sealed interface Part2Rule {
    val id: String
}

/** A computable note of the manual: [violated] is true when the officer's answers break it. */
interface CheckRule : Part2Rule {
    fun violated(context: RuleContext): Boolean
}

/** Decides whether a linked order applies, for example whether the officer is on ration strength. */
interface ConditionRule : Part2Rule {
    fun holds(context: RuleContext): Boolean
}

/** A check made from [violated], so a rule family is a list of rules and not a class per rule. */
internal fun checkRule(
    id: String,
    violated: (RuleContext) -> Boolean,
): CheckRule =
    object : CheckRule {
        override val id = id

        override fun violated(context: RuleContext) = violated(context)
    }

/** A condition made from [holds]; see [checkRule]. */
internal fun conditionRule(
    id: String,
    holds: (RuleContext) -> Boolean,
): ConditionRule =
    object : ConditionRule {
        override val id = id

        override fun holds(context: RuleContext) = holds(context)
    }

/**
 * The rules the app implements, found by id. The bundle declares the same ids (kind, severity, citation) and a contract test keeps the
 * two lists equal ([problemsAgainst]). Each rule family (annual leave, ration, area allowances) is its own set of rules, so a new family
 * is a new list given to the registry, not an edit to the engine.
 */
class Part2Rules(rules: List<Part2Rule>) {
    private val byId: Map<String, Part2Rule>

    init {
        require(rules.map { it.id }.toSet().size == rules.size) { "duplicate Part II rule id" }
        byId = rules.associateBy { it.id }
    }

    val ids: Set<String> get() = byId.keys

    fun check(id: String): CheckRule? = byId[id] as? CheckRule

    fun condition(id: String): ConditionRule? = byId[id] as? ConditionRule

    /** Where this registry and the bundle's [declared] rules disagree: a rule only one side has, or one the two call different kinds. */
    fun problemsAgainst(declared: List<Part2RuleDecl>): List<String> {
        val problems = mutableListOf<String>()
        val declaredIds = declared.map { it.id }.toSet()
        declared.filter { it.id !in byId }.forEach { problems += "rule ${it.id} is declared in the bundle but the app does not implement it" }
        byId.keys.filter { it !in declaredIds }.sorted().forEach { problems += "rule $it is implemented in the app but the bundle does not declare it" }
        for (decl in declared) {
            val rule = byId[decl.id] ?: continue
            val appKind = if (rule is CheckRule) "check" else "condition"
            if (appKind != decl.kind) problems += "rule ${decl.id} is a ${decl.kind} in the bundle but a $appKind in the app"
        }
        return problems
    }

    companion object {
        /** The rules the app ships. Each content batch adds its rule family here and nowhere else. */
        fun shipped(): Part2Rules = Part2Rules(LeaveRules.all + RationRules.all + AreaRules.all)
    }
}
