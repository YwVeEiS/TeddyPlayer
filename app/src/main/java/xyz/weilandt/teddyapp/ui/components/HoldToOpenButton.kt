package xyz.weilandt.teddyapp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import xyz.weilandt.teddyapp.ui.theme.TeddyColors
import xyz.weilandt.teddyapp.ui.theme.TeddyTheme

const val HOLD_DURATION_MS = 3_000

/**
 * Unauffälliges Zahnrad für Eltern: löst erst nach [HOLD_DURATION_MS] Gedrückthalten aus.
 * Ein Ring zeigt den Fortschritt. Kurzes Tippen bewirkt nichts.
 */
@Composable
fun HoldToOpenButton(
    onTriggered: () -> Unit,
    modifier: Modifier = Modifier,
    initialProgress: Float = 0f,
) {
    val progress = remember { Animatable(initialProgress) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val currentOnTriggered = rememberUpdatedState(onTriggered)

    Box(
        modifier = modifier
            .size(56.dp)
            .semantics {
                role = Role.Button
                contentDescription = "Elternbereich (gedrückt halten)"
                onClick { currentOnTriggered.value(); true }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    val animation = scope.launch {
                        progress.animateTo(1f, tween(HOLD_DURATION_MS, easing = LinearEasing))
                    }
                    val released = withTimeoutOrNull(HOLD_DURATION_MS.toLong()) { waitForUpOrCancellation() }
                    animation.cancel()
                    if (released == null) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentOnTriggered.value()
                        // Finger noch auf dem Display – auf Loslassen warten
                        waitForUpOrCancellation()
                    }
                    scope.launch { progress.snapTo(0f) }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (progress.value > 0f) {
            CircularProgressIndicator(
                progress = { progress.value },
                modifier = Modifier.fillMaxSize(),
                color = TeddyColors.Primary,
                strokeWidth = 3.dp,
            )
        }
        Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = null,
            tint = TeddyColors.Muted.copy(alpha = 0.6f),
            modifier = Modifier.size(28.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HoldToOpenButtonIdlePreview() = TeddyTheme {
    HoldToOpenButton(onTriggered = {}, modifier = Modifier.padding(8.dp))
}

@Preview(showBackground = true)
@Composable
private fun HoldToOpenButtonHoldingPreview() = TeddyTheme {
    HoldToOpenButton(onTriggered = {}, modifier = Modifier.padding(8.dp), initialProgress = 0.6f)
}
