package co.sorsby.debugtoolkit.feature

import co.sorsby.debugtoolkit.core.model.DnsResult
import co.sorsby.debugtoolkit.core.model.HttpInspection
import co.sorsby.debugtoolkit.core.model.TlsResult
import co.sorsby.debugtoolkit.core.model.ToolState
import co.sorsby.debugtoolkit.data.dns.DnsRecordType
import co.sorsby.debugtoolkit.data.http.HttpMethod

data class DnsUiState(
    val input: String = "",
    val type: DnsRecordType = DnsRecordType.A,
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
