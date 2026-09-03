package com.sonara.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionStore by preferencesDataStore(name = "sonara_session")

/** How the current session was established. */
enum class AuthMode { NONE, GOOGLE, ANONYMOUS }

/**
 * Application-level session state, persisted with DataStore. Holds only
 * non-sensitive identifiers — never tokens or credentials (those belong to
 * the credential system, if a provider needs them).
 */
data class Session(
    val onboardingCompleted: Boolean = false,
    val authMode: AuthMode = AuthMode.NONE,
    val accountId: String = "",
    val accountName: String = "",
    val libraryAccountName: String = "",
) {
    val hasSession: Boolean get() = onboardingCompleted
}

class SessionRepository(context: Context) {

    private val store = context.applicationContext.sessionStore

    val session: Flow<Session> = store.data.map { prefs ->
        Session(
            onboardingCompleted = prefs[KEY_ONBOARDING] == "true",
            authMode = prefs[KEY_AUTH_MODE]?.let { runCatching { AuthMode.valueOf(it) }.getOrNull() }
                ?: AuthMode.NONE,
            accountId = prefs[KEY_ACCOUNT_ID].orEmpty(),
            accountName = prefs[KEY_ACCOUNT_NAME].orEmpty(),
            libraryAccountName = prefs[KEY_LIBRARY_ACCOUNT].orEmpty(),
        )
    }

    /** Remembers the Google account chosen for YouTube Music library access. */
    suspend fun setLibraryAccount(name: String) {
        store.edit { prefs -> prefs[KEY_LIBRARY_ACCOUNT] = name }
    }

    suspend fun completeOnboarding(mode: AuthMode, accountId: String, accountName: String) {
        store.edit { prefs ->
            prefs[KEY_ONBOARDING] = "true"
            prefs[KEY_AUTH_MODE] = mode.name
            prefs[KEY_ACCOUNT_ID] = accountId
            prefs[KEY_ACCOUNT_NAME] = accountName
        }
    }

    /** Clears the session; unrelated preferences (settings) are preserved. */
    suspend fun clearSession() {
        store.edit { prefs ->
            prefs[KEY_ONBOARDING] = "false"
            prefs[KEY_AUTH_MODE] = AuthMode.NONE.name
            prefs.remove(KEY_ACCOUNT_ID)
            prefs.remove(KEY_ACCOUNT_NAME)
        }
    }

    private companion object {
        val KEY_ONBOARDING = stringPreferencesKey("onboarding_completed")
        val KEY_AUTH_MODE = stringPreferencesKey("auth_mode")
        val KEY_ACCOUNT_ID = stringPreferencesKey("account_id")
        val KEY_ACCOUNT_NAME = stringPreferencesKey("account_name")
        val KEY_LIBRARY_ACCOUNT = stringPreferencesKey("library_account_name")
    }
}
