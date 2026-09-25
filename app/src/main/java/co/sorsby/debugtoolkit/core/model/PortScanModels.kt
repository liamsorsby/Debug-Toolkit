package co.sorsby.debugtoolkit.core.model

enum class PortState {
    OPEN,
    CLOSED,
    FILTERED,
}

data class PortScanEntry(
    val port: Int,
    val state: PortState,
)

data class PortScanResult(
    val host: String,
    val entries: List<PortScanEntry>,
    val elapsedMs: Long,
)
