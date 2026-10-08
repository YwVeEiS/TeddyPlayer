package xyz.weilandt.teddyapp.core.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Basis für alle Screens nach dem MVI-Muster:
 *
 * Intent (UI) → [onIntent] → Seiteneffekte / Daten → Result → [reduce] → neuer State
 *
 * [reduce] ist eine reine Funktion und damit ohne Android isoliert testbar.
 * Einmalige Ereignisse (Navigation, Wackeln, …) laufen über [effects].
 */
abstract class MviViewModel<State, Intent, Result, Effect>(initialState: State) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _effects = Channel<Effect>(Channel.BUFFERED)
    val effects: Flow<Effect> = _effects.receiveAsFlow()

    abstract fun onIntent(intent: Intent)

    protected abstract fun reduce(state: State, result: Result): State

    protected fun dispatch(result: Result) {
        _state.update { reduce(it, result) }
    }

    protected fun emit(effect: Effect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
