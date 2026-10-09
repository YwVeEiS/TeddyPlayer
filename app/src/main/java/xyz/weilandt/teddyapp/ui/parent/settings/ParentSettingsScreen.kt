package xyz.weilandt.teddyapp.ui.parent.settings

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import xyz.weilandt.teddyapp.R
import xyz.weilandt.teddyapp.domain.model.DownloadStatus
import xyz.weilandt.teddyapp.domain.model.ServerUrl
import xyz.weilandt.teddyapp.ui.components.DownloadBadge
import xyz.weilandt.teddyapp.ui.components.NotificationPermission
import xyz.weilandt.teddyapp.ui.components.TonieCover
import xyz.weilandt.teddyapp.ui.components.rememberNotificationPermission
import xyz.weilandt.teddyapp.ui.preview.SampleData
import xyz.weilandt.teddyapp.ui.theme.TeddyColors
import xyz.weilandt.teddyapp.ui.theme.TeddyTheme

@Composable
fun ParentSettingsRoute(
    onBack: () -> Unit,
    viewModel: ParentSettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect {
            when (it) {
                ParentSettingsEffect.NavigateBack -> onBack()
            }
        }
    }
    ParentSettingsScreen(state, viewModel::onIntent, rememberNotificationPermission(openSettingsWhenBlocked = true))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentSettingsScreen(
    state: ParentSettingsState,
    onIntent: (ParentSettingsIntent) -> Unit,
    notifications: NotificationPermission? = null,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.parent_area)) },
                navigationIcon = {
                    IconButton(onClick = { onIntent(ParentSettingsIntent.Back) }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            // On tablets, a readable centered column instead of rows across the full width
            modifier = Modifier
                .fillMaxSize()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 720.dp),
        ) {
            item { ServerSection(state, onIntent) }
            item { HorizontalDivider() }
            item {
                DisplaySection(
                    showTitles = state.showTitles,
                    onShowTitlesChange = { onIntent(ParentSettingsIntent.ShowTitlesChanged(it)) },
                )
            }
            item { HorizontalDivider() }
            if (notifications != null && !notifications.isGranted) {
                item { NotificationSection(onAllow = notifications::request) }
                item { HorizontalDivider() }
            }
            item { StorageSection(state, onIntent) }
            items(state.downloads, key = { it.tonie.id }) { entry ->
                DownloadRow(entry, onDelete = { onIntent(ParentSettingsIntent.DeleteDownload(entry.tonie.id)) })
            }
            if (state.downloads.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.settings_no_downloads),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TeddyColors.Muted,
                    )
                }
            }
        }
    }

    if (state.confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { onIntent(ParentSettingsIntent.DeleteAllDismissed) },
            title = { Text(stringResource(R.string.settings_delete_all_title)) },
            text = { Text(stringResource(R.string.settings_delete_all_message)) },
            confirmButton = {
                TextButton(onClick = { onIntent(ParentSettingsIntent.DeleteAllConfirmed) }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(ParentSettingsIntent.DeleteAllDismissed) }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun ServerSection(state: ParentSettingsState, onIntent: (ParentSettingsIntent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.settings_server), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = state.serverUrlInput,
            onValueChange = { onIntent(ParentSettingsIntent.UrlChanged(it)) },
            label = { Text(stringResource(R.string.address)) },
            placeholder = { Text(ServerUrl.DEFAULT) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { onIntent(ParentSettingsIntent.SaveUrl) },
                enabled = state.connection != ConnectionTest.Testing,
            ) {
                Text(
                    stringResource(
                        if (state.isUrlChanged) R.string.settings_check_and_save else R.string.settings_check_connection
                    )
                )
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = { onIntent(ParentSettingsIntent.Refresh) },
                enabled = !state.isRefreshing,
            ) {
                if (state.isRefreshing) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.settings_reload_list))
            }
        }
        ConnectionStatus(state.connection, state.tonieCount)
    }
}

@Composable
private fun DisplaySection(showTitles: Boolean, onShowTitlesChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = showTitles, role = Role.Switch, onValueChange = onShowTitlesChange),
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.show_titles), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.settings_show_titles_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = TeddyColors.Muted,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = showTitles, onCheckedChange = null)
    }
}

@Composable
private fun NotificationSection(onAllow: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.settings_notifications), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.settings_notifications_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = TeddyColors.Muted,
            )
        }
        Spacer(Modifier.width(12.dp))
        OutlinedButton(onClick = onAllow) { Text(stringResource(R.string.settings_notifications_allow)) }
    }
}

@Composable
private fun ConnectionStatus(connection: ConnectionTest, tonieCount: Int) {
    val (text, color) = when (connection) {
        ConnectionTest.Idle -> pluralStringResource(R.plurals.settings_stories_in_app, tonieCount, tonieCount) to TeddyColors.Muted
        ConnectionTest.Testing -> stringResource(R.string.settings_connecting) to TeddyColors.Muted
        is ConnectionTest.Success -> pluralStringResource(R.plurals.connection_success, connection.tonieCount, connection.tonieCount) to TeddyColors.Success
        ConnectionTest.Failed -> stringResource(R.string.settings_connection_failed) to TeddyColors.Primary
        ConnectionTest.InvalidUrl -> stringResource(R.string.connection_invalid_url) to TeddyColors.Primary
    }
    Text(text, color = color, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun StorageSection(state: ParentSettingsState, onIntent: (ParentSettingsIntent) -> Unit) {
    val completedCount = state.downloads.count { it.status == DownloadStatus.Completed }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.settings_saved_offline), style = MaterialTheme.typography.titleMedium)
            Text(
                pluralStringResource(R.plurals.settings_downloads_summary, completedCount, completedCount, formatBytes(state.usedBytes)),
                style = MaterialTheme.typography.bodyMedium,
                color = TeddyColors.Muted,
            )
        }
        OutlinedButton(
            onClick = { onIntent(ParentSettingsIntent.DeleteAllRequested) },
            enabled = state.downloads.isNotEmpty(),
        ) { Text(stringResource(R.string.settings_delete_all)) }
    }
}

@Composable
private fun DownloadRow(entry: DownloadEntry, onDelete: () -> Unit) {
    ListItem(
        leadingContent = {
            TonieCover(entry.tonie.id, entry.tonie.coverUrl, contentDescription = null, modifier = Modifier.size(48.dp))
        },
        headlineContent = { Text(entry.tonie.displayName, maxLines = 2) },
        supportingContent = { Text(formatBytes(entry.bytes)) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DownloadBadge(entry.status, isUnavailable = false, size = 28.dp)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.settings_delete_download))
                }
            }
        },
        modifier = Modifier.padding(vertical = 0.dp),
    )
}

@Composable
private fun formatBytes(bytes: Long): String =
    if (LocalInspectionMode.current) "${bytes / 1_000_000} MB"
    else Formatter.formatShortFileSize(LocalContext.current, bytes)

internal class ParentSettingsStateProvider : PreviewParameterProvider<ParentSettingsState> {
    private val downloads = listOf(
        DownloadEntry(SampleData.bobo, DownloadStatus.Completed, 46_000_000),
        DownloadEntry(SampleData.conni, DownloadStatus.Downloading(0.3f), 12_000_000),
        DownloadEntry(SampleData.custom, DownloadStatus.Failed, 1_000_000),
    )
    private val base = ParentSettingsState(
        serverUrlInput = "http://tc",
        savedServerUrl = "http://tc",
        tonieCount = 62,
    )

    override val values = sequenceOf(
        base,
        base.copy(downloads = downloads, usedBytes = 59_000_000),
        base.copy(showTitles = true),
        base.copy(serverUrlInput = "192.168.1.20", connection = ConnectionTest.Testing),
        base.copy(connection = ConnectionTest.Success(62), isRefreshing = true),
        base.copy(connection = ConnectionTest.Failed),
        base.copy(serverUrlInput = "", connection = ConnectionTest.InvalidUrl),
        base.copy(downloads = downloads, usedBytes = 59_000_000, confirmDeleteAll = true),
    )
}

@Preview(name = "Handy", showBackground = true, widthDp = 400, heightDp = 860)
@Preview(name = "Tablet quer", showBackground = true, device = Devices.PIXEL_TABLET)
@Composable
private fun ParentSettingsScreenPreview(
    @PreviewParameter(ParentSettingsStateProvider::class) state: ParentSettingsState,
) = TeddyTheme {
    ParentSettingsScreen(state, onIntent = {})
}

@Preview(name = "Benachrichtigungen fehlen", showBackground = true, widthDp = 400, heightDp = 860)
@Composable
private fun ParentSettingsNotificationsPreview() = TeddyTheme {
    ParentSettingsScreen(
        state = ParentSettingsState(serverUrlInput = "http://tc", savedServerUrl = "http://tc", tonieCount = 62),
        onIntent = {},
        notifications = NotificationPermission(isGranted = false) {},
    )
}
