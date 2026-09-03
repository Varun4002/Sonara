package com.sonara

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.sonara.auth.AuthMethod
import com.sonara.auth.AuthState
import com.sonara.di.appContainer
import com.sonara.ui.onboarding.OnboardingScreen
import com.sonara.ui.shell.SonaraShell
import com.sonara.ui.theme.SonaraTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var sessionRestored by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = appContainer
        splash.setKeepOnScreenCondition { !sessionRestored }
        lifecycleScope.launch {
            container.restoreSession()
            sessionRestored = true
        }

        setContent {
            SonaraTheme {
                val authState by container.auth.state.collectAsState()

                when (authState) {
                    is AuthState.SignedOut, is AuthState.Error -> OnboardingScreen(
                        engine = container.ambient.engine,
                        authState = authState,
                        googleConfigured = container.auth.googleConfigured,
                        onSignInGoogle = { container.auth.signIn(AuthMethod.GOOGLE) },
                    )

                    else -> SonaraShell(
                        player = container.playerConnection,
                        library = container.library,
                        ambient = container.ambient,
                        musicRepo = container.musicRepository,
                    )
                }
            }
        }
    }
}
