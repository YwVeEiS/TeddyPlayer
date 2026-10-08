package xyz.weilandt.teddyapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.EmojiNature
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Sailing
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import xyz.weilandt.teddyapp.ui.theme.TeddyColors
import xyz.weilandt.teddyapp.ui.theme.TeddyTheme
import kotlin.math.absoluteValue

/** Cover eines Tonies; ohne Bild gibt es ein buntes, wiedererkennbares Symbol. */
@Composable
fun TonieCover(
    tonieId: String,
    coverUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    grayscale: Boolean = false,
) {
    if (coverUrl == null || LocalInspectionMode.current) {
        PlaceholderCover(tonieId, modifier, grayscale)
        return
    }
    SubcomposeAsyncImage(
        model = coverUrl,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        colorFilter = if (grayscale) GrayscaleFilter else null,
        modifier = modifier,
        loading = { PlaceholderCover(tonieId, Modifier.fillMaxSize(), grayscale) },
        error = { PlaceholderCover(tonieId, Modifier.fillMaxSize(), grayscale) },
    )
}

private val GrayscaleFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

private val placeholderIcons = listOf(
    Icons.Rounded.Pets, Icons.Rounded.Star, Icons.Rounded.RocketLaunch, Icons.Rounded.Favorite,
    Icons.Rounded.LocalFlorist, Icons.Rounded.WbSunny, Icons.Rounded.DirectionsCar,
    Icons.Rounded.Sailing, Icons.Rounded.MusicNote, Icons.Rounded.Cake, Icons.Rounded.EmojiNature,
)

@Composable
fun PlaceholderCover(seed: String, modifier: Modifier = Modifier, grayscale: Boolean = false) {
    val hash = seed.hashCode().absoluteValue
    val color = if (grayscale) TeddyColors.Muted else TeddyColors.Playful[hash % TeddyColors.Playful.size]
    val icon = placeholderIcons[(hash / TeddyColors.Playful.size) % placeholderIcons.size]
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(color.copy(alpha = 0.85f), shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.fillMaxSize(0.55f),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderCoverPreview() = TeddyTheme {
    PlaceholderCover("bobo", Modifier.size(120.dp))
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderCoverOtherSeedPreview() = TeddyTheme {
    PlaceholderCover("apfelkuchen", Modifier.size(120.dp))
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderCoverGrayscalePreview() = TeddyTheme {
    PlaceholderCover("bobo", Modifier.size(120.dp), grayscale = true)
}

@Preview(showBackground = true)
@Composable
private fun TonieCoverWithoutUrlPreview() = TeddyTheme {
    TonieCover("pikachu", coverUrl = null, contentDescription = null, modifier = Modifier.fillMaxWidth())
}
