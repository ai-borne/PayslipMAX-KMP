package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.guide.domain.GuideProfile
import com.payslipmax.pdfparser.guide.domain.GuideProfileProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

/**
 * A [GuideProfileProvider] tests drive by hand. [subscriptions] counts how often a screen started reading the profile, and
 * [active] how many are reading now, so a test can prove a locked card never touches the payslips and that reading stops
 * when the card leaves the screen.
 */
class FakeGuideProfileProvider(
    profile: GuideProfile? = null,
) : GuideProfileProvider {
    val current = MutableStateFlow(profile)
    var subscriptions = 0
        private set
    var active = 0
        private set

    override fun profile(): Flow<GuideProfile?> =
        current
            .onStart {
                subscriptions++
                active++
            }.onCompletion { active-- }
}
