package xyz.weilandt.teddyapp.playback

import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import xyz.weilandt.teddyapp.domain.model.Chapters

/**
 * A tonie is a single audio file with chapter marks. This player turns
 * "next/previous track" into a chapter jump – in the app, in the notification
 * and on the lock screen.
 */
@OptIn(UnstableApi::class)
class ChapterPlayer(player: Player) : ForwardingPlayer(player) {

    private val chapterStarts: List<Long>
        get() = currentMediaItem?.mediaMetadata?.extras
            ?.getLongArray(KEY_CHAPTERS)?.toList()
            .orEmpty()

    override fun getAvailableCommands(): Player.Commands =
        super.getAvailableCommands().buildUpon()
            .addAll(*CHAPTER_COMMANDS)
            .build()

    override fun isCommandAvailable(command: Int): Boolean =
        command in CHAPTER_COMMANDS || super.isCommandAvailable(command)

    override fun hasNextMediaItem(): Boolean = Chapters.nextStart(chapterStarts, currentPosition) != null

    override fun hasPreviousMediaItem(): Boolean = true

    override fun seekToNext() = seekToNextChapter()

    override fun seekToNextMediaItem() = seekToNextChapter()

    override fun seekToPrevious() = seekToPreviousChapter()

    override fun seekToPreviousMediaItem() = seekToPreviousChapter()

    private fun seekToNextChapter() {
        Chapters.nextStart(chapterStarts, currentPosition)?.let(::seekTo)
    }

    private fun seekToPreviousChapter() {
        seekTo(Chapters.previousTarget(chapterStarts, currentPosition))
    }

    companion object {
        const val KEY_CHAPTERS = "chapters"

        private val CHAPTER_COMMANDS = intArrayOf(
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
        )
    }
}
