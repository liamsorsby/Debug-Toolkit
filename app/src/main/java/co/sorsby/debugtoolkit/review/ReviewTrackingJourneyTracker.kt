package co.sorsby.debugtoolkit.review

import co.sorsby.debugtoolkit.core.model.ToolError
import co.sorsby.debugtoolkit.telemetry.DiagnosticTool
import co.sorsby.debugtoolkit.telemetry.JourneyTracker
import co.sorsby.debugtoolkit.telemetry.ToolJourney

/** Forwards to [delegate] and feeds tool successes and navigation to the review prompt. */
class ReviewTrackingJourneyTracker(
    private val delegate: JourneyTracker,
    private val coordinator: ReviewPromptCoordinator,
) : JourneyTracker by delegate {
    override fun trackNavigation(from: String?, to: String, source: String) {
        delegate.trackNavigation(from, to, source)
        coordinator.onNavigation()
    }

    override fun startTool(tool: DiagnosticTool): ToolJourney {
        val journey = delegate.startTool(tool)
        return object : ToolJourney {
            override fun succeed() {
                journey.succeed()
                coordinator.recordToolSuccess()
            }

            override fun fail(error: ToolError, cause: Exception) = journey.fail(error, cause)

            override fun cancel() = journey.cancel()
        }
    }
}
