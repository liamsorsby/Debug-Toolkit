package co.sorsby.debugtoolkit.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.OutlinedButton
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
import co.sorsby.debugtoolkit.core.model.PortScanEntry
import co.sorsby.debugtoolkit.core.model.PortScanResult
import co.sorsby.debugtoolkit.core.model.PortState
import co.sorsby.debugtoolkit.core.model.ToolState
import co.sorsby.debugtoolkit.feature.PortScanUiState
import co.sorsby.debugtoolkit.feature.PortScanViewModel
import co.sorsby.debugtoolkit.ui.components.Metric
import co.sorsby.debugtoolkit.ui.components.ResultCard
import co.sorsby.debugtoolkit.ui.components.ResultHeader
import co.sorsby.debugtoolkit.ui.components.RunToolButton
import co.sorsby.debugtoolkit.ui.components.ScreenList
import co.sorsby.debugtoolkit.ui.components.TimingMetric
import co.sorsby.debugtoolkit.ui.components.ToolInputCard
import co.sorsby.debugtoolkit.ui.components.ToolIntroCard
import co.sorsby.debugtoolkit.ui.components.ToolResult
import co.sorsby.debugtoolkit.ui.theme.DebugToolkitTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun PortScanRoute(viewModel: PortScanViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PortScanScreen(
        state = state,
        onHostChanged = viewModel::setHost,
        onPortsChanged = viewModel::setPorts,
        onUseCommonPorts = viewModel::useCommonPorts,
        onScan = viewModel::scan,
    )
}

@Composable
fun PortScanScreen(
    state: PortScanUiState,
    onHostChanged: (String) -> Unit,
    onPortsChanged: (String) -> Unit,
    onUseCommonPorts: () -> Unit,
    onScan: () -> Unit,
) {
    ScreenList {
        item {
            ToolIntroCard(
                icon = Icons.Default.Radar,
                description = stringResource(R.string.portscan_intro),
            )
        }
        item {
            ToolInputCard {
                OutlinedTextField(
                    value = state.host,
                    onValueChange = onHostChanged,
                    label = { Text(stringResource(R.string.portscan_host_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.ports,
                    onValueChange = onPortsChanged,
                    label = { Text(stringResource(R.string.portscan_ports_label)) },
                    supportingText = { Text(stringResource(R.string.portscan_ports_helper)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(
                    onClick = onUseCommonPorts,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.portscan_use_common_action))
                }
                RunToolButton(
                    text = stringResource(R.string.portscan_action),
                    state = state.result,
                    onClick = onScan,
                    enabled = state.host.isNotBlank() && state.ports.isNotBlank(),
                )
            }
        }
        item { ToolResult(state.result) { PortScanResultView(it) } }
    }
}

@Composable
private fun PortScanResultView(result: PortScanResult) {
    ResultCard {
        ResultHeader()
        TimingMetric(result.elapsedMs)
        result.entries.forEach { entry -> PortEntryRow(entry) }
    }
}

@Composable
private fun PortEntryRow(entry: PortScanEntry) {
    val stateLabel = stringResource(
        when (entry.state) {
            PortState.OPEN -> R.string.portscan_state_open
            PortState.CLOSED -> R.string.portscan_state_closed
            PortState.FILTERED -> R.string.portscan_state_filtered
        },
    )
    Metric(stringResource(R.string.portscan_port_label, entry.port), stateLabel)
}

@Preview(showBackground = true)
@Composable
private fun PortScanScreenPreview() {
    DebugToolkitTheme {
        PortScanScreen(
            state = PortScanUiState(
                host = "example.com",
                ports = "22,80,443",
                result = ToolState.Success(
                    PortScanResult(
                        host = "example.com",
                        entries = listOf(
                            PortScanEntry(22, PortState.FILTERED),
                            PortScanEntry(80, PortState.OPEN),
                            PortScanEntry(443, PortState.OPEN),
                        ),
                        elapsedMs = 512,
                    ),
                ),
            ),
            onHostChanged = {},
            onPortsChanged = {},
            onUseCommonPorts = {},
            onScan = {},
        )
    }
}
