package co.sorsby.debugtoolkit.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val settings: StateFlow<AppSettings> = repository.settings.stateIn(
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

class SpeedViewModel(private val repository: SpeedTestRepository) : ViewModel() {
    private val mutableState = MutableStateFlow<ToolState<SpeedResult>>(ToolState.Idle)
    val state = mutableState.asStateFlow()

    fun runTest() = runTool(
        update = { mutableState.value = it },
        action = repository::run,
    )
}

class DnsViewModel(private val repository: DnsRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(DnsUiState())
    val state = mutableState.asStateFlow()

    fun setInput(value: String) {
        mutableState.value = mutableState.value.copy(input = value)
    }

    fun setType(value: DnsRecordType) {
        mutableState.value = mutableState.value.copy(type = value)
    }

    fun query() = runTool(
        update = { mutableState.value = mutableState.value.copy(result = it) },
        action = { repository.query(mutableState.value.input, mutableState.value.type) },
    )
}

class TlsViewModel(private val inspector: TlsInspector) : ViewModel() {
    private val mutableState = MutableStateFlow(TlsUiState())
    val state = mutableState.asStateFlow()

    fun setInput(value: String) {
        mutableState.value = mutableState.value.copy(input = value)
    }

    fun inspect() = runTool(
        update = { mutableState.value = mutableState.value.copy(result = it) },
        action = { inspector.inspect(mutableState.value.input) },
    )
}

class HttpViewModel(private val inspector: HttpInspector) : ViewModel() {
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
        update = { mutableState.value = mutableState.value.copy(result = it) },
        action = { inspector.inspect(mutableState.value.input, mutableState.value.method) },
    )
}

private fun <T> ViewModel.runTool(
    update: (ToolState<T>) -> Unit,
    action: suspend () -> T,
) {
    viewModelScope.launch {
        update(ToolState.Loading)
        try {
            update(ToolState.Success(action()))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            update(
                ToolState.Error(
                    when (error) {
                        is IllegalArgumentException -> ToolError.INVALID_INPUT
                        is IOException -> ToolError.NETWORK
                        is IllegalStateException -> ToolError.SERVICE
                        else -> ToolError.UNKNOWN
                    },
                ),
            )
        }
    }
}
