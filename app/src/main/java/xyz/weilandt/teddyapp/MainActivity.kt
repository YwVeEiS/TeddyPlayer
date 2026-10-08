package xyz.weilandt.teddyapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import xyz.weilandt.teddyapp.ui.navigation.TeddyNavHost
import xyz.weilandt.teddyapp.ui.theme.TeddyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TeddyTheme {
                TeddyNavHost()
            }
        }
    }
}
