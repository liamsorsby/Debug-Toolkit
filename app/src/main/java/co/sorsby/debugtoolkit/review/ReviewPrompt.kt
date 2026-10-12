package co.sorsby.debugtoolkit.review

import android.content.Context
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Decides when to ask Play for an in-app review: only after the user has had several
 * successful diagnostics over more than one day, and rarely. Play applies its own quota on
 * top, so a request is not guaranteed to show a dialog.
 */
object ReviewPromptPolicy {
    const val MIN_SUCCESSFUL_RUNS = 3
    val MIN_TIME_SINCE_FIRST_USE_MS: Long = TimeUnit.DAYS.toMillis(2)
    val REQUEST_INTERVAL_MS: Long = TimeUnit.DAYS.toMillis(120)

    fun isEligible(
        successfulRuns: Int,
        firstUseAtMs: Long,
        lastRequestAtMs: Long,
        nowMs: Long,
    ): Boolean {
        if (successfulRuns < MIN_SUCCESSFUL_RUNS) return false
        if (firstUseAtMs <= 0L || nowMs - firstUseAtMs < MIN_TIME_SINCE_FIRST_USE_MS) return false
        val sinceLastRequest = nowMs - lastRequestAtMs
        // A negative gap means the clock moved backwards; don't let that block requests forever.
        return lastRequestAtMs <= 0L || sinceLastRequest < 0L ||
            sinceLastRequest >= REQUEST_INTERVAL_MS
    }
}

interface ReviewPromptStore {
    var firstUseAtMs: Long
    var successfulRuns: Int
    var lastRequestAtMs: Long
}

class SharedPreferencesReviewPromptStore(context: Context) : ReviewPromptStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    override var firstUseAtMs: Long
        get() = preferences.getLong(KEY_FIRST_USE_AT, 0L)
        set(value) = preferences.edit().putLong(KEY_FIRST_USE_AT, value).apply()

    override var successfulRuns: Int
        get() = preferences.getInt(KEY_SUCCESSFUL_RUNS, 0)
        set(value) = preferences.edit().putInt(KEY_SUCCESSFUL_RUNS, value).apply()

    override var lastRequestAtMs: Long
        get() = preferences.getLong(KEY_LAST_REQUEST_AT, 0L)
        set(value) = preferences.edit().putLong(KEY_LAST_REQUEST_AT, value).apply()

    private companion object {
        const val PREFERENCES_NAME = "review_prompt"
        const val KEY_FIRST_USE_AT = "first_use_at"
        const val KEY_SUCCESSFUL_RUNS = "successful_runs"
        const val KEY_LAST_REQUEST_AT = "last_request_at"
    }
}

/**
 * Arms a review request after a successful diagnostic, then releases it on the next screen
 * change, so the Play dialog appears at a natural break rather than over the user's results.
 */
class ReviewPromptCoordinator(
    private val store: ReviewPromptStore,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    // Conflated so a request made while no screen is collecting is held, not dropped.
    private val pendingRequests = Channel<Unit>(Channel.CONFLATED)
    val requests: Flow<Unit> = pendingRequests.receiveAsFlow()

    private var armed = false

    init {
        if (store.firstUseAtMs <= 0L) store.firstUseAtMs = clock()
    }

    @Synchronized
    fun recordToolSuccess() {
        // Capped so the stored counter can't overflow over a very long install lifetime.
        store.successfulRuns = (store.successfulRuns + 1).coerceAtMost(MAX_STORED_RUNS)
        armed = ReviewPromptPolicy.isEligible(
            successfulRuns = store.successfulRuns,
            firstUseAtMs = store.firstUseAtMs,
            lastRequestAtMs = store.lastRequestAtMs,
            nowMs = clock(),
        )
    }

    @Synchronized
    fun onNavigation() {
        if (!armed) return
        armed = false
        pendingRequests.trySend(Unit)
        store.lastRequestAtMs = clock()
    }

    private companion object {
        const val MAX_STORED_RUNS = 1_000
    }
}
