package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.data.GuideNotesRepository
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideNotesRepository
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.assertNotNull

/** Shared set-up for the notes view-model tests (M7): a Guide, a fake store, and helpers to save and delete through the model. */
abstract class GuideNotesViewModelTestBase {
    protected val dispatcher = StandardTestDispatcher()
    protected val crash = FakeCrashReporter()
    protected val store = FakeGuideNotesRepository(clock = { 1_000L })

    protected fun withRev(
        bundle: GuideBundle,
        rev: String,
    ): GuideBundle = bundle.copy(cards = bundle.cards.map { it.copy(rev = rev) })

    protected fun guide(bundle: GuideBundle = withRev(SyntheticGuideBundle.withFigures(), "rev00001")) = GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(bundle)), crash, dispatcher)

    protected fun notes(
        repository: GuideNotesRepository = store,
        guide: GuideViewModel = guide(),
    ) = GuideNotesViewModel(repository, guide, dispatcher)

    /** The state is shared only while someone collects it, as a screen does. */
    protected fun TestScope.watch(model: GuideNotesViewModel): GuideNotesViewModel {
        backgroundScope.launch(dispatcher) { model.state.collect {} }
        return model
    }

    protected fun test(
        guide: GuideViewModel = guide(),
        repository: GuideNotesRepository = store,
        block: suspend TestScope.(GuideNotesViewModel) -> Unit,
    ) = runTest(dispatcher) {
        val model = watch(notes(repository, guide))
        guide.load()
        testScheduler.advanceUntilIdle()
        block(model)
    }

    protected fun TestScope.save(
        model: GuideNotesViewModel,
        cardId: String,
        text: String,
        movedFrom: String? = null,
    ): GuideNoteOutcome {
        var outcome: GuideNoteOutcome? = null
        model.save(cardId, text, movedFrom) { outcome = it }
        testScheduler.advanceUntilIdle()
        return assertNotNull(outcome)
    }

    protected fun TestScope.delete(
        model: GuideNotesViewModel,
        sourceCardId: String,
    ): Boolean {
        var done: Boolean? = null
        model.delete(sourceCardId) { done = it }
        testScheduler.advanceUntilIdle()
        return assertNotNull(done)
    }
}
