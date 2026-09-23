package co.sorsby.debugtoolkit.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import co.sorsby.debugtoolkit.R
import co.sorsby.debugtoolkit.core.model.DnsRecord
import co.sorsby.debugtoolkit.core.model.DnsResult
import co.sorsby.debugtoolkit.core.model.ToolState
import co.sorsby.debugtoolkit.data.dns.DnsRecordType
import co.sorsby.debugtoolkit.feature.DnsUiState
import co.sorsby.debugtoolkit.feature.DnsViewModel
import co.sorsby.debugtoolkit.ui.components.InfoCard
import co.sorsby.debugtoolkit.ui.components.Metric
import co.sorsby.debugtoolkit.ui.components.ResultCard
import co.sorsby.debugtoolkit.ui.components.ResultHeader
import co.sorsby.debugtoolkit.ui.components.ScreenList
import co.sorsby.debugtoolkit.ui.components.ToolInputCard
import co.sorsby.debugtoolkit.ui.components.ToolIntroCard
import co.sorsby.debugtoolkit.ui.components.ToolResult
import co.sorsby.debugtoolkit.ui.components.yesNo
import co.sorsby.debugtoolkit.ui.theme.DebugToolkitTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun DnsRoute(viewModel: DnsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DnsScreen(
        state = state,
        onInputChanged = viewModel::setInput,
        onTypeChanged = viewModel::setType,
        onQuery = viewModel::query,
    )
}

@Composable
fun DnsScreen(
    state: DnsUiState,
    onInputChanged: (String) -> Unit,
    onTypeChanged: (DnsRecordType) -> Unit,
    onQuery: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ScreenList {
        item {
            ToolIntroCard(
                icon = Icons.Default.Dns,
                description = stringResource(R.string.dns_intro),
            )
        }
        item {
            ToolInputCard {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = onInputChanged,
                    label = {
                        Text(
                            stringResource(
                                if (state.type == DnsRecordType.PTR) R.string.dns_ip_label
                                else R.string.dns_domain_label,
                            ),
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (state.type == DnsRecordType.PTR) {
                            KeyboardType.Ascii
                        } else {
                            KeyboardType.Uri
                        },
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.dns_record_type),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(state.type.name)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        DnsRecordType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    onTypeChanged(type)
                                    expanded = false
                                },
                            )
                        }
                    }
                }
                Button(
                    onClick = onQuery,
                    enabled = state.result !is ToolState.Loading && state.input.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.dns_query_action))
                }
            }
        }
        item { ToolResult(state.result) { DnsResultView(it) } }
    }
}

@Composable
private fun DnsResultView(result: DnsResult) {
    ResultCard {
        ResultHeader()
        Metric(stringResource(R.string.dns_status), result.status.toString())
        Metric(
            stringResource(R.string.dns_timing),
            stringResource(R.string.duration_milliseconds_integer, result.elapsedMs),
        )
        Metric(stringResource(R.string.dns_authenticated), yesNo(result.authenticatedData))
        result.records.forEach {
            Metric(stringResource(R.string.dns_answer_label, it.name, it.ttlSeconds), it.value)
        }
        if (result.records.isEmpty()) {
            InfoCard(stringResource(R.string.dns_no_answers))
        }
        result.authority.forEach {
            Metric(stringResource(R.string.dns_authority_label, it.name, it.ttlSeconds), it.value)
        }
        result.additional.forEach {
            Metric(stringResource(R.string.dns_additional_label, it.name, it.ttlSeconds), it.value)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DnsScreenPreview() {
    DebugToolkitTheme {
        DnsScreen(
            state = DnsUiState(
                input = "example.com",
                result = ToolState.Success(
                    DnsResult(
                        status = 0,
                        authenticatedData = true,
                        recursionAvailable = true,
                        records = listOf(DnsRecord("example.com", 1, 300, "192.0.2.1")),
                        authority = emptyList(),
                        additional = emptyList(),
                        elapsedMs = 42,
                    ),
                ),
            ),
            onInputChanged = {},
            onTypeChanged = {},
            onQuery = {},
        )
    }
}
