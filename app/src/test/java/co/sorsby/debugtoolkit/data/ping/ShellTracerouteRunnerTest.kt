package co.sorsby.debugtoolkit.data.ping

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellTracerouteRunnerTest {
    @Test
    fun `stops as soon as the destination replies`() = runTest {
        val commands = mutableListOf<List<String>>()
        val runner = ShellTracerouteRunner(
            processRunner = object : ProcessRunner {
                override suspend fun run(command: List<String>, timeoutSeconds: Long): ProcessOutput {
                    commands += command
                    val ttl = command[command.indexOf("-t") + 1].toInt()
                    return when (ttl) {
                        1 -> ProcessOutput(0, "From 192.168.1.1 icmp_seq=1 Time to live exceeded")
                        else -> ProcessOutput(
                            0,
                            "64 bytes from 93.184.216.34: icmp_seq=1 ttl=56 time=11.2 ms",
                        )
                    }
                }
            },
        )

        val result = runner.traceroute("example.com", maxHops = 30)

        assertEquals(2, commands.size)
        assertTrue(result.reachedDestination)
        assertEquals(2, result.hops.size)
        assertEquals("192.168.1.1", result.hops[0].address)
        assertEquals("93.184.216.34", result.hops[1].address)
    }

    @Test
    fun `stops at the hop limit when the destination never replies`() = runTest {
        val runner = ShellTracerouteRunner(
            processRunner = object : ProcessRunner {
                override suspend fun run(command: List<String>, timeoutSeconds: Long): ProcessOutput =
                    ProcessOutput(1, "Destination unreachable")
            },
        )

        val result = runner.traceroute("example.com", maxHops = 3)

        assertEquals(3, result.hops.size)
        assertEquals(false, result.reachedDestination)
        assertTrue(result.hops.all { it.address == null })
    }

    @Test
    fun `rejects a hop count outside the supported range`() = runTest {
        val runner = ShellTracerouteRunner(processRunner = object : ProcessRunner {
            override suspend fun run(command: List<String>, timeoutSeconds: Long): ProcessOutput =
                throw AssertionError("Should not run a command for invalid input.")
        })

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { runner.traceroute("example.com", maxHops = 31) }
        }
    }
}
