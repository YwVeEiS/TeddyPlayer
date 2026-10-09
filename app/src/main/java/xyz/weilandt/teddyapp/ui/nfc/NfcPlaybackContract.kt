package xyz.weilandt.teddyapp.ui.nfc

/** Kurze Rückmeldung, wenn eine Figur nicht abgespielt werden kann. */
enum class NfcFeedback {
    /** Figur ist TeddyCloud nicht bekannt. */
    Unknown,

    /** Bekannt, aber offline und nicht heruntergeladen. */
    Unavailable,
}

data class NfcPlaybackState(
    val feedback: NfcFeedback? = null,
)

sealed interface NfcPlaybackIntent {
    /** Eine Figur wurde ans Gerät gehalten (UID als Hex). */
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
