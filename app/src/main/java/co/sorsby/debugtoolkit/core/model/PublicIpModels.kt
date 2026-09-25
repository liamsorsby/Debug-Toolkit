package co.sorsby.debugtoolkit.core.model

/**
 * Result of a public IP and edge location lookup, sourced from Cloudflare's trace endpoint. This
 * is the address and routing point the rest of the internet sees for this device, which is useful
 * for confirming whether a VPN or proxy is active: [warpEnabled] is true when traffic is going
 * through Cloudflare WARP, and [cloudflareColo] is the nearest Cloudflare edge location that
 * received the request rather than the device's own physical location.
 */
data class PublicIpResult(
    val ipAddress: String,
    val countryCode: String?,
    val cloudflareColo: String?,
    val warpEnabled: Boolean,
    val elapsedMs: Long,
)
