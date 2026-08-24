package com.sonara.di

import android.app.Application
import android.content.Context
import com.sonara.SonaraApp
import com.sonara.playback.PlayerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Hand-rolled dependency container. Deliberately not a DI framework — Sonara's
 * graph is small; revisit only if later stages make manual wiring painful.
 */
class AppContainer(app: Application) {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Lazily connects to the playback session on first UI access. */
    val playerConnection: PlayerConnection by lazy {
        PlayerConnection(app.applicationContext, applicationScope)
    }
}

/** Convenience accessor for Android layers that hold a context. */
val Context.appContainer: AppContainer
    get() = (applicationContext as SonaraApp).container
