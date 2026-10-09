package xyz.weilandt.teddyapp.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

/** State of the notification permission, needed for the download progress notification. */
@Stable
class NotificationPermission(isGranted: Boolean, private val onRequest: () -> Unit) {
    var isGranted by mutableStateOf(isGranted)
        internal set

    fun request() = onRequest()
}

/**
 * Notification permission (Android 13+), `null` on older versions where none is needed.
 *
 * With [openSettingsWhenBlocked], a denial the system no longer asks about (denied twice) opens the
 * app's notification settings instead – only for an explicit button, not for the automatic request.
 */
@Composable
fun rememberNotificationPermission(openSettingsWhenBlocked: Boolean = false): NotificationPermission? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    val permission = Manifest.permission.POST_NOTIFICATIONS
    val activity = LocalActivity.current ?: return null
    fun isGranted() = ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED

    lateinit var state: NotificationPermission
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        state.isGranted = granted
        if (!granted && openSettingsWhenBlocked && !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            activity.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
            )
        }
    }
    state = remember { NotificationPermission(isGranted()) { launcher.launch(permission) } }

    // Pick up changes made in the system settings
    LifecycleResumeEffect(state) {
        state.isGranted = isGranted()
        onPauseOrDispose {}
    }
    return state
}
