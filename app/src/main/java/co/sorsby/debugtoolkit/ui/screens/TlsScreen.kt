package co.sorsby.debugtoolkit.ui.screens

import android.icu.text.ListFormatter
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.sorsby.debugtoolkit.R
import co.sorsby.debugtoolkit.core.model.CertificateInfo
import co.sorsby.debugtoolkit.core.model.TlsResult
import co.sorsby.debugtoolkit.core.model.ToolState
import co.sorsby.debugtoolkit.feature.TlsUiState
import co.sorsby.debugtoolkit.feature.TlsViewModel
import co.sorsby.debugtoolkit.ui.components.EndpointField
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun TlsRoute(viewModel: TlsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TlsScreen(
        state = state,
        onInputChanged = viewModel::setInput,
        onInspect = viewModel::inspect,
    )
}

@Composable
fun TlsScreen(
    state: TlsUiState,
    onInputChanged: (String) -> Unit,
    onInspect: () -> Unit,
) {
    ScreenList {
        item {
            ToolIntroCard(
                icon = Icons.Default.Security,
                description = stringResource(R.string.tls_intro),
            )
        }
        item {
            ToolInputCard {
                EndpointField(state.input, onInputChanged)
                Button(
                    onClick = onInspect,
                    enabled = state.result !is ToolState.Loading && state.input.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.tls_action))
                }
            }
        }
        item { ToolResult(state.result) { TlsResultView(it) } }
    }
}

@Composable
private fun TlsResultView(result: TlsResult) {
    ResultCard {
        ResultHeader()
        Metric(stringResource(R.string.tls_connection), "${result.host}:${result.port}")
        Metric(stringResource(R.string.tls_protocol), result.protocol)
        Metric(stringResource(R.string.tls_cipher_suite), result.cipherSuite)
        Metric(
            stringResource(R.string.tls_timing),
            stringResource(R.string.duration_milliseconds_integer, result.elapsedMs),
        )
        result.certificates.forEachIndexed { index, certificate ->
            SectionLabel(stringResource(R.string.tls_certificate_number, index + 1))
            Metric(stringResource(R.string.tls_subject), certificate.subject)
            Metric(stringResource(R.string.tls_issuer), certificate.issuer)
            Metric(
                stringResource(R.string.tls_valid_from),
                formatLocalizedInstant(certificate.validFrom),
            )
            Metric(
                stringResource(R.string.tls_valid_until),
                formatLocalizedInstant(certificate.validUntil),
            )
            Metric(stringResource(R.string.tls_serial_number), certificate.serialNumber)
            Metric(
                stringResource(R.string.tls_algorithms),
                "${certificate.publicKeyAlgorithm} / ${certificate.signatureAlgorithm}",
            )
            Metric(
                stringResource(R.string.tls_subject_alternative_names),
                ListFormatter.getInstance().format(certificate.subjectAlternativeNames),
            )
        }
    }
}

private fun formatLocalizedInstant(instant: Instant): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withZone(ZoneId.systemDefault())
        .format(instant)

@Preview(showBackground = true)
@Composable
private fun TlsScreenPreview() {
    val certificate = CertificateInfo(
        subject = "CN=example.com",
        issuer = "CN=Example CA",
        serialNumber = "01",
        validFrom = Instant.parse("2026-01-01T00:00:00Z"),
        validUntil = Instant.parse("2027-01-01T00:00:00Z"),
        signatureAlgorithm = "SHA256withRSA",
        publicKeyAlgorithm = "RSA",
        subjectAlternativeNames = listOf("example.com", "www.example.com"),
    )
    DebugToolkitTheme {
        TlsScreen(
            state = TlsUiState(
                input = "example.com",
                result = ToolState.Success(
                    TlsResult("example.com", 443, "TLSv1.3", "TLS_AES_128_GCM_SHA256", listOf(certificate), 80),
                ),
            ),
            onInputChanged = {},
            onInspect = {},
        )
    }
}
