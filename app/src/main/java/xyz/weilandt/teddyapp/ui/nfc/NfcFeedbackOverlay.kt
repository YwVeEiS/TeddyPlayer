package xyz.weilandt.teddyapp.ui.nfc

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.weilandt.teddyapp.ui.theme.TeddyColors
import xyz.weilandt.teddyapp.ui.theme.TeddyTheme

/** Großes Symbol in der Bildschirmmitte, wenn eine Figur nicht abgespielt werden kann. */
@Composable
fun NfcFeedbackOverlay(feedback: NfcFeedback?, modifier: Modifier = Modifier) {
    // Letztes Symbol merken, damit es beim Ausblenden nicht verschwindet
    var shown by remember { mutableStateOf(feedback) }
    if (feedback != null) shown = feedback

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = feedback != null,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
        ) {
            NfcFeedbackBadge(shown ?: NfcFeedback.Unknown)
        }
    }
}

@Composable
private fun NfcFeedbackBadge(feedback: NfcFeedback) {
    val (icon, color) = when (feedback) {
        NfcFeedback.Unknown -> Icons.Rounded.QuestionMark to TeddyColors.Secondary
        NfcFeedback.Unavailable -> Icons.Rounded.CloudOff to TeddyColors.Muted
    }
    Surface(
        shape = CircleShape,
        color = color,
        shadowElevation = 12.dp,
        modifier = Modifier.size(180.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = when (feedback) {
                NfcFeedback.Unknown -> "Figur unbekannt"
                NfcFeedback.Unavailable -> "Gerade nicht verfügbar"
            },
            tint = Color.White,
            modifier = Modifier.padding(36.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 400)
@Composable
private fun NfcFeedbackUnknownPreview() = TeddyTheme {
    NfcFeedbackOverlay(NfcFeedback.Unknown)
}

@Preview(showBackground = true, widthDp = 400, heightDp = 400)
@Composable
private fun NfcFeedbackUnavailablePreview() = TeddyTheme {
    NfcFeedbackOverlay(NfcFeedback.Unavailable)
}

@Preview(showBackground = true, widthDp = 400, heightDp = 400)
@Composable
private fun NfcFeedbackHiddenPreview() = TeddyTheme {
    NfcFeedbackOverlay(null)
}
