package co.sorsby.debugtoolkit.data.lan

import co.sorsby.debugtoolkit.core.model.LanDevice
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SweepLanScannerTest {

    @Test
    fun `sweeps every host in the subnet and returns only the ones that answered`() = runTest {
        val probed = mutableListOf<String>()
        val scanner = SweepLanScanner(
            currentAddress = { "10.0.0.1" to 30 },
            prober = object : HostProber {
                override suspend fun probe(host: String): LanDevice? {
                    probed += host
                    return if (host == "10.0.0.2") LanDevice(host, "printer.local", 5) else null
                }
            },
        )

        val result = scanner.scan()

        assertEquals(listOf("10.0.0.2"), probed)
        assertEquals("10.0.0.0/30", result.subnetCidr)
        assertEquals(1, result.addressesScanned)
        assertEquals(listOf(LanDevice("10.0.0.2", "printer.local", 5)), result.devices)
    }

    @Test
    fun `fails when there is no private IPv4 address to scan from`() = runTest {
        val scanner = SweepLanScanner(currentAddress = { null })

        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { scanner.scan() }
        }
    }

    @Test
    fun `fails when the current network is too large to sweep`() = runTest {
        val scanner = SweepLanScanner(
            currentAddress = { "10.0.0.1" to 16 },
            prober = object : HostProber {
                override suspend fun probe(host: String): LanDevice =
                    throw AssertionError("Should not probe an oversized network.")
            },
        )

        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { scanner.scan() }
        }
    }

    @Test
    fun `fails when the current network is smaller than a sweepable network`() = runTest {
        val scanner = SweepLanScanner(
            currentAddress = { "10.0.0.1" to 31 },
            prober = object : HostProber {
                override suspend fun probe(host: String): LanDevice =
                    throw AssertionError("Should not probe a point to point network.")
            },
        )

        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { scanner.scan() }
        }
    }
}
