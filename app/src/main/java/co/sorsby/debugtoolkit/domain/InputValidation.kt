package co.sorsby.debugtoolkit.domain

import java.net.IDN
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI

object InputValidation {
    fun httpsUrl(input: String): String {
        val candidate = input.trim().let {
            if ("://" in it) it else "https://$it"
        }
        val uri = runCatching { URI(candidate) }
            .getOrElse { throw IllegalArgumentException("Enter a valid HTTPS address.") }
        require(uri.scheme.equals("https", ignoreCase = true)) {
            "Only HTTPS endpoints are supported."
        }
        require(!uri.host.isNullOrBlank() && uri.userInfo == null) {
            "Enter a valid HTTPS address without embedded credentials."
        }
        return uri.normalize().toASCIIString()
    }

    fun domain(input: String): String {
        val value = input.trim().trimEnd('.')
        require(value.isNotEmpty()) { "Enter a domain name." }
        val ascii = runCatching { IDN.toASCII(value, IDN.USE_STD3_ASCII_RULES) }
            .getOrElse { throw IllegalArgumentException("Enter a valid domain name.") }
            .lowercase()
        require(ascii.length <= 253 && ascii.split('.').all(::validLabel)) {
            "Enter a valid domain name."
        }
        return ascii
    }

    fun ptrName(input: String): String {
        val value = input.trim()
        require(value.contains(':') || IPV4.matches(value)) { "Enter a valid IP address." }
        val address = runCatching { InetAddress.getByName(value) }
            .getOrElse { throw IllegalArgumentException("Enter a valid IP address.") }
        return when (address) {
            is Inet4Address -> address.address.reversedArray()
                .joinToString(".") { it.toUByte().toString() } + ".in-addr.arpa"
            is Inet6Address -> address.address
                .reversedArray()
                .flatMap { byte ->
                    listOf(
                        (byte.toInt() and 0x0F).toString(16),
                        ((byte.toInt() ushr 4) and 0x0F).toString(16),
                    )
                }
                .joinToString(".") + ".ip6.arpa"
            else -> error("Unsupported IP address.")
        }
    }

    /** Validates a nameserver hostname or literal IP address entered for a direct DNS query. */
    fun nameserver(input: String): String {
        val value = input.trim()
        require(value.isNotEmpty()) { "Enter a nameserver hostname or IP address." }
        require(!value.any(Char::isWhitespace)) { "Enter a valid nameserver hostname or IP address." }
        return value
    }

    /**
     * Validates a bare hostname or IP address with no scheme, port, or path, for tools that
     * shell out to or open a raw socket against a target (ping, traceroute, port scanner,
     * WHOIS). Rejects anything that looks like a command line argument, since these values may
     * end up in a process argument list.
     */
    fun host(input: String): String {
        val value = input.trim()
        require(value.isNotEmpty()) { "Enter a hostname or IP address." }
        require(!value.any(Char::isWhitespace) && value.first() != '-') {
            "Enter a valid hostname or IP address."
        }
        require(value.none { it in "/\\?#@:" }) {
            "Enter a hostname or IP address without a scheme, port, or path."
        }
        return value
    }

    private fun validLabel(label: String): Boolean =
        label.isNotEmpty() &&
            label.length <= 63 &&
            label.first() != '-' &&
            label.last() != '-' &&
            label.all { it.isLetterOrDigit() || it == '-' }

    private val IPV4 = Regex("""(?:\d{1,3}\.){3}\d{1,3}""")
}

object SignalQuality {
    fun fromRssi(rssiDbm: Int?): co.sorsby.debugtoolkit.core.model.WifiSignal = when {
        rssiDbm == null -> co.sorsby.debugtoolkit.core.model.WifiSignal.UNAVAILABLE
        rssiDbm >= -50 -> co.sorsby.debugtoolkit.core.model.WifiSignal.EXCELLENT
        rssiDbm >= -60 -> co.sorsby.debugtoolkit.core.model.WifiSignal.GOOD
        rssiDbm >= -70 -> co.sorsby.debugtoolkit.core.model.WifiSignal.FAIR
        else -> co.sorsby.debugtoolkit.core.model.WifiSignal.WEAK
    }
}

object SpeedMath {
    fun megabitsPerSecond(bytes: Long, elapsedNanos: Long): Double {
        require(bytes >= 0) { "Bytes must not be negative." }
        require(elapsedNanos > 0) { "Elapsed time must be positive." }
        return bytes * 8.0 / (elapsedNanos / 1_000_000_000.0) / 1_000_000.0
    }

    fun jitterMs(latenciesMs: List<Double>): Double {
        if (latenciesMs.size < 2) return 0.0
        return latenciesMs.zipWithNext { first, second -> kotlin.math.abs(second - first) }
            .average()
    }
}
