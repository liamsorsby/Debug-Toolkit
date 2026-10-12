package co.sorsby.debugtoolkit.update

import android.content.Context
import java.util.concurrent.TimeUnit

/**
 * Decides when to offer an optional (flexible) Play update, so users aren't prompted for an
 * update Play has only just seen, and aren't re-prompted every launch after declining.
 */
object SoftUpdatePolicy {
    const val MIN_STALENESS_DAYS = 2
    val PROMPT_INTERVAL_MS: Long = TimeUnit.DAYS.toMillis(3)

    fun shouldPrompt(
        updateAvailable: Boolean,
        flexibleAllowed: Boolean,
        stalenessDays: Int?,
        nowMs: Long,
        lastPromptAtMs: Long,
    ): Boolean {
        if (!updateAvailable || !flexibleAllowed) return false
        if ((stalenessDays ?: -1) < MIN_STALENESS_DAYS) return false
        val elapsed = nowMs - lastPromptAtMs
        // A negative gap means the clock moved backwards; don't let that suppress prompts forever.
        return lastPromptAtMs <= 0L || elapsed < 0L || elapsed >= PROMPT_INTERVAL_MS
    }
}

class SoftUpdatePromptStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    var lastPromptAtMs: Long
        get() = preferences.getLong(KEY_LAST_PROMPT_AT, 0L)
        set(value) {
            preferences.edit().putLong(KEY_LAST_PROMPT_AT, value).apply()
        }

    private companion object {
        const val PREFERENCES_NAME = "soft_update"
        const val KEY_LAST_PROMPT_AT = "last_prompt_at"
    }
}
