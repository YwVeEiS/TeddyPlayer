package xyz.weilandt.teddyapp.ui

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import xyz.weilandt.teddyapp.fakes.MainDispatcherRule
import xyz.weilandt.teddyapp.ui.parent.gate.ParentGateEffect
import xyz.weilandt.teddyapp.ui.parent.gate.ParentGateIntent
import xyz.weilandt.teddyapp.ui.parent.gate.ParentGateViewModel
import kotlin.random.Random

class ParentGateViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val vm by lazy { ParentGateViewModel(Random(42)) }

    private fun type(number: Int) = number.toString().forEach {
        vm.onIntent(ParentGateIntent.Digit(it.digitToInt()))
    }

    @Test
    fun `question uses small multiplication table`() {
        val state = vm.state.value
        assertTrue(state.a in 3..9 && state.b in 3..9)
        assertEquals("", state.input)
        assertFalse(state.isWrong)
    }

    @Test
    fun `correct answer unlocks`() = runTest {
        vm.effects.test {
            type(vm.state.value.answer)
            assertEquals(ParentGateEffect.Unlocked, awaitItem())
        }
    }

    @Test
    fun `wrong answer shows hint and new question`() {
        val wrong = vm.state.value.answer + 1
        // gleiche Stellenzahl, damit sofort geprüft wird
        type(if (wrong.toString().length == vm.state.value.answer.toString().length) wrong else vm.state.value.answer - 1)

        val state = vm.state.value
        assertTrue(state.isWrong)
        assertEquals("", state.input)
    }

    @Test
    fun `delete removes last digit`() {
        vm.onIntent(ParentGateIntent.Digit(1))
        vm.onIntent(ParentGateIntent.Delete)
        assertEquals("", vm.state.value.input)
    }

    @Test
    fun `cancel closes gate`() = runTest {
        vm.effects.test {
            vm.onIntent(ParentGateIntent.Cancel)
            assertEquals(ParentGateEffect.Cancelled, awaitItem())
        }
    }
}
