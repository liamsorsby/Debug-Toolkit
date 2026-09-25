package co.sorsby.debugtoolkit.data.ping

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/** The text a shell command produced, decoupled from how it was actually run. */
data class ProcessOutput(
    val exitCode: Int,
    val output: String,
)

/**
 * Runs a shell command and captures its output. Extracted as an interface so ping and
 * traceroute logic can be unit tested without depending on a real `ping` binary being present
 * on the test JVM's PATH.
 */
interface ProcessRunner {
    suspend fun run(command: List<String>, timeoutSeconds: Long): ProcessOutput
}

class SystemProcessRunner : ProcessRunner {
    override suspend fun run(command: List<String>, timeoutSeconds: Long): ProcessOutput =
        withContext(Dispatchers.IO) {
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                throw IOException("Timed out waiting for the command to finish.")
            }
            ProcessOutput(process.exitValue(), output)
        }
}
