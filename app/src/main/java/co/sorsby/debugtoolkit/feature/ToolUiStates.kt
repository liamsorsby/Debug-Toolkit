package co.sorsby.debugtoolkit.feature

import co.sorsby.debugtoolkit.core.model.DnsResult
import co.sorsby.debugtoolkit.core.model.HttpInspection
import co.sorsby.debugtoolkit.core.model.LanScanResult
import co.sorsby.debugtoolkit.core.model.PingMode
import co.sorsby.debugtoolkit.core.model.PingResult
import co.sorsby.debugtoolkit.core.model.PortScanResult
import co.sorsby.debugtoolkit.core.model.PublicIpResult
import co.sorsby.debugtoolkit.core.model.TlsResult
import co.sorsby.debugtoolkit.core.model.ToolState
import co.sorsby.debugtoolkit.core.model.TracerouteResult
import co.sorsby.debugtoolkit.core.model.WhoisResult
import co.sorsby.debugtoolkit.data.dns.DnsRecordType
import co.sorsby.debugtoolkit.data.http.HttpMethod

data class DnsUiState(
    val input: String = "",
    val type: DnsRecordType = DnsRecordType.A,
    val nameserver: String = "",
    val result: ToolState<DnsResult> = ToolState.Idle,
)

data class TlsUiState(
    val input: String = "",
    val result: ToolState<TlsResult> = ToolState.Idle,
)

data class HttpUiState(
    val input: String = "",
    val method: HttpMethod = HttpMethod.GET,
    val showResponseBody: Boolean = false,
    val result: ToolState<HttpInspection> = ToolState.Idle,
)

data class PingUiState(
    val input: String = "",
    val mode: PingMode = PingMode.PING,
    val pingResult: ToolState<PingResult> = ToolState.Idle,
    val tracerouteResult: ToolState<TracerouteResult> = ToolState.Idle,
)

data class PortScanUiState(
    val host: String = "",
    val ports: String = "",
    val result: ToolState<PortScanResult> = ToolState.Idle,
)

data class WhoisUiState(
    val input: String = "",
    val result: ToolState<WhoisResult> = ToolState.Idle,
)

data class PublicIpUiState(
    val result: ToolState<PublicIpResult> = ToolState.Idle,
)

data class LanScanUiState(
    val result: ToolState<LanScanResult> = ToolState.Idle,
)
