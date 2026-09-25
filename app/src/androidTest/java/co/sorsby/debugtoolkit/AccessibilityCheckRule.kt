package co.sorsby.debugtoolkit

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewAction
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import org.hamcrest.Matcher
import org.hamcrest.Matchers.any
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Runs an Espresso [AccessibilityChecks] scan against the full view hierarchy after a test's
 * Compose content has settled. Espresso only inspects a view when a [ViewAction] is performed on
 * it, so a no-op action is dispatched against the root view to trigger a scan of every descendant
 * without altering any state. Checks are enabled once per test process, matching the recommended
 * usage of [AccessibilityChecks.enable].
 */
class AccessibilityCheckRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            base.evaluate()
            onView(isRoot()).perform(scanRootView())
        }
    }

    private companion object {
        init {
            AccessibilityChecks.enable().setRunChecksFromRootView(true)
        }

        fun scanRootView(): ViewAction = object : ViewAction {
            override fun getConstraints(): Matcher<View> = any(View::class.java)

            override fun getDescription(): String = "scan root view for accessibility issues"

            override fun perform(uiController: androidx.test.espresso.UiController?, view: View?) {
                // Intentionally empty: performing any action on the root view is enough for
                // Espresso's AccessibilityChecks interceptor to walk and validate the whole tree.
            }
        }
    }
}
