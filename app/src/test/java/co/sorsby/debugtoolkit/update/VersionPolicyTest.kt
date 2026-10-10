package co.sorsby.debugtoolkit.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionPolicyTest {
    @Test
    fun decodesVersionPolicy() {
        assertEquals(
            VersionPolicy(42, "https://example.com/install"),
            VersionPolicyDecoder.decode(
                """{"minimumVersionCode":42,"updateUrl":"https://example.com/install"}""",
            ),
        )
    }

    @Test
    fun ignoresAdditionalFieldsForForwardCompatibility() {
        assertEquals(
            VersionPolicy(42, "https://example.com/install"),
            VersionPolicyDecoder.decode(
                """{"minimumVersionCode":42,"updateUrl":"https://example.com/install","note":"soon"}""",
            ),
        )
    }

    @Test
    fun rejectsNegativeMinimumVersion() {
        assertNull(
            VersionPolicyDecoder.decode(
                """{"minimumVersionCode":-1,"updateUrl":"https://example.com/install"}""",
            ),
        )
    }

    @Test
    fun rejectsVersionCodesOutsideAndroidIntRange() {
        assertNull(
            VersionPolicyDecoder.decode(
                """{"minimumVersionCode":2147483648,"updateUrl":"https://example.com/install"}""",
            ),
        )
    }

    @Test
    fun rejectsNonHttpsOrCredentialedUpdateUrls() {
        assertNull(VersionPolicyDecoder.create(1, "http://example.com/install"))
        assertNull(VersionPolicyDecoder.create(1, "https://user:pass@example.com/install"))
    }

    @Test
    fun rejectsMalformedPayload() {
        assertNull(VersionPolicyDecoder.decode("""{"minimumVersionCode":"old"}"""))
    }

    @Test
    fun requiresAnUpdateOnlyWhenInstalledVersionIsBelowMinimum() {
        val policy = VersionPolicy(42, "https://example.com/install")

        assertTrue(VersionPolicyDecoder.isUpdateRequired(41, policy))
        assertFalse(VersionPolicyDecoder.isUpdateRequired(42, policy))
        assertFalse(VersionPolicyDecoder.isUpdateRequired(43, policy))
        assertFalse(VersionPolicyDecoder.isUpdateRequired(1, null))
    }
}
