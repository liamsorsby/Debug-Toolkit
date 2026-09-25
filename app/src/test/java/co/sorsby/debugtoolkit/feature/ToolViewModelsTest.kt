package co.sorsby.debugtoolkit.feature

import co.sorsby.debugtoolkit.core.model.AnalyticsConsent
import co.sorsby.debugtoolkit.core.model.AppSettings
import co.sorsby.debugtoolkit.core.model.DnsResult
import co.sorsby.debugtoolkit.core.model.HttpInspection
import co.sorsby.debugtoolkit.core.model.LanDevice
import co.sorsby.debugtoolkit.core.model.LanScanResult
import co.sorsby.debugtoolkit.core.model.NetworkSnapshot
import co.sorsby.debugtoolkit.core.model.PingMode
import co.sorsby.debugtoolkit.core.model.PingProbe
import co.sorsby.debugtoolkit.core.model.PingResult
import co.sorsby.debugtoolkit.core.model.PortScanEntry
import co.sorsby.debugtoolkit.core.model.PortScanResult
import co.sorsby.debugtoolkit.core.model.PortState
import co.sorsby.debugtoolkit.core.model.PublicIpResult
import co.sorsby.debugtoolkit.core.model.SpeedResult
import co.sorsby.debugtoolkit.core.model.ThemeMode
import co.sorsby.debugtoolkit.core.model.TlsResult
import co.sorsby.debugtoolkit.core.model.ToolState
import co.sorsby.debugtoolkit.core.model.ToolError
import co.sorsby.debugtoolkit.core.model.TracerouteHop
import co.sorsby.debugtoolkit.core.model.TracerouteResult
import co.sorsby.debugtoolkit.core.model.WhoisResult
import co.sorsby.debugtoolkit.data.dns.DnsRecordType
import co.sorsby.debugtoolkit.data.dns.DnsRepository
import co.sorsby.debugtoolkit.data.http.HttpInspector
import co.sorsby.debugtoolkit.data.http.HttpMethod
import co.sorsby.debugtoolkit.data.lan.LanScanner
import co.sorsby.debugtoolkit.data.network.NetworkMonitor
import co.sorsby.debugtoolkit.data.ping.PingRunner
import co.sorsby.debugtoolkit.data.ping.TracerouteRunner
import co.sorsby.debugtoolkit.data.portscan.PortScanner
import co.sorsby.debugtoolkit.data.publicip.PublicIpLookup
import co.sorsby.debugtoolkit.data.settings.SettingsRepository
import co.sorsby.debugtoolkit.data.speed.SpeedTestRepository
import co.sorsby.debugtoolkit.data.tls.TlsInspector
import co.sorsby.debugtoolkit.data.whois.WhoisClient
import co.sorsby.debugtoolkit.telemetry.DiagnosticTool
import co.sorsby.debugtoolkit.telemetry.JourneyTracker
import co.sorsby.debugtoolkit.telemetry.ToolJourney
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
    private lateinit var journeyTracker: RecordingJourneyTracker

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        journeyTracker = RecordingJourneyTracker()
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
        }, journeyTracker)
        success.runTest()
        advanceUntilIdle()
        assertEquals(ToolState.Success(result), success.state.value)

        val failure = SpeedViewModel(object : SpeedTestRepository {
            override suspend fun run(): SpeedResult = throw IllegalStateException("failed")
        }, journeyTracker)
        failure.runTest()
        advanceUntilIdle()
        assertEquals(ToolState.Error(ToolError.SERVICE), failure.state.value)

        val invalidInput = SpeedViewModel(object : SpeedTestRepository {
            override suspend fun run(): SpeedResult = throw IllegalArgumentException()
        }, journeyTracker)
        invalidInput.runTest()
        advanceUntilIdle()
        assertEquals(ToolState.Error(ToolError.INVALID_INPUT), invalidInput.state.value)

        val networkFailure = SpeedViewModel(object : SpeedTestRepository {
            override suspend fun run(): SpeedResult = throw IOException()
        }, journeyTracker)
        networkFailure.runTest()
        advanceUntilIdle()
        assertEquals(ToolState.Error(ToolError.NETWORK), networkFailure.state.value)
        assertEquals(List(4) { DiagnosticTool.SPEED }, journeyTracker.startedTools)
        assertEquals(
            listOf("success", "SERVICE", "INVALID_INPUT", "NETWORK"),
            journeyTracker.outcomes,
        )
    }

    @Test
    fun `DNS state captures input type success and fallback error`() = runTest(dispatcher) {
        val result = DnsResult(0, true, true, emptyList(), emptyList(), emptyList(), 1)
        val success = DnsViewModel(object : DnsRepository {
            override suspend fun query(input: String, type: DnsRecordType, nameserver: String?): DnsResult {
                assertEquals("example.com", input)
                assertEquals(DnsRecordType.MX, type)
                assertEquals(null, nameserver)
                return result
            }
        }, journeyTracker)
        success.setInput("example.com")
        success.setType(DnsRecordType.MX)
        success.query()
        advanceUntilIdle()
        assertEquals(ToolState.Success(result), success.state.value.result)

        val failure = DnsViewModel(object : DnsRepository {
            override suspend fun query(input: String, type: DnsRecordType, nameserver: String?): DnsResult =
                throw Exception()
        }, journeyTracker)
        failure.query()
        advanceUntilIdle()
        assertEquals(ToolState.Error(ToolError.UNKNOWN), failure.state.value.result)
        assertEquals(listOf(DiagnosticTool.DNS, DiagnosticTool.DNS), journeyTracker.startedTools)
        assertEquals(listOf("success", "UNKNOWN"), journeyTracker.outcomes)
    }

    @Test
    fun `TLS state captures input and success`() = runTest(dispatcher) {
        val result = TlsResult("example.com", 443, "TLSv1.3", "cipher", emptyList(), 1)
        val viewModel = TlsViewModel(object : TlsInspector {
            override suspend fun inspect(input: String): TlsResult {
                assertEquals("example.com", input)
                return result
            }
        }, journeyTracker)
        viewModel.setInput("example.com")
        viewModel.inspect()
        advanceUntilIdle()
        assertEquals(ToolState.Success(result), viewModel.state.value.result)
        assertEquals(listOf(DiagnosticTool.TLS), journeyTracker.startedTools)
        assertEquals(listOf("success"), journeyTracker.outcomes)
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
        }, journeyTracker)
        viewModel.setInput("example.com")
        viewModel.setMethod(HttpMethod.HEAD)
        assertEquals(false, viewModel.state.value.showResponseBody)
        viewModel.setShowResponseBody(true)
        viewModel.inspect()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.showResponseBody)
        assertEquals(ToolState.Success(result), viewModel.state.value.result)
        assertEquals(listOf(DiagnosticTool.HTTP), journeyTracker.startedTools)
        assertEquals(listOf("success"), journeyTracker.outcomes)
    }

    @Test
    fun `ping state runs the mode-appropriate action and records the outcome`() = runTest(dispatcher) {
        val pingResult = PingResult(
            host = "example.com",
            transmitted = 1,
            received = 1,
            packetLossPercent = 0.0,
            probes = listOf(PingProbe(1, 12.0)),
            minMs = 12.0,
            avgMs = 12.0,
            maxMs = 12.0,
            jitterMs = 0.0,
        )
        val tracerouteResult = TracerouteResult(
            host = "example.com",
            hops = listOf(TracerouteHop(1, "93.184.216.34", 12.0)),
            reachedDestination = true,
        )
        val viewModel = PingViewModel(
            pingRunner = object : PingRunner {
                override suspend fun ping(host: String, count: Int): PingResult {
                    assertEquals("example.com", host)
                    return pingResult
                }
            },
            tracerouteRunner = object : TracerouteRunner {
                override suspend fun traceroute(host: String, maxHops: Int): TracerouteResult {
                    assertEquals("example.com", host)
                    return tracerouteResult
                }
            },
            journeyTracker = journeyTracker,
        )
        viewModel.setInput("example.com")

        viewModel.run()
        advanceUntilIdle()
        assertEquals(ToolState.Success(pingResult), viewModel.state.value.pingResult)
        assertEquals(ToolState.Idle, viewModel.state.value.tracerouteResult)

        viewModel.setMode(PingMode.TRACEROUTE)
        viewModel.run()
        advanceUntilIdle()
        assertEquals(ToolState.Success(tracerouteResult), viewModel.state.value.tracerouteResult)
        assertEquals(listOf(DiagnosticTool.PING, DiagnosticTool.TRACEROUTE), journeyTracker.startedTools)
        assertEquals(listOf("success", "success"), journeyTracker.outcomes)
    }

    @Test
    fun `ping state maps a failure to a tool error`() = runTest(dispatcher) {
        val viewModel = PingViewModel(
            pingRunner = object : PingRunner {
                override suspend fun ping(host: String, count: Int): PingResult =
                    throw IllegalArgumentException("bad host")
            },
            tracerouteRunner = object : TracerouteRunner {
                override suspend fun traceroute(host: String, maxHops: Int): TracerouteResult =
                    throw IllegalStateException("unavailable")
            },
            journeyTracker = journeyTracker,
        )

        viewModel.run()
        advanceUntilIdle()
        assertEquals(ToolState.Error(ToolError.INVALID_INPUT), viewModel.state.value.pingResult)
    }

    @Test
    fun `port scan parses ports, scans, and exposes the result`() = runTest(dispatcher) {
        val result = PortScanResult(
            host = "example.com",
            entries = listOf(PortScanEntry(80, PortState.OPEN), PortScanEntry(22, PortState.FILTERED)),
            elapsedMs = 120,
        )
        val viewModel = PortScanViewModel(
            scanner = object : PortScanner {
                override suspend fun scan(host: String, ports: List<Int>): PortScanResult {
                    assertEquals("example.com", host)
                    assertEquals(listOf(22, 80), ports)
                    return result
                }
            },
            journeyTracker = journeyTracker,
        )
        viewModel.setHost("example.com")
        viewModel.setPorts("80,22")

        viewModel.scan()
        advanceUntilIdle()

        assertEquals(ToolState.Success(result), viewModel.state.value.result)
        assertEquals(listOf(DiagnosticTool.PORT_SCAN), journeyTracker.startedTools)
        assertEquals(listOf("success"), journeyTracker.outcomes)
    }

    @Test
    fun `port scan surfaces an invalid port list as invalid input`() = runTest(dispatcher) {
        val viewModel = PortScanViewModel(
            scanner = object : PortScanner {
                override suspend fun scan(host: String, ports: List<Int>): PortScanResult =
                    throw AssertionError("Should not scan with an invalid port list.")
            },
            journeyTracker = journeyTracker,
        )
        viewModel.setHost("example.com")
        viewModel.setPorts("not-a-port")

        viewModel.scan()
        advanceUntilIdle()

        assertEquals(ToolState.Error(ToolError.INVALID_INPUT), viewModel.state.value.result)
    }

    @Test
    fun `port scan use common ports fills the preset list`() {
        val viewModel = PortScanViewModel(
            scanner = object : PortScanner {
                override suspend fun scan(host: String, ports: List<Int>): PortScanResult =
                    throw AssertionError("Not used in this test.")
            },
            journeyTracker = journeyTracker,
        )

        viewModel.useCommonPorts()

        assertEquals(
            co.sorsby.debugtoolkit.data.portscan.PortListParser.COMMON_PORTS.joinToString(","),
            viewModel.state.value.ports,
        )
    }

    @Test
    fun `whois looks up a domain and exposes the result`() = runTest(dispatcher) {
        val result = WhoisResult(
            domain = "example.com",
            server = "whois.verisign-grs.com",
            rawText = "domain: EXAMPLE.COM",
            elapsedMs = 90,
        )
        val viewModel = WhoisViewModel(
            client = object : WhoisClient {
                override suspend fun lookup(domain: String): WhoisResult {
                    assertEquals("example.com", domain)
                    return result
                }
            },
            journeyTracker = journeyTracker,
        )
        viewModel.setInput("example.com")

        viewModel.lookup()
        advanceUntilIdle()

        assertEquals(ToolState.Success(result), viewModel.state.value.result)
        assertEquals(listOf(DiagnosticTool.WHOIS), journeyTracker.startedTools)
        assertEquals(listOf("success"), journeyTracker.outcomes)
    }

    @Test
    fun `whois surfaces an invalid domain as invalid input`() = runTest(dispatcher) {
        val viewModel = WhoisViewModel(
            client = object : WhoisClient {
                override suspend fun lookup(domain: String): WhoisResult =
                    throw IllegalArgumentException("Enter a valid domain name.")
            },
            journeyTracker = journeyTracker,
        )
        viewModel.setInput("not a domain")

        viewModel.lookup()
        advanceUntilIdle()

        assertEquals(ToolState.Error(ToolError.INVALID_INPUT), viewModel.state.value.result)
    }

    @Test
    fun `public ip looks up and exposes the result`() = runTest(dispatcher) {
        val result = PublicIpResult(
            ipAddress = "203.0.113.10",
            countryCode = "GB",
            cloudflareColo = "LHR",
            warpEnabled = false,
            elapsedMs = 60,
        )
        val viewModel = PublicIpViewModel(
            lookup = object : PublicIpLookup {
                override suspend fun lookup(): PublicIpResult = result
            },
            journeyTracker = journeyTracker,
        )

        viewModel.lookup()
        advanceUntilIdle()

        assertEquals(ToolState.Success(result), viewModel.state.value.result)
        assertEquals(listOf(DiagnosticTool.PUBLIC_IP), journeyTracker.startedTools)
        assertEquals(listOf("success"), journeyTracker.outcomes)
    }

    @Test
    fun `public ip surfaces a service failure`() = runTest(dispatcher) {
        val viewModel = PublicIpViewModel(
            lookup = object : PublicIpLookup {
                override suspend fun lookup(): PublicIpResult =
                    throw IllegalStateException("Trace service returned HTTP 500.")
            },
            journeyTracker = journeyTracker,
        )

        viewModel.lookup()
        advanceUntilIdle()

        assertEquals(ToolState.Error(ToolError.SERVICE), viewModel.state.value.result)
    }

    @Test
    fun `lan scan sweeps the subnet and exposes the result`() = runTest(dispatcher) {
        val result = LanScanResult(
            subnetCidr = "192.168.1.0/24",
            devices = listOf(LanDevice("192.168.1.2", "router.local", 4)),
            addressesScanned = 253,
            elapsedMs = 4_000,
        )
        val viewModel = LanScanViewModel(
            scanner = object : LanScanner {
                override suspend fun scan(): LanScanResult = result
            },
            journeyTracker = journeyTracker,
        )

        viewModel.scan()
        advanceUntilIdle()

        assertEquals(ToolState.Success(result), viewModel.state.value.result)
        assertEquals(listOf(DiagnosticTool.LAN_SCAN), journeyTracker.startedTools)
        assertEquals(listOf("success"), journeyTracker.outcomes)
    }

    @Test
    fun `lan scan surfaces missing connectivity as a service error`() = runTest(dispatcher) {
        val viewModel = LanScanViewModel(
            scanner = object : LanScanner {
                override suspend fun scan(): LanScanResult =
                    throw IllegalStateException("Connect to a Wi-Fi or Ethernet network.")
            },
            journeyTracker = journeyTracker,
        )

        viewModel.scan()
        advanceUntilIdle()

        assertEquals(ToolState.Error(ToolError.SERVICE), viewModel.state.value.result)
    }
}

private class RecordingJourneyTracker : JourneyTracker {
    val startedTools = mutableListOf<DiagnosticTool>()
    val outcomes = mutableListOf<String>()

    override fun trackScreen(route: String) = Unit

    override fun startTool(tool: DiagnosticTool): ToolJourney {
        startedTools += tool
        return object : ToolJourney {
            override fun succeed() {
                outcomes += "success"
            }

            override fun fail(error: ToolError, cause: Exception) {
                outcomes += error.name
            }

            override fun cancel() {
                outcomes += "cancelled"
            }
        }
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
