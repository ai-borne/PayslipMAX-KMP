package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.insights.gemma.BaseModelInstallState
import com.payslipmax.pdfparser.insights.gemma.GemmaBaseModelInstaller
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.telemetry.GemmaInstallTelemetry
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FakeGemmaBaseModelInstaller : GemmaBaseModelInstaller {
    private val _state = MutableStateFlow<BaseModelInstallState>(BaseModelInstallState.NotStarted)
    override val state: StateFlow<BaseModelInstallState> = _state.asStateFlow()
    var installCalled = false

    override suspend fun install() {
        installCalled = true
    }

    fun emit(state: BaseModelInstallState) {
        _state.value = state
    }
}

class FakeTelemetry : GemmaInstallTelemetry {
    var isEnabled = true
    val trackedStates = mutableListOf<BaseModelInstallState>()
    val loggedEvents = mutableListOf<Pair<String, Map<String, String>?>>()

    override fun setTelemetryEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    override fun logEvent(
        name: String,
        params: Map<String, String>?,
    ) {
        loggedEvents.add(Pair(name, params))
    }

    override fun trackInstallState(state: BaseModelInstallState) {
        trackedStates.add(state)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class GemmaInstallTelemetryIntegrationTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeDao: FakePayslipDao
    private lateinit var fakeBackupManager: FakeBackupManager
    private lateinit var repository: PayslipRepository
    private lateinit var fakeInstaller: FakeGemmaBaseModelInstaller
    private lateinit var fakeTelemetry: FakeTelemetry

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakePayslipDao()
        fakeBackupManager = FakeBackupManager()
        repository = PayslipRepository(fakeDao, FakePdfParser(), Dispatchers.Unconfined)
        fakeInstaller = FakeGemmaBaseModelInstaller()
        fakeTelemetry = FakeTelemetry()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() =
        PayslipViewModel(
            repository = repository,
            backupManager = fakeBackupManager,
            gemmaBaseModelInstaller = fakeInstaller,
            gemmaInstallTelemetry = fakeTelemetry,
        )

    @Test
    fun testTelemetryTogglePersistsAndPropagates() =
        runTest {
            val vm = createViewModel()
            advanceUntilIdle()

            // Default should be true
            assertTrue(vm.uiState.value.isTelemetryEnabled)
            assertTrue(fakeTelemetry.isEnabled)

            // Turn OFF
            vm.setTelemetryEnabled(false)
            advanceUntilIdle()

            assertFalse(vm.uiState.value.isTelemetryEnabled)
            assertFalse(fakeTelemetry.isEnabled)

            // Turn ON
            vm.setTelemetryEnabled(true)
            advanceUntilIdle()

            assertTrue(vm.uiState.value.isTelemetryEnabled)
            assertTrue(fakeTelemetry.isEnabled)
        }

    @Test
    fun testInstallStatePropagationToTelemetry() =
        runTest {
            val vm = createViewModel()
            advanceUntilIdle()

            // Emit downloading state
            fakeInstaller.emit(BaseModelInstallState.Downloading(0.25f))
            advanceUntilIdle()

            assertEquals(2, fakeTelemetry.trackedStates.size) // NotStarted is initial state, then Downloading(0.25f)
            assertEquals(BaseModelInstallState.NotStarted, fakeTelemetry.trackedStates[0])
            assertEquals(BaseModelInstallState.Downloading(0.25f), fakeTelemetry.trackedStates[1])
        }
}
