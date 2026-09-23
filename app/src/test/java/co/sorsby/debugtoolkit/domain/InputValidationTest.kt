package co.sorsby.debugtoolkit.domain

import co.sorsby.debugtoolkit.core.model.WifiSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class InputValidationTest {
    @Test
    fun `httpsUrl adds a scheme and normalizes paths`() {
        assertEquals("https://example.com/a/c", InputValidation.httpsUrl("example.com/a/./b/../c"))
        assertEquals("HTTPS://example.com", InputValidation.httpsUrl("HTTPS://example.com"))
    }

    @Test
    fun `httpsUrl rejects insecure schemes credentials and invalid hosts`() {
        assertThrows(IllegalArgumentException::class.java) {
            InputValidation.httpsUrl("http://example.com")
        }
        assertThrows(IllegalArgumentException::class.java) {
            InputValidation.httpsUrl("https://user:password@example.com")
        }
        assertThrows(IllegalArgumentException::class.java) {
            InputValidation.httpsUrl("https://user:password@")
        }
        assertThrows(IllegalArgumentException::class.java) {
            InputValidation.httpsUrl("https://")
        }
        assertThrows(IllegalArgumentException::class.java) {
            InputValidation.httpsUrl("https://[")
        }
    }

    @Test
    fun `domain normalizes case unicode and a trailing dot`() {
        assertEquals("example.com", InputValidation.domain(" Example.COM. "))
        assertEquals("xn--bcher-kva.example", InputValidation.domain("bücher.example"))
        assertEquals("a-b.123", InputValidation.domain("a-b.123"))
        assertEquals("${"a".repeat(63)}.com", InputValidation.domain("${"a".repeat(63)}.com"))
    }

    @Test
    fun `domain rejects empty oversized and malformed labels`() {
        listOf(
            "",
            "-example.com",
            "example-.com",
            "example..com",
            "${"a".repeat(64)}.com",
            "${"valid.".repeat(50)}example",
            "exam ple.com",
        )
            .forEach { value ->
                assertThrows(IllegalArgumentException::class.java) {
                    InputValidation.domain(value)
                }
            }
    }

    @Test
    fun `ptrName converts IPv4 and IPv6 literals`() {
        assertEquals("4.3.2.1.in-addr.arpa", InputValidation.ptrName("1.2.3.4"))
        assertEquals(
            "1.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.ip6.arpa",
            InputValidation.ptrName("::1"),
        )
    }

    @Test
    fun `ptrName rejects hostnames and invalid addresses`() {
        assertThrows(IllegalArgumentException::class.java) {
            InputValidation.ptrName("example.com")
        }
        assertThrows(IllegalArgumentException::class.java) {
            InputValidation.ptrName("999.2.3.4")
        }
    }

    @Test
    fun `signal quality maps documented thresholds`() {
        assertEquals(WifiSignal.UNAVAILABLE, SignalQuality.fromRssi(null))
        assertEquals(WifiSignal.EXCELLENT, SignalQuality.fromRssi(-50))
        assertEquals(WifiSignal.GOOD, SignalQuality.fromRssi(-51))
        assertEquals(WifiSignal.GOOD, SignalQuality.fromRssi(-60))
        assertEquals(WifiSignal.FAIR, SignalQuality.fromRssi(-61))
        assertEquals(WifiSignal.FAIR, SignalQuality.fromRssi(-70))
        assertEquals(WifiSignal.WEAK, SignalQuality.fromRssi(-71))
    }

    @Test
    fun `speed calculations handle valid and invalid samples`() {
        assertEquals(8.0, SpeedMath.megabitsPerSecond(1_000_000, 1_000_000_000), 0.001)
        assertEquals(0.0, SpeedMath.megabitsPerSecond(0, 1), 0.001)
        assertEquals(5.0, SpeedMath.jitterMs(listOf(10.0, 15.0, 10.0)), 0.001)
        assertEquals(0.0, SpeedMath.jitterMs(listOf(10.0)), 0.001)
        assertThrows(IllegalArgumentException::class.java) {
            SpeedMath.megabitsPerSecond(-1, 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SpeedMath.megabitsPerSecond(1, 0)
        }
    }
}
