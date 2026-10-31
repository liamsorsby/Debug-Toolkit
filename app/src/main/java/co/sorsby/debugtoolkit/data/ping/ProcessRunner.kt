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
fun interface ProcessRunner {
    suspend fun run(command: List<String>, timeoutSeconds: Long): ProcessOutput
}

class SystemProcessRunner : ProcessRunner {
    override suspend fun run(command: List<String>, timeoutSeconds: Long): ProcessOutput =
        withContext(Dispatchers.IO) {
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                val text = StringBuilder()
                val chunk = CharArray(CHUNK_CHARS)
                while (text.length < MAX_OUTPUT_CHARS) {
                    val read = reader.read(chunk, 0, minOf(chunk.size, MAX_OUTPUT_CHARS - text.length))
                    if (read == -1) break
                    text.append(chunk, 0, read)
                }
                text.toString()
            }
            val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                throw IOException("Timed out waiting for the command to finish.")
            }
            ProcessOutput(process.exitValue(), output)
        }

    private companion object {
        // Ping and traceroute print a few KB; the cap only guards against runaway output.
        const val MAX_OUTPUT_CHARS = 256 * 1024
        const val CHUNK_CHARS = 4_096
    }
}
