package com.sonara

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sonara.di.appContainer
import com.sonara.ui.shell.SonaraShell
import com.sonara.ui.theme.SonaraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = appContainer
        val player = container.playerConnection
        setContent {
            SonaraTheme {
                SonaraShell(
                    player = player,
                    library = container.library,
                    ambient = container.ambient,
                )
            }
        }
    }
}
