package co.sorsby.debugtoolkit.data.portscan

/**
 * Turns a comma separated list of ports and port ranges (for example "22,80,443,8000-8010")
 * into a sorted list of distinct port numbers. Kept as a pure function so scan requests can be
 * validated and capped before any socket is opened.
 */
object PortListParser {
    const val MAX_PORTS = 64

    /** A short list of commonly probed ports, offered as a starting point in the UI. */
    val COMMON_PORTS = listOf(21, 22, 25, 80, 110, 143, 443, 3306, 3389, 5432, 8080)

    fun parse(input: String): List<Int> {
        val tokens = input.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        require(tokens.isNotEmpty()) { "Enter at least one port or port range." }

        val ports = sortedSetOf<Int>()
        for (token in tokens) {
            val range = token.split("-").map { it.trim() }
            when (range.size) {
                1 -> ports += parsePort(range[0])
                2 -> {
                    val start = parsePort(range[0])
                    val end = parsePort(range[1])
                    require(start <= end) { "'$token' is not a valid port range." }
                    require(end - start < MAX_PORTS) { "Scan at most $MAX_PORTS ports at a time." }
                    ports += start..end
                }
                else -> throw IllegalArgumentException("'$token' is not a valid port or port range.")
            }
        }
        require(ports.size <= MAX_PORTS) { "Scan at most $MAX_PORTS ports at a time." }
        return ports.toList()
    }

    private fun parsePort(value: String): Int {
        val port = value.toIntOrNull()
            ?: throw IllegalArgumentException("'$value' is not a valid port.")
        require(port in 1..65535) { "Ports must be between 1 and 65535." }
        return port
    }
}
