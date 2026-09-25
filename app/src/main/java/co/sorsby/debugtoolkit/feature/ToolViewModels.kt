package co.sorsby.debugtoolkit.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.sorsby.debugtoolkit.core.model.AnalyticsConsent
import co.sorsby.debugtoolkit.core.model.AppSettings
import co.sorsby.debugtoolkit.core.model.NetworkSnapshot
import co.sorsby.debugtoolkit.core.model.PingMode
import co.sorsby.debugtoolkit.core.model.SpeedResult
import co.sorsby.debugtoolkit.core.model.ThemeMode
import co.sorsby.debugtoolkit.core.model.ToolState
import co.sorsby.debugtoolkit.core.model.ToolError
import co.sorsby.debugtoolkit.data.dns.DnsRecordType
import co.sorsby.debugtoolkit.data.dns.DnsRepository
import co.sorsby.debugtoolkit.data.http.HttpInspector
import co.sorsby.debugtoolkit.data.http.HttpMethod
import co.sorsby.debugtoolkit.data.lan.LanScanner
import co.sorsby.debugtoolkit.data.network.NetworkMonitor
import co.sorsby.debugtoolkit.data.ping.PingRunner
import co.sorsby.debugtoolkit.data.ping.TracerouteRunner
import co.sorsby.debugtoolkit.data.portscan.PortListParser
import co.sorsby.debugtoolkit.data.portscan.PortScanner
import co.sorsby.debugtoolkit.data.publicip.PublicIpLookup
import co.sorsby.debugtoolkit.data.settings.SettingsRepository
import co.sorsby.debugtoolkit.data.speed.SpeedTestRepository
import co.sorsby.debugtoolkit.data.tls.TlsInspector
import co.sorsby.debugtoolkit.data.whois.WhoisClient
import co.sorsby.debugtoolkit.telemetry.DiagnosticTool
import co.sorsby.debugtoolkit.telemetry.JourneyTracker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    private val hasLoadedPersistedSettings = MutableStateFlow(false)

    // True once the persisted settings have been read at least once. AppSettings() is only a
    // placeholder shown for the first frame before DataStore replies; callers that need to
    // distinguish "real value" from "default" (for example, holding the splash screen open so a
    // returning user's saved consent choice never flashes the first-launch gate) should wait on
    // this before acting on `settings`.
    val isReady: StateFlow<Boolean> = hasLoadedPersistedSettings.asStateFlow()

    val settings: StateFlow<AppSettings> = repository.settings
        .onEach { hasLoadedPersistedSettings.value = true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSettings(),
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setConsent(consent: AnalyticsConsent) {
        viewModelScope.launch { repository.setAnalyticsConsent(consent) }
    }

    fun acceptCloudflareDisclosure() {
        viewModelScope.launch { repository.acceptCloudflareDisclosure() }
    }
}

class NetworkViewModel(monitor: NetworkMonitor) : ViewModel() {
    val snapshot: StateFlow<NetworkSnapshot> = monitor.snapshots.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NetworkSnapshot(),
    )
}

class SpeedViewModel(
    private val repository: SpeedTestRepository,
    private val journeyTracker: JourneyTracker,
) : ViewModel() {
    private val mutableState = MutableStateFlow<ToolState<SpeedResult>>(ToolState.Idle)
    val state = mutableState.asStateFlow()

    fun runTest() = runTool(
        tool = DiagnosticTool.SPEED,
        journeyTracker = journeyTracker,
        update = { mutableState.value = it },
        action = repository::run,
    )
}

class DnsViewModel(
    private val repository: DnsRepository,
    private val journeyTracker: JourneyTracker,
) : ViewModel() {
    private val mutableState = MutableStateFlow(DnsUiState())
    val state = mutableState.asStateFlow()

    fun setInput(value: String) {
        mutableState.value = mutableState.value.copy(input = value)
    }

    fun setType(value: DnsRecordType) {
        mutableState.value = mutableState.value.copy(type = value)
    }

    fun setNameserver(value: String) {
        mutableState.value = mutableState.value.copy(nameserver = value)
    }

    fun query() = runTool(
        tool = DiagnosticTool.DNS,
        journeyTracker = journeyTracker,
        update = { mutableState.value = mutableState.value.copy(result = it) },
        action = {
            repository.query(
                input = mutableState.value.input,
                type = mutableState.value.type,
                nameserver = mutableState.value.nameserver.ifBlank { null },
            )
        },
    )
}

class TlsViewModel(
    private val inspector: TlsInspector,
    private val journeyTracker: JourneyTracker,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TlsUiState())
    val state = mutableState.asStateFlow()

    fun setInput(value: String) {
        mutableState.value = mutableState.value.copy(input = value)
    }

    fun inspect() = runTool(
        tool = DiagnosticTool.TLS,
        journeyTracker = journeyTracker,
        update = { mutableState.value = mutableState.value.copy(result = it) },
        action = { inspector.inspect(mutableState.value.input) },
    )
}

class HttpViewModel(
    private val inspector: HttpInspector,
    private val journeyTracker: JourneyTracker,
) : ViewModel() {
    private val mutableState = MutableStateFlow(HttpUiState())
    val state = mutableState.asStateFlow()

    fun setInput(value: String) {
        mutableState.value = mutableState.value.copy(input = value)
    }

    fun setMethod(value: HttpMethod) {
        mutableState.value = mutableState.value.copy(method = value)
    }

    fun setShowResponseBody(value: Boolean) {
        mutableState.value = mutableState.value.copy(showResponseBody = value)
    }

    fun inspect() = runTool(
        tool = DiagnosticTool.HTTP,
        journeyTracker = journeyTracker,
        update = { mutableState.value = mutableState.value.copy(result = it) },
        action = { inspector.inspect(mutableState.value.input, mutableState.value.method) },
    )
}

class PingViewModel(
    private val pingRunner: PingRunner,
    private val tracerouteRunner: TracerouteRunner,
    private val journeyTracker: JourneyTracker,
) : ViewModel() {
    private val mutableState = MutableStateFlow(PingUiState())
    val state = mutableState.asStateFlow()

    fun setInput(value: String) {
        mutableState.value = mutableState.value.copy(input = value)
    }

    fun setMode(value: PingMode) {
        mutableState.value = mutableState.value.copy(mode = value)
    }

    fun run() = when (mutableState.value.mode) {
        PingMode.PING -> runTool(
            tool = DiagnosticTool.PING,
            journeyTracker = journeyTracker,
            update = { mutableState.value = mutableState.value.copy(pingResult = it) },
            action = { pingRunner.ping(mutableState.value.input, count = PING_PROBE_COUNT) },
        )
        PingMode.TRACEROUTE -> runTool(
            tool = DiagnosticTool.TRACEROUTE,
            journeyTracker = journeyTracker,
            update = { mutableState.value = mutableState.value.copy(tracerouteResult = it) },
            action = { tracerouteRunner.traceroute(mutableState.value.input, maxHops = TRACEROUTE_MAX_HOPS) },
        )
    }

    private companion object {
        const val PING_PROBE_COUNT = 5
        const val TRACEROUTE_MAX_HOPS = 20
    }
}

class PortScanViewModel(
    private val scanner: PortScanner,
    private val journeyTracker: JourneyTracker,
) : ViewModel() {
    private val mutableState = MutableStateFlow(PortScanUiState())
    val state = mutableState.asStateFlow()

    fun setHost(value: String) {
        mutableState.value = mutableState.value.copy(host = value)
    }

    fun setPorts(value: String) {
        mutableState.value = mutableState.value.copy(ports = value)
    }

    fun useCommonPorts() {
        setPorts(PortListParser.COMMON_PORTS.joinToString(","))
    }

    fun scan() = runTool(
        tool = DiagnosticTool.PORT_SCAN,
        journeyTracker = journeyTracker,
        update = { mutableState.value = mutableState.value.copy(result = it) },
        action = {
            val ports = PortListParser.parse(mutableState.value.ports)
            scanner.scan(mutableState.value.host, ports)
        },
    )
}

class WhoisViewModel(
    private val client: WhoisClient,
    private val journeyTracker: JourneyTracker,
) : ViewModel() {
    private val mutableState = MutableStateFlow(WhoisUiState())
    val state = mutableState.asStateFlow()

    fun setInput(value: String) {
        mutableState.value = mutableState.value.copy(input = value)
    }

    fun lookup() = runTool(
        tool = DiagnosticTool.WHOIS,
        journeyTracker = journeyTracker,
        update = { mutableState.value = mutableState.value.copy(result = it) },
        action = { client.lookup(mutableState.value.input) },
    )
}

class PublicIpViewModel(
    private val lookup: PublicIpLookup,
    private val journeyTracker: JourneyTracker,
) : ViewModel() {
    private val mutableState = MutableStateFlow(PublicIpUiState())
    val state = mutableState.asStateFlow()

    fun lookup() = runTool(
        tool = DiagnosticTool.PUBLIC_IP,
        journeyTracker = journeyTracker,
        update = { mutableState.value = mutableState.value.copy(result = it) },
        action = { lookup.lookup() },
    )
}

class LanScanViewModel(
    private val scanner: LanScanner,
    private val journeyTracker: JourneyTracker,
) : ViewModel() {
    private val mutableState = MutableStateFlow(LanScanUiState())
    val state = mutableState.asStateFlow()

    fun scan() = runTool(
        tool = DiagnosticTool.LAN_SCAN,
        journeyTracker = journeyTracker,
        update = { mutableState.value = mutableState.value.copy(result = it) },
        action = { scanner.scan() },
    )
}

private fun <T> ViewModel.runTool(
    tool: DiagnosticTool,
    journeyTracker: JourneyTracker,
    update: (ToolState<T>) -> Unit,
    action: suspend () -> T,
) {
    viewModelScope.launch {
        val journey = journeyTracker.startTool(tool)
        update(ToolState.Loading)
        try {
            update(ToolState.Success(action()))
            journey.succeed()
        } catch (cancellation: CancellationException) {
            journey.cancel()
            throw cancellation
        } catch (error: Exception) {
            val toolError = when (error) {
                is IllegalArgumentException -> ToolError.INVALID_INPUT
                is IOException -> ToolError.NETWORK
                is IllegalStateException -> ToolError.SERVICE
                else -> ToolError.UNKNOWN
            }
            update(
                ToolState.Error(toolError),
            )
            journey.fail(toolError, error)
        }
    }
}
