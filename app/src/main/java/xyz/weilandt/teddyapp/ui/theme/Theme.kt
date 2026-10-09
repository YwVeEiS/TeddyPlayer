package xyz.weilandt.teddyapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object TeddyColors {
    val Background = Color(0xFFFFF6E9)
    val Surface = Color(0xFFFFFFFF)
    val Primary = Color(0xFFE5483B)
    val OnPrimary = Color(0xFFFFFFFF)
    val Secondary = Color(0xFF2E9CCA)
    val Tertiary = Color(0xFF7BC86C)
    val Ink = Color(0xFF3A2E2A)
    val Muted = Color(0xFFB9ADA4)
    val Success = Color(0xFF3FA34D)
    val Warning = Color(0xFFF2A93B)

    /** Cheerful colors for placeholder covers. */
    val Playful = listOf(
        Color(0xFFE5483B), Color(0xFFF2A93B), Color(0xFFF7D44C), Color(0xFF7BC86C),
        Color(0xFF2E9CCA), Color(0xFF8E6CD8), Color(0xFFE86BA8), Color(0xFF3FB8AF),
    )
}

private val ColorScheme = lightColorScheme(
    primary = TeddyColors.Primary,
    onPrimary = TeddyColors.OnPrimary,
    secondary = TeddyColors.Secondary,
    tertiary = TeddyColors.Tertiary,
    background = TeddyColors.Background,
    onBackground = TeddyColors.Ink,
    surface = TeddyColors.Surface,
    onSurface = TeddyColors.Ink,
)

private val TeddyShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
)

/** Deliberately always light: a kids' app with bold, easily recognizable colors. */
@Composable
fun TeddyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        shapes = TeddyShapes,
        content = content,
    )
}
