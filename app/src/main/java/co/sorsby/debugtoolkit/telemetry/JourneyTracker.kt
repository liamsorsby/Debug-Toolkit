package co.sorsby.debugtoolkit.telemetry

import co.sorsby.debugtoolkit.core.model.ToolError

interface JourneyTracker {
    fun trackScreen(route: String)

    /** A move between two screens, so funnels and drop-off can be analysed. */
    fun trackNavigation(from: String?, to: String, source: String) = Unit

    /** A user action such as a tap. Use short snake_case names and never include user input. */
    fun trackAction(action: String, screen: String? = null) = Unit

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
    WHOIS("whois"),
    PUBLIC_IP("publicip"),
    LAN_SCAN("lanscan"),
}
