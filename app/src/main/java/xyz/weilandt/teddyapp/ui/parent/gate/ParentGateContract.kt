package xyz.weilandt.teddyapp.ui.parent.gate

data class ParentGateState(
    val a: Int = 7,
    val b: Int = 8,
    val input: String = "",
    val isWrong: Boolean = false,
) {
    val answer: Int get() = a * b
    val isComplete: Boolean get() = input.length >= answer.toString().length
}

sealed interface ParentGateIntent {
    data class Digit(val digit: Int) : ParentGateIntent
    data object Delete : ParentGateIntent
    data object Cancel : ParentGateIntent
}

sealed interface ParentGateResult {
    data class DigitAdded(val digit: Int) : ParentGateResult
    data object DigitRemoved : ParentGateResult
    data class NewQuestion(val a: Int, val b: Int, val afterWrongAnswer: Boolean) : ParentGateResult
}

sealed interface ParentGateEffect {
    data object Unlocked : ParentGateEffect
    data object Cancelled : ParentGateEffect
}

object ParentGateReducer {
    fun reduce(state: ParentGateState, result: ParentGateResult): ParentGateState = when (result) {
        is ParentGateResult.DigitAdded ->
            if (state.isComplete) state else state.copy(input = state.input + result.digit, isWrong = false)
        ParentGateResult.DigitRemoved -> state.copy(input = state.input.dropLast(1))
        is ParentGateResult.NewQuestion -> ParentGateState(
            a = result.a,
            b = result.b,
            input = "",
            isWrong = result.afterWrongAnswer,
        )
    }
}
