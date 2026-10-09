package xyz.weilandt.teddyapp.ui.navigation

import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import xyz.weilandt.teddyapp.ui.library.LibraryRoute
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
