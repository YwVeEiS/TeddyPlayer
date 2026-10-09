package xyz.weilandt.teddyapp.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.weilandt.teddyapp.ui.theme.TeddyColors
import xyz.weilandt.teddyapp.ui.theme.TeddyTheme

/** Large round button with an icon and haptic feedback – for small fingers. */
@Composable
fun BigIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
    containerColor: Color = TeddyColors.Primary,
    contentColor: Color = Color.White,
    enabled: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    FilledIconButton(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        enabled = enabled,
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = TeddyColors.Muted.copy(alpha = 0.4f),
            disabledContentColor = Color.White,
        ),
        modifier = modifier
            .size(size)
            .shadow(if (enabled) 6.dp else 0.dp, CircleShape),
    ) {
        Icon(icon, contentDescription, Modifier.size(size * 0.6f))
    }
}

@Preview(showBackground = true)
@Composable
private fun BigIconButtonEnabledPreview() = TeddyTheme {
    Row {
        BigIconButton(Icons.Rounded.PlayArrow, "Abspielen", {}, Modifier.padding(8.dp), size = 120.dp)
        BigIconButton(Icons.Rounded.Pause, "Pause", {}, Modifier.padding(8.dp))
        BigIconButton(
            Icons.Rounded.Home, "Zurück", {}, Modifier.padding(8.dp),
            containerColor = TeddyColors.Secondary,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BigIconButtonDisabledPreview() = TeddyTheme {
    BigIconButton(Icons.Rounded.SkipNext, "Weiter", {}, Modifier.padding(8.dp), enabled = false)
}
