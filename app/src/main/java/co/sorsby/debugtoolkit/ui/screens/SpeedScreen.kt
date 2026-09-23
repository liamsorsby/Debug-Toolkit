package co.sorsby.debugtoolkit.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.sorsby.debugtoolkit.R
import co.sorsby.debugtoolkit.core.model.SpeedResult
import co.sorsby.debugtoolkit.core.model.ToolState
import co.sorsby.debugtoolkit.feature.SettingsViewModel
import co.sorsby.debugtoolkit.feature.SpeedViewModel
import co.sorsby.debugtoolkit.ui.components.InfoCard
import co.sorsby.debugtoolkit.ui.components.Metric
import co.sorsby.debugtoolkit.ui.components.ResultCard
import co.sorsby.debugtoolkit.ui.components.ResultHeader
import co.sorsby.debugtoolkit.ui.components.ScreenList
import co.sorsby.debugtoolkit.ui.components.ToolInputCard
import co.sorsby.debugtoolkit.ui.components.ToolIntroCard
import co.sorsby.debugtoolkit.ui.components.ToolResult
import co.sorsby.debugtoolkit.ui.theme.DebugToolkitTheme
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun SpeedRoute(
    settingsViewModel: SettingsViewModel,
    viewModel: SpeedViewModel = koinViewModel(),
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    SpeedScreen(
        disclosureAccepted = settings.cloudflareDisclosureAccepted,
        state = state,
        onAcceptDisclosure = settingsViewModel::acceptCloudflareDisclosure,
        onRunTest = viewModel::runTest,
    )
}

@Composable
fun SpeedScreen(
    disclosureAccepted: Boolean,
    state: ToolState<SpeedResult>,
    onAcceptDisclosure: () -> Unit,
    onRunTest: () -> Unit,
) {
    ScreenList {
        item {
            ToolIntroCard(
                icon = Icons.Default.Speed,
                description = stringResource(R.string.speed_intro),
            )
        }
        item {
            ToolInputCard {
                InfoCard(stringResource(R.string.speed_disclosure))
                if (!disclosureAccepted) {
                    FilledTonalButton(
                        onClick = onAcceptDisclosure,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.speed_disclosure_action))
                    }
                } else {
                    Button(
                        onClick = onRunTest,
                        enabled = state !is ToolState.Loading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.speed_action))
                    }
                }
            }
        }
        item { ToolResult(state) { SpeedResultView(it) } }
    }
}

@Composable
private fun SpeedResultView(result: SpeedResult) {
    ResultCard {
        ResultHeader()
        Metric(
            stringResource(R.string.speed_latency),
            stringResource(R.string.duration_milliseconds, result.latencyMs),
        )
        Metric(
            stringResource(R.string.speed_jitter),
            stringResource(R.string.duration_milliseconds, result.jitterMs),
        )
        Metric(
            stringResource(R.string.speed_download),
            stringResource(R.string.rate_mbps, result.downloadMbps),
        )
        Metric(
            stringResource(R.string.speed_upload),
            stringResource(R.string.rate_mbps, result.uploadMbps),
        )
        Metric(
            stringResource(R.string.speed_transferred),
            stringResource(R.string.size_megabytes, result.transferredBytes / 1_000_000.0),
        )
        Metric(stringResource(R.string.speed_measured), formatLocalizedInstant(result.measuredAt))
    }
}

private fun formatLocalizedInstant(instant: Instant): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withZone(ZoneId.systemDefault())
        .format(instant)

@Preview(showBackground = true)
@Composable
private fun SpeedScreenPreview() {
    DebugToolkitTheme {
        SpeedScreen(
            disclosureAccepted = true,
            state = ToolState.Success(
                SpeedResult(
                    latencyMs = 18.4,
                    jitterMs = 2.1,
                    downloadMbps = 94.2,
                    uploadMbps = 31.7,
                    transferredBytes = 8_000_000,
                    measuredAt = Instant.parse("2026-09-24T12:00:00Z"),
                ),
            ),
            onAcceptDisclosure = {},
            onRunTest = {},
        )
    }
}
