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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.weilandt.teddyapp.R
import xyz.weilandt.teddyapp.ui.theme.TeddyColors
import xyz.weilandt.teddyapp.ui.theme.TeddyTheme

/**
 * Progress as a string of beads: one dot per chapter. Played chapters are filled,
 * the current one is larger and fills from left to right.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChapterDots(
    chapterCount: Int,
    currentChapter: Int,
    progressInChapter: Float,
    onChapterClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    dotSize: Dp = 20.dp,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(chapterCount) { index ->
            val isCurrent = index == currentChapter
            val label = stringResource(R.string.chapter_number, index + 1)
            val size by animateDpAsState(if (isCurrent) dotSize * 1.5f else dotSize, label = "dotSize")
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(
                        if (index < currentChapter) TeddyColors.Primary else TeddyColors.Muted.copy(alpha = 0.35f)
                    )
                    .border(2.dp, if (isCurrent) TeddyColors.Primary else TeddyColors.Muted.copy(alpha = 0f), CircleShape)
                    .clickable { onChapterClick(index) }
                    .semantics { contentDescription = label },
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

@Preview(showBackground = true, widthDp = 600)
@Composable
private fun ChapterDotsLargePreview() = TeddyTheme {
    ChapterDots(chapterCount = 8, currentChapter = 2, progressInChapter = 0.6f, onChapterClick = {}, modifier = Modifier.padding(16.dp), dotSize = 30.dp)
}
