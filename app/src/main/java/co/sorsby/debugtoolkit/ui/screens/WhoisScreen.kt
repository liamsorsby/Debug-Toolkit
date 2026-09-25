package co.sorsby.debugtoolkit.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Domain
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
import co.sorsby.debugtoolkit.core.model.ToolState
import co.sorsby.debugtoolkit.core.model.WhoisResult
import co.sorsby.debugtoolkit.feature.WhoisUiState
import co.sorsby.debugtoolkit.feature.WhoisViewModel
import co.sorsby.debugtoolkit.ui.components.InfoCard
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
fun WhoisRoute(viewModel: WhoisViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    WhoisScreen(
        state = state,
        onInputChanged = viewModel::setInput,
        onLookup = viewModel::lookup,
    )
}

@Composable
fun WhoisScreen(
    state: WhoisUiState,
    onInputChanged: (String) -> Unit,
    onLookup: () -> Unit,
) {
    ScreenList {
        item {
            ToolIntroCard(
                icon = Icons.Default.Domain,
                description = stringResource(R.string.whois_intro),
            )
        }
        item {
            ToolInputCard {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = onInputChanged,
                    label = { Text(stringResource(R.string.whois_domain_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                RunToolButton(
                    text = stringResource(R.string.whois_action),
                    state = state.result,
                    onClick = onLookup,
                    enabled = state.input.isNotBlank(),
                )
            }
        }
        item { ToolResult(state.result) { WhoisResultView(it) } }
    }
}

@Composable
private fun WhoisResultView(result: WhoisResult) {
    ResultCard {
        ResultHeader()
        Metric(stringResource(R.string.whois_server_label), result.server)
        TimingMetric(result.elapsedMs)
        InfoCard(result.rawText)
    }
}

@Preview(showBackground = true)
@Composable
private fun WhoisScreenPreview() {
    DebugToolkitTheme {
        WhoisScreen(
            state = WhoisUiState(
                input = "example.com",
                result = ToolState.Success(
                    WhoisResult(
                        domain = "example.com",
                        server = "whois.iana.org",
                        rawText = "domain: EXAMPLE.COM\nstatus: ACTIVE",
                        elapsedMs = 240,
                    ),
                ),
            ),
            onInputChanged = {},
            onLookup = {},
        )
    }
}
