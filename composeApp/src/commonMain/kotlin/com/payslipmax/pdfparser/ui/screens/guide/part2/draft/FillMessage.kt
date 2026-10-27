package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

/** What a press of the fill button last did on one section, until the officer's next edit. The words are `Part2LeaveBankStrings`'. */
sealed interface FillMessage {
    /** The Leave Bank has not answered yet; the fill happens when it does. */
    data object Waiting : FillMessage

    /** [count] boxes were filled. */
    data class Filled(val count: Int) : FillMessage

    /** Nothing differed from what the boxes held. */
    data object Unchanged : FillMessage

    /** The fill reads the leave year and it was not typed. */
    data object NeedsYear : FillMessage

    /** Neither the Leave Bank nor the officer's drafts know anything to fill. */
    data object NothingKnown : FillMessage
}
