package co.sorsby.debugtoolkit.data.ping

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ShellPingRunnerTest {
    @Test
    fun `builds the expected ping command and parses its output`() = runTest {
        var capturedCommand: List<String>? = null
        val runner = ShellPingRunner(
            processRunner = object : ProcessRunner {
                override suspend fun run(command: List<String>, timeoutSeconds: Long): ProcessOutput {
                    capturedCommand = command
                    return ProcessOutput(
                        exitCode = 0,
                        output = """
                            PING example.com (93.184.216.34) 56(84) bytes of data.
                            64 bytes from 93.184.216.34: icmp_seq=1 ttl=56 time=11.2 ms

                            --- example.com ping statistics ---
                            1 packets transmitted, 1 received, 0% packet loss, time 1001ms
                        """.trimIndent(),
                    )
                }
            },
        )

        val result = runner.ping("example.com", count = 1)

        assertEquals(listOf("/system/bin/ping", "-c", "1", "example.com"), capturedCommand)
        assertEquals(1, result.transmitted)
        assertEquals(1, result.received)
    }

    @Test
    fun `rejects a probe count outside the supported range`() = runTest {
        val runner = ShellPingRunner(processRunner = object : ProcessRunner {
            override suspend fun run(command: List<String>, timeoutSeconds: Long): ProcessOutput =
                throw AssertionError("Should not run a command for invalid input.")
        })

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { runner.ping("example.com", count = 11) }
        }
    }

    @Test
    fun `rejects a host that looks like a command argument`() = runTest {
        val runner = ShellPingRunner(processRunner = object : ProcessRunner {
            override suspend fun run(command: List<String>, timeoutSeconds: Long): ProcessOutput =
                throw AssertionError("Should not run a command for invalid input.")
        })

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { runner.ping("-oProxyCommand=evil", count = 1) }
        }
    }
}
