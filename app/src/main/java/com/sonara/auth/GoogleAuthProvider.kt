package com.sonara.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.sonara.BuildConfig
import com.sonara.R

/**
 * Google sign-in via Credential Manager.
 *
 * IMPORTANT LIMITATION, stated plainly: Google authentication only grants
 * Sonara a verified identity. It does NOT by itself grant access to the
 * user's YouTube Music library, playlists, likes or history — that requires
 * separate, scoped OAuth authorization against the YouTube Data API, which
 * is not part of this stage. This provider authenticates; it does not sync.
 *
 * Configuration: requires an OAuth 2.0 web client ID from Google Cloud Console,
 * registered for the debug SHA-1 fingerprint and package `com.sonara.debug`.
 * Set via:
 *   - `R.string.google_web_client_id` (resource override), OR
 *   - `BuildConfig.GOOGLE_WEB_CLIENT_ID` (gradle.properties fallback)
 *
 * Without one of these the flow reports an honest configuration error.
 */
class GoogleAuthProvider(context: Context) : AuthProvider {

    private val appContext = context.applicationContext
    private val credentialManager = CredentialManager.create(appContext)

    override val method: AuthMethod = AuthMethod.GOOGLE

    override suspend fun signIn(): AuthAccount? {
        val clientId = resolveClientId()
        if (clientId.isEmpty()) {
            throw AuthException(
                "Google sign-in isn't configured. " +
                    "Set sonara.google.web.client.id in gradle.properties " +
                    "and rebuild.",
            )
        }

        val option = GetGoogleIdOption.Builder()
            .setServerClientId(clientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        val response = try {
            credentialManager.getCredential(appContext, request)
        } catch (e: GetCredentialCancellationException) {
            return null // user cancelled — not an error
        } catch (e: Exception) {
            Log.e(TAG, "Credential Manager error", e)
            throw AuthException("Couldn't connect your account.", e)
        }

        val credential = response.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val google = GoogleIdTokenCredential.createFrom(credential.data)
            return AuthAccount(
                id = google.id,
                displayName = google.displayName.orEmpty().ifEmpty { google.id },
                isAnonymous = false,
                email = parseEmailFromIdToken(google.idToken),
            )
        }
        throw AuthException("Couldn't connect your account.")
    }

    override suspend fun signOut() {
        runCatching {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        }
    }

    /**
     * Resolves the web client ID from string resource (primary) or
     * BuildConfig (gradle.properties fallback).
     */
    private fun resolveClientId(): String {
        // 1. Try the string resource (set via resource override).
        val fromResource = try {
            appContext.getString(R.string.google_web_client_id).trim()
        } catch (_: Exception) { "" }
        if (fromResource.isNotEmpty()) return fromResource

        // 2. Fall back to BuildConfig (gradle.properties).
        val fromBuildConfig = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
        if (fromBuildConfig.isNotEmpty()) return fromBuildConfig

        return ""
    }

    companion object {
        private const val TAG = "GoogleAuthProvider"

        /** Extracts the email claim from a Google ID token (JWT) payload. */
        fun parseEmailFromIdToken(token: String?): String? {
            if (token == null) return null
            return runCatching {
                val payload = token.split(".")[1]
                val decoded = android.util.Base64.decode(payload, android.util.Base64.URL_SAFE)
                val json = String(decoded, Charsets.UTF_8)
                // Extract "email" value with simple string search — avoids
                // adding a JSON dependency for one field.
                val key = "\"email\""
                val idx = json.indexOf(key)
                if (idx < 0) return@runCatching null
                val start = json.indexOf(':', idx + key.length) + 1
                val quoteStart = json.indexOf('"', start)
                val quoteEnd = json.indexOf('"', quoteStart + 1)
                json.substring(quoteStart + 1, quoteEnd).takeIf { it.contains('@') }
            }.getOrNull()
        }
    }
}
