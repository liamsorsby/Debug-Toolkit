package co.sorsby.debugtoolkit.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import co.sorsby.debugtoolkit.R
import co.sorsby.debugtoolkit.domain.CidrBlock
import co.sorsby.debugtoolkit.domain.CidrCalculator
import co.sorsby.debugtoolkit.domain.CidrResult
import co.sorsby.debugtoolkit.domain.Ipv6Block
import co.sorsby.debugtoolkit.ui.components.Metric
import co.sorsby.debugtoolkit.ui.components.ResultCard
import co.sorsby.debugtoolkit.ui.components.ScreenList
import co.sorsby.debugtoolkit.ui.components.ToolInputCard
import co.sorsby.debugtoolkit.ui.components.ToolIntroCard
import co.sorsby.debugtoolkit.ui.theme.DebugToolkitTheme
import java.text.NumberFormat

@Composable
fun CidrCalculatorRoute() {
    var input by rememberSaveable { mutableStateOf("") }
    CidrCalculatorScreen(input = input, onInputChanged = { input = it })
}

@Composable
fun CidrCalculatorScreen(input: String, onInputChanged: (String) -> Unit) {
    // The calculation is instant and local, so it follows the input rather than needing a
    // run button. Nothing is shown as an error until the user has typed something.
    val block = if (input.isBlank()) null else runCatching { CidrCalculator.calculateAny(input) }
    ScreenList(showAds = true) {
        item {
            ToolIntroCard(
                icon = Icons.Default.Calculate,
                description = stringResource(R.string.cidr_intro),
            )
        }
        item {
            ToolInputCard {
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChanged,
                    label = { Text(stringResource(R.string.cidr_input_label)) },
                    placeholder = { Text(stringResource(R.string.cidr_input_placeholder)) },
                    singleLine = true,
                    isError = block?.isFailure == true,
                    supportingText = if (block?.isFailure == true) {
                        { Text(stringResource(R.string.cidr_error)) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        block?.getOrNull()?.let { result ->
            item {
                when (result) {
                    is CidrResult.V4 -> CidrResultView(result.block)
                    is CidrResult.V6 -> Ipv6ResultView(result.block)
                }
            }
        }
    }
}

@Composable
private fun CidrResultView(block: CidrBlock) {
    val count = NumberFormat.getIntegerInstance()
    ResultCard {
        Metric(stringResource(R.string.cidr_notation_label), block.cidr)
        Metric(stringResource(R.string.cidr_network_label), block.networkAddress)
        Metric(
            stringResource(R.string.cidr_broadcast_label),
            block.broadcastAddress ?: stringResource(R.string.cidr_not_applicable),
        )
        Metric(stringResource(R.string.cidr_mask_label), block.subnetMask)
        Metric(stringResource(R.string.cidr_wildcard_label), block.wildcardMask)
        Metric(stringResource(R.string.cidr_first_host_label), block.firstHost)
        Metric(stringResource(R.string.cidr_last_host_label), block.lastHost)
        Metric(stringResource(R.string.cidr_total_label), count.format(block.totalAddresses))
        Metric(stringResource(R.string.cidr_hosts_label), count.format(block.usableHosts))
    }
}

@Composable
private fun Ipv6ResultView(block: Ipv6Block) {
    val count = NumberFormat.getIntegerInstance()
    val hostBits = 128 - block.prefixLength
    // 2^128 addresses is too long to read, so the exponent leads and the exact count follows.
    val total = "2^$hostBits (${count.format(block.totalAddresses)})"
    ResultCard {
        Metric(stringResource(R.string.cidr_notation_label), block.cidr)
        Metric(stringResource(R.string.cidr_network_label), block.networkAddress)
        Metric(stringResource(R.string.cidr_broadcast_label), stringResource(R.string.cidr_not_applicable))
        Metric(stringResource(R.string.cidr_last_address_label), block.lastAddress)
        Metric(stringResource(R.string.cidr_mask_label), block.subnetMask)
        Metric(stringResource(R.string.cidr_total_label), total)
        Metric(stringResource(R.string.cidr_hosts_label), total)
        Metric(
            stringResource(R.string.cidr_subnets64_label),
            block.subnets64?.let { count.format(it) } ?: stringResource(R.string.cidr_not_applicable),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CidrCalculatorScreenPreview() {
    DebugToolkitTheme {
        CidrCalculatorScreen(input = "192.0.2.10/24", onInputChanged = {})
    }
}
