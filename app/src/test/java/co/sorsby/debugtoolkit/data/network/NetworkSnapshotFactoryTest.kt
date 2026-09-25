package co.sorsby.debugtoolkit.data.network

import co.sorsby.debugtoolkit.core.model.NetworkTransport
import co.sorsby.debugtoolkit.core.model.WifiSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkSnapshotFactoryTest {
    @Test
    fun `a disconnected snapshot keeps only the sample time`() {
        val snapshot = NetworkSnapshotFactory.disconnected(sampledAtEpochMillis = 1_000)

        assertEquals(1_000, snapshot.sampledAtEpochMillis)
        assertEquals(false, snapshot.connected)
        assertEquals(WifiSignal.UNAVAILABLE, snapshot.wifiSignal)
        assertTrue(snapshot.transports.isEmpty())
        assertTrue(snapshot.localAddresses.isEmpty())
        assertNull(snapshot.wifiRssiDbm)
    }

    @Test
    fun `a connected snapshot carries the platform values through`() {
        val snapshot = connected(
            transports = setOf(NetworkTransport.WIFI, NetworkTransport.VPN),
            validated = true,
            localAddresses = listOf("192.168.1.10", "fe80::1"),
            wifiRssiDbm = -55,
            linkDownstreamKbps = 50_000,
            linkUpstreamKbps = 10_000,
        )

        assertEquals(true, snapshot.connected)
        assertEquals(true, snapshot.validated)
        assertEquals(setOf(NetworkTransport.WIFI, NetworkTransport.VPN), snapshot.transports)
        assertEquals(listOf("192.168.1.10", "fe80::1"), snapshot.localAddresses)
        assertEquals(-55, snapshot.wifiRssiDbm)
        assertEquals(50_000, snapshot.estimatedDownstreamKbps)
        assertEquals(10_000, snapshot.estimatedUpstreamKbps)
    }

    @Test
    fun `metered is the inverse of the platform's not-metered capability`() {
        assertEquals(false, connected(notMetered = true).metered)
        assertEquals(true, connected(notMetered = false).metered)
    }

    @Test
    fun `addresses the platform could not resolve are dropped`() {
        val snapshot = connected(localAddresses = listOf("192.168.1.10", "", "10.0.0.2"))

        assertEquals(listOf("192.168.1.10", "10.0.0.2"), snapshot.localAddresses)
    }

    @Test
    fun `signal quality is derived from the reported strength`() {
        val expected = listOf(
            -40 to WifiSignal.EXCELLENT,
            -50 to WifiSignal.EXCELLENT,
            -55 to WifiSignal.GOOD,
            -65 to WifiSignal.FAIR,
            -80 to WifiSignal.WEAK,
            null to WifiSignal.UNAVAILABLE,
        )

        expected.forEach { (rssi, signal) ->
            assertEquals("$rssi dBm", signal, connected(wifiRssiDbm = rssi).wifiSignal)
        }
    }

    @Test
    fun `the invalid signal sentinel is not reported as a reading`() {
        listOf(NetworkSnapshotFactory.INVALID_RSSI_DBM, -130, null).forEach { rssi ->
            assertNull("$rssi should not be usable", NetworkSnapshotFactory.usableRssi(rssi))
        }

        assertEquals(-126, NetworkSnapshotFactory.usableRssi(-126))
    }

    @Test
    fun `the sentinel is also cleared from a built snapshot`() {
        val snapshot = connected(wifiRssiDbm = NetworkSnapshotFactory.INVALID_RSSI_DBM)

        assertNull(snapshot.wifiRssiDbm)
        assertEquals(WifiSignal.UNAVAILABLE, snapshot.wifiSignal)
    }

    @Test
    fun `an unknown link bandwidth is reported as no estimate`() {
        assertNull(NetworkSnapshotFactory.positiveKbps(0))
        assertNull(NetworkSnapshotFactory.positiveKbps(-1))
        assertEquals(1, NetworkSnapshotFactory.positiveKbps(1))

        val snapshot = connected(linkDownstreamKbps = 0, linkUpstreamKbps = 0)
        assertNull(snapshot.estimatedDownstreamKbps)
        assertNull(snapshot.estimatedUpstreamKbps)
    }

    private fun connected(
        transports: Set<NetworkTransport> = setOf(NetworkTransport.WIFI),
        validated: Boolean = true,
        notMetered: Boolean = true,
        localAddresses: List<String> = emptyList(),
        wifiRssiDbm: Int? = null,
        linkDownstreamKbps: Int = 1_000,
        linkUpstreamKbps: Int = 1_000,
    ) = NetworkSnapshotFactory.connected(
        sampledAtEpochMillis = 1_000,
        transports = transports,
        validated = validated,
        notMetered = notMetered,
        localAddresses = localAddresses,
        wifiRssiDbm = wifiRssiDbm,
        linkDownstreamKbps = linkDownstreamKbps,
        linkUpstreamKbps = linkUpstreamKbps,
    )
}
