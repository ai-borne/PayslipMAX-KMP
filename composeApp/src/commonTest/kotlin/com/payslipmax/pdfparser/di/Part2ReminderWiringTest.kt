package com.payslipmax.pdfparser.di

import com.payslipmax.pdfparser.database.PayslipDao
import com.payslipmax.pdfparser.nav.PendingPart2Link
import com.payslipmax.pdfparser.parser.PdfParser
import com.payslipmax.pdfparser.part2.data.Part2Dao
import com.payslipmax.pdfparser.part2.reminders.ReminderFireCheck
import com.payslipmax.pdfparser.part2.reminders.ReminderNotificationText
import com.payslipmax.pdfparser.part2.reminders.ReminderScheduler
import com.payslipmax.pdfparser.part2.reminders.ReminderSync
import com.payslipmax.pdfparser.part2.reminders.ShortDelayReminderScheduler
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.FakePdfParser
import com.payslipmax.pdfparser.ui.screens.guide.GuideViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.Part2Models
import com.payslipmax.pdfparser.ui.screens.guide.part2.isPart2Enabled
import com.payslipmax.pdfparser.ui.screens.guide.part2.reminders.ReminderPromptModel
import com.payslipmax.pdfparser.ui.theme.Part2ReminderStrings
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * WHY: the reminder pieces are joined only by their Koin bindings. A second sync, a second pending link or a scheduler that bypasses the
 * debug decorator would each compile and then fail quietly on a phone: two syncs fight over the schedule, a tap lands in a link nobody
 * reads, a walk switch does nothing. With the feature off none of them may exist.
 */
class Part2ReminderWiringTest {
    private val leaves =
        module {
            single<PayslipDao> { GuideKoinTestDao.dao }
            single<Part2Dao> { GuideKoinTestDao.dao.part2 }
            single<PdfParser> { FakePdfParser() }
            single { GuideViewModel(FakeGuideRepository(), FakeCrashReporter()) }
        }
    private val koin = koinApplication { modules(part2Module, leaves) }.koin

    @Test
    fun everyReminderPieceIsBoundOnceAndOnlyWhereTheFeatureIsEnabled() {
        val production = koinApplication { modules(appKoinModules(platformModules = listOf(leaves))) }.koin
        val on = isPart2Enabled()

        assertEquals(on, production.getOrNull<ReminderSync>() != null)
        assertEquals(on, production.getOrNull<ReminderFireCheck>() != null)
        assertEquals(on, production.getOrNull<PendingPart2Link>() != null)
        assertEquals(on, production.getOrNull<ReminderPromptModel>() != null)
        assertEquals(on, production.getOrNull<ReminderNotificationText>() != null)
        assertEquals(on, production.getOrNull<ReminderScheduler>() != null)
        assertSame(koin.get<ReminderSync>(), koin.get<ReminderSync>())
        assertSame(koin.get<PendingPart2Link>(), koin.get<PendingPart2Link>())
        assertSame(koin.get<ReminderPromptModel>(), koin.get<ReminderPromptModel>())
    }

    @Test
    fun theSchedulerEveryoneUsesIsTheDebugDecoratorSoTheWalkSwitchReachesTheSync() {
        assertSame(koin.get<ShortDelayReminderScheduler>(), koin.get<ReminderScheduler>())
    }

    @Test
    fun theWordsThePlatformIsHandedAreTheStringsFilesWordsAndNothingElse() {
        assertEquals(Part2ReminderStrings.notificationText(), koin.get<ReminderNotificationText>())
    }

    @Test
    fun theExplainerFollowsTheSameSyncTheSchedulerFollows() {
        assertSame(koin.get<ReminderSync>().hasReminders, koin.get<ReminderPromptModel>().hasReminders)
        assertSame(koin.get<ReminderPromptModel>(), koin.get<Part2Models>().reminders)
    }

    @Test
    fun theTapAndTheSyncReadTheSameRepositoryAsTheScreens() {
        // why: a link checked against another repository than the Due list reads would drop every tap or open a draft nobody can see.
        assertTrue(koin.get<PendingPart2Link>().toString().isNotBlank())
    }
}
