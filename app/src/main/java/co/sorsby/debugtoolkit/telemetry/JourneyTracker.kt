package co.sorsby.debugtoolkit.telemetry

import co.sorsby.debugtoolkit.core.model.ToolError

interface JourneyTracker {
    fun trackScreen(route: String)
    fun startTool(tool: DiagnosticTool): ToolJourney
}

interface ToolJourney {
    fun succeed()
    fun fail(error: ToolError, cause: Exception)
    fun cancel()
}

enum class DiagnosticTool(val eventValue: String) {
    SPEED("speed"),
    DNS("dns"),
    TLS("tls"),
    HTTP("http"),
    PING("ping"),
    TRACEROUTE("traceroute"),
    PORT_SCAN("portscan"),
}
