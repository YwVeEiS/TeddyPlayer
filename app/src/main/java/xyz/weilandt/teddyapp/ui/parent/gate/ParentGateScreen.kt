package xyz.weilandt.teddyapp.ui.parent.gate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import xyz.weilandt.teddyapp.ui.theme.TeddyColors
import xyz.weilandt.teddyapp.ui.theme.TeddyTheme

@Composable
fun ParentGateRoute(
    onUnlocked: () -> Unit,
    onCancel: () -> Unit,
    viewModel: ParentGateViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect {
            when (it) {
                ParentGateEffect.Unlocked -> onUnlocked()
                ParentGateEffect.Cancelled -> onCancel()
            }
        }
    }
    ParentGateScreen(state, viewModel::onIntent)
}

@Composable
fun ParentGateScreen(
    state: ParentGateState,
    onIntent: (ParentGateIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TeddyColors.Background)
            .systemBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = { onIntent(ParentGateIntent.Cancel) }) {
                Icon(Icons.Rounded.Close, contentDescription = "Abbrechen")
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Nur für Eltern", style = MaterialTheme.typography.titleMedium, color = TeddyColors.Muted)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "${state.a} × ${state.b} = ?",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = state.input.ifEmpty { " " },
            fontSize = 40.sp,
            fontWeight = FontWeight.Medium,
            color = TeddyColors.Primary,
        )
        Text(
            text = if (state.isWrong) "Leider falsch – neue Aufgabe" else " ",
            color = TeddyColors.Warning,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.weight(1f))
        NumberPad(onDigit = { onIntent(ParentGateIntent.Digit(it)) }, onDelete = { onIntent(ParentGateIntent.Delete) })
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun NumberPad(onDigit: (Int) -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val rows = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9), listOf(null, 0, -1))
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { key ->
                    Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                        when (key) {
                            null -> Unit
                            -1 -> IconButton(onClick = onDelete, modifier = Modifier.size(80.dp)) {
                                Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = "Löschen")
                            }
                            else -> FilledTonalButton(
                                onClick = { onDigit(key) },
                                shape = CircleShape,
                                modifier = Modifier.size(80.dp),
                            ) {
                                Text(key.toString(), fontSize = 28.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

internal class ParentGateStateProvider : PreviewParameterProvider<ParentGateState> {
    override val values = sequenceOf(
        ParentGateState(a = 7, b = 8),
        ParentGateState(a = 7, b = 8, input = "5"),
        ParentGateState(a = 4, b = 6, isWrong = true),
    )
}

@Preview(showBackground = true, widthDp = 400, heightDp = 860)
@Composable
private fun ParentGateScreenPreview(@PreviewParameter(ParentGateStateProvider::class) state: ParentGateState) = TeddyTheme {
    ParentGateScreen(state, onIntent = {})
}

@Preview(showBackground = true)
@Composable
private fun NumberPadPreview() = TeddyTheme {
    NumberPad(onDigit = {}, onDelete = {}, modifier = Modifier.padding(16.dp))
}
