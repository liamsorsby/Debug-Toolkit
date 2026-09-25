package co.sorsby.debugtoolkit.data.publicip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudflareTraceParserTest {

    @Test
    fun `parses key value pairs into a map`() {
        val body = """
            fl=1f1
            ip=203.0.113.10
            loc=GB
            colo=LHR
            warp=off
        """.trimIndent()

        val fields = CloudflareTraceParser.parse(body)

        assertEquals("203.0.113.10", fields["ip"])
        assertEquals("GB", fields["loc"])
        assertEquals("LHR", fields["colo"])
        assertEquals("off", fields["warp"])
    }

    @Test
    fun `ignores blank lines and lines without a separator`() {
        val body = "ip=203.0.113.10\n\nnot-a-pair\ncolo=LHR\n"

        val fields = CloudflareTraceParser.parse(body)

        assertEquals(setOf("ip", "colo"), fields.keys)
    }

    @Test
    fun `returns an empty map for an empty body`() {
        assertTrue(CloudflareTraceParser.parse("").isEmpty())
    }
}
