package co.sorsby.debugtoolkit.feature

import co.sorsby.debugtoolkit.core.model.AnalyticsConsent
import co.sorsby.debugtoolkit.core.model.AppSettings
import co.sorsby.debugtoolkit.core.model.DnsResult
import co.sorsby.debugtoolkit.core.model.HttpInspection
import co.sorsby.debugtoolkit.core.model.NetworkSnapshot
import co.sorsby.debugtoolkit.core.model.SpeedResult
import co.sorsby.debugtoolkit.core.model.ThemeMode
import co.sorsby.debugtoolkit.core.model.TlsResult
import co.sorsby.debugtoolkit.core.model.ToolState
import co.sorsby.debugtoolkit.core.model.ToolError
import co.sorsby.debugtoolkit.data.dns.DnsRecordType
import co.sorsby.debugtoolkit.data.dns.DnsRepository
import co.sorsby.debugtoolkit.data.http.HttpInspector
import co.sorsby.debugtoolkit.data.http.HttpMethod
import co.sorsby.debugtoolkit.data.network.NetworkMonitor
import co.sorsby.debugtoolkit.data.settings.SettingsRepository
import co.sorsby.debugtoolkit.data.speed.SpeedTestRepository
import co.sorsby.debugtoolkit.data.tls.TlsInspector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ToolViewModelsTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `settings mutations are delegated and observed`() = runTest(dispatcher) {
        val repository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(repository)
        val collection = backgroundScope.launch { viewModel.settings.collect {} }

        viewModel.setThemeMode(ThemeMode.DARK)
        viewModel.setConsent(AnalyticsConsent.GRANTED)
        viewModel.acceptCloudflareDisclosure()
        advanceUntilIdle()

        assertEquals(
            AppSettings(ThemeMode.DARK, AnalyticsConsent.GRANTED, true),
            viewModel.settings.value,
        )
        collection.cancel()
    }

    @Test
    fun `network snapshot is observed`() = runTest(dispatcher) {
        val snapshots = MutableStateFlow(NetworkSnapshot())
        val viewModel = NetworkViewModel(object : NetworkMonitor {
            override val snapshots = snapshots
        })
        val collection = backgroundScope.launch { viewModel.snapshot.collect {} }
        val expected = NetworkSnapshot(connected = true, validated = true)

        snapshots.value = expected
        advanceUntilIdle()

        assertEquals(expected, viewModel.snapshot.value)
        collection.cancel()
    }

    @Test
    fun `speed test exposes success and errors`() = runTest(dispatcher) {
        val result = SpeedResult(1.0, 2.0, 3.0, 4.0, 5, Instant.EPOCH)
        val success = SpeedViewModel(object : SpeedTestRepository {
            override suspend fun run() = result
        })
        success.runTest()
        advanceUntilIdle()
        assertEquals(ToolState.Success(result), success.state.value)

        val failure = SpeedViewModel(object : SpeedTestRepository {
            override suspend fun run(): SpeedResult = throw IllegalStateException("failed")
        })
        failure.runTest()
        advanceUntilIdle()
        assertEquals(ToolState.Error(ToolError.SERVICE), failure.state.value)

        val invalidInput = SpeedViewModel(object : SpeedTestRepository {
            override suspend fun run(): SpeedResult = throw IllegalArgumentException()
        })
        invalidInput.runTest()
        advanceUntilIdle()
        assertEquals(ToolState.Error(ToolError.INVALID_INPUT), invalidInput.state.value)

        val networkFailure = SpeedViewModel(object : SpeedTestRepository {
            override suspend fun run(): SpeedResult = throw IOException()
        })
        networkFailure.runTest()
        advanceUntilIdle()
        assertEquals(ToolState.Error(ToolError.NETWORK), networkFailure.state.value)
    }

    @Test
    fun `DNS state captures input type success and fallback error`() = runTest(dispatcher) {
        val result = DnsResult(0, true, true, emptyList(), emptyList(), emptyList(), 1)
        val success = DnsViewModel(object : DnsRepository {
            override suspend fun query(input: String, type: DnsRecordType): DnsResult {
                assertEquals("example.com", input)
                assertEquals(DnsRecordType.MX, type)
                return result
            }
        })
        success.setInput("example.com")
        success.setType(DnsRecordType.MX)
        success.query()
        advanceUntilIdle()
        assertEquals(ToolState.Success(result), success.state.value.result)

        val failure = DnsViewModel(object : DnsRepository {
            override suspend fun query(input: String, type: DnsRecordType): DnsResult =
                throw Exception()
        })
        failure.query()
        advanceUntilIdle()
        assertEquals(ToolState.Error(ToolError.UNKNOWN), failure.state.value.result)
    }

    @Test
    fun `TLS state captures input and success`() = runTest(dispatcher) {
        val result = TlsResult("example.com", 443, "TLSv1.3", "cipher", emptyList(), 1)
        val viewModel = TlsViewModel(object : TlsInspector {
            override suspend fun inspect(input: String): TlsResult {
                assertEquals("example.com", input)
                return result
            }
        })
        viewModel.setInput("example.com")
        viewModel.inspect()
        advanceUntilIdle()
        assertEquals(ToolState.Success(result), viewModel.state.value.result)
    }

    @Test
    fun `HTTP state captures input method and success`() = runTest(dispatcher) {
        val result = HttpInspection(
            200, "OK", "h2", "https://example.com", 1,
            emptyList(), emptyList(), null, false, null,
        )
        val viewModel = HttpViewModel(object : HttpInspector {
            override suspend fun inspect(input: String, method: HttpMethod): HttpInspection {
                assertEquals("example.com", input)
                assertEquals(HttpMethod.HEAD, method)
                return result
            }
        })
        viewModel.setInput("example.com")
        viewModel.setMethod(HttpMethod.HEAD)
        viewModel.inspect()
        advanceUntilIdle()
        assertEquals(ToolState.Success(result), viewModel.state.value.result)
    }
}

private class FakeSettingsRepository : SettingsRepository {
    private val mutableSettings = MutableStateFlow(AppSettings())
    override val settings = mutableSettings

    override suspend fun setThemeMode(mode: ThemeMode) {
        mutableSettings.value = mutableSettings.value.copy(themeMode = mode)
    }

    override suspend fun setAnalyticsConsent(consent: AnalyticsConsent) {
        mutableSettings.value = mutableSettings.value.copy(analyticsConsent = consent)
    }

    override suspend fun acceptCloudflareDisclosure() {
        mutableSettings.value = mutableSettings.value.copy(cloudflareDisclosureAccepted = true)
    }
}
