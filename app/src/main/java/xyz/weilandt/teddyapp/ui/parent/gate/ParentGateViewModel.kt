package xyz.weilandt.teddyapp.ui.parent.gate

import xyz.weilandt.teddyapp.core.mvi.MviViewModel
import kotlin.random.Random

/** Einfache Rechenaufgabe (kleines Einmaleins), die Kleinkinder nicht lösen können. */
class ParentGateViewModel(
    private val random: Random = Random.Default,
) : MviViewModel<ParentGateState, ParentGateIntent, ParentGateResult, ParentGateEffect>(ParentGateState()) {

    init {
        dispatch(newQuestion(afterWrongAnswer = false))
    }

    override fun onIntent(intent: ParentGateIntent) {
        when (intent) {
            is ParentGateIntent.Digit -> {
                dispatch(ParentGateResult.DigitAdded(intent.digit))
                checkAnswer()
            }
            ParentGateIntent.Delete -> dispatch(ParentGateResult.DigitRemoved)
            ParentGateIntent.Cancel -> emit(ParentGateEffect.Cancelled)
        }
    }

    private fun checkAnswer() {
        val current = state.value
        if (!current.isComplete) return
        if (current.input.toIntOrNull() == current.answer) {
            emit(ParentGateEffect.Unlocked)
        } else {
            dispatch(newQuestion(afterWrongAnswer = true))
        }
    }

    private fun newQuestion(afterWrongAnswer: Boolean) =
        ParentGateResult.NewQuestion(random.nextInt(3, 10), random.nextInt(3, 10), afterWrongAnswer)

    override fun reduce(state: ParentGateState, result: ParentGateResult): ParentGateState =
        ParentGateReducer.reduce(state, result)
}
