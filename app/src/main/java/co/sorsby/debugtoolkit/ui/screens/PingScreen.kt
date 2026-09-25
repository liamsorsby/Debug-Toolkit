package co.sorsby.debugtoolkit.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.sorsby.debugtoolkit.R
import co.sorsby.debugtoolkit.core.model.PingMode
import co.sorsby.debugtoolkit.core.model.PingResult
import co.sorsby.debugtoolkit.core.model.ToolState
import co.sorsby.debugtoolkit.core.model.TracerouteResult
import co.sorsby.debugtoolkit.feature.PingUiState
import co.sorsby.debugtoolkit.feature.PingViewModel
import co.sorsby.debugtoolkit.ui.components.InfoCard
import co.sorsby.debugtoolkit.ui.components.Metric
import co.sorsby.debugtoolkit.ui.components.ResultCard
import co.sorsby.debugtoolkit.ui.components.ResultHeader
import co.sorsby.debugtoolkit.ui.components.RunToolButton
import co.sorsby.debugtoolkit.ui.components.ScreenList
import co.sorsby.debugtoolkit.ui.components.ToolInputCard
import co.sorsby.debugtoolkit.ui.components.ToolIntroCard
import co.sorsby.debugtoolkit.ui.components.ToolResult
import co.sorsby.debugtoolkit.ui.theme.DebugToolkitTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun PingRoute(viewModel: PingViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PingScreen(
        state = state,
        onInputChanged = viewModel::setInput,
        onModeChanged = viewModel::setMode,
        onRun = viewModel::run,
    )
}

@Composable
fun PingScreen(
    state: PingUiState,
    onInputChanged: (String) -> Unit,
    onModeChanged: (PingMode) -> Unit,
    onRun: () -> Unit,
) {
    val activeResult = when (state.mode) {
        PingMode.PING -> state.pingResult
        PingMode.TRACEROUTE -> state.tracerouteResult
    }
    ScreenList {
        item {
            ToolIntroCard(
                icon = Icons.Default.NetworkPing,
                description = stringResource(R.string.ping_intro),
            )
        }
        item {
            ToolInputCard {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    PingMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = state.mode == mode,
                            onClick = { onModeChanged(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, PingMode.entries.size),
                        ) {
                            Text(
                                stringResource(
                                    if (mode == PingMode.PING) {
                                        R.string.ping_mode_ping
                                    } else {
                                        R.string.ping_mode_traceroute
                                    },
                                ),
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = state.input,
                    onValueChange = onInputChanged,
                    label = { Text(stringResource(R.string.ping_host_label)) },
                    supportingText = { Text(stringResource(R.string.ping_host_helper)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                RunToolButton(
                    text = stringResource(
                        if (state.mode == PingMode.PING) {
                            R.string.ping_action
                        } else {
                            R.string.traceroute_action
                        },
                    ),
                    state = activeResult,
                    onClick = onRun,
                    enabled = state.input.isNotBlank(),
                )
            }
        }
        item {
            when (state.mode) {
                PingMode.PING -> ToolResult(state.pingResult) { PingResultView(it) }
                PingMode.TRACEROUTE -> ToolResult(state.tracerouteResult) { TracerouteResultView(it) }
            }
        }
    }
}

@Composable
private fun PingResultView(result: PingResult) {
    ResultCard {
        ResultHeader()
        Metric(stringResource(R.string.ping_transmitted), result.transmitted.toString())
        Metric(stringResource(R.string.ping_received), result.received.toString())
        Metric(
            stringResource(R.string.ping_packet_loss),
            stringResource(R.string.percentage, result.packetLossPercent),
        )
        result.minMs?.let { Metric(stringResource(R.string.ping_min), stringResource(R.string.duration_milliseconds, it)) }
        result.avgMs?.let { Metric(stringResource(R.string.ping_avg), stringResource(R.string.duration_milliseconds, it)) }
        result.maxMs?.let { Metric(stringResource(R.string.ping_max), stringResource(R.string.duration_milliseconds, it)) }
        Metric(
            stringResource(R.string.ping_jitter),
            stringResource(R.string.duration_milliseconds, result.jitterMs),
        )
        result.probes.forEach { probe ->
            Metric(
                stringResource(R.string.ping_probe_label, probe.sequence),
                probe.roundTripMs?.let { stringResource(R.string.duration_milliseconds, it) }
                    ?: stringResource(R.string.ping_probe_timeout),
            )
        }
    }
}

@Composable
private fun TracerouteResultView(result: TracerouteResult) {
    ResultCard {
        ResultHeader()
        result.hops.forEach { hop ->
            Metric(
                stringResource(R.string.traceroute_hop_label, hop.hopNumber),
                when {
                    hop.address == null -> stringResource(R.string.traceroute_hop_no_reply)
                    hop.roundTripMs != null ->
                        "${hop.address} · " + stringResource(R.string.duration_milliseconds, hop.roundTripMs)
                    else -> hop.address
                },
            )
        }
        InfoCard(
            stringResource(
                if (result.reachedDestination) {
                    R.string.traceroute_reached
                } else {
                    R.string.traceroute_not_reached
                },
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PingScreenPreview() {
    DebugToolkitTheme {
        PingScreen(
            state = PingUiState(
                input = "example.com",
                pingResult = ToolState.Success(
                    PingResult(
                        host = "example.com",
                        transmitted = 4,
                        received = 4,
                        packetLossPercent = 0.0,
                        probes = listOf(
                            co.sorsby.debugtoolkit.core.model.PingProbe(1, 12.4),
                            co.sorsby.debugtoolkit.core.model.PingProbe(2, 11.9),
                        ),
                        minMs = 11.9,
                        avgMs = 12.1,
                        maxMs = 12.4,
                        jitterMs = 0.5,
                    ),
                ),
            ),
            onInputChanged = {},
            onModeChanged = {},
            onRun = {},
        )
    }
}
