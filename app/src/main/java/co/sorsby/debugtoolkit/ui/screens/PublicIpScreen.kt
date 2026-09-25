package co.sorsby.debugtoolkit.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.sorsby.debugtoolkit.R
import co.sorsby.debugtoolkit.core.model.PublicIpResult
import co.sorsby.debugtoolkit.core.model.ToolState
import co.sorsby.debugtoolkit.feature.PublicIpUiState
import co.sorsby.debugtoolkit.feature.PublicIpViewModel
import co.sorsby.debugtoolkit.ui.components.Metric
import co.sorsby.debugtoolkit.ui.components.ResultCard
import co.sorsby.debugtoolkit.ui.components.ResultHeader
import co.sorsby.debugtoolkit.ui.components.RunToolButton
import co.sorsby.debugtoolkit.ui.components.ScreenList
import co.sorsby.debugtoolkit.ui.components.TimingMetric
import co.sorsby.debugtoolkit.ui.components.ToolInputCard
import co.sorsby.debugtoolkit.ui.components.ToolIntroCard
import co.sorsby.debugtoolkit.ui.components.ToolResult
import co.sorsby.debugtoolkit.ui.components.yesNo
import co.sorsby.debugtoolkit.ui.theme.DebugToolkitTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun PublicIpRoute(viewModel: PublicIpViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PublicIpScreen(state = state, onLookup = viewModel::lookup)
}

@Composable
fun PublicIpScreen(state: PublicIpUiState, onLookup: () -> Unit) {
    ScreenList {
        item {
            ToolIntroCard(
                icon = Icons.Default.Public,
                description = stringResource(R.string.publicip_intro),
            )
        }
        item {
            ToolInputCard {
                RunToolButton(
                    text = stringResource(R.string.publicip_action),
                    state = state.result,
                    onClick = onLookup,
                )
            }
        }
        item { ToolResult(state.result) { PublicIpResultView(it) } }
    }
}

@Composable
private fun PublicIpResultView(result: PublicIpResult) {
    ResultCard {
        ResultHeader()
        Metric(stringResource(R.string.publicip_address_label), result.ipAddress)
        Metric(
            stringResource(R.string.publicip_country_label),
            result.countryCode ?: stringResource(R.string.value_unavailable),
        )
        Metric(
            stringResource(R.string.publicip_colo_label),
            result.cloudflareColo ?: stringResource(R.string.value_unavailable),
        )
        Metric(stringResource(R.string.publicip_warp_label), yesNo(result.warpEnabled))
        TimingMetric(result.elapsedMs)
    }
}

@Preview(showBackground = true)
@Composable
private fun PublicIpScreenPreview() {
    DebugToolkitTheme {
        PublicIpScreen(
            state = PublicIpUiState(
                result = ToolState.Success(
                    PublicIpResult(
                        ipAddress = "203.0.113.10",
                        countryCode = "GB",
                        cloudflareColo = "LHR",
                        warpEnabled = false,
                        elapsedMs = 80,
                    ),
                ),
            ),
            onLookup = {},
        )
    }
}
