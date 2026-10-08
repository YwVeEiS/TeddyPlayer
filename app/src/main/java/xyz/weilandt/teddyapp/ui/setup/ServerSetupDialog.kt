package xyz.weilandt.teddyapp.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import xyz.weilandt.teddyapp.domain.model.ServerUrl
import xyz.weilandt.teddyapp.ui.parent.settings.ConnectionTest
import xyz.weilandt.teddyapp.ui.theme.TeddyColors
import xyz.weilandt.teddyapp.ui.theme.TeddyTheme

@Composable
fun ServerSetupRoute(viewModel: ServerSetupViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ServerSetupDialog(state, viewModel::onIntent)
}

/** Erscheint beim ersten Start; lässt sich nicht wegklicken, bis eine Adresse gespeichert ist. */
@Composable
fun ServerSetupDialog(
    state: ServerSetupState,
    onIntent: (ServerSetupIntent) -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        icon = { Icon(Icons.Rounded.Dns, contentDescription = null) },
        title = { Text("TeddyCloud verbinden") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Unter welcher Adresse ist dein TeddyCloud-Server im Heimnetz erreichbar?")
                OutlinedTextField(
                    value = state.urlInput,
                    onValueChange = { onIntent(ServerSetupIntent.UrlChanged(it)) },
                    label = { Text("Adresse") },
                    placeholder = { Text(ServerUrl.DEFAULT) },
                    singleLine = true,
                    enabled = !state.isTesting,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { onIntent(ServerSetupIntent.Connect) }),
                    modifier = Modifier.fillMaxWidth(),
                )
                SetupStatus(state.connection)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = state.showTitles,
                            role = Role.Switch,
                            enabled = !state.isTesting,
                            onValueChange = { onIntent(ServerSetupIntent.ShowTitlesChanged(it)) },
                        ),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Titel anzeigen", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Namen unter den Tonies – für Kinder, die schon lesen können. Später im Elternbereich änderbar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TeddyColors.Muted,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(checked = state.showTitles, onCheckedChange = null, enabled = !state.isTesting)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onIntent(ServerSetupIntent.Connect) }, enabled = !state.isTesting) {
                if (state.isTesting) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = TeddyColors.OnPrimary)
                } else {
                    Text("Verbinden")
                }
            }
        },
        dismissButton = {
            if (state.canSaveAnyway) {
                TextButton(onClick = { onIntent(ServerSetupIntent.SaveAnyway) }) { Text("Trotzdem speichern") }
            }
        },
    )
}

@Composable
private fun SetupStatus(connection: ConnectionTest) {
    val (text, color) = when (connection) {
        ConnectionTest.Idle, ConnectionTest.Testing -> return
        is ConnectionTest.Success -> "Verbunden – ${connection.tonieCount} Tonies gefunden" to TeddyColors.Success
        ConnectionTest.Failed -> "Server nicht erreichbar. Adresse prüfen oder trotzdem speichern." to TeddyColors.Primary
        ConnectionTest.InvalidUrl -> "Bitte eine Adresse eingeben" to TeddyColors.Primary
    }
    Text(text, color = color, style = MaterialTheme.typography.bodyMedium)
}

internal class ServerSetupStateProvider : PreviewParameterProvider<ServerSetupState> {
    override val values = sequenceOf(
        ServerSetupState(),
        ServerSetupState(showTitles = true),
        ServerSetupState(connection = ConnectionTest.Testing),
        ServerSetupState(urlInput = "http://192.168.1.20", connection = ConnectionTest.Failed),
        ServerSetupState(urlInput = "", connection = ConnectionTest.InvalidUrl),
        ServerSetupState(connection = ConnectionTest.Success(68)),
    )
}

@Preview(showBackground = true, widthDp = 400, heightDp = 720)
@Composable
private fun ServerSetupDialogPreview(@PreviewParameter(ServerSetupStateProvider::class) state: ServerSetupState) = TeddyTheme {
    ServerSetupDialog(state, onIntent = {})
}
