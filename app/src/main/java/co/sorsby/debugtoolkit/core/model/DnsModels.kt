package co.sorsby.debugtoolkit.core.model

data class DnsRecord(
    val name: String,
    val type: Int,
    val ttlSeconds: Long,
    val value: String,
)

data class DnsResult(
    val status: Int,
    val authenticatedData: Boolean,
    val recursionAvailable: Boolean,
    val records: List<DnsRecord>,
    val authority: List<DnsRecord>,
    val additional: List<DnsRecord>,
    val elapsedMs: Long,
    val authoritative: Boolean = false,
)
