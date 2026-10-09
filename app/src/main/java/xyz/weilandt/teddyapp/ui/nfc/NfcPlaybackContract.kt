package xyz.weilandt.teddyapp.ui.nfc

/** Short feedback when a figure can't be played. */
enum class NfcFeedback {
    /** The figure is unknown to TeddyCloud. */
    Unknown,

    /** Known, but offline and not downloaded. */
    Unavailable,
}

data class NfcPlaybackState(
    val feedback: NfcFeedback? = null,
)

sealed interface NfcPlaybackIntent {
    /** A figure was held against the device (UID as hex). */
    data class TagDetected(val uidHex: String) : NfcPlaybackIntent
}

sealed interface NfcPlaybackResult {
    data class Feedback(val feedback: NfcFeedback?) : NfcPlaybackResult
}

sealed interface NfcPlaybackEffect {
    data object NavigateToPlayer : NfcPlaybackEffect
    data object Rejected : NfcPlaybackEffect
}

object NfcPlaybackReducer {
    fun reduce(state: NfcPlaybackState, result: NfcPlaybackResult): NfcPlaybackState = when (result) {
        is NfcPlaybackResult.Feedback -> state.copy(feedback = result.feedback)
    }
}
