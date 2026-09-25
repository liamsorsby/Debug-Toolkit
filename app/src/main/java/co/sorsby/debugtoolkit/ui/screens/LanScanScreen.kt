package co.sorsby.debugtoolkit.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.sorsby.debugtoolkit.R
import co.sorsby.debugtoolkit.core.model.LanDevice
import co.sorsby.debugtoolkit.core.model.LanScanResult
import co.sorsby.debugtoolkit.core.model.ToolState
import co.sorsby.debugtoolkit.feature.LanScanUiState
import co.sorsby.debugtoolkit.feature.LanScanViewModel
import co.sorsby.debugtoolkit.ui.components.InfoCard
import co.sorsby.debugtoolkit.ui.components.Metric
import co.sorsby.debugtoolkit.ui.components.ResultCard
import co.sorsby.debugtoolkit.ui.components.ResultHeader
import co.sorsby.debugtoolkit.ui.components.ScreenList
import co.sorsby.debugtoolkit.ui.components.SectionLabel
import co.sorsby.debugtoolkit.ui.components.ToolInputCard
import co.sorsby.debugtoolkit.ui.components.ToolIntroCard
import co.sorsby.debugtoolkit.ui.components.ToolResult
import co.sorsby.debugtoolkit.ui.theme.DebugToolkitTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun LanScanRoute(viewModel: LanScanViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LanScanScreen(state = state, onScan = viewModel::scan)
}

@Composable
fun LanScanScreen(state: LanScanUiState, onScan: () -> Unit) {
    ScreenList {
        item {
            ToolIntroCard(
                icon = Icons.Default.DeviceHub,
                description = stringResource(R.string.lanscan_intro),
            )
        }
        item {
            ToolInputCard {
                Button(
                    onClick = onScan,
                    enabled = state.result !is ToolState.Loading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.lanscan_action))
                }
            }
        }
        item { ToolResult(state.result) { LanScanResultView(it) } }
    }
}

@Composable
private fun LanScanResultView(result: LanScanResult) {
    ResultCard {
        ResultHeader()
        Metric(stringResource(R.string.lanscan_subnet_label), result.subnetCidr)
        Metric(
            stringResource(R.string.lanscan_addresses_scanned_label),
            result.addressesScanned.toString(),
        )
        Metric(
            stringResource(R.string.lanscan_timing),
            stringResource(R.string.duration_milliseconds_integer, result.elapsedMs),
        )
        if (result.devices.isEmpty()) {
            InfoCard(stringResource(R.string.lanscan_no_devices))
        } else {
            SectionLabel(stringResource(R.string.tool_results_title))
            result.devices.forEach { device -> LanDeviceRow(device) }
        }
    }
}

@Composable
private fun LanDeviceRow(device: LanDevice) {
    Metric(
        device.ipAddress,
        stringResource(
            R.string.lanscan_device_response_time,
            device.hostname ?: stringResource(R.string.lanscan_device_hostname_unknown),
            device.responseTimeMs,
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun LanScanScreenPreview() {
    DebugToolkitTheme {
        LanScanScreen(
            state = LanScanUiState(
                result = ToolState.Success(
                    LanScanResult(
                        subnetCidr = "192.168.1.0/24",
                        devices = listOf(
                            LanDevice("192.168.1.1", "router.local", 4),
                            LanDevice("192.168.1.42", null, 12),
                        ),
                        addressesScanned = 253,
                        elapsedMs = 6200,
                    ),
                ),
            ),
            onScan = {},
        )
    }
}
