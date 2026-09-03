package com.sonara.di

import android.app.Application
import android.content.Context
import com.sonara.auth.AnonymousAuthProvider
import com.sonara.auth.AuthAccount
import com.sonara.auth.AuthManager
import com.sonara.auth.AuthMethod
import com.sonara.auth.GoogleAuthProvider
import com.sonara.data.AuthMode
import com.sonara.data.SessionRepository
import com.sonara.data.snapshotIn
import com.sonara.SonaraApp
import com.sonara.music.DemoMusicProvider
import com.sonara.music.MusicProvider
import com.sonara.music.MusicRepository
import com.sonara.playback.PlayerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Hand-rolled dependency container. Deliberately not a DI framework — Sonara's
 * graph is small; revisit only if later stages make manual wiring painful.
 */
class AppContainer(app: Application) {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val sessions = SessionRepository(app.applicationContext)

    /** Lazily connects to the playback session on first UI access. */
    val playerConnection: PlayerConnection by lazy {
        PlayerConnection(app.applicationContext, applicationScope)
    }

    val library by lazy { com.sonara.data.LibraryRepository() }

    val settings by lazy {
        com.sonara.data.SettingsRepository(app.applicationContext, applicationScope)
    }

    /** Settings snapshot the UI and engine can read synchronously. */
    val settingsSnapshot by lazy { settings.snapshotIn(applicationScope) }

    /**
     * The active music data provider. Real YouTube Music catalog.
     *
     * Two transports inside:
     *  - innertube (anonymous) for home/search/playback — these endpoints
     *    reject a Google OAuth Bearer token (HTTP 400);
     *  - YouTube Data API v3 (authenticated) for the user's own playlists,
     *    which *does* accept the OAuth token.
     */
    val musicProvider: MusicProvider by lazy {
        // innertube stays anonymous; personal library uses the Data API.
        val anonymousToken: suspend () -> String? = { null }
        val dataApiToken: suspend () -> String? = {
            pickedGoogleAccountName?.let { accountName ->
                val ctx = app.applicationContext
                runCatching {
                    val account = com.sonara.auth.GoogleAccountTokenSource.accountFromName(ctx, accountName)
                        ?: return@runCatching null
                    com.sonara.auth.GoogleAccountTokenSource.accessToken(ctx, account)
                }.getOrNull()?.also {
                    android.util.Log.d("AppContainer", "Data API token obtained: ${it.take(10)}...")
                }
            }
        }
        val dataClient = com.sonara.provider.youtube.YouTubeDataClient(dataApiToken)
        com.sonara.provider.youtube.YouTubeMusicProvider(
            authTokenProvider = anonymousToken,
            dataClient = dataClient,
        )
    }

    /** The Google account the user picked for YouTube Music library access. */
    @Volatile
    var pickedGoogleAccountName: String? = null

    /** Records the account chosen in the system account picker and persists it. */
    fun onAccountPicked(name: String?) {
        pickedGoogleAccountName = name
        applicationScope.launch { sessions.setLibraryAccount(name.orEmpty()) }
    }

    /** Caching layer between the UI and [musicProvider]. */
    val musicRepository: MusicRepository by lazy { MusicRepository(musicProvider) }

    val ambient by lazy {
        val visual = com.sonara.ambient.AmbientVisualEngine(applicationScope)
        val engine = com.sonara.ambient.AmbientEngine(
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

    /**
     * Authentication. Provider flows never touch DataStore directly; the
     * callbacks below are the single write path for session state.
     */
    val auth: AuthManager = AuthManager(
        scope = applicationScope,
        google = GoogleAuthProvider(app.applicationContext),
        anonymous = AnonymousAuthProvider(),
        googleConfigured = app.getString(com.sonara.R.string.google_web_client_id).isNotBlank() ||
            com.sonara.BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank(),
        onSessionEstablished = { account, method ->
            sessions.completeOnboarding(
                mode = method.toAuthMode(),
                accountId = account.id,
                accountName = account.displayName,
            )
            // Wire the same Google account for YouTube Music library access
            // so no separate account picker is needed.
            if (method == AuthMethod.GOOGLE) {
                val email = account.email
                if (!email.isNullOrBlank()) {
                    onAccountPicked(email)
                }
            }
        },
        onSignedOut = { sessions.clearSession() },
    )

    /** Restores a persisted session into [auth] at launch. */
    suspend fun restoreSession(): Boolean {
        val session = sessions.session.first()
        if (!session.hasSession) return false
        pickedGoogleAccountName = session.libraryAccountName.takeIf { it.isNotBlank() }
        val mode = when (session.authMode) {
            AuthMode.GOOGLE -> AuthMethod.GOOGLE
            else -> AuthMethod.ANONYMOUS
        }
        auth.restore(
            account = AuthAccount(
                id = session.accountId,
                displayName = session.accountName,
                isAnonymous = mode == AuthMethod.ANONYMOUS,
            ),
            method = mode,
        )
        return true
    }

    private companion object {
        fun AuthMethod.toAuthMode(): AuthMode = when (this) {
            AuthMethod.GOOGLE -> AuthMode.GOOGLE
            AuthMethod.ANONYMOUS -> AuthMode.ANONYMOUS
        }
    }
}

/** Convenience accessor for Android layers that hold a context. */
val Context.appContainer: AppContainer
    get() = (applicationContext as SonaraApp).container
