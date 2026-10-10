package co.sorsby.debugtoolkit.update

import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

data class VersionPolicy(
    val minimumVersionCode: Int,
    val updateUrl: String,
)

@Serializable
private data class VersionPolicyPayload(
    val minimumVersionCode: Int,
    val updateUrl: String,
)

object VersionPolicyDecoder {
    private val json = Json { ignoreUnknownKeys = true }

    fun decode(source: String): VersionPolicy? {
        val payload = try {
            json.decodeFromString<VersionPolicyPayload>(source)
        } catch (_: SerializationException) {
            return null
        }

        return create(payload.minimumVersionCode, payload.updateUrl)
    }

    fun create(minimumVersionCode: Int, updateUrl: String): VersionPolicy? {
        if (minimumVersionCode < 0) return null
        val normalizedUrl = normalizeHttpsUrl(updateUrl) ?: return null
        return VersionPolicy(minimumVersionCode, normalizedUrl)
    }

    fun normalizeHttpsUrl(url: String): String? =
        url.toHttpsHttpUrlOrNull()?.toString()

    fun isUpdateRequired(installedVersionCode: Int, policy: VersionPolicy?): Boolean =
        policy != null && installedVersionCode < policy.minimumVersionCode

    private fun String.toHttpsHttpUrlOrNull(): HttpUrl? {
        val parsed = toHttpUrlOrNull() ?: return null
        if (
            !parsed.isHttps ||
            parsed.host.isBlank() ||
            parsed.username.isNotEmpty() ||
            parsed.password.isNotEmpty()
        ) {
            return null
        }
        return parsed
    }
}
