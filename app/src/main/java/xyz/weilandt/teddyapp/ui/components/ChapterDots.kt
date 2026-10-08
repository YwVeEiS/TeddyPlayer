package xyz.weilandt.teddyapp.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.weilandt.teddyapp.ui.theme.TeddyColors
import xyz.weilandt.teddyapp.ui.theme.TeddyTheme

/**
 * Fortschritt als Perlenkette: jedes Kapitel ein Punkt. Gehörte Kapitel sind voll,
 * das aktuelle ist größer und füllt sich von links nach rechts.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChapterDots(
    chapterCount: Int,
    currentChapter: Int,
    progressInChapter: Float,
    onChapterClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(chapterCount) { index ->
            val isCurrent = index == currentChapter
            val size by animateDpAsState(if (isCurrent) 30.dp else 20.dp, label = "dotSize")
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(
                        if (index < currentChapter) TeddyColors.Primary else TeddyColors.Muted.copy(alpha = 0.35f)
                    )
                    .border(2.dp, if (isCurrent) TeddyColors.Primary else TeddyColors.Muted.copy(alpha = 0f), CircleShape)
                    .clickable { onChapterClick(index) }
                    .semantics { contentDescription = "Kapitel ${index + 1}" },
            ) {
                if (isCurrent) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressInChapter.coerceIn(0f, 1f))
                            .background(TeddyColors.Primary)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChapterDotsStartPreview() = TeddyTheme {
    ChapterDots(chapterCount = 8, currentChapter = 0, progressInChapter = 0f, onChapterClick = {}, modifier = Modifier.padding(16.dp))
}

@Preview(showBackground = true)
@Composable
private fun ChapterDotsMiddlePreview() = TeddyTheme {
    ChapterDots(chapterCount = 8, currentChapter = 3, progressInChapter = 0.5f, onChapterClick = {}, modifier = Modifier.padding(16.dp))
}

@Preview(showBackground = true)
@Composable
private fun ChapterDotsManyPreview() = TeddyTheme {
    ChapterDots(chapterCount = 24, currentChapter = 17, progressInChapter = 0.8f, onChapterClick = {}, modifier = Modifier.padding(16.dp))
}

@Preview(showBackground = true)
@Composable
private fun ChapterDotsSinglePreview() = TeddyTheme {
    ChapterDots(chapterCount = 1, currentChapter = 0, progressInChapter = 0.3f, onChapterClick = {}, modifier = Modifier.padding(16.dp))
}
