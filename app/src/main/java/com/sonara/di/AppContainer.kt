package com.sonara.di

import android.app.Application
import android.content.Context
import com.sonara.SonaraApp
import com.sonara.ambient.AmbientEngine
import com.sonara.ambient.AmbientVisualEngine
import com.sonara.data.LibraryRepository
import com.sonara.data.SettingsRepository
import com.sonara.data.snapshotIn
import com.sonara.playback.PlayerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

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

    val library: LibraryRepository by lazy { LibraryRepository() }

    val settings: SettingsRepository by lazy {
        SettingsRepository(app.applicationContext, applicationScope)
    }

    /** Settings snapshot the UI and engine can read synchronously. */
    val settingsSnapshot by lazy { settings.snapshotIn(applicationScope) }

    /**
     * The visual engine. Observes playback and settings once created; the UI
     * reads its state. Kept separate from playback by design.
     */
    val ambient: AmbientEngine by lazy {
        val visual = AmbientVisualEngine(applicationScope)
        val engine = AmbientEngine(
            engine = visual,
            dynamicColorsEnabled = { settingsSnapshot.value.dynamicColors },
        )
        playerConnection.state
            .onEach { engine.onPlaybackState(it); library.recordPlay(it, System.currentTimeMillis()) }
            .launchIn(applicationScope)
        settingsSnapshot
            .onEach { engine.setMode(it.ambientMode) }
            .launchIn(applicationScope)
        engine
    }
}

/** Convenience accessor for Android layers that hold a context. */
val Context.appContainer: AppContainer
    get() = (applicationContext as SonaraApp).container
