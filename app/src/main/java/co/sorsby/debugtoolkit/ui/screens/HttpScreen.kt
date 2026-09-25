package co.sorsby.debugtoolkit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import co.sorsby.debugtoolkit.core.model.HeaderValue
import co.sorsby.debugtoolkit.core.model.HttpInspection
import co.sorsby.debugtoolkit.core.model.ToolState
import co.sorsby.debugtoolkit.data.http.HttpMethod
import co.sorsby.debugtoolkit.feature.HttpUiState
import co.sorsby.debugtoolkit.feature.HttpViewModel
import co.sorsby.debugtoolkit.ui.components.EndpointField
import co.sorsby.debugtoolkit.ui.components.InfoCard
import co.sorsby.debugtoolkit.ui.components.Metric
import co.sorsby.debugtoolkit.ui.components.ResultCard
import co.sorsby.debugtoolkit.ui.components.ResultHeader
import co.sorsby.debugtoolkit.ui.components.RunToolButton
import co.sorsby.debugtoolkit.ui.components.ScreenList
import co.sorsby.debugtoolkit.ui.components.SectionLabel
import co.sorsby.debugtoolkit.ui.components.TimingMetric
import co.sorsby.debugtoolkit.ui.components.ToolInputCard
import co.sorsby.debugtoolkit.ui.components.ToolIntroCard
import co.sorsby.debugtoolkit.ui.components.ToolResult
import co.sorsby.debugtoolkit.ui.theme.DebugToolkitTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun HttpRoute(viewModel: HttpViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HttpScreen(
        state = state,
        onInputChanged = viewModel::setInput,
        onMethodChanged = viewModel::setMethod,
        onShowResponseBodyChanged = viewModel::setShowResponseBody,
        onInspect = viewModel::inspect,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HttpScreen(
    state: HttpUiState,
    onInputChanged: (String) -> Unit,
    onMethodChanged: (HttpMethod) -> Unit,
    onShowResponseBodyChanged: (Boolean) -> Unit,
    onInspect: () -> Unit,
) {
    ScreenList {
        item {
            ToolIntroCard(
                icon = Icons.Default.Language,
                description = stringResource(R.string.http_intro),
            )
        }
        item {
            ToolInputCard {
                EndpointField(state.input, onInputChanged)
                Text(
                    stringResource(R.string.http_method),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    HttpMethod.entries.forEachIndexed { index, method ->
                        SegmentedButton(
                            selected = state.method == method,
                            onClick = { onMethodChanged(method) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = HttpMethod.entries.size,
                            ),
                            label = { Text(method.name) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .toggleable(
                            value = state.showResponseBody,
                            onValueChange = onShowResponseBodyChanged,
                            role = Role.Checkbox,
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.http_show_response_body))
                    Checkbox(
                        checked = state.showResponseBody,
                        onCheckedChange = null,
                    )
                }
                RunToolButton(
                    text = stringResource(R.string.http_action),
                    state = state.result,
                    onClick = onInspect,
                    enabled = state.input.isNotBlank(),
                )
            }
        }
        item {
            ToolResult(state.result) {
                HttpResultView(it, showResponseBody = state.showResponseBody)
            }
        }
    }
}

@Composable
private fun HttpResultView(result: HttpInspection, showResponseBody: Boolean) {
    ResultCard {
        ResultHeader()
        Metric(stringResource(R.string.http_status), "${result.status} ${result.message}")
        Metric(stringResource(R.string.http_final_url), result.finalUrl)
        Metric(stringResource(R.string.http_protocol), result.protocol)
        TimingMetric(result.elapsedMs)
        result.redirects.forEach {
            Metric(stringResource(R.string.http_redirect, it.status), "${it.from}\n${it.to}")
        }
        SectionLabel(stringResource(R.string.http_headers))
        result.headers.forEach { Metric(it.name, it.value) }
        if (showResponseBody) {
            result.bodyPreview?.let {
                SectionLabel(
                    stringResource(
                        if (result.bodyTruncated) R.string.http_body_preview_truncated
                        else R.string.http_body_preview,
                    ),
                )
                InfoCard(it)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HttpScreenPreview() {
    DebugToolkitTheme {
        HttpScreen(
            state = HttpUiState(
                input = "https://example.com",
                showResponseBody = true,
                result = ToolState.Success(
                    HttpInspection(
                        status = 200,
                        message = "OK",
                        protocol = "h2",
                        finalUrl = "https://example.com",
                        elapsedMs = 120,
                        headers = listOf(HeaderValue("content-type", "text/html")),
                        redirects = emptyList(),
                        bodyPreview = "<html>...</html>",
                        bodyTruncated = false,
                        contentType = "text/html",
                    ),
                ),
            ),
            onInputChanged = {},
            onMethodChanged = {},
            onShowResponseBodyChanged = {},
            onInspect = {},
        )
    }
}
