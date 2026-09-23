package co.sorsby.debugtoolkit.core.model

data class HeaderValue(val name: String, val value: String)

data class RedirectStep(val status: Int, val from: String, val to: String)

data class HttpInspection(
    val status: Int,
    val message: String,
    val protocol: String,
    val finalUrl: String,
    val elapsedMs: Long,
    val headers: List<HeaderValue>,
    val redirects: List<RedirectStep>,
    val bodyPreview: String?,
    val bodyTruncated: Boolean,
    val contentType: String?,
)
