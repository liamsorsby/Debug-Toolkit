package co.sorsby.debugtoolkit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.sorsby.debugtoolkit.R
import co.sorsby.debugtoolkit.core.model.AnalyticsConsent
import co.sorsby.debugtoolkit.core.model.AppSettings
import co.sorsby.debugtoolkit.core.model.ThemeMode
import co.sorsby.debugtoolkit.feature.SettingsViewModel
import co.sorsby.debugtoolkit.ui.components.ScreenList
import co.sorsby.debugtoolkit.ui.theme.DebugToolkitTheme

@Composable
fun SettingsRoute(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    SettingsScreen(
        settings = settings,
        onThemeModeSelected = viewModel::setThemeMode,
        onConsentSelected = viewModel::setConsent,
    )
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onConsentSelected: (AnalyticsConsent) -> Unit,
) {
    ScreenList {
        item {
            SettingsCard(
                title = stringResource(R.string.settings_appearance),
                supportingText = stringResource(R.string.settings_appearance_supporting),
            ) {
                ThemeMode.entries.forEach { mode ->
                    ChoiceRow(
                        label = themeModeLabel(mode),
                        selected = settings.themeMode == mode,
                        onClick = { onThemeModeSelected(mode) },
                    )
                }
            }
        }
        item {
            SettingsCard(
                title = stringResource(R.string.settings_analytics),
                supportingText = stringResource(R.string.settings_analytics_supporting),
            ) {
                AnalyticsConsent.entries.forEach { consent ->
                    ChoiceRow(
                        label = consentLabel(consent),
                        selected = settings.analyticsConsent == consent,
                        onClick = { onConsentSelected(consent) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        RadioButton(selected = selected, onClick = null)
    }
}

@Composable
private fun SettingsCard(
    title: String,
    supportingText: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(
                supportingText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            content()
        }
    }
}

@Composable
private fun themeModeLabel(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    },
)

@Composable
private fun consentLabel(consent: AnalyticsConsent): String = stringResource(
    when (consent) {
        AnalyticsConsent.UNSET -> R.string.consent_unset
        AnalyticsConsent.DENIED -> R.string.consent_denied
        AnalyticsConsent.GRANTED -> R.string.consent_granted
    },
)

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    DebugToolkitTheme {
        SettingsScreen(
            settings = AppSettings(
                themeMode = ThemeMode.SYSTEM,
                analyticsConsent = AnalyticsConsent.DENIED,
            ),
            onThemeModeSelected = {},
            onConsentSelected = {},
        )
    }
}
