package xyz.weilandt.teddyapp

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import org.koin.android.ext.android.inject
import xyz.weilandt.teddyapp.domain.repository.DownloadRepository
import xyz.weilandt.teddyapp.ui.navigation.TeddyNavHost
import xyz.weilandt.teddyapp.ui.theme.TeddyTheme

class MainActivity : ComponentActivity() {

    private val downloads: DownloadRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Phones stay in portrait, tablets may rotate freely
        requestedOrientation = if (resources.configuration.smallestScreenWidthDp < 600) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        }
        enableEdgeToEdge()
        downloads.resumePending()
        setContent {
            TeddyTheme {
                TeddyNavHost()
            }
        }
    }
}
