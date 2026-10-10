package co.sorsby.debugtoolkit.update

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class VersionPolicyRepository(
    context: Context,
    private val client: OkHttpClient,
    private val endpoint: String,
) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val updateChannel = UpdateChannel
        .fromInstallerPackageName(context.installingPackageName())
    private val mutablePolicy = MutableStateFlow(
        if (endpoint.isBlank()) null else readCachedPolicy(),
    )
    val policy: StateFlow<VersionPolicy?> = mutablePolicy.asStateFlow()

    suspend fun refresh() {
        if (endpoint.isBlank()) return
        val url = endpoint.toHttpUrlOrNull()
        if (url == null || !url.isHttps || url.username.isNotEmpty() || url.password.isNotEmpty()) {
            Log.w(TAG, "Version policy endpoint must be a valid HTTPS URL")
            return
        }

        val requestUrl = url.newBuilder()
            .removeAllQueryParameters(CHANNEL_PARAMETER)
            .addQueryParameter(CHANNEL_PARAMETER, updateChannel.queryValue)
            .build()
        val request = Request.Builder().url(requestUrl).get().build()
        try {
            withContext(Dispatchers.IO) {
                client.newCall(request).apply {
                    timeout().timeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                }.execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "Version policy request failed with HTTP ${response.code}")
                        return@withContext
                    }
                    val body = response.body
                    if (body.contentLength() > MAX_POLICY_BYTES) {
                        Log.w(TAG, "Version policy response body is too large")
                        return@withContext
                    }
                    val payload = readPolicyPayload(body)
                    if (payload == null) {
                        Log.w(TAG, "Version policy response body exceeds the size limit")
                        return@withContext
                    }
                    val decoded = VersionPolicyDecoder.decode(payload)
                    if (decoded == null) {
                        Log.w(TAG, "Version policy response is invalid")
                        return@withContext
                    }
                    cache(decoded)
                }
            }
        } catch (exception: IOException) {
            Log.w(TAG, "Version policy refresh failed; retaining cached policy", exception)
        }
    }

    private fun Context.installingPackageName(): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            packageManager.getInstallSourceInfo(packageName).installingPackageName
        } else {
            legacyInstallerPackageName()
        }

    @Suppress("DEPRECATION")
    private fun Context.legacyInstallerPackageName(): String? =
        packageManager.getInstallerPackageName(packageName)

    private fun readCachedPolicy(): VersionPolicy? {
        val versionCode = preferences.getString(KEY_MINIMUM_VERSION_CODE, null)
            ?.toIntOrNull()
            ?: return null
        val updateUrl = preferences.getString(KEY_UPDATE_URL, null) ?: return null
        return VersionPolicyDecoder.create(versionCode, updateUrl)
    }

    private fun cache(policy: VersionPolicy) {
        preferences.edit()
            .putString(KEY_MINIMUM_VERSION_CODE, policy.minimumVersionCode.toString())
            .putString(KEY_UPDATE_URL, policy.updateUrl)
            .apply()
        mutablePolicy.value = policy
    }

    private fun readPolicyPayload(body: ResponseBody): String? {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_SIZE)
        val input = body.byteStream()
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (output.size() + count > MAX_POLICY_BYTES) return null
            output.write(buffer, 0, count)
        }
        return String(output.toByteArray(), StandardCharsets.UTF_8)
    }

    private companion object {
        const val TAG = "VersionPolicy"
        const val PREFERENCES_NAME = "version_policy"
        const val KEY_MINIMUM_VERSION_CODE = "minimum_version_code"
        const val KEY_UPDATE_URL = "update_url"
        const val CHANNEL_PARAMETER = "channel"
        const val MAX_POLICY_BYTES = 8 * 1024L
        const val BUFFER_SIZE = 1024
        const val REQUEST_TIMEOUT_SECONDS = 8L
    }
}
