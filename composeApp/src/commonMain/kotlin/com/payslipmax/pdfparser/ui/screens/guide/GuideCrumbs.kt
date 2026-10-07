package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.domain.GuideIndex
import com.payslipmax.pdfparser.ui.theme.GuideStrings

/** One breadcrumb link: its label and the levels above Home that lead to it (see [GuideNavState.upTo]). */
data class GuideCrumb(val label: String, val path: List<GuideDestination>)

/**
 * The breadcrumb above a feed (Guide, area) or a card (Guide, area, case). A card leads back to the feed it was
 * opened from, which for an "also relevant here" link is not its home case; a card opened from anywhere else
 * leads to the case it is homed in. Other levels have none (the back header is enough there).
 */
internal fun GuideIndex.crumbs(stack: List<GuideDestination>): List<GuideCrumb> {
    val home = GuideCrumb(GuideStrings.breadcrumbHome, emptyList())
    return when (val top = stack.lastOrNull()) {
        is GuideDestination.Case -> {
            val area = areaOfCase(top.caseId) ?: return listOf(home)
            listOf(home, GuideCrumb(area.title, listOf(GuideDestination.Area(area.id))))
        }
        is GuideDestination.Card -> {
            val feed =
                stack.dropLast(1).lastOrNull { it is GuideDestination.Case } as? GuideDestination.Case
                    ?: card(top.cardId)?.let { GuideDestination.Case(it.nav) }
                    ?: return listOf(home)
            val area = areaOfCase(feed.caseId) ?: return listOf(home)
            val areaLevel = GuideDestination.Area(area.id)
            listOf(
                home,
                GuideCrumb(area.title, listOf(areaLevel)),
                GuideCrumb(case(feed.caseId)?.title.orEmpty(), listOf(areaLevel, feed)),
            )
        }
        else -> emptyList()
    }
}
