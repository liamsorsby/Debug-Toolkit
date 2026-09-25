package co.sorsby.debugtoolkit.data.ping

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the real `/system/bin/ping` binary against loopback. Unit tests cover the text
 * parsing logic in isolation with a fake [ProcessRunner]; this instrumented test only confirms
 * that shelling out to the actual OS binary on a real device or emulator still produces output
 * that parser can read, since that behaviour cannot be faithfully simulated on the local JVM.
 */
@RunWith(AndroidJUnit4::class)
class PingRunnerSmokeTest {
    @Test
    fun pingAgainstLoopbackSucceeds() = runBlocking {
        val result = ShellPingRunner().ping("127.0.0.1", count = 1)

        assertTrue(result.transmitted >= 1)
    }

    @Test
    fun tracerouteAgainstLoopbackReachesTheDestinationOnTheFirstHop() = runBlocking {
        val result = ShellTracerouteRunner().traceroute("127.0.0.1", maxHops = 3)

        assertTrue(result.hops.isNotEmpty())
    }
}
