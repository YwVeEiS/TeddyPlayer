package xyz.weilandt.teddyapp.ui.navigation

import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import xyz.weilandt.teddyapp.ui.library.LibraryRoute
import xyz.weilandt.teddyapp.ui.nfc.NfcFeedbackOverlay
import xyz.weilandt.teddyapp.ui.nfc.NfcPlaybackEffect
import xyz.weilandt.teddyapp.ui.nfc.NfcPlaybackViewModel
import xyz.weilandt.teddyapp.ui.parent.gate.ParentGateRoute
import xyz.weilandt.teddyapp.ui.parent.settings.ParentSettingsRoute
import xyz.weilandt.teddyapp.ui.player.PlayerRoute

@Serializable data object LibraryDestination
@Serializable data object PlayerDestination
@Serializable data object ParentGateDestination
@Serializable data object ParentSettingsDestination

@Composable
fun TeddyNavHost() {
    val navController = rememberNavController()
    val nfcViewModel: NfcPlaybackViewModel = koinViewModel()
    val nfcState by nfcViewModel.state.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    // Figure detected → go to the player from any screen
    LaunchedEffect(nfcViewModel) {
        nfcViewModel.effects.collect { effect ->
            when (effect) {
                NfcPlaybackEffect.NavigateToPlayer -> {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    navController.navigate(PlayerDestination) { launchSingleTop = true }
                }
                NfcPlaybackEffect.Rejected -> haptics.performHapticFeedback(HapticFeedbackType.Reject)
            }
        }
    }

    Box {
        TeddyDestinations(navController)
        NfcFeedbackOverlay(nfcState.feedback)
    }
}

@Composable
private fun TeddyDestinations(navController: NavHostController) {
    NavHost(navController = navController, startDestination = LibraryDestination) {
        composable<LibraryDestination> {
            LibraryRoute(
                onOpenPlayer = { navController.navigate(PlayerDestination) { launchSingleTop = true } },
                onOpenParentGate = { navController.navigate(ParentGateDestination) { launchSingleTop = true } },
            )
        }
        composable<PlayerDestination>(
            enterTransition = { slideInVertically { it } },
            exitTransition = { slideOutVertically { it } },
        ) {
            PlayerRoute(onBack = { navController.popBackStack() })
        }
        composable<ParentGateDestination> {
            ParentGateRoute(
                onUnlocked = {
                    navController.navigate(ParentSettingsDestination) {
                        popUpTo(ParentGateDestination) { inclusive = true }
                    }
                },
                onCancel = { navController.popBackStack() },
            )
        }
        composable<ParentSettingsDestination> {
            ParentSettingsRoute(onBack = { navController.popBackStack() })
        }
    }
}
