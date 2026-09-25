package co.sorsby.debugtoolkit

import android.app.Activity
import android.view.View
import androidx.compose.ui.platform.ViewRootForTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckPreset
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesViews
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityViewCheckResult
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import org.hamcrest.Matcher
import org.hamcrest.Matchers.instanceOf
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Scans the resumed activity's view hierarchy for accessibility defects once a test body has
 * finished and its Compose content has settled.
 *
 * The scan is driven directly by [AccessibilityValidator] against the activity decor view rather
 * than through an Espresso view action. Espresso resolves its target by waiting for a root window
 * that both holds input focus and has finished laying out, a precondition that headless emulators
 * never satisfy; driving the validator directly removes that dependency while preserving identical
 * check coverage.
 */
class AccessibilityCheckRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            base.evaluate()
            scanResumedActivity()
        }
    }

    private companion object {
        /**
         * Suppresses findings reported against the Compose host view itself.
         *
         * The checks walk the Android [View] tree, in which the whole Compose UI is a single
         * focusable host view carrying no content description. Compose instead publishes its
         * content through a virtual accessibility hierarchy derived from semantics, which these
         * checks cannot see, so the host view is always reported as unlabelled. That host is
         * identified by [ViewRootForTest], the public marker it implements. Accessibility of the
         * Compose content is asserted through semantics in the tests themselves.
         */
        val suppressComposeHost: Matcher<in AccessibilityViewCheckResult> =
            matchesViews(instanceOf(ViewRootForTest::class.java))

        val validator: AccessibilityValidator = AccessibilityValidator()
            .setCheckPreset(AccessibilityCheckPreset.LATEST)
            .setRunChecksFromRootView(true)
            .setSuppressingResultMatcher(suppressComposeHost)
            .setThrowExceptionForErrors(true)

        /**
         * Runs the validator against the decor view of the activity that is currently resumed.
         *
         * The scan happens on the main thread because the checks read view state that is only safe
         * to touch there. When no activity is resumed the test has already torn its UI down and
         * there is nothing left to inspect, so the scan is skipped rather than failed.
         */
        fun scanResumedActivity() {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            var failure: Throwable? = null
            instrumentation.runOnMainSync {
                val decorView = resumedActivity()?.window?.decorView ?: return@runOnMainSync
                failure = runCatching { validator.check(decorView) }.exceptionOrNull()
            }
            instrumentation.waitForIdleSync()
            failure?.let { throw it }
        }

        fun resumedActivity(): Activity? =
            ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .firstOrNull()
    }
}
