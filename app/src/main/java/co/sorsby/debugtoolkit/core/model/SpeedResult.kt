package co.sorsby.debugtoolkit.core.model

import java.time.Instant

data class SpeedResult(
    val latencyMs: Double,
    val jitterMs: Double,
    val downloadMbps: Double,
    val uploadMbps: Double,
    val transferredBytes: Long,
    val measuredAt: Instant,
)
