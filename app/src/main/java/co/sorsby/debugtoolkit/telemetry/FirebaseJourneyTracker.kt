package co.sorsby.debugtoolkit.telemetry

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import co.sorsby.debugtoolkit.core.model.AnalyticsConsent
import co.sorsby.debugtoolkit.core.model.ToolError
import co.sorsby.debugtoolkit.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

class FirebaseJourneyTracker(
    context: Context,
    settingsRepository: SettingsRepository,
    scope: CoroutineScope,
) : JourneyTracker {
    private val firebaseApp = FirebaseApp.getApps(context).firstOrNull()
    private val analytics = firebaseApp?.let { FirebaseAnalytics.getInstance(context) }
    private val crashlytics = firebaseApp?.let { FirebaseCrashlytics.getInstance() }
    private val performance = firebaseApp?.let { FirebasePerformance.getInstance() }
    private val consentLock = Any()

    @Volatile
    private var journeyCollectionEnabled = false

    init {
        crashlytics?.setCrashlyticsCollectionEnabled(true)
        // Performance monitoring reports app-health metrics (not user behavior) and runs
        // unconditionally, independent of analytics consent, matching Crashlytics.
        performance?.isPerformanceCollectionEnabled = true
        scope.launch {
            settingsRepository.settings.collectLatest { settings ->
                setJourneyCollectionEnabled(
                    settings.analyticsConsent == AnalyticsConsent.GRANTED,
                )
            }
        }
    }

    override fun trackScreen(route: String) {
        crashlytics?.log("screen:$route")
        withJourneyCollection {
            analytics?.logEvent(
                FirebaseAnalytics.Event.SCREEN_VIEW,
                Bundle().apply {
                    putString(FirebaseAnalytics.Param.SCREEN_NAME, route)
                    putString(FirebaseAnalytics.Param.SCREEN_CLASS, "compose")
                },
            )
        }
    }

    override fun startTool(tool: DiagnosticTool): ToolJourney {
        crashlytics?.log("tool:${tool.eventValue}:started")
        val trace = performance?.newTrace("tool_${tool.eventValue}")?.also(Trace::start)
        withJourneyCollection {
            analytics?.logEvent("tool_started", tool.parameters())
        }
        return FirebaseToolJourney(
            tool = tool,
            crashlytics = crashlytics,
            trace = trace,
            complete = ::completeTool,
        )
    }

    private fun setJourneyCollectionEnabled(enabled: Boolean) {
        synchronized(consentLock) {
            analytics?.setConsent(
                mapOf(
                    FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE to enabled.consentStatus(),
                    FirebaseAnalytics.ConsentType.AD_STORAGE to
                        FirebaseAnalytics.ConsentStatus.DENIED,
                    FirebaseAnalytics.ConsentType.AD_USER_DATA to
                        FirebaseAnalytics.ConsentStatus.DENIED,
                    FirebaseAnalytics.ConsentType.AD_PERSONALIZATION to
                        FirebaseAnalytics.ConsentStatus.DENIED,
                ),
            )
            analytics?.setAnalyticsCollectionEnabled(enabled)
            journeyCollectionEnabled = enabled
        }
    }

    private fun completeTool(
        tool: DiagnosticTool,
        trace: Trace?,
        outcome: String,
        elapsedMs: Long,
        errorType: String?,
    ) {
        synchronized(consentLock) {
            if (journeyCollectionEnabled) {
                analytics?.logEvent(
                    "tool_completed",
                    tool.parameters(outcome, elapsedMs, errorType),
                )
            }
        }
        trace?.putAttribute("outcome", outcome)
        errorType?.let { trace?.putAttribute("error_type", it) }
        trace?.stop()
    }

    private fun <T> withJourneyCollection(action: () -> T): T? =
        synchronized(consentLock) {
            if (journeyCollectionEnabled) action() else null
        }
}

private class FirebaseToolJourney(
    private val tool: DiagnosticTool,
    private val crashlytics: FirebaseCrashlytics?,
    private val trace: Trace?,
    private val complete: (
        tool: DiagnosticTool,
        trace: Trace?,
        outcome: String,
        elapsedMs: Long,
        errorType: String?,
    ) -> Unit,
) : ToolJourney {
    private val startedAt = TimeSource.Monotonic.markNow()

    override fun succeed() {
        crashlytics?.log("tool:${tool.eventValue}:succeeded")
        complete(tool, trace, "success", startedAt.elapsedNow().inWholeMilliseconds, null)
    }

    override fun fail(error: ToolError, cause: Exception) {
        crashlytics?.apply {
            log("tool:${tool.eventValue}:failed:${error.eventValue}")
            setCustomKey("diagnostic_tool", tool.eventValue)
            setCustomKey("diagnostic_error", error.eventValue)
            recordException(
                DiagnosticFailureException(
                    tool = tool.eventValue,
                    errorType = error.eventValue,
                    causeType = cause::class.java.simpleName,
                ),
            )
        }
        complete(
            tool,
            trace,
            "failure",
            startedAt.elapsedNow().inWholeMilliseconds,
            error.eventValue,
        )
    }

    override fun cancel() {
        crashlytics?.log("tool:${tool.eventValue}:cancelled")
        complete(
            tool,
            trace,
            "cancelled",
            startedAt.elapsedNow().inWholeMilliseconds,
            null,
        )
    }
}

private class DiagnosticFailureException(
    tool: String,
    errorType: String,
    causeType: String,
) : Exception("Diagnostic $tool failed: $errorType ($causeType)")

private fun DiagnosticTool.parameters(
    outcome: String? = null,
    elapsedMs: Long? = null,
    errorType: String? = null,
) = Bundle().apply {
    putString("tool", eventValue)
    outcome?.let { putString("outcome", it) }
    elapsedMs?.let { putLong("duration_ms", it) }
    errorType?.let { putString("error_type", it) }
}

private val ToolError.eventValue: String
    get() = name.lowercase()

private fun Boolean.consentStatus(): FirebaseAnalytics.ConsentStatus =
    if (this) {
        FirebaseAnalytics.ConsentStatus.GRANTED
    } else {
        FirebaseAnalytics.ConsentStatus.DENIED
    }
