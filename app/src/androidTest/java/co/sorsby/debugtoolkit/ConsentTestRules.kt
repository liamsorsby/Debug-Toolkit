package co.sorsby.debugtoolkit

import co.sorsby.debugtoolkit.core.model.AnalyticsConsent
import co.sorsby.debugtoolkit.data.settings.SettingsRepository
import kotlinx.coroutines.runBlocking
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.koin.core.context.GlobalContext

/**
 * Forces analytics consent to [AnalyticsConsent.GRANTED] before [MainActivity] launches, so tests
 * that exercise the main app content are not blocked by the first-launch consent gate. Must sit
 * between the permission rule and the compose test rule in a [org.junit.rules.RuleChain], since
 * it relies on Koin already having been started by [DebugToolkitApplication.onCreate] but must
 * run before the activity under test is created.
 */
class GrantedConsentRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            setConsent(AnalyticsConsent.GRANTED)
            base.evaluate()
        }
    }
}

/**
 * Forces analytics consent back to [AnalyticsConsent.UNSET] before [MainActivity] launches, so
 * tests can verify the first-launch consent gate itself renders as expected.
 */
class UnsetConsentRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            setConsent(AnalyticsConsent.UNSET)
            base.evaluate()
        }
    }
}

private fun setConsent(consent: AnalyticsConsent) = runBlocking {
    GlobalContext.get().get<SettingsRepository>().setAnalyticsConsent(consent)
}
